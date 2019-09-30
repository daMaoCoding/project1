package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.InterestUtils;
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
import java.util.List;
import java.util.Objects;

/**
 * 结息处理
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_INTEREST)
public class CommonInterestSuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(CommonInterestSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBankLog())
				|| Objects.isNull(check)) {
			return false;
		}
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return interest(template, base, lg, check);
	}

	protected boolean interest(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		if (Interest == lg.getStatus() || InterestUtils.interest(base, lg)) {
			if (SystemAccountUtils.currentDayOfMonth() >= 21) {
				List<BizSysLog> sysList = storeHandler.findSysLogFromCache(base.getId());
				if (sysList.stream().filter(p -> lg.getAmount().compareTo(p.getAmount()) == 0
						&& Objects.equals(p.getType(), SysLogType.Interest.getTypeId())).count() > 0) {
					logger.info("SB{} [ FLOW INTEREST DUPLICATE ] >> amount: {} flowId: {}", lg.getFromAccount(),
							lg.getAmount(), lg.getId());
					return false;
				}
			}
			BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), lg.getAmount().abs(), null, false);
			if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
				bs[0] = lg.getBalance();
			Integer st = lg.getStatus();
			lg.setStatus(BankLogStatus.Interest.getStatus());
			check.init(bs, null);
			storeHandler.transInterest(lg.getFromAccount(), lg, bs);
			lg.setStatus(st);
			logger.info("SB{} [ FLOW INTEREST CONFIRMED ] >> amount: {} flowId: {}", lg.getFromAccount(),
					lg.getAmount(), lg.getId());
			return true;
		}
		return false;
	}
}
