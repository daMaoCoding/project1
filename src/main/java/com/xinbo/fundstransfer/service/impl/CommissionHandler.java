package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.entity.activity.BizAccountFlwActivity;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.domain.entity.agent.BizRebateAgentSyn;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AccountReturnSummary;
import com.xinbo.fundstransfer.domain.repository.*;
import com.xinbo.fundstransfer.domain.repository.activity.RebateActivitySynRepository;
import com.xinbo.fundstransfer.domain.repository.agent.BizRebateAgentSynRepository;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3RateItem;
import com.xinbo.fundstransfer.restful.v3.pojo.ResV3DailyLogItem;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.service.impl.activity.ActivityUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CommissionHandler extends ApplicationObjectSupport {
	private static final Logger logger = LoggerFactory.getLogger(CommissionHandler.class);
	@Autowired
	private AllocateIncomeAccountService allocInAccSer;
	@Autowired
	private SysUserProfileService userProSer;
	@Autowired
	private AccountRebateDayRepository rebateDayDao;
	@Autowired
	private AccountReturnSummaryRepository returnSummaryDao;
	@Autowired
	private AccountRepository accDao;
	@Autowired
	private BankLogRepository bankLogDao;
	@Autowired
	@Lazy
	private AccountService accSer;
	@Autowired
	AccountMoreRepository accountMoreDao;
	@Autowired
	AccountFlwActivityService accountFlwActivityService;
	@Autowired
	AccountReturnSummaryRepository accountReturnSummaryDao;
	@Autowired
	BankLogCommissionService bankLogCommissionService;
	@Autowired
	RebateActivitySynRepository flwActivitySynDao;
	@Lazy
	@Autowired
	RebateApiService rebateApiSer;
	@Autowired
	AccountMoreService accountMoreService;
	@Autowired
	BizRebateAgentSynRepository agentSynRepository;

	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * a timer to compute part-time duty personal's daily commission.
	 */
	@Scheduled(fixedRate = 3600000)
	public void commissionSummaryAutomatically() {
		if (!allocInAccSer.checkHostRunRight()) {
			logger.trace("Compute Commission Summary  Automatically -> {} No Right", CommonUtils.getInternalIp());
			return;
		}
		String commissionTime = valOfCommissionTime();
		Date[] stEd = ActivityUtil.startAndEndTimeOfCommission(commissionTime);
		if (Objects.isNull(stEd) || stEd.length != 2 || Objects.isNull(stEd[0]) || Objects.isNull(stEd[1]))
			return;
		logger.info("Compute Commission Summary  Automatically -> {}  start {} ...", CommonUtils.getInternalIp(),
				commissionTime);
		List<Integer> summaryDoneList = returnSummaryDao.findReturnSummaryByCalcTime(commissionTime);
		List<Integer> allList = accDao.findAccountId4Rebate();
		List<Integer> doneList = rebateDayDao.findAccountByCalcTime(commissionTime);
		List<Integer> unfinished = allList.stream().filter(p -> !doneList.contains(p)).collect(Collectors.toList());
		List<ReqV3RateItem> baseRateList = buildBaseRate();
		Map<String, List<BizAccountFlwActivity>> uidActivity = new HashMap<>();
		Map<Integer, BizFlwActivitySyn> activitySynMap = new HashMap<>();
		boolean finish = true;
		for (Integer unfinish : unfinished) {
			try {
				AccountBaseInfo base = accSer.getFromCacheById(unfinish);
				if (Objects.isNull(base) || StringUtils.isBlank(base.getMobile()))
					continue;
				BizAccountMore more = accountMoreDao.findByMoible(base.getMobile());
				if (Objects.isNull(more))
					continue;
				BizFlwActivitySyn syn = accountFlwActivityService.synFromCache(uidActivity, activitySynMap, more);
				BizAccountFlwActivity activity = accountFlwActivityService.activityFromCache(uidActivity, more);
				summary(more, base, commissionTime, baseRateList, syn, activity, summaryDoneList);
			} catch (Exception e) {
				finish = false;
			}
		}
		if (!finish)
			return;
		List<BizRebateAgentSyn> agentList = agentSynRepository.findByIsAgentAndAgentType(true,
				AgentEnums.AgentType.Separate.getType());
		if (CollectionUtils.isEmpty(agentList)) {
			logger.info("AgentSummary [ no agent exists ]  ->   {}  end {} ...", CommonUtils.getInternalIp(),
					commissionTime);
		} else {
			BigDecimal agentRate = new BigDecimal("0.0005");
			for (BizRebateAgentSyn agent : agentList) {
				summary4Agent(commissionTime, agent, agentRate);
			}
		}
		valOfCommissionTime(commissionTime);
		logger.info("Compute Commission Summary  Automatically -> {}  end {} ...", CommonUtils.getInternalIp(),
				commissionTime);
	}

	public void commissionSummarymanually(String commissionTime) {
		Date[] stEd = ActivityUtil.startAndEndTimeOfCommission(commissionTime);
		if (Objects.isNull(stEd) || stEd.length != 2 || Objects.isNull(stEd[0]) || Objects.isNull(stEd[1]))
			return;
		List<Integer> allList = accDao.findAccountId4Rebate();
		List<Integer> dailyDoneList = rebateDayDao.findAccountByCalcTime(commissionTime);
		List<Integer> unfinished = allList.stream().filter(p -> !dailyDoneList.contains(p))
				.collect(Collectors.toList());
		List<ReqV3RateItem> baseRateList = buildBaseRate();
		Map<String, List<BizAccountFlwActivity>> uidActivity = new HashMap<>();
		Map<Integer, BizFlwActivitySyn> activitySynMap = new HashMap<>();
		boolean finish = true;
		for (Integer unfinish : unfinished) {
			try {
				AccountBaseInfo base = accSer.getFromCacheById(unfinish);
				if (Objects.isNull(base) || StringUtils.isBlank(base.getMobile()))
					continue;
				BizAccountMore more = accountMoreDao.findByMoible(base.getMobile());
				if (Objects.isNull(more))
					continue;
				bankLogCommissionService.deleteByAccountIdAndCalcTime(unfinish, commissionTime);
				BizFlwActivitySyn syn = accountFlwActivityService.synFromCache(uidActivity, activitySynMap, more);
				BizAccountFlwActivity activity = accountFlwActivityService.activityFromCache(uidActivity, more);
				summary(more, base, commissionTime, baseRateList, syn, activity, dailyDoneList);
			} catch (Exception e) {
				logger.info("Commission Summary Manually >> {} msg :", unfinish, e);
				finish = false;
				break;
			}
		}
		if (!finish)
			return;
		List<BizRebateAgentSyn> agentList = agentSynRepository.findByIsAgentAndAgentType(true,
				AgentEnums.AgentType.Separate.getType());
		if (CollectionUtils.isEmpty(agentList)) {
			logger.info("AgentSummary [ no agent exists ]  ->   {}  end {} ...", CommonUtils.getInternalIp(),
					commissionTime);
		} else {
			BigDecimal agentRate = new BigDecimal("0.0005");
			for (BizRebateAgentSyn agent : agentList) {
				summary4Agent(commissionTime, agent, agentRate);
			}
		}
		valOfCommissionTime(commissionTime);
		logger.info("Compute Commission Summary  Automatically -> {}  end {} ...", CommonUtils.getInternalIp(),
				commissionTime);
	}

	public void approve4VerifyCommission(BizAccountMore more, String commissionTime,
			List<BizAccountReturnSummary> summaries) {
		logger.info("Approve4VerifyCommission >> [ START...  ] more: {} commissionTime: {}  summaries: {}",
				more.getUid(), commissionTime, summaries.size());
		try {
			accountFlwActivityService.approve4VerifyCommission(more, commissionTime, summaries);
			logger.info("Approve4VerifyCommission >> [ END  ] more: {} commissionTime: {}  summaries: {}",
					more.getUid(), commissionTime, summaries.size());
		} catch (Exception e) {
			more = accountMoreService.findByMobile(more.getMoible());
			if (Objects.nonNull(more))
				accountMoreService.saveAndFlash(more);
			logger.info("Approve4VerifyCommission {} [ EXCEPTION ]>> commissionTime: {}", more.getUid(), commissionTime,
					e);
		}
	}

	public void summary(BizAccountMore more, AccountBaseInfo base, String commissionTime,
			List<ReqV3RateItem> baseRateList, BizFlwActivitySyn syn, BizAccountFlwActivity activity,
			List<Integer> summaryDoneList) {
		if (Objects.isNull(more) || Objects.isNull(base) || StringUtils.isBlank(commissionTime)
				|| CollectionUtils.isEmpty(baseRateList))
			return;
		Date[] stEd = ActivityUtil.startAndEndTimeOfCommission(commissionTime);
		if (Objects.isNull(stEd) || stEd.length != 2 || Objects.isNull(stEd[0]) || Objects.isNull(stEd[1]))
			return;
		Date start = stEd[0], end = stEd[1];
		List<BizBankLog> logList = bankLogDao.findRebateRecord(base.getId(), start, end);
		// 如果没有流水也保存信息，需要统计可提现余额
		if (CollectionUtils.isEmpty(logList)) {
			accountFlwActivityService.saveSummary(more, base, commissionTime);
			return;
		}
		List<Integer> ownAccIdList = Stream.of(StringUtils.trimToEmpty(more.getAccounts()).split(","))
				.filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
		// 判断是否零信用额度，如果是零信用额度 判断是否存在多张卡，如果有多张卡 只能返50
		boolean flag = false;
		if (Objects.nonNull(more.getMargin()) && more.getMargin().compareTo(new BigDecimal("1000")) == 0) {
			if (ownAccIdList.size() > 1) {
				flag = !CollectionUtils
						.isEmpty(returnSummaryDao.findReturnSummaryByCalcTime(commissionTime, ownAccIdList));
			}
		}
		if (flag)
			return;
		ReqV3RateItem baseRate = buildCommisson(baseRateList, more);
		if (Objects.isNull(baseRate)) {
			return;
		}
		if (!accountFlwActivityService.avail4CommissionTime(syn, activity, start, end)) {
			syn = null;
			activity = null;
		}
		BigDecimal total = BigDecimal.ZERO, amount = BigDecimal.ZERO, totalActivity = BigDecimal.ZERO,
				reallyAmount = BigDecimal.ZERO;
		List<BizBankLogCommission> cms = new ArrayList<>();
		for (BizBankLog log : logList) {
			ResV3DailyLogItem daily = new ResV3DailyLogItem(log.getId(), log.getAmount(), log.getCommission());
			total = total.add(log.getAmount());
			BigDecimal baseAmount = BigDecimal.ZERO;
			if (Objects.nonNull(daily.getCashbackAmount()))
				baseAmount = daily.getCashbackAmount();
			amount = amount.add(baseAmount);
			reallyAmount = amount;
			BigDecimal activityAmount = BigDecimal.ZERO;
			boolean inActivity = avail4Log(syn, activity, log);
			Integer flwActivity = !inActivity ? null : syn.getId();
			if (inActivity && baseAmount.compareTo(BigDecimal.ZERO) > 0) {
				List<ReqV3RateItem> giftRates = accountFlwActivityService.findActivityByActivitySyn(syn);
				ReqV3RateItem giftRate = buildCommisson(giftRates, more);
				if (Objects.nonNull(giftRate)) {
					ResV3DailyLogItem gift = new ResV3DailyLogItem(log.getId(), log.getAmount(), giftRate);
					activityAmount = gift.getCashbackAmount();
				}
			}
			if (baseAmount.compareTo(BigDecimal.ZERO) != 0 || activityAmount.compareTo(BigDecimal.ZERO) != 0) {
				BizBankLogCommission cm = new BizBankLogCommission();
				cm.setBankLogId(log.getId());
				cm.setReturnSummaryId(null);
				cm.setAccountId(log.getFromAccount());
				cm.setUid(more.getUid());
				cm.setCalcTime(commissionTime);
				cm.setCommission(commission(daily.getCashbackAmount(), activityAmount));
				cm.setAmount(log.getAmount());
				cm.setFlwActivity(flwActivity);
				cms.add(cm);
			}
			totalActivity = totalActivity.add(activityAmount);
		}
		// 如果是第一档 多余50给50 不够50给50 如果超过第一档不够50给50
		if (Objects.nonNull(baseRate.getUplimit()) && baseRate.getUplimit() > 0) {
			amount = new BigDecimal(baseRate.getUplimit());
			// amount = amount.floatValue() > rate.getUplimit() ? new
			// BigDecimal(rate.getUplimit()) : amount;
		} else {
			amount = amount.floatValue() > 50 ? amount : new BigDecimal("50");
		}
		// 如果不是零信用额度的，当前账号返利的是50 则需要查看其它账号是否返利超过50.如果超过了则当前账号只能返利对应的利率
		boolean fla = false;
		if (null != more.getMargin() && more.getMargin().compareTo(new BigDecimal(1000)) == 1
				&& amount.compareTo(new BigDecimal(50)) == 0) {
			// 其它账号返利金
			BigDecimal rebateAmounts = bankLogDao.findRebateBanks(ownAccIdList, start, end);
			if (rebateAmounts.compareTo(new BigDecimal("50")) == 1)
				amount = reallyAmount;
			if (ownAccIdList.size() > 1) {
				fla = !CollectionUtils
						.isEmpty(returnSummaryDao.findReturnSummaryByCalcTime5(commissionTime, ownAccIdList));
			}
		}
		if (fla)
			return;
		total = total.abs();
		amount = amount.abs();
		accountFlwActivityService.saveSummary(more, base, total, amount, totalActivity, commissionTime, cms);
		summaryDoneList.add(base.getId());
	}

	private void summary4Agent(String commissionTime, BizRebateAgentSyn agent, BigDecimal agentRate) {
		if (Objects.isNull(agent))
			return;
		if (StringUtils.isBlank(agent.getUid())) {
			logger.info("AgentSummary{} [ uid is empty ]  ->  agent: {}  {}  end {} ", agent.getUid(), agent.getId(),
					CommonUtils.getInternalIp(), commissionTime);
			return;
		}
		BizAccountMore more = accountMoreDao.findByUid(StringUtils.trimToEmpty(agent.getUid()));
		if (Objects.isNull(more)) {
			logger.info("AgentSummary{} [ the account more doesn't exist ]  ->  agent: {}  {}  end {} ", agent.getUid(),
					agent.getId(), CommonUtils.getInternalIp(), commissionTime);
			return;
		}
		if (!more.getAgent()) {
			logger.info("AgentSummary{} [ the account more isn't a agent ]  ->  agent: {}  {} moreAgent: {} end {} ",
					agent.getUid(), agent.getId(), CommonUtils.getInternalIp(), more.getAgent(), commissionTime);
			return;
		}
		if (!agent.getIsAgent()) {
			logger.info("AgentSummary{} [ the Agent isn't in agent mode ]  ->  agent: {}  {} agentMode: {} end {} ",
					agent.getUid(), agent.getId(), CommonUtils.getInternalIp(), agent.getIsAgent(), commissionTime);
			return;
		}
		List<BizAccountReturnSummary> upSummaryList = accountReturnSummaryDao.findByCalcTimeAndUid(commissionTime,
				StringUtils.trimToEmpty(more.getUid()));
		if (upSummaryList.stream().filter(p -> Objects.nonNull(p.getAgentAmount())).count() > 0) {
			logger.info("AgentSummary{} [ the Agent Amount already calculate ]  ->  agent: {} {} end {} ",
					agent.getUid(), agent.getId(), CommonUtils.getInternalIp(), commissionTime);
			return;
		}
		List<BizRebateAgentSyn.AgentSubUsers> subUsers = agent.getAgentSubUserIds();
		if (CollectionUtils.isEmpty(subUsers)) {
			logger.info("AgentSummary{} [ subUsers empty ]  ->  agent: {}  {}  end {} ", agent.getUid(), agent.getId(),
					CommonUtils.getInternalIp(), commissionTime);
			return;
		}
		Set<String> uidSet = subUsers.stream().map(p -> StringUtils.trimToNull(p.getUid())).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(uidSet)) {
			logger.info("AgentSummary{} [ uidSet empty ]  ->  agent: {}  {}  end {} ", agent.getUid(), agent.getId(),
					CommonUtils.getInternalIp(), commissionTime);
			return;
		} else {
			logger.info("AgentSummary{} [ UID COLLECTIONS ]  ->  agent: {}  {} subUid: {} end {} ", agent.getUid(),
					agent.getId(), CommonUtils.getInternalIp(), uidSet, commissionTime);
		}
		List<BizAccountReturnSummary> subSummaryList = accountReturnSummaryDao.findByCalcTimeAndUidIn(commissionTime,
				uidSet);
		if (CollectionUtils.isEmpty(subSummaryList)) {
			logger.info("AgentSummary{} [ sub return summary empty ]  ->  agent: {}  {}  end {} ", agent.getUid(),
					agent.getId(), CommonUtils.getInternalIp(), commissionTime);
			return;
		}
		BigDecimal totalAmount = subSummaryList.stream().map(p -> p.getTotalAmount()).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
			logger.info("AgentSummary{} [ Total Amount Zero ]  ->  agent: {}  {}  end {} ", agent.getUid(),
					agent.getId(), CommonUtils.getInternalIp(), commissionTime);
			return;
		}
		BigDecimal agentAmount = totalAmount.multiply(agentRate).setScale(2, BigDecimal.ROUND_HALF_UP);
		accountFlwActivityService.saveSummaryAgent(more, agent, upSummaryList, agentAmount, commissionTime,
				totalAmount);
	}

	/**
	 * get this computing commission time
	 *
	 * @return time format 'yyyy-MM-dd'
	 * @see this#valOfCommissionTime(String)
	 */
	private String valOfCommissionTime() {
		String proKey = UserProfileKey.RECORD_TIME_4_DAILY_COMMISSION.getValue();
		List<SysUserProfile> proList = userProSer.findByPropertyKey(proKey).stream()
				.filter(p -> Objects.equals(p.getUserId(), AppConstants.USER_ID_4_ADMIN)).collect(Collectors.toList());
		SysUserProfile pro = CollectionUtils.isEmpty(proList) ? null : proList.get(0);
		String dayB2 = CommonUtils.getDateFormatyyyyMMdd2Str(new Date(System.currentTimeMillis() - 86400000 * 2));
		String dayB1 = CommonUtils.getDateFormatyyyyMMdd2Str(new Date(System.currentTimeMillis() - 86400000));
		if (Objects.isNull(pro)) {
			pro = new SysUserProfile();
			pro.setPropertyValue(dayB2);
			pro.setUserId(AppConstants.USER_ID_4_ADMIN);
			pro.setPropertyName("返利网每日佣金计算 日期");
			pro.setPropertyKey(proKey);
			userProSer.save(pro);
			return dayB1;
		} else if (StringUtils.isBlank(pro.getPropertyValue())) {
			pro.setPropertyValue(dayB2);
			userProSer.save(pro);
			return dayB1;
		} else {
			String[] yyyyMMdd = pro.getPropertyValue().split("-");
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.YEAR, Integer.valueOf(yyyyMMdd[0]));
			calendar.set(Calendar.MONTH, Integer.valueOf(yyyyMMdd[1]) - 1);
			calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(yyyyMMdd[2]) + 1);
			String d1 = CommonUtils.getDateFormatyyyyMMdd2Str(calendar.getTime());
			String d2 = CommonUtils.getDateFormatyyyyMMdd2Str(new Date());
			return Objects.equals(d1, d2) ? null : d1;
		}
	}

	/**
	 * set this computing commission time
	 *
	 * @param time
	 *            format 'yyyy-MM-dd'
	 * @see this#valOfCommissionTime()
	 */
	private void valOfCommissionTime(String time) {
		if (StringUtils.isBlank(time)) {
			return;
		}
		String proKey = UserProfileKey.RECORD_TIME_4_DAILY_COMMISSION.getValue();
		List<SysUserProfile> proList = userProSer.findByPropertyKey(proKey).stream()
				.filter(p -> Objects.equals(p.getUserId(), AppConstants.USER_ID_4_ADMIN)).collect(Collectors.toList());
		SysUserProfile pro = CollectionUtils.isEmpty(proList) ? new SysUserProfile() : proList.get(0);
		pro.setPropertyValue(time);
		pro.setUserId(AppConstants.USER_ID_4_ADMIN);
		pro.setPropertyName("返利网每日佣金计算 日期");
		pro.setPropertyKey(proKey);
		userProSer.save(pro);
	}

	public List<ReqV3RateItem> buildBaseRate() {
		List<SysUserProfile> proList = userProSer.findByPropertyKey(UserProfileKey.REBATE_SYS_RATE_SETTING.getValue())
				.stream().filter(p -> Objects.equals(p.getUserId(), AppConstants.USER_ID_4_ADMIN))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(proList) || StringUtils.isBlank(proList.get(0).getPropertyValue()))
			return null;
		try {
			return mapper.readValue(proList.get(0).getPropertyValue(), new TypeReference<List<ReqV3RateItem>>() {
			});
		} catch (Exception e) {
			return null;
		}
	}

	private ReqV3RateItem buildCommisson(List<ReqV3RateItem> rateList, BizAccountMore base) {
		if (Objects.isNull(base) || Objects.isNull(base.getMargin())) {
			return null;
		}
		rateList.sort((o1, o2) -> {
			float o3 = o1.getAmount() - o2.getAmount();
			return (o3 > 0) ? 1 : (o3 < 0 ? -1 : 0);
		});
		float peek = base.getMargin().floatValue();
		if (peek == 0) {
			rateList = rateList.stream().filter(p -> p.getAmount() == 0).collect(Collectors.toList());
			return CollectionUtils.isEmpty(rateList) ? null : rateList.get(0);
		}
		ReqV3RateItem target = rateList.get(rateList.size() - 1);
		for (int i = 0; i < rateList.size(); i++) {
			ReqV3RateItem rate = rateList.get(i);
			if (peek < rate.getAmount()) {
				if (i >= 1) {
					target = rateList.get(i - 1);
				}
				break;
			} else if ((i + 1) == rateList.size()) {
				target = rateList.get(i);
			}
		}
		// for (ReqV3RateItem rate : rateList) {
		// if (peek < rate.getAmount()) {
		// target = rate;
		// break;
		// }
		// }
		return target;
	}

	private String commission(BigDecimal baseCommission, BigDecimal activityCommission) {
		baseCommission = Objects.isNull(baseCommission) ? BigDecimal.ZERO : baseCommission;
		activityCommission = Objects.isNull(activityCommission) ? BigDecimal.ZERO : activityCommission;
		String format = "{\"baseCommission\":%s,\"activityCommission\":%s}";
		return String.format(format, baseCommission, activityCommission);
	}

	private boolean avail4Log(BizFlwActivitySyn syn, BizAccountFlwActivity activity, BizBankLog log) {
		if (Objects.isNull(syn) || Objects.isNull(activity) || Objects.isNull(log)) {
			return false;
		}
		if (Objects.isNull(syn.getActivityStartTime())
				|| syn.getActivityStartTime().compareTo(log.getCreateTime()) > 0) {
			return false;
		}
		if (Objects.nonNull(syn.getActivityEndTime()) && syn.getActivityEndTime().compareTo(log.getCreateTime()) < 0) {
			return false;
		}
		if (Objects.isNull(activity.getUserStartTime())
				|| activity.getUserStartTime().compareTo(log.getCreateTime()) > 0) {
			return false;
		}
		if (Objects.nonNull(activity.getUserEndTime())
				&& activity.getUserEndTime().compareTo(log.getCreateTime()) < 0) {
			return false;
		}
		return true;
	}
}
