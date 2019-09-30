package com.xinbo.fundstransfer.report.acc.classify;

import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.SysInvstType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.ActionDeStruct;
import com.xinbo.fundstransfer.report.acc.Error;
import com.xinbo.fundstransfer.report.acc.ErrorOpp;
import com.xinbo.fundstransfer.report.acc.ErrorUp;
import com.xinbo.fundstransfer.report.acc.SysAccPush;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 回冲流水处理方式:
 * <p>
 * 1.内部中转任务:发现回冲，直接丢弃，不予处理
 * <p>
 * 2.返利往任务:发现回冲,直接取消该笔返利提现任务
 * <p>
 * 3.会员出款任务：发现回冲,转人工处理（原因：可能会员把提款账号录入错误，收款人姓名输入错误，人工出款可以进一步排查）
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionRefund)
public class ActionRefund extends Error {

	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (Objects.isNull(data) || !(data instanceof BizBankLog))
			return "信息有误";
		try {
			AccountBaseInfo base = accSer.getFromCacheById(target);
			BizBankLog refund = (BizBankLog) data;
			if (Objects.isNull(errorId) || errorId == 0) {
				return autoCognize(base, refund, remark);
			} else {
				return manualCognize(errorId, base, refund, operator, remark, others);
			}
		} catch (Exception e) {
			return "内部错误";
		}
	}

	/**
	 * 自动识别
	 */
	private String autoCognize(AccountBaseInfo base, BizBankLog refund, String remark) {
		if (Objects.isNull(refund.getStatus()) || Refunding != refund.getStatus() && Refunded != refund.getStatus())
			return "非回冲流水";
		if (StringUtils.isEmpty(remark)) {
			remark = "回冲" + refund.getId();
		}
		if (SysBalTrans.TASK_TYPE_OUTMEMEBER == refund.getTaskType()) {
			deal4Outward(base, refund.getTaskId(), refund.getTradingTime(), remark, null);
		} else if (SysBalTrans.TASK_TYPE_OUTREBATE == refund.getTaskType()) {
			deal4Rebate(base, refund.getTaskId(), refund.getTradingTime(), remark, null);
		}
		return StringUtils.EMPTY;
	}

	/**
	 * 人工识别
	 * 
	 * @param others
	 *            [0]:orderNo,[1]:orderType
	 */
	private String manualCognize(Long errorId, AccountBaseInfo base, BizBankLog refund, SysUser operator, String remark,
			String[] others) {
		if (others == null || others.length < 2 || StringUtils.isBlank(others[0]) || !StringUtils.isNumeric(others[1]))
			return "订单号为空";
		String orderNo = StringUtils.trimToEmpty(others[0]);
		int orderType = Integer.parseInt(others[1]);
		if (SysBalTrans.TASK_TYPE_OUTMEMEBER != orderType && SysBalTrans.TASK_TYPE_OUTREBATE != orderType)
			return "订单分类不符";
		String ret = checkBankLog(refund.getFromAccount(), refund.getId());// 检测：流水是否已经被处理
		if (StringUtils.isNotBlank(ret))
			return ret;
		BizSysErr err = sysErrSer.findById(errorId);// 检测：BizSysErr信息
		ret = checkError(err, operator);
		if (StringUtils.isNotBlank(ret))
			return ret;
		// 系统余额加上回冲金额
		BigDecimal[] bs = storeHandler.setSysBal(accountingStringRedisTemplate, base.getId(), refund.getAmount().abs(),
				null, false);
		BigDecimal bankBal = bs[0], sysBal = bs[1];
		Date d = new Date();
		String summary = StringUtils.trimToEmpty(refund.getSummary());
		if (orderType == SysBalTrans.TASK_TYPE_OUTMEMEBER)
			summary = "会员出款-" + summary;
		if (orderType == SysBalTrans.TASK_TYPE_OUTREBATE)
			summary = "返利提现-" + summary;
		ErrorOpp opp;
		if (orderType == SysBalTrans.TASK_TYPE_OUTREBATE) {// 返利提现处理
			BizAccountRebate rebate = accRebateDao.findLatestByTid(orderNo);
			if (Objects.isNull(rebate))
				return "返利提现任务不存在";
			deal4Rebate(base, rebate.getId(), refund.getTradingTime(), remark, null);
			opp = genOpp(rebate);
		} else {// 出款任务处理
			BizOutwardTask task = outwardTaskDao.findLatestByOrderNo(orderNo);
			if (Objects.isNull(task))
				return "会员出款任务不存在";
			deal4Outward(base, task.getId(), refund.getTradingTime(), remark, null);
			opp = genOpp(task);
		}
		Date occurTime = Objects.isNull(refund.getCreateTime()) ? d : refund.getCreateTime();
		BizSysInvst record = sysInvstSer.saveAndFlush(refund.getFromAccount(), refund.getId(), refund.getAmount(),
				sysBal, bankBal, opp.getOppId(), opp.getOppHandicap(), opp.getOppAccount(), opp.getOppOwner(), errorId,
				err.getBatchNo(), opp.getOrderId(), opp.getOrderNo(), SysInvstType.Refund.getType(), summary, remark,
				operator.getId(), d, occurTime, d.getTime() - occurTime.getTime());
		saveErrorBankLog(refund, operator, SysInvstType.Refund.getMessage());
		log.info("INVST{} [ REFUND MANUAL ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
				base.getId(), refund.getAmount(), operator.getUid(), remark, refund.getId(), record.getId());
		return StringUtils.EMPTY;
	}
}
