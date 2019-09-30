package com.xinbo.fundstransfer.service.impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.runtime.task.ToolResponseData;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.apache.shiro.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AccountChangeServiceImpl implements AccountChangeService {
	private static final Logger log = LoggerFactory.getLogger(AccountChangeServiceImpl.class);
	@Autowired
	private RedisService redisSer;
	@Autowired
	@Lazy
	private AccountService accSer;
	@PersistenceContext
	private EntityManager enMgr;
	@Autowired
	private CabanaService cabanaSer;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	private AllocateTransService allTransSer;
	@Autowired
	private AllocateTransferService transferSer;
	@Autowired
	private AllocateIncomeAccountService allInAccSer;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SystemAccountManager systemAccountManager;
	@Autowired
	private AccountExtraService accountExtraService;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private AllocateIncomeAccountService incomeAccountAllocateService;
	private ObjectMapper mapper = new ObjectMapper();

	private volatile static Thread THREAD_ACC_CHANGE = null;
	private static final ConcurrentLinkedQueue<Integer> QUENE_ACC_ID = new ConcurrentLinkedQueue<>();
	private static final ConcurrentHashSet<Integer> DUPLICATE_ACC_STATUS = new ConcurrentHashSet<>();
	static final Cache<Integer, CabanaStatus> CACHE_ACC_STATUS = CacheBuilder.newBuilder().maximumSize(60000)
			.expireAfterWrite(300, TimeUnit.SECONDS).build();
	private static final ConcurrentHashMap<Integer, Long> ACC_RECONCILIATE_STATUS = new ConcurrentHashMap<>();

	private static final int TOLERANCE_OUT_AMT_DAILY = 4000;

	private static final long RECONCILIATE_EXPIRE = 15 * 60 * 1000;

	@Override
	public Integer margin(AccountBaseInfo target) {
		if (Objects.isNull(target))
			return 0;
		if (Objects.nonNull(target.getFlag()) && Objects.equals(AccountFlag.REFUND.getTypeId(), target.getFlag())) {
			BizAccountMore more = accMoreSer.getFromCacheByMobile(target.getMobile());
			log.debug("账号id:{},是返利网:{},根据accountmore 获取linelimit值 ：{}", target.getId(), target.getFlag(),
					more != null ? more.getLinelimit() : "more空");
			int ret = Objects.isNull(more) || Objects.isNull(more.getLinelimit()) ? 0 : more.getLinelimit().intValue();
			if (Objects.nonNull(more) && Objects.nonNull(more.getTmpMargin())) {
				ret = ret + more.getTmpMargin().intValue();
			}
			log.debug("账号id:{}  TmpMargin() :{}   Linelimit+TmpMargin ：{} ", target.getId(),
					more == null ? "more为空" : more.getTmpMargin(), ret);
			return ret;
		}
		if (target.getPeakBalance() != null && target.getPeakBalance() > 0) {
			log.debug("非返利网账号id:{},获取 peakbalance:{}", target.getId(), target.getPeakBalance());
			return target.getPeakBalance();
		}
		Integer ret = target.getLimitBalance() != null && target.getLimitBalance() > 0 ? target.getLimitBalance()
				: 12000;
		log.debug("非返利网账号id:{},获取 LimitBalance:{},返回值:{} ", target.getId(), target.getLimitBalance(), ret);
		return ret;
	}

	/**
	 * k:账号id v:账号对应的marigin值
	 * 
	 * @return
	 */
	@Override
	public Map<Integer, Integer> allMargin(List<Integer> accountIds) {
		Assert.notNull(accountIds, "账号id集合为空");
		Map<Integer, Integer> res = Maps.newLinkedHashMap();
		Map<Integer, BizAccountMore> accountMores = accMoreSer.allFromCacheByMobile(accountIds);
		if (!CollectionUtils.isEmpty(accountIds)) {
			for (int i = 0, size = accountIds.size(); i < size; i++) {
				Integer accountId = accountIds.get(i);

				AccountBaseInfo target = accSer.getFromCacheById(accountId);
				if (Objects.nonNull(target.getFlag())
						&& Objects.equals(AccountFlag.REFUND.getTypeId(), target.getFlag())) {
					BizAccountMore more = CollectionUtils.isEmpty(accountMores) ? null : accountMores.get(accountId);
					if (more == null) {
						more = accMoreSer.getFromCacheByMobile(target.getMobile());
					}
					log.debug("账号id:{},是返利网:{},根据accountmore 获取linelimit值 ：{}", target.getId(), target.getFlag(),
							more != null ? more.getLinelimit() : "more空");

					int ret = Objects.isNull(more) || Objects.isNull(more.getLinelimit()) ? 0
							: more.getLinelimit().intValue();
					if (Objects.nonNull(more) && Objects.nonNull(more.getTmpMargin())) {
						ret = ret + more.getTmpMargin().intValue();
						log.debug("账号id:{}  TmpMargin() :{}   Linelimit+TmpMargin ：{} ", target.getId(),
								more.getTmpMargin(), ret);
					}
					if (target.checkMobile() && Objects.equals(target.getType(), AccountType.InBank.getTypeId())) {
						log.debug("账号id :{} 返利网 云闪付或者可以出款的卡( 边入边出、先入后出入款卡)可以超过信用额度百分比 ACCOUNT_INCOME_OVER_CREDIT ",
								target.getId());
						ret = ret + ret * CommonUtils.getAccountInComeOverCredit() / 100;
					}
					res.put(accountId, ret);
				} else {
					if (target.getPeakBalance() != null && target.getPeakBalance() > 0) {
						log.debug("非返利网账号id:{},获取 peakbalance:{}", target.getId(), target.getPeakBalance());
						res.put(accountId, target.getPeakBalance());
					} else {
						Integer ret = target.getLimitBalance() != null && target.getLimitBalance() > 0
								? target.getLimitBalance()
								: 12000;
						log.debug("非返利网账号id:{},获取 LimitBalance:{},返回值:{} ", target.getId(), target.getLimitBalance(),
								ret);
						res.put(accountId, ret);
					}
				}
			}
		}
		return res;
	}

	@Override
	public Integer creditLimit(AccountBaseInfo target) {
		if (Objects.isNull(target))
			return 0;
		if (Objects.nonNull(target.getFlag()) && Objects.equals(AccountFlag.REFUND.getTypeId(), target.getFlag())) {
			BizAccountMore more = accMoreSer.getFromCacheByMobile(target.getMobile());
			return Objects.isNull(more) || Objects.isNull(more.getMargin()) ? 0 : more.getMargin().intValue();
		}
		if (target.getPeakBalance() != null && target.getPeakBalance() > 0)
			return target.getPeakBalance();
		return target.getLimitBalance() != null && target.getLimitBalance() > 0 ? target.getLimitBalance() : 12000;
	}

	@Override
	public Integer currCredits(AccountBaseInfo target) {
		if (Objects.isNull(target)) {
			return 0;
		}
		Integer margin = margin(target);
		if (target.checkMobile() && Objects.equals(target.getType(), AccountType.InBank.getTypeId())) {
			log.debug("id :{} 返利网 云闪付或者可以出款的卡 边入边出、先入后出入款卡超过信用额度百分比 ACCOUNT_INCOME_OVER_CREDIT ", target.getId());
			margin = margin + margin * CommonUtils.getAccountInComeOverCredit() / 100;
		}
		if (margin == null) {
			log.debug("margin 值 为空 id:{}", target.getId());
			return 0;
		}

		// 当卡当日收款大于入款限额-5000时，当前可用额度返回0，不进行收款
		if (Objects.equals(target.getType(), AccountType.InBank.getTypeId()) && !allTransSer.checkDailyIn(target)) {
			log.info("currCredits>>id {},收款大于入款限额-5000,不再收款", target.getId());
			return 0;
		}

		// 出款卡出款限额 小于 当日出款 + 信用额度时，返回当前额度为0
		BigDecimal realBal = allTransSer.getCurrBalance(target.getId());
		Float OutDaily = accSer.findAmountDailyByTotal(1, target.getId()).floatValue();
		Integer limitOut = target.getLimitOut();
		if (limitOut != null) {
			if (target.getLimitOut() <= OutDaily + realBal.intValue()) {
				log.debug("当日累计出款 + 当前余额 已经超过日出款限额，当前额度为0 id:{}", target.getId());
				return 0;
			}
		} else {
			String proVal = MemCacheUtils.getInstance().getSystemProfile()
					.get(UserProfileKey.OUTDRAW_LIMIT_CHECKOUT_TODAY.getValue());
			if (StringUtils.isNotBlank(proVal)) {
				limitOut = Integer.parseInt(proVal);
			} else {
				limitOut = Integer.MAX_VALUE;
			}
		}
		BigDecimal occuLimit = getOccuLimit(target.getId());
		int credit;
		if (realBal == null) {
			log.debug("真实余额为空 margin:{},occuLimit:{}, id:{}", margin, occuLimit, target.getId());
			credit = margin - occuLimit.intValue();
			return credit <= 0 ? 0 : credit;
		}
		log.debug("获取可用额度:账号:{},可用额度:{}", target.getAccount(), (margin - realBal.intValue() - occuLimit.intValue()));
		credit = margin - realBal.intValue() - occuLimit.intValue();
		credit = Math.min(credit, limitOut - OutDaily.intValue() - realBal.intValue());
		return credit <= 0 ? 0 : credit;
	}

	/**
	 * k:账号id v:信用额度值
	 * 
	 * @return
	 */
	@Override
	public Map<Integer, Integer> allCurrCredits(List<Integer> accountIds) {

		Map<Integer, Integer> allCurrCredits = Maps.newLinkedHashMap();
		Map<Integer, Integer> allMargin = allMargin(accountIds);
		if (CollectionUtils.isEmpty(allMargin)) {
			log.debug("allMargin 值 为空 ");
			return allCurrCredits;
		}
		Map<Integer, BigDecimal> amountDailyOut = accSer.allAmountDailyTotal(1, accountIds);
		Map<Integer, BigDecimal> amountDailyIn = accSer.allAmountDailyTotal(0, accountIds);
		// 当卡当日收款大于入款限额-5000时，当前可用额度返回0，不进行收款
		Map<Integer, BigDecimal> allCurrBalance = allTransSer.allCurrBalance(accountIds);
		Map<Integer, BigDecimal> allOccuLimit = allOccuLimit(accountIds);
		for (Map.Entry<Integer, Integer> entry : allMargin.entrySet()) {
			Integer accountId = entry.getKey();
			Integer margin = entry.getValue();
			if (margin == null) {
				log.debug("账号id:{} margin为空:{} !", accountId, margin);
				allCurrCredits.put(accountId, 0);
				continue;
			}
			AccountBaseInfo target = accSer.getFromCacheById(accountId);
			if (null == target) {
				continue;
			}
			if (Objects.equals(target.getType(), AccountType.InBank.getTypeId())
					&& !allTransSer.checkDailyIn2(amountDailyIn, target)) {
				log.debug("currCredits>>id {},收款大于入款限额-5000,不再收款", target.getId());
				allCurrCredits.put(accountId, 0);
				continue;
			}

			// 出款卡出款限额 小于 当日出款 + 信用额度时，返回当前额度为0
			BigDecimal realBal = null;
			if (!CollectionUtils.isEmpty(allCurrBalance)) {
				realBal = allCurrBalance.get(target.getId());
			}
			if (realBal == null) {
				realBal = allTransSer.getCurrBalance(target.getId());
			}

			Float OutDaily = null;
			if (!CollectionUtils.isEmpty(amountDailyOut)) {
				BigDecimal amountDailyOut2 = amountDailyOut.get(accountId);
				if (amountDailyOut2 != null) {
					OutDaily = amountDailyOut2.floatValue();
				}
			}
			if (OutDaily == null) {
				BigDecimal amountDailyOut2 = accSer.findAmountDailyByTotal(1, target.getId());
				if (null != amountDailyOut2) {
					OutDaily = amountDailyOut2.floatValue();
				}
			}
			OutDaily = OutDaily == null ? 0.0F : OutDaily;

			Integer limitOut = target.getLimitOut();
			if (limitOut != null) {
				if (target.getLimitOut() <= OutDaily + realBal.intValue()) {
					log.debug("当日累计出款 + 当前余额 已经超过日出款限额，当前额度为0 id:{}", target.getId());
					allCurrCredits.put(accountId, 0);
					continue;
				}
			} else {
				String proVal = MemCacheUtils.getInstance().getSystemProfile()
						.get(UserProfileKey.OUTDRAW_LIMIT_CHECKOUT_TODAY.getValue());
				if (StringUtils.isNotBlank(proVal)) {
					limitOut = Integer.parseInt(proVal);
				} else {
					limitOut = Integer.MAX_VALUE;
				}
			}

			BigDecimal occuLimit = null;// getOccuLimit(target.getId());
			if (!CollectionUtils.isEmpty(allOccuLimit)) {
				occuLimit = allOccuLimit.get(accountId);
			}
			if (occuLimit == null) {
				occuLimit = getOccuLimit(target.getId());
			}
			occuLimit = occuLimit == null ? BigDecimal.ZERO : occuLimit;

			int credit;
			if (realBal == null) {
				log.debug("真实余额为空 margin:{},occuLimit:{}, id:{}", margin, occuLimit, target.getId());
				credit = margin - occuLimit.intValue();
				allCurrCredits.put(accountId, credit <= 0 ? 0 : credit);
				continue;
				// return credit <= 0 ? 0 : credit;
			}
			log.debug("获取可用额度:账号:{},可用额度:{}", target.getAccount(),
					(margin - realBal.intValue() - occuLimit.intValue()));
			credit = margin - realBal.intValue() - occuLimit.intValue();
			credit = Math.min(credit, limitOut - OutDaily.intValue() - realBal.intValue());
			allCurrCredits.put(accountId, credit <= 0 ? 0 : credit);
			// return credit <= 0 ? 0 : credit;
		}
		return allCurrCredits;
	}

	@Override
	public Integer peakBalance(AccountBaseInfo target) {
		if (Objects.isNull(target)) {
			return 0;
		}
		if (Objects.nonNull(target.getFlag()) && Objects.equals(AccountFlag.REFUND.getTypeId(), target.getFlag())) {
			BizAccountMore more = accMoreSer.getFromCacheByMobile(target.getMobile());
			return Objects.isNull(more) || Objects.isNull(more.getMargin()) ? 0 : more.getMargin().intValue();
		}
		if (target.getPeakBalance() != null && target.getPeakBalance() > 0)
			return target.getPeakBalance();
		return 0;
	}

	@Override
	public void monitor(CabanaStatus status) {
		Integer monitorId = status.getId();
		if (allInAccSer.checkHostRunRight()) {
			CACHE_ACC_STATUS.put(monitorId, status);
		}
		if (DUPLICATE_ACC_STATUS.contains(monitorId) || !allInAccSer.checkHostRunRight())
			return;
		DUPLICATE_ACC_STATUS.add(monitorId);
		QUENE_ACC_ID.add(monitorId);
		if (AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_CHANGE)) {
			try {
				THREAD_ACC_CHANGE.interrupt();
			} catch (Exception e) {
				log.error("Error,stop thread for account change. id:{} ", monitorId);
			} finally {
				THREAD_ACC_CHANGE = null;
			}
			return;
		} else if (Objects.nonNull(THREAD_ACC_CHANGE) && THREAD_ACC_CHANGE.isAlive()) {
			log.trace("thread for account change already exist. id:{}", monitorId);
			return;
		}
		THREAD_ACC_CHANGE = new Thread(() -> {
			for (;;) {
				Integer st = null;
				try {
					st = QUENE_ACC_ID.poll();
					if (Objects.isNull(st)) {
						if (!AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_CHANGE)) {
							try {
								Thread.sleep(2000);
							} catch (Exception e) {
								log.error("Thread for account change Exception.");
							}
							continue;
						} else {
							break;
						}
					}
					CabanaStatus accStatus = CACHE_ACC_STATUS.getIfPresent(st);
					if (accStatus != null) {
						compute(accStatus);
					}
				} catch (Exception e) {
					log.error("Thread for account change Exception. ", e);
				} finally {
					if (Objects.nonNull(st))
						DUPLICATE_ACC_STATUS.remove(st);
				}
			}
		});
		THREAD_ACC_CHANGE.setName("THREAD_ACC_CHANGE");
		THREAD_ACC_CHANGE.start();
	}

	@Override
	public BigDecimal buildRelBal(Integer accId) {
		BigDecimal ret = allTransSer.buildRealBalInCache(accId);
		if (Objects.isNull(ret)) {
			BizAccount acc = accSer.getById(accId);
			ret = Objects.nonNull(acc) ? acc.getBankBalance() : BigDecimal.ZERO;
		}
		return Objects.isNull(ret) ? BigDecimal.ZERO : ret;
	}

	public ChgAcc buildChgAcc(Set<String> keys) {
		Iterator<String> it = keys.iterator();
		return it.hasNext() ? new ChgAcc(it.next()) : null;
	}

	public void firstUseToLogin(String mobile, Integer accId, long curMils) {
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Long expire = CommonUtils.getExpireTime4AmountDaily();
		String msg = ChgAcc.genMsg(mobile, accId, ChgAcc.STATUS_NORMAL, 1, 0, null, curMils, expire);
		template.boundHashOps(RedisKeys.ACC_CHG).put(mobile, msg);
		redisSer.getFloatRedisTemplate().boundHashOps(RedisKeys.COUNT_FAILURE_TRANS).delete(accId.toString());
		log.info("AccountChange ( {} ) >> fist use too to login in every day. msg: {}", accId, msg);
	}

	/**
	 * 将数据加到占用信用额度的集合中
	 *
	 * @param handicapCode
	 * @param accounts
	 * @param amount
	 */
	public void addToOccuCredits(Integer handicapCode, List<String> accounts, Number amount) {
		int expire = CommonUtils.getAccountInComeExpireTime() * 60 * 1000;
		long currTime = System.currentTimeMillis();
		log.debug("addToOccuCredits>>分配成功以后，增加到临时占用额度中 handicapCode {},accounts {},amount {},expireTime {}",
				handicapCode, accounts, amount, expire);
		if (expire != 0 && handicapCode != null && !CollectionUtils.isEmpty(accounts) && amount != null) {
			BizHandicap handicap = handicapService.findFromCacheByCode(handicapCode.toString());
			BigDecimal ab = (BigDecimal) amount;
			if (handicap != null) {
				for (String account : accounts) {
					AccountBaseInfo base = accSer.getFromCacheByHandicapIdAndAccount(handicap.getId(), account);
					if (base != null) {
						try {
							Object obj = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_AMOUNT)
									.get(base.getId().toString());
							List<AccountIncome> list = null;
							if (Objects.nonNull(obj)) {
								list = JSON.parseArray((String) obj, AccountIncome.class);
								list = list.stream().filter(p -> currTime - p.getIncomeTime() < expire)
										.collect(Collectors.toList());
							}
							if (list == null) {
								list = new ArrayList<>();
							}
							AccountIncome income = new AccountIncome(base.getId(), currTime,
									ab.setScale(2, BigDecimal.ROUND_HALF_UP));
							list.add(income);
							redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_AMOUNT)
									.put(base.getId().toString(), JSON.toJSON(list).toString());
							log.debug("addToOccuCredits>>分配成功以后，增加到临时占用额度中 handicapCode {},account {},amount {}",
									handicapCode, account, amount);
						} catch (Exception e) {
							log.error("addToOccuCredits>>分配成功以后，增加到临时占用额度失败 handicapCode {},account {},amount {}",
									handicapCode, account, amount);
						}
					}
				}
			}
		}
	}

	/**
	 * 对账确认：流水上报时间在对账指令发送时间的2分钟之前，相应的对账确认成功，或对账过期时间小于当前时间，删除缓存中的数据
	 * 
	 * @param logs
	 */
	public void ackReConciliate(String logs) {
		try {
			ToolResponseData data = mapper.readValue(logs, ToolResponseData.class);
			if (Objects.nonNull(data) && !CollectionUtils.isEmpty(data.getBanklogs())) {
				Integer accId = data.getBanklogs().get(0).getFromAccount();
				Long lastTime = ACC_RECONCILIATE_STATUS.get(accId);
				if (lastTime != null && (lastTime - RECONCILIATE_EXPIRE < System.currentTimeMillis() - 2 * 60 * 1000
						|| lastTime < System.currentTimeMillis())) {
					ACC_RECONCILIATE_STATUS.remove(accId);
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	// @Transactional
	public BizAccountMore calculateMoreLineLimit(AccountBaseInfo onlineRuningAccount) {
		if (Objects.isNull(onlineRuningAccount) || StringUtils.isBlank(onlineRuningAccount.getMobile())
				|| !onlineRuningAccount.checkMobile())
			return null;
		BizAccountMore more = accMoreSer.getFromCacheByMobile(onlineRuningAccount.getMobile());
		if (Objects.isNull(more) || !Objects.equals(AccountFlag.REFUND.getTypeId(), more.getClassify()))
			return more;
		if (StringUtils.isBlank(more.getAccounts()) || Objects.isNull(more.getMargin()))
			return more;
		if (Objects.equals(onlineRuningAccount.getStatus(), AccountStatus.Inactivated.getStatus())
				&& (onlineRuningAccount.getPeakBalance() == null
						|| onlineRuningAccount.getPeakBalance().floatValue() <= 0))
			return more;
		BigDecimal ret = BigDecimal.ZERO;
		List<Integer> ids = Stream.of(StringUtils.trimToEmpty(more.getAccounts()).split(","))
				.filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(ids))
			return more;
		for (Integer id : ids) {
			if (!Objects.equals(onlineRuningAccount.getId(), id))
				ret = ret.add(buildRelBal(id));
		}
		ret = more.getMargin().subtract(ret);
		if (!Objects.equals(ret, more.getLinelimit())) {
			more.setLinelimit(ret);
			more = accMoreSer.saveAndFlash(more);
		}
		return more;
	}

	@Override
	// @Transactional
	public BizAccountMore calculateMoreLineLimit(BizAccountMore more, BigDecimal difference) {
		if (Objects.isNull(more) || Objects.isNull(difference))
			return more;
		BigDecimal lineLimit = more.getLinelimit();
		if (Objects.isNull(lineLimit))
			lineLimit = BigDecimal.ZERO;
		lineLimit = lineLimit.add(difference);
		more.setLinelimit(lineLimit);
		return accMoreSer.saveAndFlash(more);
	}

	private static final Integer Normal = AccountStatus.Normal.getStatus(), Enabled = AccountStatus.Enabled.getStatus();

	private static final Integer OutBank = AccountType.OutBank.getTypeId(),
			BindCommon = AccountType.BindCommon.getTypeId(), InBank = AccountType.InBank.getTypeId();

	private void compute(CabanaStatus st) {
		Integer accId = st.getId();
		AccountBaseInfo target = accSer.getFromCacheById(accId);
		if (Objects.isNull(target) || StringUtils.isBlank(target.getMobile()) || !target.checkMobile()
				|| !Objects.equals(target.getFlag(), AccountFlag.REFUND.getTypeId())
						&& !Objects.equals(target.getFlag(), AccountFlag.SPARETIME.getTypeId())) {
			log.trace("AccountChange ( {} ) >> account doesn't exist | can't pass [CHECK MOBILE] | mobile is empty",
					accId);
			return;
		}
		BizAccountMore more = accMoreSer.getFromCacheByMobile(target.getMobile());
		if (Objects.equals(AccountFlag.SPARETIME.getTypeId(), target.getFlag()))
			createSpareTimeMore(target);
		if (Objects.isNull(more)) {
			log.trace("AccountChange ( {} ) >> account more doesn't exist.", accId);
			return;
		}
		long curMils = System.currentTimeMillis();
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		String mobile = target.getMobile();
		Boolean firstLogin = checkFirstLogin(mobile, accId);
		// 上报账号和登录的账号不一致，触发切换账号操作
		if (Objects.isNull(firstLogin)) {
			cabanaSer.updAcc(accId);
		} else if (firstLogin) {
			firstUseToLogin(mobile, accId, curMils);
		} else {
			String keys4tar = template.boundHashOps(RedisKeys.ACC_CHG).get(mobile).toString();
			ChgAcc chg = new ChgAcc(keys4tar);
			boolean checkBlack = transferSer.checkBlack(accId);
			boolean checkCntDaily = checkOutCntDaily(target),
					checkAmtDaily = !allTransSer.exceedAmountSumDailyOutward(target.getId()),
					checkAmtDailyIn = allTransSer.checkDailyIn(target);
			log.debug("AccountChange ( {} ) >>  checkCntDaily: {} checkAmtDaily: {} checkAmtDailyIn：{}  result: {}",
					accId, checkCntDaily, checkAmtDaily, checkAmtDailyIn,
					(checkCntDaily && checkAmtDaily && checkAmtDailyIn));
			// 日出款笔数、日出款限额、入款限额都未达到，不用切换账号
			if (checkCntDaily && checkAmtDaily && checkAmtDailyIn) {
				String msg = ChgAcc.genMsg(mobile, accId, ChgAcc.STATUS_NORMAL, chg.getTotal(), chg.getNextAcc(),
						chg.getUsedAcc(), chg.getChgTm(), chg.getExpTm());
				if (!checkBlack && Objects.equals(target.getStatus(), Normal))
					if (transferSer.blackCanDel(accId)) {
						transferSer.rmFrBlackList(accId);
					} else {
						log.info("AccountChange ( {} ) >>  NORMAL .黑名单不能被自动删除", accId);
					}
				if (chg.getNextAcc() == null || chg.getNextAcc() == 0) {
					calMargin(target, true, true);
					String msg1 = ChgAcc.genMsg(chg.getMobile(), chg.getCurrAcc(), chg.getStatus(), chg.getTotal(), 1,
							chg.getUsedAcc(), chg.getChgTm(), chg.getExpTm());
					template.boundHashOps(RedisKeys.ACC_CHG).put(mobile, msg1);
				} else if (Objects.equals(target.getStatus(), Enabled))
					chgSt(target, Normal);
				log.info("AccountChange ( {} ) >>  NORMAL . msg: {}", accId, msg);
				// 检测当前上报的卡对应的返利网账号下是否还有其他卡状态为在用的状态，如果有切换为可用
				String[] accounts = more.getAccounts().split(",");
				for (String str : accounts) {
					if (StringUtils.isNotBlank(str) && !Objects.equals(Integer.parseInt(str), target.getId())) {
						AccountBaseInfo base = accSer.getFromCacheById(Integer.parseInt(str));
						if (base == null || !Objects.equals(base.getStatus(), Normal)) {
							continue;
						}
						try {
							BizAccount account = accSer.getById(Integer.parseInt(str));
							BizAccount oldAccount = new BizAccount();
							BeanUtils.copyProperties(oldAccount, account);
							account.setStatus(AccountStatus.Enabled.getStatus());
							account.setUpdateTime(new Date());
							accSer.updateBaseInfo(account);
							accountExtraService.saveAccountExtraLog(oldAccount, account, "系统管理员");
							accSer.broadCast(account);
							hostMonitorService.update(account);
							log.info("卡切换时系统自动修改账号:{}下其他卡的状态为可用,修改的卡ID：{}", more.getUid(), str);
						} catch (Exception e) {
							log.error("卡切换自动修改账号下其他卡为可用状态时异常：{}", e.getMessage());
						}
					}
				}
				return;
			}
			log.info("AccountChange ( {} ) >>  checkCntDaily: {} checkAmtDaily: {} checkAmtDailyIn：{}  result: {}",
					accId, checkCntDaily, checkAmtDaily, checkAmtDailyIn,
					(checkCntDaily && checkAmtDaily && checkAmtDailyIn));
			// 增加到黑名单
			if (checkBlack)
				transferSer.addToBlackList(accId, true);
			BigDecimal bal = buildRelBal(accId);
			int balInt = bal.intValue();
			int type = target.getType();
			if (type == BindCommon && CommonUtils.getTriggerClearBalance4BindCommon() < balInt
					|| type != BindCommon && CommonUtils.getTriggerClearBalance4OtherCard() < balInt) {
				// 加到出款、下发优先集合中
				if (type == OutBank || type == InBank) {
					redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.OUTWARD_HIGH_PRIORITY).put(accId + "",
							balInt + "");
				} else {
					redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ISSUED_HIGH_PRIORITY).put(accId + "",
							balInt + "");
				}
				/**
				 * if (OutBank == type) { BizOutwardTask task = oTaskDao.applyTask(accId,
				 * OutwardTaskStatus.Undeposit.getStatus()); if (Objects.isNull(task))
				 * allTransSer.allocByAccAndBal(target, 20, balInt - 20); } else {
				 * TransferEntity trans = allTransSer.applyTrans(accId, false, 0F, false); if
				 * (Objects.isNull(trans)) allTransSer.allocByAccAndBal(target, balInt - 100,
				 * balInt - 50); }
				 **/
				return;
			}
			switch (chg.getStatus()) {
			case ChgAcc.STATUS_NORMAL:
			case ChgAcc.STATUS_SUSPEND_DISBURSE_TO_MEMBER:
				if (checkConciliate(target, chg)) {
					chg.setChgTm(curMils);
					String msg = ChgAcc.genMsg(mobile, accId, ChgAcc.STATUS_FINISH_CONCILIATE, chg.getTotal(),
							chg.getNextAcc(), chg.getUsedAcc(), chg.getChgTm(), chg.getExpTm());
					template.boundHashOps(RedisKeys.ACC_CHG).put(mobile, msg);
				}
				break;
			case ChgAcc.STATUS_FINISH_CONCILIATE:
				String[] accounts = more.getAccounts().split(",");
				log.debug("STATUS_FINISH_CONCILIATE accounts:{}", accounts.toString());
				Integer next = null;
				// 查看用户下是否存在可以切换的卡
				for (String str : accounts) {
					log.debug("STATUS_FINISH_CONCILIATE str:{}", str);
					if (StringUtils.isNotBlank(str) && !Objects.equals(Integer.parseInt(str), target.getId())) {
						AccountBaseInfo base = accSer.getFromCacheById(Integer.parseInt(str));
						if (log.isDebugEnabled()) {
							log.debug(
									"STATUS_FINISH_CONCILIATE status:{} checkOutCntDaily:{} exceedAmountSumDailyOutward:{}  checkDailyIn:{}",
									base.getStatus(), checkOutCntDaily(base),
									!allTransSer.exceedAmountSumDailyOutwardNew(base.getId()),
									allTransSer.checkDailyIn(base));
						}
						if ((Objects.equals(base.getStatus(), Normal) || Objects.equals(base.getStatus(), Enabled))
								&& checkOutCntDaily(base) && !allTransSer.exceedAmountSumDailyOutwardNew(base.getId())
								&& allTransSer.checkDailyIn(base)) {
							next = Integer.parseInt(str);
							break;
						}
					}
				}
				log.debug("STATUS_FINISH_CONCILIATE next:{}", next);
				if (next != null) {
					if (chgSt(accSer.getFromCacheById(next), Normal)) {
						firstUseToLogin(mobile, next, curMils);
						chgSt(target, Enabled);
					}
				} else {
					chgSt(target, Enabled);
				}
			}
		}
	}

	// 本方法只用于校验账号切换时：是否有未匹配的流水或订单，是：false 否：true
	private boolean checkConciliate(AccountBaseInfo target, ChgAcc chg) {
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		if (existsTransFromOrTo(target, true) || existsTransFromOrTo(target, false)) {
			log.info("AccountChange ( {} ) >>  existsTransFromOrTo", chg.getCurrAcc());
			return false;
		}
		if (Objects.equals(ChgAcc.STATUS_NORMAL, chg.getStatus())) {
			chg.setChgTm(System.currentTimeMillis());
			String msg = ChgAcc.genMsg(chg.getMobile(), chg.getCurrAcc(), ChgAcc.STATUS_SUSPEND_DISBURSE_TO_MEMBER,
					chg.getTotal(), chg.getNextAcc(), chg.getUsedAcc(), chg.getChgTm(), chg.getExpTm());
			template.boundHashOps(RedisKeys.ACC_CHG).put(chg.getMobile(), msg);
			log.info("AccountChange ( {} ) >>  STATUS_SUSPEND_DISBURSE_TO_MEMBER . msg: {}", chg.getCurrAcc(), msg);
		}
		if (!checkReConciliate(target, chg)) {
			log.info("AccountChange ( {} ) >>  STATUS_SUSPEND_DISBURSE_TO_MEMBER . checkReConciliate false: {}",
					chg.getCurrAcc());
			return false;
		}
		// if (!systemAccountManager.check4AccountingIn(target.getId())
		// || !systemAccountManager.check4AccountingOut(target.getId())) {
		// log.info("AccountChange ( {} ) >> STATUS_SUSPEND_DISBURSE_TO_MEMBER
		// .SYSBAL|CHECK .", target.getId());
		// }
		/*
		 * check whether {@code target} exists unmatched bank statements and unmatched
		 * income request. {@code true} doesn't exists; {@code false} exists.
		 */
		// boolean[] statistic = new boolean[] { true };
		// String sql = String.format(FORMAT_SQL_CONCILIATE, target.getId(),
		// CommonUtils.getStartTimeOfCurrDay(),
		// target.getId(), CommonUtils.getStartTimeOfCurrDay());
		// enMgr.createNativeQuery(sql).getResultList()
		// .forEach(p -> statistic[0] = statistic[0] & (((BigInteger) ((Object[])
		// p)[1]).intValue() <= 0));
		// if (!statistic[0])
		Set<Integer> accException = systemAccountManager.accountingException();
		accException.addAll(systemAccountManager.accountingSuspend());
		if (accException.contains(target.getId())) {
			log.info("AccountChange ( {} ) >>  STATUS_SUSPEND_DISBURSE_TO_MEMBER .isException：{}", target.getId(),
					false);
			return false;
		}
		return true;
	}

	// 日出款笔数校验，未设置日出款笔数或入款卡通过校验
	private boolean checkOutCntDaily(AccountBaseInfo target) {
		if (Objects.isNull(target) || Objects.isNull(target.getLimitOutCount()) || target.getLimitOutCount() <= 0
				|| Objects.equals(target.getType(), InBank))
			return true;
		Float count = accSer.findOutCountDaily(target.getId());
		return Objects.isNull(count) || count <= target.getLimitOutCount();
	}

	// 日出款金额校验，未设置日出款金额或入款卡通过校验
	private boolean checkOutAmtDaily(AccountBaseInfo target) {
		if (Objects.isNull(target) || Objects.isNull(target.getLimitOut()) || target.getLimitOut() <= 0
				|| Objects.equals(target.getType(), InBank))
			return true;
		float outDaily = accSer.findAmountDailyByTotal(1, target.getId()).floatValue();
		return outDaily <= (target.getLimitOut() - TOLERANCE_OUT_AMT_DAILY);
	}

	private static final String FMT_ACCLIST4MOBILE = "select id from biz_account where mobile='%s' and status in (%d,%d) and flag in (1,2)";

	@SuppressWarnings("unchecked")
	private List<Integer> buildAccList4Mobile(String mobile) {
		return (List<Integer>) enMgr.createNativeQuery(String.format(FMT_ACCLIST4MOBILE, mobile, Normal, Enabled))
				.getResultList();
	}

	private void calMargin(AccountBaseInfo target, boolean checkCnt, boolean checkAmt) {
		if (Objects.isNull(target) || StringUtils.isBlank(target.getMobile()) || !target.checkMobile())
			return;
		BizAccountMore more = accMoreSer.getFromCacheByMobile(target.getMobile());
		if (Objects.isNull(more) || !Objects.equals(AccountFlag.REFUND.getTypeId(), more.getClassify()))
			return;
		BigDecimal ret = BigDecimal.ZERO;
		if (checkCnt && checkAmt && StringUtils.isNotBlank(more.getAccounts()) && Objects.nonNull(more.getMargin())) {
			for (String id : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
				if (StringUtils.isBlank(id) || !StringUtils.isNumeric(id))
					continue;
				Integer idInt = Integer.valueOf(id);
				if (Objects.equals(target.getId(), idInt)
						|| (Objects.equals(target.getStatus(), AccountStatus.Inactivated.getStatus())
								&& (target.getPeakBalance() == null || target.getPeakBalance().floatValue() <= 0)))
					continue;
				ret = ret.add(buildRelBal(idInt));
			}
			ret = more.getMargin().subtract(ret);
		}
		if (!Objects.equals(ret, more.getLinelimit())) {
			more.setLinelimit(ret);
			accMoreSer.saveAndFlash(more);
			log.info("this user calMargin is accId:{}", target.getId());
		}
	}

	/**
	 * change account {@code base} status {@code toSt}
	 * 
	 * @param base
	 *            bank account base information.
	 * @param toSt
	 *            refer to {@link AccountStatus}, value only be
	 *            {@link AccountStatus#Normal} and {@link AccountStatus#Enabled}
	 * @return {@code true} : change successfully ,otherwise, {@code false}
	 */
	private boolean chgSt(AccountBaseInfo base, int toSt) {
		if (Objects.isNull(base)
				|| !Objects.equals(base.getStatus(), Normal) && !Objects.equals(base.getStatus(), Enabled))
			return false;
		if (Objects.equals(base.getStatus(), toSt))
			return true;
		// 切换成可用时，将优先级里面的数据删除掉
		if (Objects.equals(toSt, Enabled)) {
			redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.OUTWARD_HIGH_PRIORITY).delete(base.getId() + "");
			redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ISSUED_HIGH_PRIORITY).delete(base.getId() + "");
		}
		BizAccount vo = accSer.getById(base.getId());
		if (Objects.nonNull(vo)) {
			if (!Objects.equals(vo.getStatus(), toSt)) {
				vo.setStatus(toSt);
				vo.setHolder(null);
				accSer.updateBaseInfo(vo);
			}
			log.info("chgSt>> id {} base {} to status {}", base.getId(), base.toString(), toSt);
			accSer.broadCast(vo);
			cabanaSer.updAcc(base.getId());
		}
		return Objects.nonNull(vo);
	}

	/**
	 * add {@link BizAccountMore} for spare time
	 */
	private void createSpareTimeMore(AccountBaseInfo target) {
		if (Objects.isNull(target) || StringUtils.isBlank(target.getMobile()) || !target.checkMobile()
				|| !Objects.equals(AccountFlag.SPARETIME.getTypeId(), target.getFlag()))
			return;
		BizAccountMore more = accMoreSer.getFromCacheByMobile(target.getMobile());
		if (Objects.nonNull(more) && StringUtils.isNotBlank(more.getAccounts())
				&& more.getAccounts().contains(String.format(",%d,", target.getId())))
			return;
		BizAccount vo = accSer.getById(target.getId());
		if (Objects.nonNull(vo))
			accSer.saveRebateAcc(vo, more, target.getMobile(), AccountFlag.SPARETIME);
	}

	private BigDecimal getOccuLimit(Integer accid) {
		int expire = CommonUtils.getAccountInComeExpireTime() * 60 * 1000;
		if (expire == 0) {
			return BigDecimal.ZERO;
		}
		long currTime = System.currentTimeMillis();
		Object obj = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_AMOUNT).get(accid + "");
		if (Objects.nonNull(obj)) {
			List<AccountIncome> list = JSON.parseArray((String) obj, AccountIncome.class);
			double amount = list.stream().filter(p -> currTime - p.getIncomeTime() < expire)
					.mapToDouble(p -> p.getAmount().doubleValue()).sum();
			return BigDecimal.valueOf(amount);
		}
		return BigDecimal.ZERO;
	}

	private Map<Integer, BigDecimal> allOccuLimit(List<Integer> accountIds) {
		Assert.notNull(accountIds, "账号参数为空");
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		String key = RedisKeys.ACCOUNT_INCOME_AMOUNT;
		Map<Integer, BigDecimal> res = Maps.newLinkedHashMap();
		if (template.hasKey(key)) {
			HashOperations hashOperations = template.opsForHash();
			List list1 = hashOperations.multiGet(key,
					accountIds.stream().map(p -> p.toString()).collect(Collectors.toList()));
			if (!CollectionUtils.isEmpty(list1)) {
				int expire = CommonUtils.getAccountInComeExpireTime() * 60 * 1000;
				long currTime = System.currentTimeMillis();
				for (int i = 0, size = accountIds.size(); i < size; i++) {
					Integer accountId = accountIds.get(i);
					Object obj = list1.get(i);
					if (obj == null) {
						res.put(accountId, BigDecimal.ZERO);
					} else {
						BigDecimal amount = BigDecimal.ZERO;
						List<AccountIncome> list = JSON.parseArray(obj.toString(), AccountIncome.class);
						amount = list.parallelStream().filter(p -> currTime - p.getIncomeTime() < expire)
								.map(p -> p.getAmount()).reduce(amount, BigDecimal::add);
						res.put(accountId, amount);
					}
				}
			}
		}
		return res;
	}

	// 判断当前账号是否首次登录，redis中没有对应手机号的数据或对应的数据和当前Id不一致或当前数据已过期，为首次登录
	private Boolean checkFirstLogin(String mobile, Integer accId) {
		if (redisSer.getStringRedisTemplate().opsForHash().hasKey(RedisKeys.ACC_CHG, mobile)) {
			String chgStr = redisSer.getStringRedisTemplate().opsForHash().get(RedisKeys.ACC_CHG, mobile).toString();
			ChgAcc chgAcc = new ChgAcc(chgStr);
			if (!Objects.equals(chgAcc.getCurrAcc(), accId)) {
				AccountBaseInfo base = accSer.getFromCacheById(accId);
				if (!checkOutCntDaily(base) || allTransSer.exceedAmountSumDailyOutward(base.getId())
						|| !allTransSer.checkDailyIn(base)) {
					log.error("checkFirstLogin>>账号切换不成功！currAcc {} report Acc {}", chgAcc.getCurrAcc(), accId);
					return null;
				}
			}
			if (chgAcc.getExpTm() < System.currentTimeMillis()) {
				return true;
			}
			return false;
		} else {
			return true;
		}
	}

	private boolean existsTransFromOrTo(AccountBaseInfo target, boolean isFrom) {
		String key;
		if (isFrom) {
			key = RedisKeys.genPattern4TransferAccountLock_from(target.getId());
		} else {
			key = RedisKeys.genPattern4TransferAccountLock_to(target.getId());
		}
		Set<String> keys = redisSer.getStringRedisTemplate().keys(key);
		/*
		 * check whether {@code target} exists transfer tasks. {@code true} doesn't
		 * exists; {@code false} exists;
		 */
		for (String k : keys) {
			TransLock l = new TransLock(k);
			if (Objects.equals(TransLock.STATUS_ALLOC, l.getStatus())
					|| Objects.equals(TransLock.STATUS_CLAIM, l.getStatus())) {
				log.info("AccountChange ( {} ) >>  STATUS_SUSPEND_DISBURSE_TO_MEMBER . ALLOC|CLAIM .", target.getId());
				return true;
			}
		}
		return false;
	}

	// 校验账号是否完成对账或对账是否超时
	private boolean checkReConciliate(AccountBaseInfo target, ChgAcc chg) {
		if (Objects.equals(ChgAcc.STATUS_NORMAL, chg.getStatus())) {
			cabanaSer.conciliate(target.getId(), CommonUtils.getNowDate().substring(0, 10));
			ACC_RECONCILIATE_STATUS.put(target.getId(), System.currentTimeMillis() + RECONCILIATE_EXPIRE);
		}
		Long lastTime = ACC_RECONCILIATE_STATUS.get(target.getId());
		if (lastTime == null || lastTime < System.currentTimeMillis()) {
			if (lastTime != null)
				ACC_RECONCILIATE_STATUS.remove(target.getId());
			return true;
		}
		return false;
	}
}
