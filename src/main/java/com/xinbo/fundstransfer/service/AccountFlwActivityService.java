package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizAccountReturnSummary;
import com.xinbo.fundstransfer.domain.entity.BizBankLogCommission;
import com.xinbo.fundstransfer.domain.entity.activity.BizAccountFlwActivity;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.domain.entity.agent.BizRebateAgentSyn;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3RateItem;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface AccountFlwActivityService {

	List<ReqV3RateItem> findActivityByActivitySyn(BizFlwActivitySyn syn);

	void approve4VerifyCommission(BizAccountMore more, String commissionTime, List<BizAccountReturnSummary> summaries);

	void saveSummary(BizAccountMore more, AccountBaseInfo base, BigDecimal total, BigDecimal amount,
			BigDecimal totalActivity, String time, List<BizBankLogCommission> commissions);

	void saveSummaryAgent(BizAccountMore more, BizRebateAgentSyn agent, List<BizAccountReturnSummary> upSummaryList,
			BigDecimal agentAmount, String time, BigDecimal totalAgentAmount);

	void saveSummary(BizAccountMore more, AccountBaseInfo base, String time);

	boolean avail4CommissionTime(BizFlwActivitySyn syn, BizAccountFlwActivity activity, Date start, Date end);

	BizFlwActivitySyn synFromCache(Map<String, List<BizAccountFlwActivity>> cache4Activity,
			Map<Integer, BizFlwActivitySyn> cache4Syn, BizAccountMore more);

	BizAccountFlwActivity activityFromCache(Map<String, List<BizAccountFlwActivity>> cache4Activity,
			BizAccountMore more);
}
