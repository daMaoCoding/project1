package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 兼职人员提升信用额度 :确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_ENHANCECREDIT)
public class CommonEnhanceCreditSuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(CommonEnhanceCreditSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBankLog())
				|| Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return enhanceCredit(template, lg, check);
	}

	protected boolean enhanceCredit(StringRedisTemplate template, BizBankLog lg, ReportCheck check) {
		if (lg.getTaskType() == null || lg.getTaskType() != SysBalTrans.TASK_TYPE_INNER || lg.getTaskId() == null
				|| StringUtils.isBlank(lg.getOrderNo()) || StringUtils.startsWith(lg.getOrderNo().toUpperCase(), "I")
				|| lg.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			return false;
		BizIncomeRequest income = incomeDao.findById2(lg.getTaskId());
		if (Objects.isNull(income) || !Objects.equals(income.getType(), IncomeRequestType.RebateLimit.getType()))
			return false;
		BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), lg.getAmount().abs(), null, false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, null);
		storeHandler.enhanceCredit(lg, income, bs);
		logger.info("SB{} [ FLOW ENHANCE-CREDIT CONFIRMED ] >> amount: {} flowId: {}", lg.getFromAccount(),
				lg.getAmount(), lg.getId());
		return true;
	}
}
