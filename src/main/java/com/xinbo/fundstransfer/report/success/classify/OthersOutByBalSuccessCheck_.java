package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.FeeUtil;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
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
 * 转出按照余额确认(人工)
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_OUTBYBAL_)
public class OthersOutByBalSuccessCheck_ extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(OthersOutByBalSuccessCheck_.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getBalance()) || Objects.isNull(param.getBenchmark()) || Objects.isNull(check))
			return false;
		return out_(handler, template, base, param.getBenchmark(), param.getBalance(), check);
	}

	private boolean out_(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base,
			BigDecimal benchmark, BigDecimal realBal, ReportCheck check) {
		if (realBal.compareTo(benchmark) > 0)
			return false;
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_CURBAL)
						&& SysBalUtils.valid(p, Report.VALID_MILLIS_CURBAL)
						&& (p.getBefore().subtract(p.getAmt()).compareTo(realBal) == 0 || p.getBefore()
								.subtract(p.getAmt()).subtract(FeeUtil.fee(base.getBankType(), p.getAmt()))
								.subtract(realBal).abs().intValue() < Report.FEE_TOLERANCE
								&& SysBalUtils.fee050(p.getBefore().subtract(p.getAmt()).subtract(realBal).negate())))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList) || dataList.size() > 1)
			return false;
		dataList = dataList.stream().filter(p -> !p.ackFr()).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return false;
		SysBalTrans outts = dataList.get(0);
		BigDecimal fee = outts.getBefore().subtract(outts.getAmt()).subtract(realBal);
		BizAccount acc = accSer.getById(base.getId());
		if (Objects.isNull(acc) || Objects.isNull(acc.getBalance())
				|| acc.getBalance().subtract(realBal).compareTo(fee) == 0)
			return false;
		outts.setSys(SysBalTrans.SYS_SUB);
		outts.setAckByCurrBal(SysBalTrans.ACK_ACK);
		outts.setAckTm(System.currentTimeMillis());
		BigDecimal[] bs = storeHandler.setSysBal(template, outts.getFrId(), outts.getAmt(), fee, true);
		bs[0] = realBal;
		check.init(bs, outts);
		long[] sg = storeHandler.transFrom(outts, fee, null, bs);
		outts.setSysLgId(sg[0]);
		outts.setOppSysLgId(sg[1]);
		String k = handler.deStruct(template, outts, bs, ACK_FR);
		logger.info(
				"SB{} SB{} [ OTHERS BANK BALANCE OUT CONFIRMED ] >> before: {} amount: {} fee: {} realBal: {} benchmark: {}  msg: {}",
				outts.getFrId(), outts.getToId(), outts.getBefore(), outts.getAmt(), fee, realBal, benchmark, k);
		return true;
	}

}
