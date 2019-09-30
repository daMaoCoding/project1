package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.FeeUtil;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.fail.*;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 根据回冲流水确定转账失败</br>
 * 流水摘要中含有回冲等关键字,程序能够自动识别为回冲流水的处理
 */
@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_REFUND_0)
public class Refund0FailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(Refund0FailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(handler) || Objects.isNull(param)
				|| Objects.isNull(check) || Objects.isNull(param.getBankLog()))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		// 回冲流水检查：流水状态为 回冲待处理/回冲已处理
		if (!Objects.equals(BankLogStatus.Refunding.getStatus(), lg.getStatus())
				&& !Objects.equals(BankLogStatus.Refunded.getStatus(), lg.getStatus()))
			return false;
		List<BizBankLog> bankList = storeHandler.findBankLogFromCache(base.getId());
		SysBalTrans trans = findRefundTrans(lg, bankList, check.getTransOutAll());
		if (Objects.isNull(trans))
			return false;
		if (SysBalTrans.SYS_REFUND == trans.getSys())
			return true;
		trans.setAckTm(System.currentTimeMillis());
		if (SysBalTrans.SYS_NONE == trans.getSys()) {
			trans.setAckByCurrFlow(SysBalTrans.ACK_CANCEL);
			trans.setSys(SysBalTrans.SYS_REFUND);
		} else if (SysBalTrans.SYS_SUB == trans.getSys()) {
			trans.setAckByCurrFlow(SysBalTrans.ACK_CANCEL);
			trans.setSys(SysBalTrans.SYS_REFUND);
			BizSysLog sys = storeHandler.findSysOne(check.getTarget(), trans.getSysLgId());
			BigDecimal fee = Objects.isNull(sys) ? BigDecimal.ZERO : sys.getFee();
			// 回冲手续费转换
			fee = FeeUtil.fee4Refund(base, fee);
			BigDecimal[] bs = storeHandler.setSysBal(template, trans.getFrId(), trans.getAmt(), fee.abs(), false);
			if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
				bs[0] = lg.getBalance();
			check.init(bs, trans);
			long[] sg = storeHandler.refund(trans, lg, bs);
			trans.setSysLgId(sg[0]);
			trans.setOppSysLgId(sg[1]);
			lg.setTaskId(trans.getTaskId());
			lg.setTaskType(trans.getTaskType());
		}
		String remark = SysBalUtils.autoRemark4Refund(0, lg.getId());
		handler.fail(template, trans, lg, remark);
		String k = SysBalTrans.genMsg(trans);
		logger.info("SB{} SB{} [ FLOW REFUND CONFIRMED ] >> amount: {} flowId: {}  msg: {}", trans.getFrId(),
				trans.getToId(), trans.getAmt(), lg.getId(), k);
		return true;
	}
}
