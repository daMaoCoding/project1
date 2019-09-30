package com.xinbo.fundstransfer.assign;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.entity.OutAccount;
import com.xinbo.fundstransfer.assign.entity.OutOnly;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateTransService;
import com.xinbo.fundstransfer.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 可用卡缓存信息
 * 
 * @author Administrator
 *
 */
@Slf4j
@Component
public class AvailableCardCache {
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private RedisService redisService;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private AccountFactory accountFactory;
	@Autowired
	private SystemAccountManager systemAccountManager;

	public Pair<List<OutAccount>, Integer> getOutAccountByCategory(Cache<Integer, Integer> atedTarget,
			String category) {
		// 2. 找出所有可用卡
		Pair<List<OutAccount>, Integer> accountPair = getOutAccount(atedTarget);
		if (accountPair == null) {
			return null;
		}
		Integer maxRound = accountPair.getRight();
		List<OutAccount> allOutAccount = accountPair.getLeft();
		if (allOutAccount.size() == 0) {
			return null;
		}
		Map<String, List<OutAccount>> outAccountMap = allOutAccount.stream().collect(Collectors.groupingBy(s -> {
			if (null != s) {
				OutAccount outAccount = (OutAccount) s;
				if (outAccount.getOutType() == Constants.TARGET_TYPE_USER
						|| outAccount.getOutType() == Constants.TARGET_TYPE_THIRD) {
					return outAccount.getZone() + "-" + outAccount.getManualOut();
				} else {
					return outAccount.getZone() + "-" + outAccount.getLevelId() + "-" + outAccount.getManualOut();
				}
			} else {
				return "0-0-0";
			}
		}));
		return Pair.of(outAccountMap.get(category), maxRound);
	}
	
	public List<OutAccount> getOutAccountByCategory(String category) {
		// 2. 找出所有可用卡
		Set<ZSetOperations.TypedTuple<String>> getAll = getFromOutBank();
		// 当不存在 被分配对象时：抛出特殊异常
		if (CollectionUtils.isEmpty(getAll) || getAll.size() == 1) {
			log.debug("没有出款卡");
			return null;
		}
		List<OutAccount> allOutAccount = new ArrayList<OutAccount>();
		ArrayList<ZSetOperations.TypedTuple<String>> sortedAll = new ArrayList<>(getAll);
		for (ZSetOperations.TypedTuple<String> get : sortedAll) {
			Optional<OutOnly> outAccountOpt = accountFactory.generateOutAccount(get);
			if (outAccountOpt.isPresent()) {
				OutAccount outAccount = outAccountOpt.get();
				if (outAccount.getOutType() == Constants.TARGET_TYPE_USER
						|| outAccount.getOutType() == Constants.TARGET_TYPE_THIRD
						|| outAccount.getAmount() < CommonUtils.getThirdToOutMoreBalance()) {
					continue;
				}
				allOutAccount.add(outAccount);
			}
		}
		if (allOutAccount.size() == 0) {
			return null;
		}
		Map<String, List<OutAccount>> outAccountMap = allOutAccount.stream().collect(Collectors.groupingBy(s -> {
			if (null != s) {
				OutAccount outAccount = (OutAccount) s;
				return outAccount.getZone() + "-" + outAccount.getLevelId();
			} else {
				return "0-0";
			}
		}));
		return outAccountMap.get(category);
	}

