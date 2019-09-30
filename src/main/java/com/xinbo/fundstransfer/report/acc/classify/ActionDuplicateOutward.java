package com.xinbo.fundstransfer.report.acc.classify;

import com.xinbo.fundstransfer.AppConstants;
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
 * 重复出款处理方式
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionDuplicateOutward)
public class ActionDuplicateOutward extends Error {

	/**
	 * others[0] orderNo ,others[1] orderType
	 */
	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (errorId == null || data == null || StringUtils.isBlank(remark) || operator == null || others == null
				|| others.length < 2 || StringUtils.isBlank(others[0]) || !StringUtils.isNumeric(others[1]))
			return "参数为空";
		if (!(data instanceof BizBankLog) || operator.getId() == AppConstants.USER_ID_4_ADMIN)
			return "参数非法";
		BizBankLog duplicate = (BizBankLog) data;
		String ret = checkBankLogAmount(false, duplicate.getAmount(), null);// 检测：流水金额
		if (StringUtils.isNotBlank(ret))
			return ret;
		ret = checkBankLog(duplicate.getFromAccount(), duplicate.getId());// 检测：流水是否已经被处理
		if (StringUtils.isNotBlank(ret))
			return ret;
		BizSysErr err = sysErrSer.findById(errorId);// 检测：BizSysErr信息
		ret = checkError(err, operator);
		if (StringUtils.isNotBlank(ret))
			return ret;
		BigDecimal[] bs = storeHandler.setSysBal(accountingStringRedisTemplate, target, duplicate.getAmount(), null,
				true);
		BigDecimal bankBal = bs[0], sysBal = bs[1];
		AccountBaseInfo base = accSer.getFromCacheById(target);
		Date d = new Date();
		String summary = StringUtils.trimToEmpty(duplicate.getSummary());
		String orderNo = others[0];
		ErrorOpp opp;
		int orderType = Integer.parseInt(others[1]);
		if (orderType == SysBalTrans.TASK_TYPE_OUTMEMEBER) {
			summary = "会员出款-" + summary;
			BizOutwardTask task = outwardTaskDao.findLatestByOrderNo(orderNo);
			if (Objects.isNull(task))
				return "会员出款任务不存在";
			opp = genOpp(task);
		} else if (orderType == SysBalTrans.TASK_TYPE_OUTREBATE) {
			summary = "返利提现-" + summary;
			BizAccountRebate rebate = accRebateDao.findLatestByTid(orderNo);
			if (Objects.isNull(rebate))
				return "返利提现任务不存在";
			opp = genOpp(rebate);
		} else {
			opp = genOpp(duplicate);
		}
		if (StringUtils.isBlank(opp.getOppAccount())) {
			opp.setOppAccount(duplicate.getToAccount());
		}
		if (StringUtils.isBlank(opp.getOppOwner())) {
			opp.setOppOwner(duplicate.getToAccountOwner());
		}
		if (opp.getOppHandicap() == null) {
			opp.setOppHandicap(base.getHandicapId());
		}
		Date occurTime = (Objects.isNull(duplicate.getCreateTime()) ? d : duplicate.getCreateTime());
		BizSysInvst actionFee = sysInvstSer.saveAndFlush(duplicate.getFromAccount(), duplicate.getId(),
				duplicate.getAmount(), sysBal, bankBal, opp.getOppId(), opp.getOppHandicap(), opp.getOppAccount(),
				opp.getOppOwner(), errorId, err.getBatchNo(), opp.getOrderId(), opp.getOrderNo(),
				SysInvstType.DuplicateOutward.getType(), summary, remark, operator.getId(), d, occurTime,
				d.getTime() - occurTime.getTime());
		saveErrorBankLog(duplicate, operator, SysInvstType.DuplicateOutward.getMessage());
		log.info("INVST{} [ DUPLICATEOUTWARD ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
				base.getId(), duplicate.getAmount(), operator.getUid(), remark, duplicate.getId(), actionFee.getId());
		return StringUtils.EMPTY;
	}
}
