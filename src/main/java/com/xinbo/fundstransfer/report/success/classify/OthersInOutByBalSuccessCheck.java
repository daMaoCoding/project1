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
 * 其他卡转入转出 被余额确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_INOUTBYBAL)
public class OthersInOutByBalSuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(OthersInOutByBalSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBenchmark())
				|| Objects.isNull(param.getBalance()) || Objects.isNull(check))
			return false;
		return inout(handler, template, base, param.getBenchmark(), param.getBalance(), check);
	}

	private boolean inout(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base,
			BigDecimal benchmark, BigDecimal realBal, ReportCheck check) {
		// 转出
		List<SysBalTrans> outList = check.getTransOutAll().stream()
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_CURBAL)
						&& SysBalUtils.valid(p, Report.VALID_MILLIS_CURBAL) && SysBalTrans.SYS_REFUND != p.getSys())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(outList) || outList.size() > 1)
			return false;
		outList = outList.stream().filter(p -> !p.ackFr()).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(outList))
			return false;
		// 转入
		List<SysBalTrans> inList = check.getTransInAll().stream()
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_OPPBAL)
						&& SysBalUtils.valid(p, Report.VALID_MILLIS_CURBAL) && !p.ackTo()
						&& SysBalTrans.SYS_REFUND != p.getSys())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(inList))
			return false;
		SysBalTrans tsOut = outList.get(0);
		SysBalTrans tsIn = inList.get(0);
		if (SysBalUtils.radix2(tsIn.getAmt().subtract(tsOut.getAmt()).abs())
				.compareTo(SysBalUtils.radix2(benchmark.subtract(realBal).abs())) != 0)
			return false;
		{
			// 转入
			tsIn.setSys(SysBalTrans.SYS_SUB);
			tsIn.setAckByOppBal(SysBalTrans.ACK_ACK);
			tsIn.setAckTm(System.currentTimeMillis());
			BigDecimal[] bs = storeHandler.setSysBal(template, tsIn.getToId(), tsIn.getAmt(), BigDecimal.ZERO, false);
			bs[0] = realBal.add(tsOut.getAmt());
			check.init(bs, tsIn);
			long[] sg = storeHandler.transTo(tsIn, null, bs);
			tsIn.setSysLgId(sg[0]);
			tsIn.setOppSysLgId(sg[1]);
			String k = handler.deStruct(template, tsIn, bs, ACK_TO);
			logger.info(
					"SB{} SB{} [ OTHERS BANK BALANCE INOUT-IN CONFIRMED ] >> amtin: {} benchmark: {} realBal: {}  msg: {}",
					tsIn.getFrId(), tsIn.getToId(), tsIn.getAmt(), benchmark, realBal, k);
		}
		{
			// 转出
			tsOut.setSys(SysBalTrans.SYS_SUB);
			tsOut.setAckByCurrBal(SysBalTrans.ACK_ACK);
			tsOut.setAckTm(System.currentTimeMillis());
			BigDecimal[] bs = storeHandler.setSysBal(template, tsOut.getFrId(), tsOut.getAmt(), BigDecimal.ZERO, true);
			bs[0] = realBal;
			check.init(bs, tsOut);
			long[] sg = storeHandler.transFrom(tsOut, BigDecimal.ZERO, null, bs);
			tsOut.setSysLgId(sg[0]);
			tsOut.setOppSysLgId(sg[1]);
			String k = handler.deStruct(template, tsOut, bs, ACK_FR);
			logger.info(
					"SB{} SB{} [ OTHERS BANK BALANCE INOUT-OUT CONFIRMED ] >> amtout: {} benchmark: {} realBal: {}  msg: {}",
					tsOut.getFrId(), tsOut.getToId(), tsOut.getAmt(), benchmark, realBal, k);
		}
		return true;
	}
}
