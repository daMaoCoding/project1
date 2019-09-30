package com.xinbo.fundstransfer.assign;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.entity.InAccount;
import com.xinbo.fundstransfer.assign.entity.OutAccount;
import com.xinbo.fundstransfer.assign.entity.OutOnly;
import com.xinbo.fundstransfer.assign.entity.ReserveAccount;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AssignBalance {
	@Autowired
	private RedisService redisService;
	@Autowired
	private SystemAccountManager systemAccountManager;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AllocateOutwardTaskService allOTaskSer;
	@Autowired
	private AccountFactory accountFactory;
	@Autowired
	private AllocateIncomeAccountService alloIAcntSer;
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private AvailableCardCache availableCardCache;
	@Autowired
	private RebateUserActivityService rebateUserActivityService;
	@Autowired
	private AccountChangeService accChangeService;

	@Value("${funds.transfer.version}")
	private String CURR_VERSION;
	/**
	 * 配置：上次无效数据清理时间；清理周期</br>
	 * ###CLEAN_INVLD_DATA_LAST_TM 上次清理时间 </br>
	 * ###CLEAN_INVLD_DATA_INTR 清理周期
	 */
	private static volatile long CLEAN_INVLD_DATA_LAST_TM = 0L;
	private static String LAST_ALLOC_TRANS_CNST = StringUtils.SPACE;
	/**
	 * CLEAN_INVLD_DATA_ONETIME_LAST_TM 一天只清理一次，清理时间
	 */
	private static volatile long CLEAN_INVLD_DATA_ONETIME_LAST_TM = 0L;

	private static volatile int MINIUM_MONEY_NEXT_ROUND = 10000;

	// need已经分配过的出款卡集合
	private List<OutOnly> usedOutList = new ArrayList<OutOnly>();

	private final Cache<Trans2OutAccount, LinkedBlockingQueue<OutOnly>> lockedForNextRound = CacheBuilder.newBuilder()
			.maximumSize(5000).expireAfterWrite(10, TimeUnit.MINUTES).build();

	private final Cache<Integer, ReserveAccount> lockedReserveIn = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(3, TimeUnit.MINUTES).build();

	private final Cache<Integer, OutOnly> lockedThridToOut = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	// 下发卡时间戳集合
	private final Cache<Integer, Long> bindcommonTransTime = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(24, TimeUnit.HOURS).build();

	@Scheduled(fixedDelay = 100)
	public void trans() throws InterruptedException {
		if (!preCondition()) {
			return;
		}
		// int minBal = buildMinBal(true).intValue();
		// 清除need已经分配过的出款卡集合
		usedOutList.clear();

		// 数据准备：1. 有问题的出款账号和入款账号
		Triple<Set<Integer>, Set<Integer>, Set<String>> alarmed = alarmedAccount();
		Set<Integer> inAccountAlarm = alarmed.getLeft(); // 转入未对账,不能进行入款
		Set<Integer> outAccountAlarm = alarmed.getMiddle(); // 转出未对账,不能进行出款
		Set<String> evading = alarmed.getRight();
		log.debug(">> assignBalance alarmed outAccount:{},inAccount{}", outAccountAlarm, inAccountAlarm);

		// 数据准备：2. 有问题的入款账号，转出流水告警，不能进行入款
		Set<Integer> buildAcc = buildAcc4FlowOutMatching();
		inAccountAlarm.addAll(buildAcc);

		// 数据准备：3. 入款卡，将“超过10万的”入款卡和“不超过10万的”入款卡分类(默认10万，配置表中可修改)
		Pair<List<InAccount>, List<InAccount>> inAccountPair = dividInAccountBy10W(outAccountAlarm);
		List<InAccount> inBankWithBigBalance = inAccountPair.getLeft();
		List<InAccount> inBankOther = inAccountPair.getRight();
		log.debug(">> assignBalance dividInAccountBy2W is inBankWithBigBalance:{},inBankOther{}", inBankWithBigBalance,
				inBankOther);

		// 数据准备：4. 备用卡， 区分出可入款和可下发
		Pair<List<ReserveAccount>, List<ReserveAccount>> reserveAccouts = findReserveAccount(inAccountAlarm,
				outAccountAlarm);
		List<ReserveAccount> reserveInAccount = reserveAccouts.getLeft();
		List<ReserveAccount> reserveOutAccount = reserveAccouts.getRight();
		log.debug(">> assignBalance findReserveAccount is reserveInAccount:{},reserveOutAccount{}", reserveInAccount,
				reserveOutAccount);
		// 数据准备：获取所有卡的余额信息
		Map<Object, Object> outAccRealBalMap = buildAccRealBal();
		log.debug("outAccRealBalMap:{}", outAccRealBalMap);

		// 数据准备：6. 所有的出款卡，所需金额大于3000
		Pair<List<OutOnly>, List<OutOnly>> availableOutAccounts = findOutAccount(inAccountAlarm, outAccRealBalMap);
		List<OutOnly> moreOutAccounts = availableOutAccounts.getLeft();
		log.debug(">> assignBalance findReserveAccount is moreOutAccounts:{}", moreOutAccounts);

		// 数据准备：7. 缺钱的出款卡，分为缺1000以下和1000以上
		Pair<List<OutOnly>, List<OutOnly>> needOutAccouts = prepareNeedOutAccount(inAccountAlarm);
		List<OutOnly> needLessOutAccounts = needOutAccouts.getLeft();
		List<OutOnly> needManyOutAccounts = needOutAccouts.getRight();

		// 数据准备：8. 下发卡
		Pair<List<InAccount>, List<Integer>> bindcomAccouts = findBindcommonAccount(outAccountAlarm);
		log.debug(">> assignBalance findReserveAccount is bindcomAccouts:{}", bindcomAccouts.getLeft());

		// 数据准备：9.过滤卡需要的redis数据
		Map<Object, Object> transAccLockTime = redisService.getStringRedisTemplate()
				.boundHashOps(RedisKeys.TRANSFER_ACCOUNT_LOCK_TIME).entries();
		Set<String> activityAcc = rebateUserActivityService.getAllAccountsInActivity();
		Map<Object, Object> issuedPriority = redisService.getStringRedisTemplate()
				.boundHashOps(RedisKeys.ISSUED_HIGH_PRIORITY).entries();

		// 数据准备：10. 下发卡对应的额度
		Map<Object, Object> allBindComRedit = getAllBindcomAccoutsRedit(bindcomAccouts);

		// 特殊处理：需要尽快把余额清零的卡优先处理
		List<OutOnly> specialProcessFailed = doNeedOutAccounts(
				new ArrayList<Trans2OutAccount>(bindcomAccouts.getLeft()), moreOutAccounts, evading, true,
				transAccLockTime, activityAcc, issuedPriority, allBindComRedit);
		log.info("特殊处理下发不成功：{}, {}", specialProcessFailed, specialProcessFailed.size());

		// 下发：1. 如果专注入款卡的余额超过上限（比如：10万），下发单笔49000给备用卡
		List<InAccount> inBankLeft = doInBankWithBigBalance(inBankWithBigBalance, reserveInAccount, evading);
		inBankOther.addAll(inBankLeft);
		log.debug(">> assignBalance doInBankWithBigBalance is inBankLeft:{}", inBankLeft);

		// 下发：2. 预留的备用卡下发给预留的出款卡
		List<Trans2OutAccount> trans2OutAccount = new ArrayList<Trans2OutAccount>(inBankOther);
		trans2OutAccount.addAll(reserveOutAccount);
		Map<Trans2OutAccount, OutOnly> successMap = doNeedOutAccountsLocked(moreOutAccounts, trans2OutAccount, evading);
		log.debug(">> assignBalance doNeedOutAccountsLocked {} usedOutList {}", successMap, usedOutList);

		// 下发: 3. 入款卡和备用卡 下发 给出款卡
		log.debug(">> assignBalance doNeedOutAccounts is availableOutAccounts:{},inBankOther:{}", moreOutAccounts,
				inBankOther);
		moreOutAccounts.removeAll(usedOutList);
		List<OutOnly> inToFromOutFailed = doNeedOutAccounts(trans2OutAccount, moreOutAccounts, evading, false,
				transAccLockTime, activityAcc, issuedPriority, allBindComRedit);
		log.info("入款卡给出款卡所需金额大于3000补钱不成功：{}, {}", inToFromOutFailed, inToFromOutFailed.size());

		// 下发：4. 备用卡余额太多一次分配不完，预留缺钱的出款卡，以便备用卡下一轮能继续下发
		List<OutOnly> outOnlyForNextRound = new ArrayList<>(inToFromOutFailed);
		for (int i = 0; i < 3; i++) {
			outOnlyForNextRound.removeAll(usedOutList);
			log.debug(">> assignBalance trans2OutAccount {}", trans2OutAccount);
			outOnlyForNextRound = lockForNextRound(new ArrayList<Trans2OutAccount>(trans2OutAccount),
					outOnlyForNextRound, evading);
		}
		log.debug(">> assignBalance lockForNextRound {}", lockedForNextRound.asMap());

		// 下发：5. 如果还有超过限额的入款卡，下发给 备用卡
		List<InAccount> inBankLast = doInBankWithBigBalance(inBankOther, reserveInAccount, evading);
		log.info("超过限额下发不出去的入款卡：{}, {}", inBankLast.size(), inBankLast);

		// 下发：6. 特殊处理:缺钱1000以下的并且当日出款小于100的出款卡优先下发一笔
		List<OutOnly> needLessOutFailed = new ArrayList<OutOnly>();
		if (needLessOutAccounts.size() > 0) {
			// 用下发卡补钱
			needLessOutFailed = doNeedOutAccounts(new ArrayList<Trans2OutAccount>(bindcomAccouts.getLeft()),
					needLessOutAccounts, evading, false, transAccLockTime, activityAcc, issuedPriority,
					allBindComRedit);
			log.info("缺钱1000以下并且当日出款小于100的出款卡补钱不成功的：{}, {}", needLessOutFailed, needLessOutFailed.size());
		}
		needManyOutAccounts.addAll(needLessOutFailed);

		// 下发：7. 用其它所有卡给差钱的出款卡补钱
		log.debug(">> assignBalance prepareNeedOutAccount is needOutAccounts:{},", needManyOutAccounts);
		if (needManyOutAccounts.size() > 0) {
			// 删除已经分配过下发任务的出款卡
			needManyOutAccounts.removeAll(usedOutList);
			List<Trans2OutAccount> canIssuedAccounts = new ArrayList<Trans2OutAccount>();
			canIssuedAccounts.addAll(inBankOther);
			canIssuedAccounts.addAll(reserveOutAccount);
			canIssuedAccounts.addAll(bindcomAccouts.getLeft());
			List<OutOnly> failed = doNeedOutAccounts(canIssuedAccounts, needManyOutAccounts, evading, false,
					transAccLockTime, activityAcc, issuedPriority, allBindComRedit);
			log.info("补钱不成功的出款卡：{}, {}", failed.size(), failed);
		}
	}

	// 先分配预留的出款卡和备用卡
	private Map<Trans2OutAccount, OutOnly> doNeedOutAccountsLocked(List<OutOnly> moreOutAccounts,
			List<Trans2OutAccount> reserveOutAccount, Set<String> evading) {
		Map<Trans2OutAccount, OutOnly> successMap = new HashMap<Trans2OutAccount, OutOnly>();
		for (Entry<Trans2OutAccount, LinkedBlockingQueue<OutOnly>> entry : lockedForNextRound.asMap().entrySet()) {
			try {
				Trans2OutAccount reserve = entry.getKey();
				LinkedBlockingQueue<OutOnly> outQueue = entry.getValue();
				if (outQueue == null || outQueue.size() == 0) {
					lockedForNextRound.invalidate(reserve);
					continue;
				}
				ArrayList<OutOnly> list = new ArrayList<OutOnly>(outQueue);
				log.debug("备用卡ID：{}", reserve.getAccountId());
				if (allocateTransService.hasTrans(reserve.getAccountId())) {
					for (OutOnly out : list) {
						usedOutList.add(out);
					}
					continue;
				}

				boolean allocated = false;
				outQueue.clear();
				for (OutOnly out : list) {
					if (allocated) {
						usedOutList.add(out);
						outQueue.offer(out);
					} else {
						if (moreOutAccounts.contains(out) && reserveOutAccount.contains(reserve)) {
							Trans2OutAccount currReserve = reserveOutAccount.get(reserveOutAccount.indexOf(reserve));
							OutOnly currOut = moreOutAccounts.get(moreOutAccounts.indexOf(out));
							Boolean peerTrans = allOTaskSer
									.checkPeerTrans(currReserve.getAccountBaseInfo().getBankType());
							boolean success = currOut.transInWhenTooLitlleBalance(currReserve, evading, peerTrans);
							if (success) {
								allocated = true;
								if (!ObjectUtils.isEmpty(currReserve)) {
									currReserve.setUsed(true);
									successMap.put(currReserve, currOut);
								}
								usedOutList.add(currOut);
							}
						} else {
							usedOutList.add(out);
							outQueue.offer(out);
						}
					}
				}
			} catch (Exception e) {
				log.error("错误", e);
			}
		}
		return successMap;
	}

	// 对于太多钱的备用卡，预留一些出款卡，用于下一次下发
	private List<OutOnly> lockForNextRound(List<Trans2OutAccount> trans2OutAccount, List<OutOnly> inToFromOutFailed,
			Set<String> evading) {
		List<OutOnly> failed = new ArrayList<OutOnly>();
		List<Trans2OutAccount> nextTurnAccounts = trans2OutAccount.stream()
				.filter(p -> (p.isUsed() && p.getRemain() > MINIUM_MONEY_NEXT_ROUND)).collect(Collectors.toList());
		if (nextTurnAccounts.size() == 0) {
			return inToFromOutFailed;
		}
		for (OutOnly outAccount : inToFromOutFailed) {
			try {
				if (Objects.isNull(outAccount.getAccountBaseInfo())
						|| !Objects.equals(outAccount.getAccountBaseInfo().getStatus(), Constants.NORMAL)) {
					ldel4ONeed(String.valueOf(outAccount.getAccountId()));
					log.debug("AllocOAcc >> the oAcc doesn't exist | not Normal Status. id:{}",
							outAccount.getAccountId());
					continue;
				}
				// 入款卡中金额最接近的一张
				int closestPos = closest(outAccount.getNeedMoney(), nextTurnAccounts, true);
				if (closestPos == -1) {
					log.debug(">> AssignBalance outcrad need closestPos is -1");
					failed.add(outAccount);
					continue;
				}
				// 优先找出入款卡金额大于所需金额的卡进行下发
				List<Trans2OutAccount> tailInBanks = nextTurnAccounts.subList(closestPos, nextTurnAccounts.size());
				boolean success = assignNexRound(outAccount, tailInBanks, evading);
				if (!success) {
					log.debug(">>AssignBalance outAccount need not balance exceed account.");
					// 再找出入款卡金额小于所需金额的卡进行下发
					List<Trans2OutAccount> headInBanks = nextTurnAccounts.subList(0, closestPos);
					List<Trans2OutAccount> newHeadInBanks = new ArrayList<Trans2OutAccount>(headInBanks);
					Collections.reverse(newHeadInBanks);
					success = assignNexRound(outAccount, newHeadInBanks, evading);
				}
				if (!success) {
					failed.add(outAccount);
				}
			} catch (Exception e) {
				log.error("错误", e);
			}
		}
		return failed;
	}

	private boolean assignNexRound(OutOnly outAccount, List<Trans2OutAccount> trans2OutAccounts, Set<String> evading) {
		log.debug(">>AssignBalance Start Trans2OutAccount.");
		for (Trans2OutAccount trans2OutAccount : trans2OutAccounts) {
			if (trans2OutAccount.getRemain() < MINIUM_MONEY_NEXT_ROUND) {
				continue;
			}
			LinkedBlockingQueue<OutOnly> queue = lockedForNextRound.getIfPresent(trans2OutAccount);
			if (queue == null) {
				queue = new LinkedBlockingQueue<OutOnly>();
			}
			if (queue.size() >= 3) {
				continue;
			}
			Boolean peerTrans = allOTaskSer.checkPeerTrans(trans2OutAccount.getAccountBaseInfo().getBankType());
			boolean maintain = allOTaskSer.checkMaintain(trans2OutAccount.getAccountBaseInfo().getBankType());
			log.debug(">>AssignBalance assignInAccount peerTrans:{} and maintain:{}", peerTrans, maintain);
			if (peerTrans == null && maintain) {
				continue;
			}
			Optional<Integer> toIntOpt = outAccount.canTransInWhenTooLitlleBalance(trans2OutAccount, evading,
					peerTrans);
			if (toIntOpt.isPresent()) {
				log.debug("备用卡：{} 预留的出款卡：{}", trans2OutAccount.getAccountId(), outAccount.getAccountId());
				trans2OutAccount.minusRemain(toIntOpt.get());
				usedOutList.add(outAccount);
				queue.offer(outAccount);
				lockedForNextRound.put(trans2OutAccount, queue);
				return true;
			}
		}
		return false;
	}

	// 出款卡找专注入款卡(或下发卡)下发所需金额
	private List<OutOnly> doNeedOutAccounts(List<Trans2OutAccount> inAccounts, List<OutOnly> needOutAccounts,
			Set<String> evading, boolean isSpecial, Map<Object, Object> transAccLockTime, Set<String> activityAcc,
			Map<Object, Object> issuedPriority, Map<Object, Object> allBindComRedit) {
		List<OutOnly> failed = new ArrayList<OutOnly>();
		inAccounts.sort(Comparator.comparingInt(Trans2OutAccount::getAmount));
		for (OutOnly outAccount : needOutAccounts) {// [0]ID;[1]score;[2]priority;[3]tm;[4]need
			try {
				if (transAccLockTime.containsKey(String.valueOf(outAccount.getAccountId()))) {
					log.debug(">>AssignBalance assignInAccount outAccount id:{} genPattern4TransferAccountLock.",
							outAccount.getAccountId());
					continue;
				}
				if (Objects.isNull(outAccount.getAccountBaseInfo())
						|| !Objects.equals(outAccount.getAccountBaseInfo().getStatus(), Constants.NORMAL)) {
					ldel4ONeed(String.valueOf(outAccount.getAccountId()));
					log.debug("AllocOAcc >> the oAcc doesn't exist | not Normal Status. id:{}",
							outAccount.getAccountId());
					continue;
				}
				// 入款卡中金额最接近的一张
				int closestPos = closest(outAccount.getNeedMoney(), inAccounts, false);
				if (closestPos == -1) {
					log.debug(">> AssignBalance outcrad need closestPos is -1");
					failed.add(outAccount);
					continue;
				}
				// 优先找出入款卡金额大于所需金额的卡进行下发，inbankFirst=true：入款卡优先，入款卡不满足时再找下发卡
				List<Trans2OutAccount> tailInBanks = inAccounts.subList(closestPos, inAccounts.size());
				boolean success = assignInAccount(outAccount, tailInBanks, evading, true, isSpecial, activityAcc,
						issuedPriority, allBindComRedit);
				if (!success) {
					log.debug(">>AssignBalance outAccount need not balance exceed account.");
					// 再找出入款卡金额大于所需金额的卡进行下发，inbankFirst=false：不区分入款卡和下发卡
					List<Trans2OutAccount> headInBanks = inAccounts.subList(0, closestPos);
					List<Trans2OutAccount> newHeadInBanks = new ArrayList<Trans2OutAccount>(headInBanks);
					Collections.reverse(newHeadInBanks);
					success = assignInAccount(outAccount, newHeadInBanks, evading, true, isSpecial, activityAcc,
							issuedPriority, allBindComRedit);
				}
				if (!success) {
					failed.add(outAccount);
				}
			} catch (Exception e) {
				log.error("错误", e);
			}
		}
		return failed;
	}

	// 将doNeedOutAccounts中的出款卡，分配到合适的入款进行下发。inbankFirst=true：入款卡优先，入款卡不满足时再找下发卡；inbankFirst=false
	// 不区分入款卡和下发卡
	private boolean assignInAccount(OutOnly outAccount, List<Trans2OutAccount> trans2OutAccounts, Set<String> evading,
			boolean inbankFirst, boolean isSpecial, Set<String> activityAcc, Map<Object, Object> issuedPriority,
			Map<Object, Object> allBindComRedit) {
		List<Trans2OutAccount> bindCommonBanks = new ArrayList<Trans2OutAccount>();
		List<Trans2OutAccount> reserveBanks = new ArrayList<Trans2OutAccount>();
		log.debug(">>AssignBalance Start Trans2OutAccount.");
		for (Trans2OutAccount trans2OutAccount : trans2OutAccounts) {
			try {
				if (trans2OutAccount.isUsed()) {
					log.debug(">>AssignBalance assignInAccount is inAccount id:{} isUsed true",
							trans2OutAccount.getAccountId());
					continue;
				}
				if (inbankFirst && trans2OutAccount.getAccountBaseInfo().getType() != Constants.INBANK) {
					log.debug(">>AssignBalance Trans2OutAccount type is:{}",
							trans2OutAccount.getAccountBaseInfo().getType());
					if (trans2OutAccount.getAccountBaseInfo().getType() == Constants.BINDCOMMON) {
						bindCommonBanks.add(trans2OutAccount);
					} else if (trans2OutAccount.getAccountBaseInfo().getType() == Constants.RESERVEBANK) {
						reserveBanks.add(trans2OutAccount);
					}
					continue;
				}
				Boolean peerTrans = allOTaskSer.checkPeerTrans(trans2OutAccount.getAccountBaseInfo().getBankType());
				boolean maintain = allOTaskSer.checkMaintain(trans2OutAccount.getAccountBaseInfo().getBankType());
				log.debug(">>AssignBalance assignInAccount peerTrans:{} and maintain:{}", peerTrans, maintain);
				if (peerTrans == null && maintain) {
					continue;
				}
				OutOnly outOnly = lockedThridToOut.getIfPresent(Integer.valueOf(outAccount.getAccountId()));
				if (outOnly != null && (trans2OutAccount.getAmount()
						- CommonUtils.getLessEnactmentOutAmount() < outAccount.getNeedMoney())) {
					continue;
				}
				if (outAccount.transInWhenTooLitlleBalance(trans2OutAccount, evading, peerTrans)) {
					trans2OutAccount.setUsed(true);
					usedOutList.add(outAccount);
					return true;
				}
			} catch (Exception e) {
				log.error("错误", e);
			}
		}
		log.debug(">>AssignBalance not inAccount,Start reserverAccount.");
		for (Trans2OutAccount reserveAccount : reserveBanks) {
			try {
				Boolean peerTrans = allOTaskSer.checkPeerTrans(reserveAccount.getAccountBaseInfo().getBankType());
				boolean maintain = allOTaskSer.checkMaintain(reserveAccount.getAccountBaseInfo().getBankType());
				log.debug(">>AssignBalance assignInAccount peerTrans:{} and maintain:{}", peerTrans, maintain);
				if (peerTrans == null && maintain) {
					continue;
				}
				OutOnly outOnly = lockedThridToOut.getIfPresent(Integer.valueOf(outAccount.getAccountId()));
				if (outOnly != null && (reserveAccount.getAmount()
						- CommonUtils.getLessEnactmentOutAmount() < outAccount.getNeedMoney())) {
					continue;
				}
				if (outAccount.transInWhenTooLitlleBalance(reserveAccount, evading, peerTrans)) {
					reserveAccount.setUsed(true);
					usedOutList.add(outAccount);
					return true;
				}
			} catch (Exception e) {
				log.error("错误", e);
			}
		}
		if (bindCommonBanks != null && inbankFirst) {
			bindCommonBanks.sort((o1, o2) -> {
				// 下发卡优先级：1.余额超信用额度 2.参加了返利网活动 3.时间戳(时间越靠后优先级越高) 4.余额
				boolean o1IsOverLimit = allBindComRedit.containsKey(o1.getAccountId());
				boolean o2IsOverLimit = allBindComRedit.containsKey(o2.getAccountId());
				if (o1IsOverLimit && !o2IsOverLimit) {
					return -1;
				} else if (!o1IsOverLimit && o2IsOverLimit) {
					return 1;
				}
				boolean o1Flag = activityAcc.contains(String.valueOf(o1.getAccountId()));
				boolean o2Flag = activityAcc.contains(String.valueOf(o2.getAccountId()));
				if (o1Flag && !o2Flag) {
					return -1;
				} else if (!o1Flag && o2Flag) {
					return 1;
				}
				Long taskTimeO1 = bindcommonTransTime.getIfPresent(Integer.valueOf(o1.getAccountId()));
				Long taskTimeO2 = bindcommonTransTime.getIfPresent(Integer.valueOf(o2.getAccountId()));
				if (taskTimeO1 == null && taskTimeO2 == null) {
					return 0;
				}
				if (taskTimeO1 == null && taskTimeO2 != null) {
					return -1;
				}
				if (taskTimeO2 == null && taskTimeO1 != null) {
					return 1;
				}
				return String.valueOf(taskTimeO1).compareTo(String.valueOf(taskTimeO2));
			});
		}
		List<OutAccount> outOnlyCache = availableCardCache
				.getOutAccountByCategory(outAccount.getZone() + "-" + outAccount.getLevelId());
		log.debug(">>AssignBalance not reserverAccount,Start bindCommonAccount.{}", outOnlyCache);

		for (Trans2OutAccount bindCommonAccount : bindCommonBanks) {
			try {
				if (isSpecial && !issuedPriority
						.containsKey(String.valueOf(bindCommonAccount.getAccountBaseInfo().getId()))) {
					continue;
				}
				Boolean peerTrans = allOTaskSer.checkPeerTrans(bindCommonAccount.getAccountBaseInfo().getBankType());
				boolean maintain = allOTaskSer.checkMaintain(bindCommonAccount.getAccountBaseInfo().getBankType());
				log.debug(">>AssignBalance assignInAccount peerTrans:{} and maintain:{}", peerTrans, maintain);
				if (peerTrans == null && maintain) {
					continue;
				}

				if (lockedThridToOut.getIfPresent(outAccount.getAccountId()) != null) {
					continue;
				}
				boolean acountFlag = false;
				if (outOnlyCache != null) {
					if (outAccount.getLevelId() == CurrentSystemLevel.Outter.getValue()
							&& outOnlyCache.size() > CommonUtils.getThirdToOutOutterAmount()) {
						acountFlag = true;
					}
					if (outAccount.getLevelId() == CurrentSystemLevel.Inner.getValue()
							&& outOnlyCache.size() > CommonUtils.getThirdToOutInterAmount()) {
						acountFlag = true;
					}
				}
				// 给第三方下发规则：1.返利网的出款卡 2.区域为MNL 3.余额小于1000（可配置） 4.缺钱大于20000（可配置）
				// 5.满足第三方下发条件
				// 6.同区域同层级中出款卡的余额大于2万（可配置）的数量超过10/15（可配置）
				// 7.已回收的卡在黑名单中不能给第三方下发
				log.debug("这个出款卡id：{},flag:{},zone:{},balance:{},acountFLag:{},needMoney:{},level:{}",
						outAccount.getAccountId(), outAccount.getAccountBaseInfo().getFlag(), outAccount.getZone(),
						outAccount.getBalance(), acountFlag, outAccount.getNeedMoney(), outAccount.getLevelId());
				if (outAccount.getAccountBaseInfo().getFlag().equals(AccountFlag.REFUND.getTypeId())
						&& outAccount.getZone() != 1
						&& outAccount.getBalance() < CommonUtils.getThirdToOutBelowBalance() && acountFlag
						&& outAccount.getNeedMoney() >= CommonUtils.getThirdToOutLessBalance()
						&& Objects.nonNull(outAccount.getPriority())
						&& outAccount.getPriority() != Constants.O_SCR_N_MONY_Y_TASK_N_BAL && !isSpecial
						&& checkBlack(evading, "0", outAccount.getAccountId())) {
					log.debug(">>AssignBalance addNeedThirdDrawToOutCardList accountId:{}  needMoney:{}",
							outAccount.getAccountId(), outAccount.getNeedMoney());
					accountService.addNeedThirdDrawToOutCardList(outAccount.getAccountId(), outAccount.getNeedMoney(),
							outAccount.getAccountBaseInfo().getType());
					lockedThridToOut.put(outAccount.getAccountId(), outAccount);
					usedOutList.add(outAccount);
					continue;
				}
				if (outAccount.transInWhenTooLitlleBalance(bindCommonAccount, evading, peerTrans)) {
					bindcommonTransTime.put(bindCommonAccount.getAccountId(), System.currentTimeMillis());
					bindCommonAccount.setUsed(true);
					usedOutList.add(outAccount);
					return true;
				}
			} catch (Exception e) {
				log.error("错误", e);
			}
		}
		return false;
	}

	// 2. 如果专注入款卡的余额超过上限（比如：5万），下发单笔49000给备用卡
	private List<InAccount> doInBankWithBigBalance(List<InAccount> inBankWithBigBalance,
			List<ReserveAccount> reserveInAccount, Set<String> evading) {
		log.debug(">>AssignBalance start doInBankWithBigBalance");
		// 找出备用卡
		List<ReserveAccount> availablePCReserveAcc = new ArrayList<ReserveAccount>();
		List<ReserveAccount> availableRefundReserveAcc = new ArrayList<ReserveAccount>();

		for (ReserveAccount reserveAccount : reserveInAccount) {
			if (reserveAccount.getAccountBaseInfo().getFlag().equals(AccountFlag.PC.getTypeId())) {
				availablePCReserveAcc.add(reserveAccount);
			} else {
				availableRefundReserveAcc.add(reserveAccount);
			}
		}
		if (availablePCReserveAcc.size() <= 0) {
			log.debug(">>AssignBalance doInBankWithBigBalance not pc reserveAccount.");
			return inBankWithBigBalance;
		}

		List<InAccount> inBankLeft = new ArrayList<InAccount>();
		List<ReserveAccount> usedPCReserveAccount = new ArrayList<ReserveAccount>();

		// 入款卡余额多的优先下发
		inBankWithBigBalance.sort(Comparator.comparingInt(InAccount::getAmount).reversed());
		// PC备用卡余额少的优先下发
		availablePCReserveAcc.sort(Comparator.comparingInt(ReserveAccount::getAmount));
		for (InAccount inAccount : inBankWithBigBalance) {
			try {
				if (inAccount.isUsed()) {
					log.debug(">>AssignBalance start In to Reserve is inAccount id:{} isUsed true.",
							inAccount.getAccountId());
					continue;
				}
				if (Constants.NORMAL != inAccount.getAccountBaseInfo().getStatus()) {
					log.debug(">>AssignBalance start In to Reserve is inAccount id:{} status not NORMAL.",
							inAccount.getAccountId());
					continue;
				}
				boolean cuccess = false;
				for (ReserveAccount reserveAccount : availablePCReserveAcc) {
					if (Constants.NORMAL != reserveAccount.getAccountBaseInfo().getStatus()) {
						log.debug(">>AssignBalance start In:{} to Reserve:{} is status not NORMAL.",
								inAccount.getAccountId(), reserveAccount.getAccountId());
						continue;
					}
					if (usedPCReserveAccount.contains(reserveAccount)) {
						continue;
					}
					if (reserveAccount.isInUsed()) {
						log.debug(">>AssignBalance start In:{} to Reserve:{} is isUsed true.", inAccount.getAccountId(),
								reserveAccount.getAccountId());
						continue;
					}
					if (inAccount.transReserveWhenTooMuchBalance(reserveAccount, evading,
							getInBankMinBalance(inAccount.getAccountBaseInfo()))) {
						inAccount.setUsed(true);
						usedPCReserveAccount.add(reserveAccount);
						cuccess = true;
						lockedReserveIn.put(reserveAccount.getAccountId(), reserveAccount);
						break;
					}
				}
				if (!cuccess) {
					inBankLeft.add(inAccount);
				}
			} catch (Exception e) {
				log.error("错误", e);
			}
		}
		log.debug("AssignBalance >> doInBankWithBigBalance:{}", usedPCReserveAccount);
		return inBankLeft;
	}

	// 获取缺钱的出款卡
	private Pair<List<OutOnly>, List<OutOnly>> prepareNeedOutAccount(Set<Integer> inAccountAlarm) {
		List<Number[]> oneedList = new ArrayList<>();// [0]ID;[1]score;[2]priority;[3]tm;[4]need
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		Set<ZSetOperations.TypedTuple<String>> valScrSet = template.boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI)
				.rangeWithScores(0, -1);
		if (!CollectionUtils.isEmpty(valScrSet)) {
			for (ZSetOperations.TypedTuple<String> valScr : valScrSet) {
				double scr = valScr.getScore();
				Number[] infs = deScore4Out(scr);
				Number[] ret = Arrays.copyOf(new Number[] { Integer.valueOf(valScr.getValue()), scr }, infs.length + 2);
				System.arraycopy(infs, 0, ret, 2, infs.length);
				oneedList.add(ret);
			}
			// 带出款任务的出款卡优先, 其它情况需求量大的出款卡优先
			oneedList.sort((o1, o2) -> {
				if (o1[2].intValue() == Constants.O_SCR_N_MONY_Y_TASK_N_BAL
						&& o2[2].intValue() != Constants.O_SCR_N_MONY_Y_TASK_N_BAL) {
					return -1;
				} else if (o1[2].intValue() != Constants.O_SCR_N_MONY_Y_TASK_N_BAL
						&& o2[2].intValue() == Constants.O_SCR_N_MONY_Y_TASK_N_BAL) {
					return 1;
				}
				if (o1[2].intValue() == Constants.O_SCR_MANUAL_N_MONY
						&& o2[2].intValue() != Constants.O_SCR_MANUAL_N_MONY) {
					return -1;
				} else if (o1[2].intValue() != Constants.O_SCR_MANUAL_N_MONY
						&& o2[2].intValue() == Constants.O_SCR_MANUAL_N_MONY) {
					return 1;
				}
				if (o1[2].intValue() == Constants.O_SCR_N_MONY_Y_TASK
						&& o2[2].intValue() != Constants.O_SCR_N_MONY_Y_TASK) {
					return -1;
				} else if (o1[2].intValue() != Constants.O_SCR_N_MONY_Y_TASK
						&& o2[2].intValue() == Constants.O_SCR_N_MONY_Y_TASK) {
					return 1;
				}
				double d = o2[4].intValue() - o1[4].intValue();
				return d > 0 ? 1 : (d < 0 ? -1 : 0);
			});
		}

		List<OutOnly> needManyOutAccounts = new ArrayList<OutOnly>();
		List<OutOnly> needLessOutAccounts = new ArrayList<OutOnly>();

		Map<Object, Object> amountSumMap = redisService.getFloatRedisTemplate().opsForHash()
				.entries(RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD);

		Map<Object, Object> outAccRealBalMap = buildAccRealBal();
		log.debug("outAccRealBalMap:{}", outAccRealBalMap);
		for (Number[] alloc : oneedList) {// [0]ID;[1]score;[2]priority;[3]tm;[4]need
			if (Objects.isNull(alloc)) {
				break;
			}
			Optional<OutOnly> outAccountOpt = accountFactory.generateOutOnlyAccount(alloc, outAccRealBalMap);
			if (!outAccountOpt.isPresent()) {
				ldel4ONeed(String.valueOf(alloc[0]));
				continue;
			}
			if (inAccountAlarm.contains(outAccountOpt.get().getAccountId())) {
				log.debug("AllocOAcc >> the acc （{}） have flows that need handle.", outAccountOpt.get().getAccountId());
				continue;
			}
			if (outAccountOpt.get().getNeedMoney() < 1000) {
				Object dailyOutwardSum = amountSumMap.get(String.valueOf(outAccountOpt.get().getAccountId()));
				if (Objects.isNull(dailyOutwardSum)
						|| (Objects.nonNull(dailyOutwardSum) && (Float) dailyOutwardSum < 100)) {
					needLessOutAccounts.add(outAccountOpt.get());
					continue;
				}
			}
			needManyOutAccounts.add(outAccountOpt.get());
		}
		return Pair.of(needLessOutAccounts, needManyOutAccounts);
	}

	private boolean preCondition() {
		try {
			String nativeHost = CommonUtils.getInternalIp();
			if (!alloIAcntSer.checkHostRunRight()) {
				log.debug("the host {} have no right to execute the allocation transfer schedule at present.",
						nativeHost);
				Thread.sleep(5000L);
				return false;
			}
			long currTm = System.currentTimeMillis();
			if (cleanInvalidData(currTm, CLEAN_INVLD_DATA_LAST_TM, Constants.CLEAN_INVLD_DATA_INTR)) {
				CLEAN_INVLD_DATA_LAST_TM = currTm;
				log.debug("the host {} executed the clean the invalid data in cache.", nativeHost);
			}
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			String allocNewTransCnst = template.boundValueOps(Constants.ALLOC_NEW_TRANS_CNST).get();
			if (Objects.equals(allocNewTransCnst, LAST_ALLOC_TRANS_CNST)) {
				log.debug(
						"the host {} have right , but no need to execute the allocation transfer schedule at present.",
						nativeHost);
				Thread.sleep(2000L);
				return false;
			}
			LAST_ALLOC_TRANS_CNST = Objects.isNull(allocNewTransCnst) ? String.valueOf(System.currentTimeMillis())
					: allocNewTransCnst;
			if (Objects.isNull(allocNewTransCnst)) {
				template.boundValueOps(Constants.ALLOC_NEW_TRANS_CNST).set(LAST_ALLOC_TRANS_CNST);
			}
			log.debug("the host {}  execute the allocation transfer schedule.", nativeHost);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// 有问题的出款账号和入款账号
	private Triple<Set<Integer>, Set<Integer>, Set<String>> alarmedAccount() {
		Set<Integer> inAccountAlarm = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_MACH_ALARM)
				.keys().stream().map(p -> Integer.valueOf(p.toString())).collect(Collectors.toSet());
		inAccountAlarm = inAccountAlarm == null ? new HashSet<>() : inAccountAlarm;
		Set<Integer> outAccountAlarm = new HashSet<>();
		Set<Integer> failure = allocateTransService.buildFailureTrans();
		if (!CollectionUtils.isEmpty(failure)) {
			inAccountAlarm.addAll(failure);
			outAccountAlarm.addAll(failure);
		}
		Set<Integer> invSet = systemAccountManager.accountingException();
		Set<Integer> outvSet = systemAccountManager.accountingException();
		invSet.addAll(systemAccountManager.accountingSuspend());
		outvSet.addAll(systemAccountManager.accountingSuspend());
		if (!CollectionUtils.isEmpty(invSet)) {
			for (Integer id : invSet) {
				AccountBaseInfo base = accountService.getFromCacheById(id);
				if (base != null && base.getHolder() == null) {
					inAccountAlarm.add(id);
				}
			}
		}
		if (!CollectionUtils.isEmpty(outvSet)) {
			for (Integer id : outvSet) {
				AccountBaseInfo base = accountService.getFromCacheById(id);
				if (base != null && base.getHolder() == null) {
					outAccountAlarm.add(id);
				}
			}
		}
		// inAccountAlarm.addAll(sysBalService.unkownOutward());
		// outAccountAlarm.addAll(sysBalService.unkownOutward());
		Set<String> evading = findBlackList();
		log.debug("转入黑名单：" + inAccountAlarm + " 转出黑名单：" + outAccountAlarm + ", " + evading);

		return Triple.of(inAccountAlarm, outAccountAlarm, evading);
	}

	// 1. 将“超过10万的”入款卡和“不超过10万的”入款卡分类(默认10万，配置表中可修改)
	private Pair<List<InAccount>, List<InAccount>> dividInAccountBy10W(Set<Integer> outAccountAlarm) {
		Pair<List<InAccount>, List<Integer>> inBankList = getAllAccount(Constants.INBANK, outAccountAlarm);
		inBankList.getLeft().sort(Comparator.comparingInt(InAccount::getAmount));
		int closestPos = exceedMin(CommonUtils.balanceOfTransAllOut(), inBankList.getLeft());
		List<InAccount> inBankOther;
		List<InAccount> inBankWithBigBalance = new ArrayList<>();
		if (closestPos == -1) {
			inBankOther = inBankList.getLeft(); // 全部低于10万
		} else {
			inBankOther = inBankList.getLeft().subList(0, closestPos); // 低于10万
			inBankWithBigBalance = inBankList.getLeft().subList(closestPos, inBankList.getLeft().size());// 高于10万
		}
		return Pair.of(inBankWithBigBalance, inBankOther);
	}

	// 找出下发卡，用于给出款卡补钱
	private Pair<List<InAccount>, List<Integer>> findBindcommonAccount(Set<Integer> inAccountAlarm) {
		Pair<List<InAccount>, List<Integer>> bindCommonBank = getAllAccount(Constants.BINDCOMMON, inAccountAlarm);
		return Pair.of(bindCommonBank.getLeft(), bindCommonBank.getRight());
	}

	// 分别找出可接受入款的和可进行下发的备用卡
	private Pair<List<ReserveAccount>, List<ReserveAccount>> findReserveAccount(Set<Integer> inAccountAlarm,
			Set<Integer> OutAccountAlarm) {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		Double[] reserveCriteria = new Double[] { enScore4Fr(Constants.RESERVEBANK, 0, 0, 0, 0),
				enScore4Fr(Constants.RESERVEBANK + 1, 0, 0, 0, 0) };
		Set<ZSetOperations.TypedTuple<String>> reserveBankS = template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
				.rangeByScoreWithScores(reserveCriteria[0], reserveCriteria[1]);

		List<ReserveAccount> reserveInBankList = new ArrayList<ReserveAccount>();
		List<ReserveAccount> reserveOutBankList = new ArrayList<ReserveAccount>();
		for (ZSetOperations.TypedTuple<String> reserveBank : reserveBankS) {
			Optional<ReserveAccount> reserveAccountOpt = accountFactory.generateReserveAccount(reserveBank);
			if (reserveAccountOpt.isPresent()) {
				ReserveAccount reserveAccount = reserveAccountOpt.get();
				if (!inAccountAlarm.contains(reserveAccount.getAccountId())) {
					ReserveAccount queueReserve = lockedReserveIn.getIfPresent(reserveAccount.getAccountId());
					if (queueReserve == null) {
						reserveInBankList.add(reserveAccount);
					}
				}
				if (!OutAccountAlarm.contains(reserveAccount.getAccountId())) {
					reserveOutBankList.add(reserveAccount);
				}
			}
		}
		return Pair.of(reserveInBankList, reserveOutBankList);
	}

	// 找出所有出款卡，用于给不缺钱的补满
	private Pair<List<OutOnly>, List<OutOnly>> findOutAccount(Set<Integer> outAccountAlarm,
			Map<Object, Object> outAccRealBalMap) {
		// 找出PC出款卡
		List<OutOnly> moreOutAccounts = new ArrayList<OutOnly>();
		List<OutOnly> lessOutAccounts = new ArrayList<OutOnly>();
		Double[] outCriteria = new Double[] { enScore4Fr(Constants.OUTBANK, 0, 0, 0, 0),
				enScore4Fr(Constants.OUTBANK + 1, 0, 0, 0, 0) };
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		Set<ZSetOperations.TypedTuple<String>> outBankS = template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
				.rangeByScoreWithScores(outCriteria[0], outCriteria[1]);
		for (ZSetOperations.TypedTuple<String> outBank : outBankS) {
			Optional<OutOnly> outAccountOpt = accountFactory.generateOutAccount(outBank, outAccRealBalMap);
			if (!outAccountOpt.isPresent()) {
				continue;
			}
			if (outAccountAlarm.contains(outAccountOpt.get().getAccountId())) {
				continue;
			}
			if (outAccountOpt.get().getNeedMoney() < CommonUtils.outNeedMoneyLessValue()) {
				lessOutAccounts.add(outAccountOpt.get());
				continue;
			}
			moreOutAccounts.add(outAccountOpt.get());
		}
		lessOutAccounts.sort(Comparator.comparingInt(OutOnly::getNeedMoney).reversed());
		moreOutAccounts.sort(Comparator.comparingInt(OutOnly::getNeedMoney).reversed());
		return Pair.of(moreOutAccounts, lessOutAccounts);
	}

	private Pair<List<InAccount>, List<Integer>> getAllAccount(int inType, Set<Integer> outAccountAlarm) {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		Double[] inCriteria = new Double[] { enScore4Fr(inType, 0, 0, 0, 0), enScore4Fr(inType + 1, 0, 0, 0, 0) };
		Set<ZSetOperations.TypedTuple<String>> inBankS = template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
				.rangeByScoreWithScores(inCriteria[0], inCriteria[1]);

		List<InAccount> inBankList = new ArrayList<InAccount>();
		List<Integer> bindComAccIdList = new ArrayList<Integer>();
		for (ZSetOperations.TypedTuple<String> inBank : inBankS) {
			Optional<InAccount> inAccountOpt = accountFactory.generateInAccount(inBank);
			if (inAccountOpt.isPresent()) {
				InAccount inAccount = inAccountOpt.get();
				if (outAccountAlarm.contains(inAccount.getAccountId())) {
					continue;
				}
				inBankList.add(inAccount);
				bindComAccIdList.add(inAccount.getAccountId());
			}
		}
		return Pair.of(inBankList, bindComAccIdList);
	}

	// 找出入款卡中余额最接近of的序号
	private int closest(int of, List<Trans2OutAccount> in, boolean isRemain) {
		int min = Integer.MAX_VALUE;
		int closest = -1;

		for (int i = 0; i < in.size(); i++) {
			Trans2OutAccount v = in.get(i);
			final int diff = Math.abs(isRemain ? v.getRemain() : v.getAmount() - of);

			if (diff < min) {
				min = diff;
				closest = i;
			}
		}

		return closest;
	}

	// 找出入款卡中余额高于一键转出额度最少的卡of序号
	private int exceedMin(int of, List<InAccount> in) {
		for (int i = 0; i < in.size(); i++) {
			InAccount v = in.get(i);
			if (v.getAmount() > of) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 获取:账号预留余额设置</br>
	 * 生产环境：isRobot ? TRANS_ROBOT_BAL : TRANS_MANUAL_BAL</br>
	 * 测试环境：BigDecimal.ONE
	 */
	@SuppressWarnings("unused")
	private BigDecimal buildMinBal(boolean isRobot) {
		if (CommonUtils.checkProEnv(CURR_VERSION)) {
			return isRobot ? new BigDecimal(50) : new BigDecimal(20);
		} else {
			return BigDecimal.ONE;
		}
	}

	/**
	 * * 根据内层/外层；银行卡区域；银行卡类型；盘口；金额生成一个分值
	 * <p>
	 * 分值格式：12000000.12345678
	 * </p>
	 * <p>
	 * 整数部分:共七位，从左往右</br>
	 * 第一、二位：银行卡类型</br>
	 * 第三、四位：区域</br>
	 * 第 五 位 ：内层/中层/外层；</br>
	 * 第六，七，八:盘口ID [001~999],公司盘口最大数量为999个</br>
	 * </p>
	 * <p>
	 * 小数部分：共8位，金额的整数部分
	 * </p>
	 *
	 * @param type
	 *            银行卡类型
	 * @param zone
	 *            区域
	 * @param l
	 *            内层/外层/中层
	 * @param handi
	 *            盘口
	 * @param bal
	 *            银行卡余额
	 */
	private Double enScore4Fr(int type, int zone, int l, int handi, int bal) {
		int l_ = CommonUtils.isMergeMidInner() ? (l == Constants.Outter ? Constants.Outter : Constants.Inner) : l;
		return Double.valueOf(String.format("%02d%02d%d%03d.%08d", type, zone, l_, handi, bal));
	}

	/**
	 * 解析出款卡补钱分值
	 *
	 * @return Number[0] priority </br>
	 *         Number[1] tm </br>
	 *         Number[2] need
	 * @see this#deScore4Out(Double)
	 */
	private Number[] deScore4Out(Double score) {
		String sc = CommonUtils.getNumberFormat(7, 9).format(score);
		return new Number[] { Integer.valueOf(sc.substring(0, 1)),
				Long.valueOf(sc.substring(1, 7) + sc.substring(8, 12) + "000"), Integer.valueOf(sc.substring(12, 17)) };
	}

	/**
	 * 查询所有黑名单集
	 */
	private Set<String> findBlackList() {
		return redisService.getStringRedisTemplate().keys(RedisKeys.genPattern4TransBlack());
	}

	private void ldel4ONeed(String tar) {
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI).remove(tar);
	}

	/**
	 * clean the invalid data in cache
	 *
	 * @param currTm
	 *            the millis of the current time.
	 * @param lastTm
	 *            the cleaning time last time.
	 * @param internal
	 *            the cleaning internal | period
	 * @return true: executed the cleaning action.</br>
	 *         false: didn't execute the cleaning action.
	 */
	private boolean cleanInvalidData(long currTm, long lastTm, long internal) {
		if (lastTm + internal > currTm) {
			return false;
		}
		int EXPR_INBANK = 300000, EXPR_ISSUE = 180000, EXPR_RSV = 180000, EXPR_OUTBANK = 300000;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		template.boundHashOps(RedisKeys.ACC_API_REVOKE_TM).entries().forEach((k, v) -> {
			boolean clean = false;
			AccountBaseInfo base = Objects.isNull(k) ? null
					: accountService.getFromCacheById(Integer.valueOf((String) k));
			if (Objects.isNull(base) || Objects.isNull(v)) {
				clean = true;
			} else {
				int type = base.getType();
				if (Constants.INBANK == type)
					clean = currTm - Long.valueOf(v.toString()) >= EXPR_INBANK;
				else if (Constants.RESERVEBANK == type)
					clean = currTm - Long.valueOf(v.toString()) >= EXPR_RSV;
				else if (Constants.OUTBANK == type)
					clean = currTm - Long.valueOf(v.toString()) >= EXPR_OUTBANK;
				else
					clean = currTm - Long.valueOf(v.toString()) >= EXPR_ISSUE;
			}
			if (clean) {
				ldel4ONeed(String.valueOf(k));
				template.boundHashOps(RedisKeys.ACC_REAL_BAL).delete(k);
				template.boundHashOps(RedisKeys.ACC_REAL_BAL_RPT_TM).delete(k);
				template.boundHashOps(RedisKeys.ACC_API_REVOKE_TM).delete(k);
				template.boundHashOps(RedisKeys.REAL_BAL_LASTTIME).delete(k);
				template.boundHashOps(RedisKeys.INBANK_ONLINE).delete(k);
			}
		});
		if (CLEAN_INVLD_DATA_ONETIME_LAST_TM == 0L) {
			long difTm = currTm % Constants.ONE_DAY_TIMESTAMP;
			// 第一次凌晨4点到5点之前清理测试转账的数据
			if (difTm > 72000000 && difTm < 75600000) {
				template.delete(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS);
				template.delete(RedisKeys.ACTIVE_ACCOUNT_KEYS);
				CLEAN_INVLD_DATA_ONETIME_LAST_TM = currTm;
				log.info("记录清理一键转出、激活数据的时间");
			}
		}
		// 非第一次清理，在上次清理时间往后推一天再进行清理
		else if (CLEAN_INVLD_DATA_ONETIME_LAST_TM + Constants.ONE_DAY_TIMESTAMP < currTm
				&& CLEAN_INVLD_DATA_ONETIME_LAST_TM + Constants.ONE_DAY_TIMESTAMP + 1800000L > currTm) {
			template.delete(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS);
			// 如果需要激活的账号已经激活则 不需要删除key frId:amount这样的key 因为存在需要激活的账号当天没有抓到流水
			// 没有初始化peakbalance
			// ActiveAccountTestTransKey 数据格式
			// key frId:amount
			// val
			// ActiveAccountTestTrans:frId:toId:frAccount:toAccount:toOwner:toBankType:amount
			String activeKeys = redisService.getStringRedisTemplate().opsForHash()
					.entries(RedisKeys.ACTIVE_ACCOUNT_KEYS).toString().replace("{", "").replace("}", "");
			String[] keys = activeKeys.split(",");
			for (String str : keys) {
				if (str != null && str.length() != 0) {
					String key = str.split("=")[0];
					String frId = str.split(":")[2];
					AccountBaseInfo toAccount = accountService.getFromCacheById(Integer.parseInt(frId));
					if (null != toAccount && null != toAccount.getStatus()
							&& toAccount.getStatus() == AccountStatus.Inactivated.getStatus()) {
						redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACTIVE_ACCOUNT_KEYS).delete(key);
					}
				}
			}
			// template.delete(RedisKeys.ACTIVE_ACCOUNT_KEYS);
			CLEAN_INVLD_DATA_ONETIME_LAST_TM = currTm;
			log.info("记录清理一键转出、激活数据的时间");
		}
		return true;
	}

	public Set<Integer> buildAcc4FlowOutMatching() {
		return redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_MACH_ALARM).keys().stream()
				.map(p -> Integer.valueOf(p.toString())).collect(Collectors.toSet());
	}

	public Map<Object, Object> buildAccRealBal() {
		return redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_REAL_BAL).entries();
	}

	private Integer getInBankMinBalance(AccountBaseInfo base) {
		int minBalance;
		if (base.getMinBalance() != null) {
			minBalance = base.getMinBalance().intValue();
		} else {
			minBalance = (int) (Math.random() * 100 + 101);
		}
		return minBalance;
	}

	/**
	 * 获取下发卡余额是否有超出信用额度
	 * 
	 * @param bindcomAccouts
	 * @return
	 */
	public Map<Object, Object> getAllBindcomAccoutsRedit(Pair<List<InAccount>, List<Integer>> bindcomAccouts) {
		Map<Object, Object> isOverLimitMap = new HashMap<Object, Object>();
		Map<Integer, Integer> allAccMarginMap = accChangeService.allMargin(bindcomAccouts.getRight());
		for (int i = 0; i < bindcomAccouts.getLeft().size(); i++) {
			InAccount bindcom = bindcomAccouts.getLeft().get(i);
			int margin = allAccMarginMap.get(bindcom.getAccountId());
			if (bindcom.getAmount() - margin >= 0)
				isOverLimitMap.put(bindcom.getAccountId(), true);
		}
		return isOverLimitMap;
	}
	
	protected boolean checkBlack(Set<String> black, String frAcc, int toId) {
		boolean ret = CollectionUtils.isEmpty(black)
				|| (black.stream().filter(p -> p.startsWith(RedisKeys.gen4TransBlack(Constants.WILD_CARD_ACCOUNT, toId, 0))
						|| p.startsWith(RedisKeys.gen4TransBlack(frAcc, toId, 0))).count() == 0);
		return ret;
	}

	public void validateDate(Integer accId) {
		lockedThridToOut.invalidate(accId);
	}
}