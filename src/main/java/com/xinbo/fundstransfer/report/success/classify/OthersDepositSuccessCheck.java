package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 其他卡转入确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_DEPOSIT)
public class OthersDepositSuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(OthersDepositSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getBankLog()) || Objects.isNull(check))
			return false;
		int TP = base.getType();
		String FR_ACC_3 = SysBalUtils.last3letters(base.getAccount());
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return deposit(handler, template, base, FR_ACC_3, TP, lg, check);
	}

	/**
	 * 存入流水处理
	 */
	private boolean deposit(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base, String FR_ACC_3,
			int TP, BizBankLog lg, ReportCheck check) {
		if (Refunding == lg.getStatus() || Refunded == lg.getStatus() || Interest == lg.getStatus()
				|| Fee == lg.getStatus() || lg.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			return false;
		if (BindWechat == TP || BindAli == TP || ThirdCommon == TP || BindCommon == TP)
			return false;
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		String FRACC3 = SysBalUtils.last3letters(lg.getToAccount());
		String FROWN2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		List<SysBalTrans> dataList = check.getTransInAll().stream().filter(p -> SysBalTrans.SYS_REFUND != p.getSys())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		List<SysBalTrans> l = dataList.stream()
				.filter(p -> !p.ackTo() && p.getAmt().compareTo(TR_AMT) == 0
						&& (SysBalUtils.radix(p.getAmt()) || (StringUtils.isBlank(FRACC3)
								&& (StringUtils.isBlank(FROWN2) || Objects.equals(FROWN2, p.getFrOwn2Last()))
								|| StringUtils.isNotBlank(FRACC3) && Objects.equals(FRACC3, p.getFrAcc3Last()))))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(l)) {
			l = dataList.stream()
					.filter(p -> p.getOppBankLgId() == 0 && p.ackTo() && p.getAmt().compareTo(TR_AMT) == 0
							&& (SysBalUtils.radix(p.getAmt()) || (StringUtils.isBlank(FRACC3)
									&& (StringUtils.isBlank(FROWN2) || Objects.equals(FROWN2, p.getFrOwn2Last()))
									|| StringUtils.isNotBlank(FRACC3) && Objects.equals(FRACC3, p.getFrAcc3Last()))))
					.collect(Collectors.toList());
			if (CollectionUtils.isEmpty(l))
				return false;
			SysBalTrans tsIn = l.get(0);
			BizSysLog sys = storeHandler.findSysOne(check.getTarget(), tsIn.getOppSysLgId());
			if (Objects.nonNull(sys) && (Objects.isNull(sys.getBankLogId()) || sys.getBankLogId() == 0)) {
				sys.setBankLogId(lg.getId());
				storeHandler.saveAndFlush(sys);
				tsIn.setOppBankLgId(lg.getId());
				check.init(new BigDecimal[] { sys.getBankBalance(), sys.getBalance() }, tsIn);
				String k = handler.deStruct(template, tsIn, null, ACK_TO);
				logger.info("SB{} SB{} [ FLOW DEPOSIT MATCHED AFTER CONFIRMED ] >> amount: {} flowId: {}  msg: {}",
						tsIn.getFrId(), tsIn.getToId(), tsIn.getAmt(), lg.getId(), k);
			}
			return true;
		}
		SysBalTrans trans = l.get(0);
		trans.setSys(SysBalTrans.SYS_SUB);
		trans.setAckByOppFlow(SysBalTrans.ACK_ACK);
		trans.setAckTm(System.currentTimeMillis());
		BigDecimal[] bs = storeHandler.setSysBal(template, trans.getToId(), trans.getAmt(), null, false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, trans);
		long[] sg = storeHandler.transTo(trans, lg, bs);
		trans.setSysLgId(sg[0]);
		trans.setOppSysLgId(sg[1]);
		trans.setOppBankLgId(lg.getId());
		String k = handler.deStruct(template, trans, bs, ACK_TO);
		logger.info("SB{} SB{} [ FLOW DEPOSIT CONFIRMED ] >> amount: {} flowId: {}  msg: {}", trans.getFrId(),
				trans.getToId(), trans.getAmt(), lg.getId(), k);
		return true;
	}
}
