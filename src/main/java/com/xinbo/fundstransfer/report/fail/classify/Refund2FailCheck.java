package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.FeeUtil;
import com.xinbo.fundstransfer.RefundUtil;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.acc.SysAccPush;
import com.xinbo.fundstransfer.report.fail.*;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 流水姓名中 带有冲正字眼，或 流水对方姓名与汇款账号的姓名一样 ：该流水为回冲流水
 */
@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_REFUND_2)
public class Refund2FailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(Refund2FailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(handler) || Objects.isNull(param)
				|| Objects.isNull(param.getBankLog()) || Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		Integer st = lg.getStatus();
		if (Objects.nonNull(st) && (Refunding == st || Refunded == st || Interest == st || Fee == st))
			return false;
		String toOwner = lg.getToAccountOwner();
		if (StringUtils.isBlank(toOwner))
			return false;
		// 流水姓名中 带有冲正字眼，或 流水对方姓名与汇款账号的姓名一样 ：该流水为回冲流水
		if (!RefundUtil.refund(base.getBankType(), toOwner)
				&& !Objects.equals(SysBalUtils.last2letters(base.getOwner()), SysBalUtils.last2letters(toOwner)))
			return false;
		if (Objects.nonNull(lg.getTaskId()) && lg.getTaskId() > 0)
			return false;
		List<BizBankLog> bankList = storeHandler.findBankLogFromCache(base.getId());
		SysBalTrans ts = findRefundTrans(lg, bankList, check.getTransOutAll());
		if (Objects.isNull(ts))
			return false;
		if (SysBalTrans.SYS_REFUND == ts.getSys())
			return true;
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
			ts.setOppSysLgId(sg[1]);
			ts.setSysLgId(sg[0]);
			lg.setTaskId(ts.getTaskId());
			lg.setTaskType(ts.getTaskType());
		}
		// 把该笔流水标记为：冲正已处理
		lg.setStatus(BankLogStatus.Refunded.getStatus());
		bankLogSer.updateBankLog(lg.getId(), BankLogStatus.Refunded.getStatus());
		String remark = SysBalUtils.autoRemark4Refund(2, lg.getId());
		handler.fail(template, ts, lg, remark);
		String k = SysBalTrans.genMsg(ts);
		logger.info("SB{} SB{} [ FLOW REFUND_2 CONFIRMED ] >> amount: {} flowId: {}  msg: {}", ts.getFrId(),
				ts.getToId(), ts.getAmt(), lg.getId(), k);
		return true;
	}

}