	private Pair<List<OutAccount>, Integer> getOutAccount(Cache<Integer, Integer> atedTarget) {
		List<OutAccount> allOutAccount = new ArrayList<OutAccount>();

		Set<ZSetOperations.TypedTuple<String>> getAll = getOutBank();
		// 当不存在 被分配对象时：抛出特殊异常
		if (CollectionUtils.isEmpty(getAll) || getAll.size() == 1) {
			log.debug("没有出款卡");
			return null;
		}
		int maxRound = 0;
		ArrayList<ZSetOperations.TypedTuple<String>> sortedAll = new ArrayList<>(getAll);
		Set<String> scores = redisService.getStringRedisTemplate().boundZSetOps(Constants.OUT_ACCOUNT_ORDERED).range(0,
				-1);
		if (!CollectionUtils.isEmpty(scores)) {
			ArrayList<String> sortedScores = new ArrayList<>(scores);
			/**
			 * sortedAll.sort(Comparator.comparing(l ->
			 * sortedScores.indexOf((l.getValue().split(":"))[1]), (s1, s2) -> { return
			 * s1.compareTo(s2); }));
			 **/
			Double score = redisService.getStringRedisTemplate().boundZSetOps(Constants.OUT_ACCOUNT_ORDERED)
					.score(sortedScores.get(sortedScores.size() - 1));
			if (score == null) {
				score = 0d;
			}
			int bankTypeRound = score.intValue();
			maxRound = bankTypeRound > 10 ? Integer.parseInt((bankTypeRound + "").substring(1)) : 0;
		}
		// 如果轮次太大（超过1000），就从0开始计算轮次
		if (maxRound > 1000) {
			log.debug("allocate4Out>> maxRound more than 1000,set to zero");
			redisService.getStringRedisTemplate().delete(Constants.OUT_ACCOUNT_ORDERED);
			maxRound = 0;
		}
		for (ZSetOperations.TypedTuple<String> get : sortedAll) {
			Integer accountId = Integer.parseInt((get.getValue().split(":"))[1]);
			if (atedTarget.getIfPresent(accountId) != null) {
				continue;
			}
			Optional<OutAccount> outAccountOpt = accountFactory.generateOutAccount4Out(get);
			if (outAccountOpt.isPresent()) {
				OutAccount outAccount = outAccountOpt.get();
				if (outAccount.getAmount() < 100) {
					continue;
				}
				allOutAccount.add(outAccount);
			}
		}
		return Pair.of(allOutAccount, maxRound);
	}

	public Set<ZSetOperations.TypedTuple<String>> getOutBank() {
		return cardCache.getUnchecked("OUTBANK");
	}
	
	public Set<ZSetOperations.TypedTuple<String>> getFromOutBank() {
		return cardCache.getUnchecked("FROM_OUTBANK");
	}

