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
 * 转入按照流水匹配确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_WITHDRAW)
public class CommonWithdrawSuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(CommonWithdrawSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBankLog())
				|| Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return withdraw(handler, template, lg, param.getFee(), check);
	}

	protected boolean withdraw(SuccessHandler handler, StringRedisTemplate template, BizBankLog lg, BigDecimal fee,
			ReportCheck check) {
		if (Refunding == lg.getStatus() || Refunded == lg.getStatus() || Interest == lg.getStatus()
				|| Fee == lg.getStatus() || lg.getAmount().compareTo(BigDecimal.ZERO) > 0)
			return false;
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount());
		String TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.filter(p -> TR_AMT.compareTo(p.getAmt()) == 0 && SysBalTrans.SYS_REFUND != p.getSys()
						&& (SysBalUtils.radix(p) || StringUtils.isBlank(TO_ACC_3) && StringUtils.isBlank(TO_OWN_2)
								|| StringUtils.isNotBlank(TO_OWN_2) && Objects.equals(TO_OWN_2, p.getToOwn2Last())
								|| StringUtils.isNotBlank(TO_ACC_3) && Objects.equals(TO_ACC_3, p.getToAcc3Last())))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		List<SysBalTrans> reList = dataList.stream().filter(p -> !p.ackFr()).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(reList)) {
			// 在转账实体上报过程中，工具抓取转账后余额，银行还没有来得及扣除手续费,则：转账前余额-转账金额=转账后余额,而实际该笔交易有手续费
			reList = dataList.stream().filter(p -> p.ackFr() && p.getSysLgId() != 0).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(reList))
				return false;
			SysBalTrans tsout = reList.get(0);
			BizSysLog sys = storeHandler.findSysOne(lg.getFromAccount(), tsout.getSysLgId());
			if (Objects.isNull(sys))
				return false;
			boolean ret = false;
			// 反写：银行流水ID
			if (Objects.isNull(sys.getBankLogId()) || sys.getBankLogId() == 0) {
				// 设置系统账目银行流水ID
				sys.setBankLogId(lg.getId());
				sys = storeHandler.saveAndFlush(sys);
				check.init(new BigDecimal[] { sys.getBankBalance(), sys.getBalance() }, null);
				// 设置转账任务银行流水ID
				tsout.setBankLgId(lg.getId());
				String k = handler.deStruct(template, tsout, null, ACK_FR);
				logger.info("SB{} SB{} [ FLOW WITHDRAW MATCHED AFTER CONFIRMED ] >> amount: {} flowId: {}  msg: {}",
						tsout.getFrId(), tsout.getToId(), tsout.getAmt(), lg.getId(), k);
				ret = true;
			}
			if (Objects.isNull(fee) || fee.compareTo(BigDecimal.ZERO) == 0)
				return ret;
			// 设置系统账目手续费
			if (Objects.isNull(sys.getFee()) || sys.getFee().compareTo(BigDecimal.ZERO) == 0) {
				fee = fee.abs();
				BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), fee, null, true);
				check.init(bs, null);
				sys.setFee(fee.negate());
				storeHandler.saveAndFlush(sys);
				logger.info("SB{} SB{} [ FLOW WITHDRAW FEE ] >> amount: {} sysLogId: {} flowId: {} fee: {}",
						tsout.getFrId(), tsout.getToId(), tsout.getAmt(), sys.getId(), lg.getId(), fee);
				ret = true;
			}
			return ret;
		}
		SysBalTrans trans = reList.get(0);
		trans.setAckByCurrFlow(SysBalTrans.ACK_ACK);
		trans.setSys(SysBalTrans.SYS_SUB);
		trans.setAckTm(System.currentTimeMillis());
		BigDecimal[] bs = storeHandler.setSysBal(template, trans.getFrId(), trans.getAmt(), fee, true);
		if (lg.getBalance() != null && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, trans);
		long[] sg = storeHandler.transFrom(trans, fee, lg, bs);
		trans.setSysLgId(sg[0]);
		trans.setOppSysLgId(sg[1]);
		trans.setBankLgId(lg.getId());
		String k = handler.deStruct(template, trans, bs, ACK_FR);
		logger.info("SB{} SB{} [ FLOW WITHDRAW CONFIRMED ] >> amount: {} flowId: {}  msg: {}", trans.getFrId(),
				trans.getToId(), trans.getAmt(), lg.getId(), k);
		return true;
	}
}
