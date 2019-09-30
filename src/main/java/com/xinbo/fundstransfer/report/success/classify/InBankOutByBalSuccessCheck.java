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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 入款卡 余额上报转出确认.
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_OUT_BY_BAL)
public class InBankOutByBalSuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(InBankOutByBalSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBalance())
				|| Objects.isNull(param.getAcc()) || Objects.isNull(check)) {
			return false;
		}
		out(handler, template, base, param.getBalance(), param.getAcc(), check);
		return true;
	}

	private void out(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base, BigDecimal realBal,
			BizAccount acc, ReportCheck check) {
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_CURBAL) && !p.ackFr()
						&& SysBalTrans.SYS_REFUND != p.getSys()
						&& (Objects.isNull(p.getAfter()) || p.getAfter().compareTo(BigDecimal.ZERO) == 0))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return;
		SysBalTrans tsout = dataList.get(0);
		// 入款卡余额确认
		// 1.转账前余额等于系统余额
		// 2.转账前余额-转账金额=转账后余额
		// 3.转账前余额-转账金额-手续费=转账后余额
		if (Objects.nonNull(acc) && Objects.nonNull(acc.getBalance())
				&& acc.getBalance().compareTo(tsout.getBefore()) == 0
				&& (tsout.getBefore().subtract(tsout.getAmt()).compareTo(realBal) == 0 || tsout.getBefore()
						.subtract(tsout.getAmt()).subtract(FeeUtil.fee(base.getBankType(), tsout.getAmt()))
						.subtract(realBal).abs().intValue() < Report.FEE_TOLERANCE
						&& SysBalUtils.fee050(tsout.getBefore().subtract(tsout.getAmt()).subtract(realBal).negate()))) {
			BigDecimal fee = tsout.getBefore().subtract(tsout.getAmt()).subtract(realBal);
			tsout.setSys(SysBalTrans.SYS_SUB);
			tsout.setAckByCurrTrans(SysBalTrans.ACK_ACK);
			BigDecimal[] bs = storeHandler.setSysBal(template, tsout.getFrId(), tsout.getAmt(), fee, true);
			bs[0] = realBal;
			check.init(bs, tsout);
			long[] sg = storeHandler.transFrom(tsout, fee, null, bs);
			tsout.setSysLgId(sg[0]);
			tsout.setOppSysLgId(sg[1]);
			String k = handler.deStruct(template, tsout, bs, ACK_FR);
			logger.info("SB{} SB{} [ BALANCE INBANK EQ CONFIRMED ] >> before: {}  after: {}  amount: {} msg: {}",
					tsout.getFrId(), tsout.getToId(), tsout.getBefore(), tsout.getAfter(), tsout.getAmt(), k);
			return;
		}
		// 入款卡的挂起操作,发生在 转账实体上报.(转账实体一上报，就挂起该转账任务，无论 转账前余额-转账金额~转账金额 是否成立)，
		// 而此处，余额上报仅仅起，修改转账实体中，对AFTER重新赋值(仅当AFTER IS NULL OR AFTER IS 0)
		dataList = dataList.stream().filter(p -> p.getAckTm() > 0).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return;
		tsout = dataList.get(0);
		tsout.setAfter(realBal);
		String k = handler.reWriteMsg(template, tsout, SysBalUtils.expireAck(tsout.getGetTm()), TimeUnit.MINUTES);
		logger.info("SB{} SB{} [ INBANK BANK BALANCE UPDATE AFTER ] >> before: {} amount: {} after: {}  msg: {}",
				tsout.getFrId(), tsout.getToId(), tsout.getBefore(), tsout.getAmt(), tsout.getAfter(), k);
	}
}