	// --------------- 以下为可用卡缓存信息 ------------------------
	private LoadingCache<String, Set<ZSetOperations.TypedTuple<String>>> cardCache = CacheBuilder.newBuilder()
			.concurrencyLevel(1).refreshAfterWrite(3, TimeUnit.SECONDS) // 每3秒自动刷新
			.build(new CacheLoader<String, Set<ZSetOperations.TypedTuple<String>>>() {
				public Set<ZSetOperations.TypedTuple<String>> load(String key) {
					log.info("开始定时刷新分配目标缓存对象");
					// 未匹配流水告警数据
					// Set<Integer> unMatched = buildAllUnMatched();
					// log.trace("cache unMatched>> {}", unMatched);
					// 银行余额上报有效时间
					Set<Integer> accIdSetInValTm = allocateTransService.buildValidAcc();
					log.trace("cache accIdSetInValTm>> {}", accIdSetInValTm);
					// 出款频率限制
					Set<Integer> idsByNonExprie = buildIdByNonExprie();
					log.trace("cache idsByNonExprie>> {}", idsByNonExprie);
					// 转出限制
					// Set<Integer> invSet = sysBalService.alarm(true);
					// log.trace("cache invSet>> {}", invSet);
					// 当日出款限额
					Map<String, Float> outCtnDaily = accountService.findOutCountDaily();
					// 连续转账失败
					Set<Integer> failure = allocateTransService.buildFailureTrans();
					log.trace("cache outCtnDaily>> {}", outCtnDaily);
					// 账号设备告警
					Map<Object, Object> deviceAlarm = redisService.getStringRedisTemplate()
							.boundHashOps(RedisKeys.PROBLEM_ACC_ALARM).entries();
					// 对账异常账号
					Set<Integer> accountingException = systemAccountManager.accountingException();
					accountingException.addAll(systemAccountManager.accountingSuspend());
					log.debug("cache accountingException>> {}", accountingException);
					if ("OUTBANK".equals(key)) {
						Map<Object, Object> outwardPriority = redisService.getStringRedisTemplate()
								.boundHashOps(RedisKeys.OUTWARD_HIGH_PRIORITY).entries();
						// 计算可用卡：1.状态正常，2.不在黑名单中, 3...
						Set<ZSetOperations.TypedTuple<String>> getAll = redisService.getStringRedisTemplate()
								.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).rangeWithScores(0, -1);
						getAll = getAll.stream().filter(p -> checkInvokeTm(
								accountService.getFromCacheById(Integer.parseInt(p.getValue().split(":")[1])),
								accIdSetInValTm)
								&& checkNonExpire(idsByNonExprie, Integer.parseInt(p.getValue().split(":")[1]))
								&& (outwardPriority.containsKey(p.getValue().split(":")[1]) || (checkOutCtnDaily(
										accountService.getFromCacheById(Integer.parseInt(p.getValue().split(":")[1])),
										outCtnDaily)
										&& checkDailyOut(accountService
												.getFromCacheById(Integer.parseInt(p.getValue().split(":")[1])))))
								&& checkDeviceAlarm(deviceAlarm, p.getValue().split(":")[1])
								&& allocateTransService.checkFailureTrans(failure,
										Integer.parseInt(p.getValue().split(":")[1]))
								&& (checkNull(Integer.parseInt(p.getValue().split(":")[1]))
										|| checkMaintain(Integer.parseInt(p.getValue().split(":")[1])))
								&& (!accountingException.contains(Integer.parseInt(p.getValue().split(":")[1]))))
								.collect(Collectors.toSet());
						return getAll;
					} else if ("FROM_OUTBANK".equals(key)) {
						Map<Object, Object> issuedPriority = redisService.getStringRedisTemplate()
								.boundHashOps(RedisKeys.ISSUED_HIGH_PRIORITY).entries();
						// 计算可用卡：1.状态正常，2.不在黑名单中, 3...
						Double[] outCriteria = new Double[] { enScore4Fr(Constants.OUTBANK, 0, 0, 0, 0),
								enScore4Fr(Constants.OUTBANK + 1, 0, 0, 0, 0) };
						StringRedisTemplate template = redisService.getStringRedisTemplate();
						Set<ZSetOperations.TypedTuple<String>> getAll = template
								.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
								.rangeByScoreWithScores(outCriteria[0], outCriteria[1]);
						getAll = getAll.stream().filter(p -> checkInvokeTm(
								accountService.getFromCacheById(Integer.parseInt(p.getValue())), accIdSetInValTm)
								&& checkNonExpire(idsByNonExprie, Integer.parseInt(p.getValue()))
								&& (issuedPriority.containsKey(p.getValue()) || (checkOutCtnDaily(
										accountService.getFromCacheById(Integer.parseInt(p.getValue())), outCtnDaily)
										&& checkDailyOut(
												accountService.getFromCacheById(Integer.parseInt(p.getValue())))))
								&& checkDeviceAlarm(deviceAlarm, p.getValue())
								&& allocateTransService.checkFailureTrans(failure, Integer.parseInt(p.getValue()))
								&& (checkNull(Integer.parseInt(p.getValue()))
										|| checkMaintain(Integer.parseInt(p.getValue())))
								&& (!accountingException.contains(Integer.parseInt(p.getValue()))))
								.collect(Collectors.toSet());
						return getAll;
					}
					return null;
				}
			});

	public String statusReason(Integer accountId) {
		StringBuilder reason = new StringBuilder();
		// 银行余额上报有效时间
		Set<Integer> accIdSetInValTm = allocateTransService.buildValidAcc();
		if (!accIdSetInValTm.contains(accountId)) {
			reason.append("余额上报时间失效");
		}
		
		Set<Integer> accountingException = systemAccountManager.accountingException();
		if (accountingException != null && accountingException.contains(accountId)) {
			if (StringUtils.isNotBlank(reason.toString())) {
				reason.append(";系统账目异常!");
			} else {
				reason.append("系统账目异常!");
			}
		}
		
		Set<Integer> failure = allocateTransService.buildFailureTrans();
		if (failure != null && failure.contains(accountId)) {
			if (StringUtils.isNotBlank(reason.toString())) {
				reason.append(";连续转账失败多次!");
			} else {
				reason.append("连续转账失败多次!");
			}
		}
		boolean flag = checkInvokeTm(accountService.getFromCacheById(accountId), accIdSetInValTm);
		if (!flag) {
			reason.append("余额上报时间失效");
		}
		Set<Integer> idsByNonExprie = buildIdByNonExprie();
		boolean flag2 = checkNonExpire(idsByNonExprie, accountId);
		if (!flag2) {
			if (StringUtils.isNotBlank(reason.toString())) {
				reason.append(";出款频率不满足!");
			} else {
				reason.append("出款频率不满足!");
			}
		}
		Map<String, Float> outCtnDaily = accountService.findOutCountDaily();
		boolean flag3 = checkOutCtnDaily(accountService.getFromCacheById(accountId), outCtnDaily);
		if (!flag3) {
			if (StringUtils.isNotBlank(reason.toString())) {
				reason.append(";当日出款笔数不满足!");
			} else {
				reason.append("当日出款笔数不满足!");
			}
		}
		// 账号设备告警
		Map<Object, Object> deviceAlarm = redisService.getStringRedisTemplate()
				.boundHashOps(RedisKeys.PROBLEM_ACC_ALARM).entries();
		boolean flag4 = checkDeviceAlarm(deviceAlarm, accountId.toString());
		if (!flag4) {
			if (StringUtils.isNotBlank(reason.toString())) {
				reason.append(";在账号告警里!");
			} else {
				reason.append("在账号告警里!");
			}
		}
		AccountBaseInfo baseInfo = accountService.getFromCacheById(accountId);
		if (baseInfo != null) {
			// 当日出款
			boolean flag8 = checkDailyOut(baseInfo);
			if (!flag8) {
				if (StringUtils.isNotBlank(reason.toString())) {
					reason.append(";当日出款限额不满足!");
				} else {
					reason.append("当日出款限额不满足!");
				}
			}
		}
		boolean flag5 = baseInfo == null;
		if (flag5) {
			if (StringUtils.isNotBlank(reason.toString())) {
				reason.append(";账号缓存为空!");
			} else {
				reason.append("账号缓存为空!");
			}
		}
		if (baseInfo != null) {
			boolean flag6 = checkMaintain(baseInfo.getBankType());
			if (flag6) {
				if (StringUtils.isNotBlank(reason.toString())) {
					reason.append(";维护中!");
				} else {
					reason.append("维护中!");
				}
			}
			if (baseInfo.getType().equals(AccountType.InBank.getTypeId())) {
				boolean flag7 = checkBankOnline(baseInfo.getId());
				if (!flag7) {
					if (StringUtils.isNotBlank(reason.toString())) {
						reason.append(";不在入款缓存里!");
					} else {
						reason.append("不在入款缓存里!");
					}
				}
			} else if (baseInfo.getType().equals(AccountType.OutBank.getTypeId())) {
				boolean flag7 = checkBankOnline(baseInfo.getId());
				if (!flag7) {
					if (StringUtils.isNotBlank(reason.toString())) {
						reason.append(";不在出款缓存里!");
					} else {
						reason.append("不在出款缓存里!");
					}
				}
			} else {
				if (StringUtils.isNotBlank(reason.toString())) {
					reason.append(";其他卡不明原因!");
				} else {
					reason.append("其他卡不明原因!");
				}
			}
		}
		if (StringUtils.isBlank(reason.toString())) {
			Set<Object> ids = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY)
					.keys();
			List<Integer> list = CollectionUtils.isEmpty(ids) ? null
					: ids.stream().map(p -> Integer.valueOf(p.toString())).collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(list) && list.contains(accountId)) {
				reason.append("工具端暂停!");
			} else {
				reason.append("不明原因暂停!");
			}
		}
		if(accountService.hasAccountNotice(accountId)){
			reason.append("系统账目错误!");
		}
		log.debug("查询转暂停原因:{},参数:{}", reason.toString(), accountId);
		return reason.toString();
	}

	private boolean checkNull(Integer accountId) {
		boolean flag = accountService.getFromCacheById(accountId) == null;
		log.debug("账号缓存为空 :accontId:{},flag:{}", accountId, flag);
		return flag;
	}

	private boolean checkMaintain(Integer accountId) {
		boolean flag = !checkMaintain(accountService.getFromCacheById(accountId).getBankType());
		log.debug("账号维护里:accontId:{},flag:{}", accountId, flag);
		return flag;
	}

	private boolean checkDeviceAlarm(Map<Object, Object> deviceAlarm, String accountId) {
		boolean flag = !deviceAlarm.containsKey(accountId);
		log.debug("是否在设备异常检测里 :异常设备集合:{},id:{},是否在!flag:{}", deviceAlarm, accountId, flag);
		return flag;
	}

	private LoadingCache<String, Set<Integer>> onlineCache = CacheBuilder.newBuilder().concurrencyLevel(1)
			.refreshAfterWrite(30, TimeUnit.SECONDS) // 每30秒自动刷新
			.build(new CacheLoader<String, Set<Integer>>() {
				public Set<Integer> load(String key) {
					log.info("开始定时刷新分配目标缓存对象");
					// 未匹配流水告警数据
					// Set<Integer> unMatched = buildAllUnMatched();
					// log.trace("cache unMatched>> {}", unMatched);
					// 银行余额上报有效时间
					Set<Integer> accIdSetInValTm = allocateTransService.buildValidAcc();
					log.trace("cache accIdSetInValTm>> {}", accIdSetInValTm);
					// 出款频率限制
					Set<Integer> idsByNonExprie = buildIdByNonExprie();
					log.trace("cache idsByNonExprie>> {}", idsByNonExprie);
					// 转出限制
					// Set<Integer> invSet = sysBalService.alarm(true);
					// log.trace("cache invSet>> {}", invSet);
					// 当日出款限额
					Map<String, Float> outCtnDaily = accountService.findOutCountDaily();
					log.trace("cache outCtnDaily>> {}", outCtnDaily);
					// 连续出款失败
					Set<Integer> failure = allocateTransService.buildFailureTrans();
					// 账号设备告警
					Map<Object, Object> deviceAlarm = redisService.getStringRedisTemplate()
							.boundHashOps(RedisKeys.PROBLEM_ACC_ALARM).entries();
					// 对账异常账号
					Set<Integer> accountingException = systemAccountManager.accountingException();
					log.debug("cache accountingException>> {}", accountingException);
					if ("ALLBANK".equals(key) && !CollectionUtils.isEmpty(accIdSetInValTm)) {
						Set<Integer> inbankSet = accIdSetInValTm.stream()
								.filter(p -> checkInvokeTm(accountService.getFromCacheById(p), accIdSetInValTm)
										&& checkNonExpire(idsByNonExprie, p)
										&& checkOutCtnDaily(accountService.getFromCacheById(p), outCtnDaily)
										&& checkDailyOut(accountService.getFromCacheById(p))
										&& checkDeviceAlarm(deviceAlarm, p.toString())
										&& (checkNull(p) || checkMaintain(p)) && !failure.contains(p)
										&& (!accountingException.contains(p)))
								.collect(Collectors.toSet());
						log.debug("入款缓存过滤之后结果:{}", inbankSet);
						return inbankSet;
					}
					return new HashSet<>();
				}
			});

	public boolean checkBankOnline(Integer accountId) {
		Set<Integer> accountIds = onlineCache.getUnchecked("ALLBANK");
		if (!CollectionUtils.isEmpty(accountIds)) {
			log.debug("入款卡缓存数据:{}", accountIds.toString());
			boolean flag = accountIds.contains(accountId);
			log.debug("入款卡是否在可用缓存里:{},id:{}", flag, accountId);
			return flag;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private Set<Integer> buildAllUnMatched() {
		String sql = String.format(
				"select case when operator is null then account_id else operator end from biz_outward_task where status in (0,1,2) and account_id is not null and asign_time>='%s' group by case when operator is null then account_id else operator end having count(1) >5",
				CommonUtils.getStartTimeOfCurrDay());
		List<Object> result = entityManager.createNativeQuery(sql).getResultList();
		if (!CollectionUtils.isEmpty(result)) {
			Set<Integer> unmatched = new HashSet<>();
			for (Object obj : result) {
				unmatched.add(Integer.parseInt(obj.toString()));
			}
			return unmatched;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Set<Integer> buildIdByNonExprie() {
		String rateLimitStr = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTDRAW_RATE_LIMIT", "0");
		int rateLimit = StringUtils.isNumeric(rateLimitStr) ? Integer.parseInt(rateLimitStr) : 0;
		if (rateLimit <= 0)
			return Collections.EMPTY_SET;
		rateLimit = (rateLimit > 3600 ? 3600 : rateLimit) * 1000;
		Object obj = redisService.getStringRedisTemplate().opsForHash().entries(RedisKeys.LAST_TIME_OUTWARD);
		Map<String, String> data = (Map<String, String>) obj;
		if (CollectionUtils.isEmpty(data))
			return Collections.EMPTY_SET;
		Set<Integer> ret = new HashSet<>();
		long curr = System.currentTimeMillis();
		Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> item = it.next();
			String key = item.getKey(), value = item.getValue();
			if (Objects.nonNull(key) && Objects.nonNull(value)) {
				if (curr < (Long.parseLong(value) + rateLimit)) {
					ret.add(Integer.valueOf(key));
					it.remove();
				}
			}
		}
		if (!CollectionUtils.isEmpty(data))
			redisService.getStringRedisTemplate().opsForHash().delete(RedisKeys.LAST_TIME_OUTWARD,
					data.keySet().toArray());
		return ret;
	}

	private boolean checkUnmatch(Integer id, Set<Integer> unMatched) {
		return Objects.isNull(id) || CollectionUtils.isEmpty(unMatched) || !unMatched.contains(id);
	}

	private boolean checkNonExpire(Set<Integer> ids, Integer target) {
		if (Objects.isNull(ids) || Objects.isNull(target))
			return true;
		boolean flag = ids.contains(target);
		log.debug("出款频率:{},出款频率限制集合:{},是否包含!flag:{}", target, ids, !flag);
		return !flag;
	}

	private boolean checkInvokeTm(AccountBaseInfo acnt, Set<Integer> accIdSetInValTm) {
		boolean flag = true;
		if (Objects.nonNull(acnt) && Objects.isNull(acnt.getHolder()) && Objects.equals(acnt.getFlag(), 1)) {
			flag = !CollectionUtils.isEmpty(accIdSetInValTm) && accIdSetInValTm.contains(acnt.getId());
		}
		log.debug("余额是否有上报时间:{}", flag);
		return flag;
	}

	private boolean checkInv(AccountBaseInfo base, Set<Integer> ids) {
		if (Objects.isNull(base) || Objects.isNull(base.getId()) || CollectionUtils.isEmpty(ids)
				|| Objects.nonNull(base.getHolder()))
			return true;
		return !ids.contains(base.getId());
	}

	private boolean checkOutCtnDaily(AccountBaseInfo acc, Map<String, Float> outCtnDaily) {
		log.debug("出款笔数校验：acc:{}, outCtnDaily:{}", acc, outCtnDaily);
		if (Objects.isNull(acc) || Objects.isNull(acc.getLimitOutCount()) || acc.getLimitOutCount() <= 0
				|| Objects.isNull(outCtnDaily))
			return true;
		Float ctn = outCtnDaily.get(String.valueOf(acc.getId()));
		log.debug("出款笔数：{},卡限定出款笔数:{}", ctn, acc.getLimitOutCount());
		return Objects.isNull(ctn) || ctn <= acc.getLimitOutCount();
	}

	/**
	 * 检测 当日出款
	 */
	private boolean checkDailyOut(AccountBaseInfo acnt) {
		if (acnt == null) {
			return true;
		}
		AccountBaseInfo base = accountService.getFromCacheById(acnt.getId());
		// 入款卡\备用卡\客户绑定卡不校验当日出款限额
		if (base == null || Constants.INBANK == base.getType() || Constants.RESERVEBANK == base.getType()
				|| Constants.BINDCUSTOMER == base.getType()) {
			return true;
		}
		int income0outward1 = 1;
		float outDaily = accountService.findAmountDailyByTotal(income0outward1, acnt.getId()).floatValue();
		log.debug("当日出款额度:{},卡设置当日限额:{}", outDaily, acnt.getLimitOut());
		if (acnt.getLimitOut() != null) {
			return acnt.getLimitOut() > outDaily;
		}
		String proVal = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_LIMIT_CHECKOUT_TODAY.getValue());
		log.debug("当日出款额度proVal:{},卡设置当日限额:{}", proVal, acnt.getLimitOut());
		if (StringUtils.isBlank(proVal)) {
			log.debug("当日出款限额检测  请设置系统当日出款限额");
			return true;
		}
		boolean flag = Float.parseFloat(proVal) > outDaily;
		log.debug("proVal:{},当日出款限额:{},是否满足proVal>outDaily条件:{}", proVal, outDaily, flag);
		return flag;
	}

	private boolean checkMaintain(String currBank) {
		if (StringUtils.isBlank(currBank)) {
			return false;
		}
		String banks = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_SYS_MAINTAIN_BANKTYPE.getValue());
		boolean flag = StringUtils.isNotBlank(banks) && banks.contains(currBank);
		log.debug("维护中:{},参数:{}", flag, currBank);
		return flag;
	}

	private Double enScore4Fr(int type, int zone, int l, int handi, int bal) {
		int l_ = CommonUtils.isMergeMidInner() ? (l == Constants.Outter ? Constants.Outter : Constants.Inner) : l;
		return Double.valueOf(String.format("%02d%02d%d%03d.%08d", type, zone, l_, handi, bal));
	}
}
