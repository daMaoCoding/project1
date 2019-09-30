package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 转入按照余额确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_INBYBAL)
public class OthersInByBalSuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(OthersInByBalSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getBalance()) || Objects.isNull(param.getBenchmark()) || Objects.isNull(check))
			return false;
		return inByBal(handler, template, base, param.getBenchmark(), param.getBalance(), check);
	}

	protected boolean inByBal(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base,
			BigDecimal benchmark, BigDecimal realBal, ReportCheck check) {
		if (benchmark.compareTo(realBal) > 0) {
			return false;
		}
		BigDecimal income = realBal.subtract(benchmark);
		List<SysBalTrans> oppList = check.getTransInAll().stream()
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_OPPBAL)
						&& SysBalUtils.valid(p, Report.VALID_MILLIS_CURBAL) && !p.ackTo()
						&& p.getAmt().compareTo(income) == 0)
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(oppList)) {
			SysBalTrans ints = oppList.get(0);
			ints.setSys(SysBalTrans.SYS_SUB);
			ints.setAckTm(System.currentTimeMillis());
			ints.setAckByOppBal(SysBalTrans.ACK_ACK);
			BigDecimal[] bs = storeHandler.setSysBal(template, ints.getToId(), ints.getAmt(), null, false);
			bs[0] = realBal;
			check.init(bs, ints);
			long[] sg = storeHandler.transTo(ints, null, bs);
			ints.setSysLgId(sg[0]);
			ints.setOppSysLgId(sg[1]);
			String k = handler.deStruct(template, ints, bs, ACK_TO);
			logger.info(
					"SB{} SB{} [ OTHERS BANK BALANCE INBYBAL INCOME CONFIRMED ] >> amtin: {} benchmark: {} realBal: {}  msg: {}",
					ints.getFrId(), ints.getToId(), ints.getAmt(), benchmark, realBal, k);
			return true;
		}
		return false;
	}
}
