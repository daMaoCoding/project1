package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * 入款卡-云闪付：APP漏抓流水,程序自动填充一笔入款系统账目，当流水抓上来时，数据处理
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK
		+ SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_YunSFAbsentIncomeByBankLog)
public class InBankYunSFAbsentIncomeByBankLogSuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(InBankYunSFAbsentIncomeByBankLogSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBankLog())
				|| Objects.isNull(check))
			return false;
		return yunSFAbsentIncomeByBankLog(base, param.getBankLog());
	}

	protected boolean yunSFAbsentIncomeByBankLog(AccountBaseInfo base, BizBankLog lg) {
		if (!SystemAccountUtils.ysf(base) || Objects.isNull(lg.getAmount())
				|| lg.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			return false;
		BigDecimal AMT = SysBalUtils.radix2(lg.getAmount());
		Optional<BizSysLog> optional = storeHandler.findSysLogFromCache(base.getId()).stream()
				.filter(p -> AMT.compareTo(p.getAmount()) == 0
						&& Objects.equals(p.getType(), SysLogType.Income.getTypeId())
						&& (Objects.isNull(p.getBankLogId()) || p.getBankLogId() == 0))
				.sorted((o1, o2) -> SysBalUtils
						.oneZeroMinus(o2.getCreateTime().getTime() - o1.getCreateTime().getTime()))
				.findFirst();
		if (!optional.isPresent())
			return false;
		BizSysLog sys = optional.get();
		sys.setOppOwner(lg.getToAccountOwner());
		sys.setOppAccount(lg.getToAccount());
		sys.setOrderNo(lg.getOrderNo());
		sys.setOrderId(lg.getTaskId());
		sys.setBankLogId(lg.getId());
		sys.setSummary(lg.getSummary() + "[" + lg.getBalance() + "]");
		storeHandler.saveAndFlush(sys);
		logger.info(
				"SB{} [ INBANK YUNSF ABSENT INCOME BY BANK LOG ] >>  bank: {} sys: {} amt: {} sysId: {}  oppAccount: {} oppOwner: {} summery: {}",
				lg.getFromAccount(), sys.getBankBalance(), sys.getBalance(), lg.getAmount(), sys.getId(),
				lg.getToAccount(), lg.getToAccountOwner(), sys.getSummary());
		return true;
	}
}
