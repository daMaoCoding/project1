package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.http.v3.RebateHttpClient;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.entity.activity.BizAccountFlwActivity;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.domain.entity.agent.BizRebateAgentSyn;
import com.xinbo.fundstransfer.domain.enums.AccountRebateReturnSummaryStatus;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.Commission;
import com.xinbo.fundstransfer.domain.repository.AccountRebateRepository;
import com.xinbo.fundstransfer.domain.repository.AccountReturnSummaryRepository;
import com.xinbo.fundstransfer.domain.repository.activity.RebateActivitySynRepository;
import com.xinbo.fundstransfer.domain.repository.activity.RebateUserActivityRepository;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3RateItem;
import com.xinbo.fundstransfer.restful.v3.pojo.ResV3DailyLogItem;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.service.impl.activity.ActivityUtil;
import com.xinbo.fundstransfer.service.impl.activity.RebateActivityCommission;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AccountFlwActivityServiceImpl implements AccountFlwActivityService {
	private static final Logger logger = LoggerFactory.getLogger(AccountFlwActivityServiceImpl.class);
	@Autowired
	private RebateUserActivityRepository accountFlwActivityDao;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	private AccountRebateDayService accRebateDaySer;
	@Lazy
	@Autowired
	private AccountService accSer;
	@Autowired
	private AppProperties appPros;
	@Autowired
	private AccountReturnSummaryRepository accountReturnSummaryDao;
	@Autowired
	private AccountRebateRepository accRebateDao;
	@Autowired
	private BankLogCommissionService bankLogCommissionService;
	@Autowired
	private AccountReturnSummaryRepository returnSummaryDao;
	@Autowired
	private RebateActivitySynRepository flwActivitySynDao;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private RebateApiService rebateApiSer;
	@Autowired
	private AccountChangeService accChgSer;

	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public List<ReqV3RateItem> findActivityByActivitySyn(BizFlwActivitySyn syn) {
		if (Objects.isNull(syn) || StringUtils.isBlank(syn.getGiftMarginRule()))
			return Collections.EMPTY_LIST;
		String rule = StringUtils.trimToEmpty(syn.getGiftMarginRule());
		List<ReqV3RateItem> result = null;
		try {
			result = mapper.readValue(rule, new TypeReference<List<ReqV3RateItem>>() {
			});
		} catch (Exception e) {
		}
		return result;
	}

	@Override
	@Transactional
	public void approve4VerifyCommission(BizAccountMore more, String commissionTime,
			List<BizAccountReturnSummary> summaries) {
		Date[] stEd = ActivityUtil.startAndEndTimeOfCommission(commissionTime);
		if (Objects.isNull(stEd) || stEd.length != 2 || Objects.isNull(stEd[0]) || Objects.isNull(stEd[1]))
			return;
		// 查询这次返利的黑名单
		List<Integer> blackList = accRebateDao.findBlackList(commissionTime);
		// 活动利率
		Map<String, List<BizAccountFlwActivity>> uidActivity = new HashMap<>();
		Map<Integer, BizFlwActivitySyn> activitySynMap = new HashMap<>();
		BizFlwActivitySyn syn = synFromCache(uidActivity, activitySynMap, more);
		BizAccountFlwActivity activity = activityFromCache(uidActivity, more);
		if (!avail4CommissionTime(syn, activity, stEd[0], stEd[1])) {
			syn = null;
			activity = null;
		}
		// 详情数据分组处理
		List<Long> summaryIdList = summaries.stream().map(BizAccountReturnSummary::getId).filter(Objects::nonNull)
				.collect(Collectors.toList());
		List<BizBankLogCommission> cms = bankLogCommissionService.findByReturnSummaryIdIn(summaryIdList);
		Map<Integer, List<BizBankLogCommission>> groupByAccId = cms.stream()
				.collect(Collectors.groupingBy(BizBankLogCommission::getAccountId));
		int l = summaries.size();
		BigDecimal totalAmount = BigDecimal.ZERO, totalTotal = BigDecimal.ZERO;
		List<RebateActivityCommission> items = new ArrayList<>();
		for (int index = 0; index < l; index++) {
			BizAccountReturnSummary summary = summaries.get(index);
			if (Objects.isNull(summary.getAccount()) || blackList.contains(summary.getAccount())) {
				logger.info("Approve4VerifyCommission 当天不返利的兼职账号 >> host: {} accId: {} time: {}  already compute.",
						CommonUtils.getInternalIp(), summary.getAccount(), commissionTime);
				continue;
			}
			List<BizBankLogCommission> commissions = groupByAccId.get(summary.getAccount());
			if (Objects.isNull(commissions))
				commissions = new ArrayList<>();
			// 返利时候 查询是否存在需要扣除的返佣，存在则需要扣除
			BigDecimal amount = summary.getAmount();
			BigDecimal total = summary.getTotalAmount();
			BigDecimal totalAgentAmount = summary.getTotalAgentAmount();
			AccountBaseInfo base = accSer.getFromCacheById(summary.getAccount());
			BizDeductAmount deductAmount = rebateUserService.deductAmountByUid(more.getUid());// 降低额度处理
			String FLAG = "返利时候扣除返佣";
			if (Objects.nonNull(deductAmount) && deductAmount.getAmount().compareTo(BigDecimal.ZERO) >= 0) {// 如果返佣多余需要扣除的返佣
				if (amount.compareTo(deductAmount.getAmount()) > 0) {
					rebateApiSer.deductAmount(more.getUid(), deductAmount.getAmount(), "0", base.getAccount(), FLAG);
					amount = amount.subtract(deductAmount.getAmount());
					String remarks = "从" + base.getAlias() + "返佣扣除佣金:" + deductAmount.getAmount();
					deductAmount.setRemark(CommonUtils.genRemark(deductAmount.getRemark(), remarks, new Date(), "系统"));
					deductAmount.setAmount(BigDecimal.ZERO);
					rebateUserService.saveDeductAmount(deductAmount);
				} else {// 返佣小于扣除的佣金
					rebateApiSer.deductAmount(more.getUid(), amount, "0", base.getAccount(), FLAG);
					deductAmount.setAmount(deductAmount.getAmount().subtract(amount));
					String remarks = "从" + base.getAlias() + "返佣扣除佣金:" + amount;
					deductAmount.setRemark(CommonUtils.genRemark(deductAmount.getRemark(), remarks, new Date(), "系统"));
					rebateUserService.saveDeductAmount(deductAmount);
					amount = BigDecimal.ZERO;
				}
			}
			// 代理佣金 扣除
			BigDecimal agentAmount = Objects.isNull(summary.getAgentAmount()) ? BigDecimal.ZERO
					: summary.getAgentAmount();
			if (Objects.nonNull(deductAmount) && Objects.nonNull(deductAmount.getAmount())
					&& deductAmount.getAmount().compareTo(BigDecimal.ZERO) > 0
					&& agentAmount.compareTo(BigDecimal.ZERO) > 0) {
				if (agentAmount.compareTo(deductAmount.getAmount()) > 0) {
					rebateApiSer.deductAmount(more.getUid(), deductAmount.getAmount(), "0", base.getAccount(), FLAG);
					agentAmount = agentAmount.subtract(deductAmount.getAmount());
					String remarks = "从" + base.getAlias() + "代理佣金扣除:" + deductAmount.getAmount();
					deductAmount.setRemark(CommonUtils.genRemark(deductAmount.getRemark(), remarks, new Date(), "系统"));
					deductAmount.setAmount(BigDecimal.ZERO);
					rebateUserService.saveDeductAmount(deductAmount);
				} else {// 返佣小于扣除的佣金
					rebateApiSer.deductAmount(more.getUid(), agentAmount, "0", base.getAccount(), FLAG);
					deductAmount.setAmount(deductAmount.getAmount().subtract(agentAmount));
					String remarks = "从" + base.getAlias() + "代理佣金扣除:" + agentAmount;
					deductAmount.setRemark(CommonUtils.genRemark(deductAmount.getRemark(), remarks, new Date(), "系统"));
					rebateUserService.saveDeductAmount(deductAmount);
					agentAmount = BigDecimal.ZERO;
				}
			}
			total = total.abs();
			amount = amount.abs();
			totalTotal = totalTotal.add(total);
			totalAmount = totalAmount.add(amount);
			List<ResV3DailyLogItem> tid = commission(commissions);
			BigDecimal activityAmount = Objects.isNull(summary.getActivityAmount()) ? BigDecimal.ZERO
					: summary.getActivityAmount();
			activityAmount = ActivityUtil.radix2(activityAmount);
			if (Objects.isNull(syn)) {
				if (Objects.nonNull(agentAmount) && agentAmount.compareTo(BigDecimal.ZERO) > 0) {
					commissionDaily(more, StringUtils.trimToEmpty(base.getAccount()), total, amount, commissionTime,
							base.getId(), agentAmount, totalAgentAmount);
				} else {
					rebateApiSer.commissionDaily(more, StringUtils.trimToEmpty(base.getAccount()), total, amount,
							commissionTime, tid, base.getId());
				}
				buildTotalFlowAndRebate(more, total, amount);
			} else {
				items.add(new RebateActivityCommission(base.getId(), StringUtils.trimToEmpty(base.getAccount()), total,
						amount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, commissionTime, activityAmount,
						agentAmount, totalAgentAmount));
			}
		}
		if (Objects.nonNull(syn)) {
			commissionActivityDaily(more, syn, activity, items, commissionTime);
			buildTotalFlowAndRebate(more, totalTotal, BigDecimal.ZERO);
		}
	}

	private void commissionDaily(BizAccountMore more, String acc, BigDecimal total, BigDecimal baseAmount, String time,
			int accId, BigDecimal agentAmount, BigDecimal totalAgentAmount) {
		String accTrim = StringUtils.trimToEmpty(acc);
		// 空转换
		agentAmount = Objects.isNull(agentAmount) ? BigDecimal.ZERO : agentAmount;
		baseAmount = Objects.isNull(baseAmount) ? BigDecimal.ZERO : baseAmount;
		total = Objects.isNull(total) ? BigDecimal.ZERO : total;
		// 小数位保留
		agentAmount = agentAmount.setScale(2, RoundingMode.HALF_UP);
		baseAmount = baseAmount.setScale(2, RoundingMode.HALF_UP);
		total = total.setScale(2, RoundingMode.HALF_UP);
		totalAgentAmount = totalAgentAmount.setScale(2, RoundingMode.HALF_UP);
		BigDecimal amount = baseAmount.add(agentAmount).setScale(2, RoundingMode.HALF_UP);
		BigDecimal balance = Objects.isNull(more.getBalance()) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: more.getBalance().setScale(2, RoundingMode.HALF_UP);
		BizAccount account = accSer.getById(accId);
		BigDecimal bankBalance = account.getBankBalance();
		bankBalance = Objects.isNull(bankBalance) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: bankBalance.setScale(2, RoundingMode.HALF_UP);
		String preCtn = accTrim + total + amount + balance + bankBalance + time;
		logger.info(
				"Approve4VerifyCommission CommissionDaily >> pre-data  acc: {} total: {} amount: {} balance: {} realTimeBalance: {}  time:{}  preCtn: {} UID:{}",
				accTrim, total, amount, balance, bankBalance, time, preCtn, more.getUid());
		Map<String, Object> param = new HashMap<>();
		param.put("acc", accTrim);
		param.put("total", total);
		param.put("amount", amount);
		param.put("balance", balance);
		param.put("realTimeBalance", bankBalance);
		param.put("time", time);
		param.put("uid", more.getUid());
		if (agentAmount.compareTo(BigDecimal.ZERO) > 0) {
			List<Map<String, BigDecimal>> commissions = new ArrayList<>();
			param.put("commissions", commissions);
			Map<String, BigDecimal> item = new HashMap<>();
			item.put("base", baseAmount);
			commissions.add(item);
		}
		param.put("token", CommonUtils.md5digest(preCtn + appPros.getRebatesalt()));
		String msg;
		try {
			msg = mapper.writeValueAsString(param);
		} catch (Exception e) {
			throw new RuntimeException("数据错误");
		}
		more.setBalance(Objects.isNull(more.getBalance()) ? amount : more.getBalance().add(amount));
		accMoreSer.saveAndFlash(more);
		BizAccountRebateDay rebateDay = new BizAccountRebateDay();
		rebateDay.setAccount(accId);
		rebateDay.setTotalAmount(total);
		rebateDay.setAmount(amount);
		rebateDay.setActivityAmount(null);
		rebateDay.setCreateTime(new Date());
		rebateDay.setCalcTime(time);
		accRebateDaySer.saveAndFlash(rebateDay);
		RebateHttpClient.getInstance().getPlatformServiceApi().rebateDaily(buildReqBody(msg)).subscribe(d -> {
			if (d.getStatus() == 1)
				logger.info("CommissionDaily >> send [ SUCCESS ] data: {}", msg);
			else
				logger.error("CommissionDaily >> send [FAIL ] {} data: {}", d.getMessage(), msg);
		}, e -> {
			if (StringUtils.isBlank(e.getMessage()) || !e.getMessage().contains("HTTP 500")) {
				logger.error("CommissionDaily >> errorExp  data: {} e:", msg, e);
				throw new RuntimeException("接口调用出问题");
			} else {
				logger.error("CommissionDaily >> [ HTTP500 ] data: {}", msg);
			}
		});
		if (agentAmount.compareTo(BigDecimal.ZERO) > 0) {
			String pre = accTrim + totalAgentAmount + amount + balance + bankBalance + time;
			Map<String, Object> agentParam = new HashMap<>();
			agentParam.put("acc", accTrim);
			agentParam.put("total", totalAgentAmount);
			agentParam.put("amount", amount);
			agentParam.put("balance", balance);
			agentParam.put("realTimeBalance", bankBalance);
			agentParam.put("time", time);
			agentParam.put("uid", more.getUid());
			List<Map<String, BigDecimal>> agentCommissions = new ArrayList<>();
			agentParam.put("commissions", agentCommissions);
			Map<String, BigDecimal> agentItem = new HashMap<>();
			agentItem.put("agent", agentAmount);
			agentCommissions.add(agentItem);
			agentParam.put("token", CommonUtils.md5digest(pre + appPros.getRebatesalt()));
			String agentMsg;
			try {
				agentMsg = mapper.writeValueAsString(agentParam);
			} catch (Exception e) {
				throw new RuntimeException("数据错误");
			}
			RebateHttpClient.getInstance().getPlatformServiceApi().rebateDaily(buildReqBody(agentMsg)).subscribe(d -> {
				if (d.getStatus() == 1)
					logger.info("CommissionDaily >> send [ SUCCESS ] data: {}", msg);
				else
					logger.error("CommissionDaily >> send [FAIL ] {} data: {}", d.getMessage(), msg);
			}, e -> {
				if (StringUtils.isBlank(e.getMessage()) || !e.getMessage().contains("HTTP 500")) {
					logger.error("CommissionDaily >> errorExp  data: {} e:", msg, e);
					throw new RuntimeException("接口调用出问题");
				} else {
					logger.error("CommissionDaily >> [ HTTP500 ] data: {}", msg);
				}
			});
		}
	}

	private void commissionActivityDaily(BizAccountMore more, BizFlwActivitySyn syn, BizAccountFlwActivity activity,
			List<RebateActivityCommission> cms, String time) {
		if (Objects.isNull(syn) || Objects.isNull(activity))
			return;
		// 兼职信息
		String uid = StringUtils.trimToEmpty(more.getUid());
		// 总活动赠送量
		BigDecimal hisTotalBalance = ActivityUtil.radix2(more.getBalance());
		BigDecimal curTotalBalance = hisTotalBalance;
		BigDecimal diffBalance = BigDecimal.ZERO;
		// 活动赠送额度
		BigDecimal totalTmp = ActivityUtil.radix2(more.getTmpMargin());// 总赠送额度
		BigDecimal tmpOfActivity = ActivityUtil.radix2(activity.getActivityTmpMargin());// 本此活动赠送额度
		// 兼职信用额度
		BigDecimal hisTotalMargin = ActivityUtil.radix2(more.getMargin());
		BigDecimal curTotalMargin = hisTotalMargin;
		BigDecimal diffMargin = BigDecimal.ZERO;
		// 活动额外返佣
		BigDecimal hisTotalActivity = ActivityUtil.radix2(more.getActivityTotalAmount());
		BigDecimal curTotalActivity = hisTotalActivity;
		BigDecimal diffActivity = BigDecimal.ZERO;
		// 任意账号
		RebateActivityCommission agentCm = null;
		for (RebateActivityCommission cm : cms) {
			BizAccount acc = accSer.getById(cm.getAccId());
			if (Objects.nonNull(acc)) {
				cm.setAcc(StringUtils.trimToEmpty(acc.getAccount()));
				cm.setRealTimeBalance(ActivityUtil.radix2(acc.getBankBalance()));
			}
			// 可提现额度
			cm.setBalance(hisTotalBalance);
			// 基本佣金转信用额度
			BigDecimal baseAmount = ActivityUtil.radix2(cm.getAmount());
			diffMargin = diffMargin.add(baseAmount);
			curTotalMargin = curTotalMargin.add(baseAmount);
			cm.setCreditValue(curTotalMargin);
			more.setMargin(curTotalMargin);
			// 活动额外返佣=>活动额外返佣总额
			BigDecimal activityAmount = ActivityUtil.radix2(cm.getActivityAmount());
			diffActivity = diffActivity.add(activityAmount);
			curTotalActivity = curTotalActivity.add(activityAmount);
			more.setActivityTotalAmount(curTotalActivity);
			if (Objects.isNull(activity.getActivityAmount())) {
				activity.setActivityAmount(activityAmount);
			} else {
				activity.setActivityAmount(activity.getActivityAmount().add(activityAmount));
			}
			// 活动额外转信用额度
			diffMargin = diffMargin.add(activityAmount);
			curTotalMargin = curTotalMargin.add(activityAmount);
			cm.setCreditValue(curTotalMargin);
			more.setMargin(curTotalMargin);
			if (Objects.nonNull(cm.getAgentAmount()) && cm.getAgentAmount().compareTo(BigDecimal.ZERO) > 0)
				agentCm = cm;
		}
		if (!CollectionUtils.isEmpty(cms)) {
			String rmk = String.format("额度变化 %s [%s %s] 额外返利变化 %s [%s %s]", diffMargin, hisTotalMargin, curTotalMargin,
					diffActivity, hisTotalActivity, curTotalActivity);
			more.setRemark(CommonUtils.genRemark(more.getRemark(), rmk, new Date(), "REB"));
			logger.info("Approve4VerifyCommission REBATE{} time: {} 额度变化 {} [{} {}] 额外返利变化 {} [{} {}]", uid, time,
					diffMargin, hisTotalMargin, curTotalMargin, diffActivity, hisTotalActivity, curTotalActivity);
		} else {
			logger.info("Approve4VerifyCommission REBATE{} time: {} 该用户在该统计日没有返利流 信用额度： {}  ", uid, time,
					hisTotalMargin);
		}
		boolean finish = curTotalMargin.compareTo(ActivityUtil.radix2(syn.getTopMargin())) >= 0;
		boolean JOIN_FLW_ACTIVITY_PARAMS_execute = false;
		int JOIN_FLW_ACTIVITY_PARAMS_status = 1;
		int JOIN_FLW_ACTIVITY_PARAMS_quitStatus = 0;
		BigDecimal JOIN_FLW_ACTIVITY_PARAMS_amount = BigDecimal.ZERO;
		if (finish) {// 正常结束
			// 1. 扣除活动额外返佣
			BigDecimal amtOfActivity = ActivityUtil.radix2(activity.getActivityAmount());
			curTotalActivity = curTotalActivity.subtract(amtOfActivity);
			more.setActivityTotalAmount(curTotalActivity);
			// 2. 扣除活动临时额度
			totalTmp = totalTmp.subtract(tmpOfActivity);
			more.setTmpMargin(totalTmp);
			// 3.修改活动完成标识
			more.setInFlwActivity(ActivityEnums.AccountMoreActivityInStatus.NO.getNum());
			activity.setUserEndTime(new Date());
			activity.setUserStatus(ActivityEnums.UserActivityStatus.FinishActivity.getNum());
			if (Objects.equals(syn.getActivityStatus(), ActivityEnums.ActivityStatus.CANCEL.getNum())) {
				activity.setUserStatus(ActivityEnums.UserActivityStatus.ActivityCancel.getNum());
			} else if (Objects.nonNull(syn.getActivityEndTime())
					&& System.currentTimeMillis() > syn.getActivityEndTime().getTime()) {
				activity.setUserStatus(ActivityEnums.UserActivityStatus.ActivityTimeFinish.getNum());
			}
			String rmk = String.format("活动正常结束 清空临时金额 %s 活动金额 %s", tmpOfActivity, amtOfActivity);
			more.setRemark(CommonUtils.genRemark(more.getRemark(), rmk, new Date(), "REB"));
			JOIN_FLW_ACTIVITY_PARAMS_execute = true;
			JOIN_FLW_ACTIVITY_PARAMS_quitStatus = 1;
			JOIN_FLW_ACTIVITY_PARAMS_amount = BigDecimal.ZERO;
			logger.info("Approve4VerifyCommission REBATE{} time: {} 活动正常结束 清空临时金额 {} 活动金额 {}", uid, time, tmpOfActivity,
					amtOfActivity);
		} else if (Objects.equals(syn.getActivityStatus(), ActivityEnums.ActivityStatus.CANCEL.getNum())) {// 异常结束
			// 1. 扣除活动额外返佣
			BigDecimal amtOfActivity = ActivityUtil.radix2(activity.getActivityAmount());
			curTotalActivity = curTotalActivity.subtract(amtOfActivity);
			more.setActivityTotalAmount(curTotalActivity);
			// 2. 扣除活动临时额度
			totalTmp = totalTmp.subtract(tmpOfActivity);
			more.setTmpMargin(totalTmp);
			// 4.活动：状态
			more.setInFlwActivity(ActivityEnums.AccountMoreActivityInStatus.NO.getNum());
			activity.setUserEndTime(new Date());
			activity.setUserStatus(ActivityEnums.UserActivityStatus.ActivityTimeFinish.getNum());
			if (Objects.equals(syn.getActivityStatus(), ActivityEnums.ActivityStatus.CANCEL.getNum()))
				activity.setUserStatus(ActivityEnums.UserActivityStatus.ActivityCancel.getNum());
			String rmk = String.format("活动异常结束 清空临时金额 %s 与活动金额 %s", tmpOfActivity, amtOfActivity);
			more.setRemark(CommonUtils.genRemark(more.getRemark(), rmk, new Date(), "REB"));
			JOIN_FLW_ACTIVITY_PARAMS_execute = true;
			JOIN_FLW_ACTIVITY_PARAMS_quitStatus = 2;
			JOIN_FLW_ACTIVITY_PARAMS_amount = BigDecimal.ZERO;
			logger.info("Approve4VerifyCommission REBATE{} time: {} 活动异常结束 清空临时金额 {} 与活动金额 {}", uid, time,
					tmpOfActivity, amtOfActivity);
		} else if (Objects.nonNull(syn.getActivityEndTime())
				&& System.currentTimeMillis() > syn.getActivityEndTime().getTime()) {// 活动到期
			// 1. 扣除活动额外返佣
			BigDecimal amtOfActivity = ActivityUtil.radix2(activity.getActivityAmount());
			curTotalActivity = curTotalActivity.subtract(amtOfActivity);
			more.setActivityTotalAmount(curTotalActivity);
			// 2.返佣额外佣金从兼职人员信用额度中扣除
			diffMargin = diffMargin.subtract(amtOfActivity);
			curTotalMargin = curTotalMargin.subtract(amtOfActivity);
			more.setMargin(curTotalMargin);
			// 3. 扣除活动临时额度
			totalTmp = totalTmp.subtract(tmpOfActivity);
			more.setTmpMargin(totalTmp);
			// 4.活动：状态
			more.setInFlwActivity(ActivityEnums.AccountMoreActivityInStatus.NO.getNum());
			activity.setUserEndTime(new Date());
			activity.setUserStatus(ActivityEnums.UserActivityStatus.ActivityTimeFinish.getNum());
			if (Objects.equals(syn.getActivityStatus(), ActivityEnums.ActivityStatus.CANCEL.getNum()))
				activity.setUserStatus(ActivityEnums.UserActivityStatus.ActivityCancel.getNum());
			String rmk = String.format("活动到期 清空临时金额 %s 与活动金额 %s 降额活动额外返利: %s", tmpOfActivity, amtOfActivity,
					amtOfActivity);
			more.setRemark(CommonUtils.genRemark(more.getRemark(), rmk, new Date(), "REB"));
			JOIN_FLW_ACTIVITY_PARAMS_execute = true;
			JOIN_FLW_ACTIVITY_PARAMS_quitStatus = 2;
			JOIN_FLW_ACTIVITY_PARAMS_amount = amtOfActivity;
			logger.info("Approve4VerifyCommission REBATE{} time: {} 活动到期 清空临时金额 {} 与活动金额 {} 降额活动额外返利: {}", uid, time,
					tmpOfActivity, amtOfActivity, amtOfActivity);
		}
		accMoreSer.saveAndFlash(more);
		more = accChgSer.calculateMoreLineLimit(more, diffMargin);
		activity = accountFlwActivityDao.saveAndFlush(activity);
		logger.info("Approve4VerifyCommission REBATE{} time: {} lineLimit 变化值 diffLineLimit： {} margin: {}", uid, time,
				diffMargin, more.getMargin());
		// 记录每日返佣记录
		for (RebateActivityCommission cm : cms) {
			BizAccountRebateDay rebateDay = new BizAccountRebateDay();
			rebateDay.setAccount(cm.getAccId());
			rebateDay.setTotalAmount(cm.getTotal());
			rebateDay.setAmount(cm.getAmount());
			rebateDay.setCreateTime(new Date());
			rebateDay.setCalcTime(time);
			rebateDay.setActivityAmount(
					Objects.isNull(cm.getActivityAmount()) ? BigDecimal.ZERO : cm.getActivityAmount());
			accRebateDaySer.saveAndFlash(rebateDay);
		}
		// 返利活动：发送返佣信息到返利网
		if (!CollectionUtils.isEmpty(cms)) {
			syncDoubleCreditEvent(uid, cms);
		}
		if (JOIN_FLW_ACTIVITY_PARAMS_execute) {
			joinFlwActivity(uid, syn.getActivityNumber(), JOIN_FLW_ACTIVITY_PARAMS_status,
					JOIN_FLW_ACTIVITY_PARAMS_quitStatus, JOIN_FLW_ACTIVITY_PARAMS_amount);
		}
		// 代理返佣
		if (Objects.nonNull(agentCm)) {
			commissionDaily(more, agentCm.getAcc(), agentCm.getTotal(), BigDecimal.ZERO, time, agentCm.getAccId(),
					agentCm.getAgentAmount(), agentCm.getTotalAgentAmount());
		}
	}

	@Override
	@Transactional
	public void saveSummary(BizAccountMore more, AccountBaseInfo base, BigDecimal total, BigDecimal amount,
			BigDecimal totalActivity, String time, List<BizBankLogCommission> commissions) {
		total = total.setScale(2, RoundingMode.HALF_UP);
		amount = amount.setScale(2, RoundingMode.HALF_UP);
		List<BizAccountReturnSummary> summaries = accountReturnSummaryDao.findByCalcTimeAndAccount(time, base.getId());
		BizAccountReturnSummary summary = CollectionUtils.isEmpty(summaries) ? null : summaries.get(0);
		if (Objects.nonNull(summary)) {
			summary.setTotalAmount(total);
			summary.setAmount(amount);
			summary.setActivityAmount(totalActivity);
			summary = accountReturnSummaryDao.saveAndFlush(summary);
		} else {
			BizAccountReturnSummary rebateDay = new BizAccountReturnSummary();
			rebateDay.setAgentAmount(null);
			rebateDay.setUid(StringUtils.trimToEmpty(more.getUid()));
			rebateDay.setAccount(base.getId());
			rebateDay.setTotalAmount(total);
			rebateDay.setAmount(amount);
			rebateDay.setActivityAmount(totalActivity);
			rebateDay.setCreateTime(new Date());
			rebateDay.setCalcTime(time);

			BigDecimal balance = Objects.isNull(more.getBalance()) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
					: more.getBalance().setScale(2, RoundingMode.HALF_UP);
			List<Integer> tmp = Stream.of(StringUtils.trimToEmpty(more.getAccounts()).split(","))
					.filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
			// 记录每天的可提现额度，只允许一个账号加上可提现额度，其它账号只加当前返利金额，统计的时候会sum，如果都加上 有问题。
			boolean flage = true;
			if (tmp.size() > 1)
				flage = CollectionUtils.isEmpty(returnSummaryDao.findReturnSummaryByCalcTime(time, tmp));
			if (flage) {
				// 没有参加活动才把本次返利加到余额里面
				if (null == more.getInFlwActivity() || 1 != more.getInFlwActivity()) {
					rebateDay.setBalance(balance.add(amount));
				} else {
					rebateDay.setBalance(balance);
				}
			} else {
				// 没有参加活动才把本次返利加到余额里面
				if (null == more.getInFlwActivity() || 1 != more.getInFlwActivity()) {
					rebateDay.setBalance(amount);
				}
			}

			String oldRemark = accRebateDao.findRemark(time);
			if (Objects.nonNull(oldRemark) && !Objects.equals(StringUtils.EMPTY, oldRemark)) {
				rebateDay.setRemark(oldRemark);
				rebateDay.setStatus(Integer.parseInt(accRebateDao.findStatus(time)));
			} else {
				rebateDay.setStatus(AccountRebateReturnSummaryStatus.Auditing.getStatus());
			}
			summary = accRebateDaySer.saveAndFlash(rebateDay);
		}
		if (Objects.isNull(summary))
			return;
		Long summaryId = summary.getId();
		commissions.forEach(p -> p.setReturnSummaryId(summaryId));
		bankLogCommissionService.save(commissions);
	}

	@Override
	@Transactional
	public void saveSummaryAgent(BizAccountMore more, BizRebateAgentSyn agent,
			List<BizAccountReturnSummary> upSummaryList, BigDecimal agentAmount, String time,
			BigDecimal totalAgentAmount) {
		BigDecimal balance = Objects.isNull(more.getBalance()) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: more.getBalance().setScale(2, RoundingMode.HALF_UP);
		balance = balance.add(agentAmount);
		if (!CollectionUtils.isEmpty(upSummaryList)) {
			BizAccountReturnSummary summary = upSummaryList.get(0);
			summary.setAgentAmount(agentAmount);
			summary.setUid(more.getUid());
			summary.setBalance(balance);
			summary.setTotalAgentAmount(totalAgentAmount);
			accountReturnSummaryDao.saveAndFlush(summary);
			logger.info("AgentSummary{} [ Agent Amount 0 ]  ->  agent: {}  {} account: {} agentAmount: {} end {} ",
					agent.getUid(), agent.getId(), CommonUtils.getInternalIp(), summary.getAccount(), agentAmount,
					time);
		} else {
			List<Integer> accounts = Stream.of(StringUtils.trimToEmpty(more.getAccounts()).split(","))
					.filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(accounts)) {
				logger.info(
						"AgentSummary{} [ The Parent Doesn't Have Accounts ]  ->  agent: {}  {}  agentAmount: {} end {} ",
						agent.getUid(), agent.getId(), CommonUtils.getInternalIp(), agentAmount, time);
				return;
			}
			Integer target = accounts.get(0);
			BizAccountReturnSummary summary = new BizAccountReturnSummary();
			summary.setAccount(target);
			summary.setTotalAmount(BigDecimal.ZERO);
			summary.setTotalAgentAmount(totalAgentAmount);
			summary.setAmount(BigDecimal.ZERO);
			summary.setActivityAmount(BigDecimal.ZERO);
			summary.setCreateTime(new Date());
			summary.setCalcTime(time);
			summary.setUid(more.getUid());
			summary.setAgentAmount(agentAmount);
			summary.setBalance(balance);
			accountReturnSummaryDao.saveAndFlush(summary);
			logger.info("AgentSummary{} [ Agent Amount 1 ]  ->  agent: {}  {} account: {} agentAmount: {} end {} ",
					agent.getUid(), agent.getId(), CommonUtils.getInternalIp(), summary.getAccount(), agentAmount,
					time);
		}
	}

	@Override
	public boolean avail4CommissionTime(BizFlwActivitySyn syn, BizAccountFlwActivity activity, Date start, Date end) {
		if (Objects.isNull(syn) || Objects.isNull(activity) || Objects.isNull(start) || Objects.isNull(end)) {
			return false;
		}
		if (Objects.nonNull(syn.getActivityStartTime()) && syn.getActivityStartTime().compareTo(end) > 0) {
			return false;
		}
		if (Objects.nonNull(syn.getActivityEndTime()) && syn.getActivityEndTime().compareTo(start) < 0) {
			return false;
		}
		if (Objects.nonNull(activity.getUserStartTime()) && activity.getUserStartTime().compareTo(end) > 0) {
			return false;
		}
		if (Objects.nonNull(activity.getUserEndTime()) && activity.getUserEndTime().compareTo(start) < 0) {
			return false;
		}
		return true;
	}

	public BizFlwActivitySyn synFromCache(Map<String, List<BizAccountFlwActivity>> cache4Activity,
			Map<Integer, BizFlwActivitySyn> cache4Syn, BizAccountMore more) {
		BizFlwActivitySyn syn = null;
		if (Objects.isNull(more))
			return syn;
		if (Objects.equals(ActivityEnums.AccountMoreActivityInStatus.YES.getNum(), more.getInFlwActivity())) {
			List<BizAccountFlwActivity> activities = cache4Activity.get(more.getUid());
			if (Objects.isNull(activities)) {
				activities = findInProccessByUid(more.getUid());
				cache4Activity.put(more.getUid(), activities == null ? Collections.EMPTY_LIST : activities);
				if (!CollectionUtils.isEmpty(activities)) {
					Integer activityId = activities.get(0).getActivityId();
					BizFlwActivitySyn tmp = flwActivitySynDao.findById2(activityId);
					cache4Syn.put(activityId, tmp);
				}
			}
			if (!CollectionUtils.isEmpty(activities))
				syn = cache4Syn.get(activities.get(0).getActivityId());
		}
		return syn;
	}

	public BizAccountFlwActivity activityFromCache(Map<String, List<BizAccountFlwActivity>> cache4Activity,
			BizAccountMore more) {
		BizAccountFlwActivity activity = null;
		if (Objects.isNull(more))
			return activity;
		if (Objects.equals(ActivityEnums.AccountMoreActivityInStatus.YES.getNum(), more.getInFlwActivity())) {
			List<BizAccountFlwActivity> activities = cache4Activity.get(more.getUid());
			if (Objects.isNull(activities)) {
				activities = findInProccessByUid(more.getUid());
				cache4Activity.put(more.getUid(), activities == null ? Collections.EMPTY_LIST : activities);
			}
			activity = CollectionUtils.isEmpty(activities) ? null : activities.get(0);
		}
		return activity;
	}

	private void joinFlwActivity(String userId, String activityNumber, int status, int quitStatus, BigDecimal amount)
			throws RuntimeException {
		userId = StringUtils.trimToEmpty(userId);
		activityNumber = StringUtils.trimToEmpty(activityNumber);
		amount = ActivityUtil.radix2(amount);
		String sy = userId + activityNumber + status;
		String token = CommonUtils.md5digest(sy + appPros.getRebatesalt());
		HashMap<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		params.put("activityNumber", activityNumber);
		params.put("status", status);
		params.put("quitStatus", quitStatus);
		params.put("amount", amount);
		params.put("token", token);
		Boolean[] ret = new Boolean[] { Boolean.FALSE };
		RebateHttpClient.getInstance().getPlatformServiceApi().joinFlwEvent(buildReqBody(params)).subscribe(d -> {
			if (d.getStatus() == 1) {
				ret[0] = true;
				logger.info("Approve4VerifyCommission JoinFlwActivity >> send [ SUCCESS ] data: {}  sy: {}", params,
						sy);
			} else {
				logger.error("Approve4VerifyCommission JoinFlwActivity >> send [FAIL ] {} data: {}  sy: {}",
						d.getMessage(), params, sy);
			}
		}, e -> logger.error("Approve4VerifyCommission JoinFlwActivity >> [ HTTP500 ] data: {}  sy: {}", params, sy));
		if (!ret[0])
			throw new RuntimeException();
	}

	private void syncDoubleCreditEvent(String uid, List<RebateActivityCommission> cms) throws RuntimeException {
		uid = StringUtils.trimToEmpty(uid);
		StringJoiner sj = new StringJoiner(StringUtils.EMPTY);
		sj.add(uid);
		Map<String, Object> params = new TreeMap<>();
		params.put("uid", uid);
		List<Map<String, Object>> data = new ArrayList<>();
		params.put("data", data);
		for (RebateActivityCommission cm : cms) {
			Map<String, Object> em1 = new TreeMap<>();
			String acc = StringUtils.trimToEmpty(cm.getAcc());
			em1.put("acc", acc);
			sj.add(acc);
			BigDecimal total = ActivityUtil.radix2(cm.getTotal());
			em1.put("total", total);
			sj.add(total.toString());
			BigDecimal amount = ActivityUtil.radix2(cm.getAmount());
			em1.put("amount", amount);
			sj.add(amount.toString());
			BigDecimal balance = ActivityUtil.radix2(cm.getBalance());
			em1.put("balance", balance);
			sj.add(balance.toString());
			BigDecimal creditValue = ActivityUtil.radix2(cm.getCreditValue())
					.subtract(ActivityUtil.radix2(cm.getActivityAmount()));
			em1.put("creditValue", creditValue);
			sj.add(creditValue.toString());
			BigDecimal realTimeBalance = ActivityUtil.radix2(cm.getRealTimeBalance());
			em1.put("realTimeBalance", realTimeBalance);
			sj.add(realTimeBalance.toString());
			String time = StringUtils.trimToEmpty(cm.getTime());
			em1.put("time", time);
			sj.add(time);
			em1.put("type", "0");
			sj.add("0");
			data.add(em1);

			Map<String, Object> em2 = new TreeMap<>();
			acc = StringUtils.trimToEmpty(cm.getAcc());
			em2.put("acc", acc);
			sj.add(acc);
			total = ActivityUtil.radix2(cm.getTotal());
			em2.put("total", total);
			sj.add(total.toString());
			BigDecimal activityAmount = ActivityUtil.radix2(cm.getActivityAmount());
			em2.put("amount", activityAmount);
			sj.add(activityAmount.toString());
			balance = ActivityUtil.radix2(cm.getBalance());
			em2.put("balance", balance);
			sj.add(balance.toString());
			creditValue = ActivityUtil.radix2(cm.getCreditValue());
			em2.put("creditValue", creditValue);
			sj.add(creditValue.toString());
			realTimeBalance = ActivityUtil.radix2(cm.getRealTimeBalance());
			em2.put("realTimeBalance", realTimeBalance);
			sj.add(realTimeBalance.toString());
			time = StringUtils.trimToEmpty(cm.getTime());
			em2.put("time", time);
			sj.add(time);
			em2.put("type", "1");
			sj.add("1");
			data.add(em2);
		}
		String sy = sj.toString();
		params.put("token", CommonUtils.md5digest(sj.toString() + appPros.getRebatesalt()));
		Boolean[] ret = new Boolean[] { Boolean.FALSE };
		RebateHttpClient.getInstance().getPlatformServiceApi().syncDoubleCreditEvent(buildReqBody(params))
				.subscribe(d -> {
					if (d.getStatus() == 1) {
						logger.info(
								"Approve4VerifyCommission SyncDoubleCreditEvent >> send [ SUCCESS ] data: {}  sy: {}",
								params, sy);
						ret[0] = true;
					} else {
						logger.error(
								"Approve4VerifyCommission SyncDoubleCreditEvent >> send [FAIL ] {} data: {}  sy: {}",
								d.getMessage(), params, sy);
					}
				}, e -> logger.error("Approve4VerifyCommission SyncDoubleCreditEvent >> [ HTTP500 ] data: {}  sy: {}",
						params, sy));
		if (!ret[0])
			throw new RuntimeException();
	}

	private RequestBody buildReqBody(Map<String, Object> params) {
		try {
			return buildReqBody(mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	private RequestBody buildReqBody(String params) {
		try {
			return RequestBody.create(MediaType.parse("application/json"), params);
		} catch (Exception e) {
			return null;
		}
	}

	private List<ResV3DailyLogItem> commission(List<BizBankLogCommission> commissions) {
		List<ResV3DailyLogItem> result = new ArrayList<>();
		for (BizBankLogCommission cm : commissions) {
			if (Objects.isNull(cm))
				continue;
			try {
				Commission commission = mapper.readValue(cm.getCommission(), Commission.class);
				if (Objects.isNull(commission))
					continue;
				result.add(new ResV3DailyLogItem(cm.getBankLogId(), cm.getAmount(), commission.getBaseCommission()));
			} catch (Exception e) {
				continue;
			}
		}
		return result;
	}

	private List<BizAccountFlwActivity> findInProccessByUid(String uid) {
		if (Objects.isNull((uid = StringUtils.trimToNull(uid))))
			return null;
		List<BizAccountFlwActivity> dataList = accountFlwActivityDao.findByUidAndUserStatus(uid,
				ActivityEnums.UserActivityStatus.InActivity.getNum());
		return CollectionUtils.isEmpty(dataList) ? null : dataList;
	}

	private BizAccountMore buildTotalFlowAndRebate(BizAccountMore more, BigDecimal dailyOutFlow,
			BigDecimal dailyRebate) {
		if (Objects.nonNull(more)) {
			if (more.getTotalOutFlow() == null || more.getTotalOutFlow().intValue() == 0) {
				Map<String, Map<String, BigDecimal>> totalMap = accRebateDaySer.getTotalRebateAndOutFlow();
				if (totalMap != null && totalMap.get(more.getMoible()) != null) {
					Map<String, BigDecimal> total = totalMap.get(more.getMoible());
					more.setTotalRebate(total.get("totalRebate"));
					more.setTotalOutFlow(total.get("totalFlow"));
				}
			} else {
				more.setTotalOutFlow(more.getTotalOutFlow().add(dailyOutFlow));
				BigDecimal curr = more.getTotalRebate() == null ? BigDecimal.ZERO : more.getTotalRebate();
				more.setTotalRebate(curr.add(dailyRebate));
			}
			accMoreSer.saveAndFlash(more);
		}
		return more;
	}

	@Override
	@Transactional
	public void saveSummary(BizAccountMore more, AccountBaseInfo base, String time) {
		List<BizAccountReturnSummary> summaries = accountReturnSummaryDao.findByCalcTimeAndAccount(time, base.getId());
		BizAccountReturnSummary summary = CollectionUtils.isEmpty(summaries) ? null : summaries.get(0);
		if (Objects.nonNull(summary)) {
			summary.setUid(StringUtils.trimToEmpty(StringUtils.trimToEmpty(more.getUid())));
			summary.setTotalAmount(BigDecimal.ZERO);
			summary.setAmount(BigDecimal.ZERO);
			summary.setActivityAmount(BigDecimal.ZERO);
			summary = accountReturnSummaryDao.saveAndFlush(summary);
		} else {
			BizAccountReturnSummary rebateDay = new BizAccountReturnSummary();
			rebateDay.setUid(StringUtils.trimToEmpty(StringUtils.trimToEmpty(more.getUid())));
			rebateDay.setAccount(base.getId());
			rebateDay.setTotalAmount(BigDecimal.ZERO);
			rebateDay.setAmount(BigDecimal.ZERO);
			rebateDay.setActivityAmount(BigDecimal.ZERO);
			rebateDay.setCreateTime(new Date());
			rebateDay.setCalcTime(time);
			BigDecimal balance = Objects.isNull(more.getBalance()) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
					: more.getBalance().setScale(2, RoundingMode.HALF_UP);
			List<Integer> tmp = Stream.of(StringUtils.trimToEmpty(more.getAccounts()).split(","))
					.filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
			// 记录每天的可提现额度，只允许一个账号加上可提现额度，其它账号只加当前返利金额，统计的时候会sum，如果都加上 有问题。
			boolean flage = true;
			if (tmp.size() > 1)
				flage = CollectionUtils.isEmpty(returnSummaryDao.findReturnSummaryByCalcTime(time, tmp));
			if (flage) {
				// 没有参加活动才把本次返利加到余额里面
				if (null == more.getInFlwActivity() || 1 != more.getInFlwActivity()) {
					rebateDay.setBalance(balance.add(BigDecimal.ZERO));
				} else {
					rebateDay.setBalance(balance);
				}
			}

			String oldRemark = accRebateDao.findRemark(time);
			if (Objects.nonNull(oldRemark) && !Objects.equals(StringUtils.EMPTY, oldRemark)) {
				rebateDay.setRemark(oldRemark);
				rebateDay.setStatus(Integer.parseInt(accRebateDao.findStatus(time)));
			} else {
				rebateDay.setStatus(AccountRebateReturnSummaryStatus.Auditing.getStatus());
			}
			summary = accRebateDaySer.saveAndFlash(rebateDay);
		}
	}

}
