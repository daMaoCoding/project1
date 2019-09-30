package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.FeeUtil;
import com.xinbo.fundstransfer.RefundUtil;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.fail.*;
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
 * 如果上报的流水有其相应的订单与其匹配 则：该订单失败</br>
 * 前提条件：<br>
 * 1.该流水处理已经经过转出的相关确认 </br>
 * 2.该流水已经经过回冲0的相关确认
 */
@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_REFUND_1)
public class Refund1FailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(Refund1FailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(handler) || Objects.isNull(param)
				|| Objects.isNull(param.getBankLog()) || Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		if (Objects.nonNull(lg.getTaskId()) && lg.getTaskId() != 0)
			return false;
		Integer st = lg.getStatus();
		if (Objects.nonNull(st) && (Refunding == st || Refunded == st || Interest == st || Fee == st))
			return false;
		if (StringUtils.isNotBlank(lg.getSummary()) && !RefundUtil.noneRefund(lg.getSummary()))
			return false;
		return confirm4InBank(template, handler, base, lg, check) || confirm4Others(template, handler, base, lg, check);
	}

	private boolean confirm4Others(StringRedisTemplate template, FailHandler handler, AccountBaseInfo base,
			BizBankLog lg, ReportCheck check) {
		if (Objects.equals(Inbank, base.getType()))
			return false;
		if (lg.getAmount().compareTo(BigDecimal.ZERO) < 0) {
			if (Objects.isNull(lg.getBalance()))
				return false;
			// 上一条流水交易后余额大于该条交易后余额，则：该流水为非回冲流水
			BizBankLog last = bankLogSer.findByIdLessThanEqual(lg.getFromAccount(), lg.getId());
			if (Objects.isNull(last) || Objects.isNull(last.getBalance())
					|| last.getBalance().compareTo(lg.getBalance()) > 0)
				return false;
		}
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount());
		String TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		List<SysBalTrans> reList = check.getTransOutAll().stream()
				.filter(p -> TR_AMT.compareTo(p.getAmt()) == 0 && !p.ackTo()
						&& (SysBalUtils.radix(p) || StringUtils.isBlank(TO_ACC_3) && StringUtils.isBlank(TO_OWN_2)
								|| StringUtils.isNotBlank(TO_OWN_2) && Objects.equals(TO_OWN_2, p.getToOwn2Last())
								|| StringUtils.isNotBlank(TO_ACC_3) && Objects.equals(TO_ACC_3, p.getToAcc3Last()))
						&& SysBalTrans.SYS_REFUND != p.getSys())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		return (!CollectionUtils.isEmpty(reList)) && ack(template, handler, base, reList.get(0), lg, check);
	}

	private boolean confirm4InBank(StringRedisTemplate template, FailHandler handler, AccountBaseInfo base,
			BizBankLog lg, ReportCheck check) {
		if (!Objects.equals(Inbank, base.getType()))
			return false;
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount());
		String TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		if (StringUtils.isBlank(TO_ACC_3) && StringUtils.isBlank(TO_OWN_2))
			return false;
		List<SysBalTrans> reList = check.getTransOutAll().stream()
				.filter(p -> TR_AMT.compareTo(p.getAmt()) == 0 && SysBalTrans.SYS_REFUND != p.getSys() && !p.ackTo()
						&& (StringUtils.isNotBlank(TO_OWN_2) && Objects.equals(TO_OWN_2, p.getToOwn2Last())
								|| StringUtils.isNotBlank(TO_ACC_3) && Objects.equals(TO_ACC_3, p.getToAcc3Last())))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		return (!CollectionUtils.isEmpty(reList)) && ack(template, handler, base, reList.get(0), lg, check);
	}

	private boolean ack(StringRedisTemplate template, FailHandler handler, AccountBaseInfo base, SysBalTrans ts,
			BizBankLog lg, ReportCheck check) {
		ts.setAckTm(System.currentTimeMillis());
		if (ts.ackFr()) {
			{
				// 下面两行代码一定要放在@{code if(ts.ackFr()){ } 内
				ts.setSys(SysBalTrans.SYS_REFUND);
				ts.setAckByCurrFlow(SysBalTrans.ACK_CANCEL);
			}
			BizSysLog sysLog = storeHandler.findSysOne(check.getTarget(), ts.getSysLgId());
			BigDecimal fee = Objects.isNull(sysLog) ? BigDecimal.ZERO : sysLog.getFee();
			// 回冲手续费转换
			fee = FeeUtil.fee4Refund(base, fee);
			BigDecimal[] bs = storeHandler.setSysBal(template, ts.getFrId(), ts.getAmt(), fee.abs(), false);
			if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
				bs[0] = lg.getBalance();
			check.init(bs, ts);
			long[] sg = storeHandler.refund(ts, lg, bs);
			ts.setSysLgId(sg[0]);
			ts.setOppSysLgId(sg[1]);
			lg.setTaskId(ts.getTaskId());
			lg.setTaskType(ts.getTaskType());
		}
		// 把该笔流水标记为：冲正已处理
		lg.setStatus(BankLogStatus.Refunded.getStatus());
		bankLogSer.updateBankLog(lg.getId(), BankLogStatus.Refunded.getStatus());
		String remark = SysBalUtils.autoRemark4Refund(1, lg.getId());
		handler.fail(template, ts, lg, remark);
		String k = SysBalTrans.genMsg(ts);
		logger.info("SB{} SB{} [ FLOW REFUND_ CONFIRMED ] >> amount: {} flowId: {}  msg: {}", ts.getFrId(),
				ts.getToId(), ts.getAmt(), lg.getId(), k);
		return true;
	}
}