package com.xinbo.fundstransfer.report.acc.classify;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.SysInvstType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
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
 * 人工处理手续费流水:系统余额减去手续费，保存人工处理记录
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionFee)
public class ActionFee extends Error {

	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (errorId == null || data == null || StringUtils.isBlank(remark) || operator == null)
			return "参数为空";
		if (!(data instanceof BizBankLog) || operator.getId() == AppConstants.USER_ID_4_ADMIN)
			return "参数非法";
		AccountBaseInfo base = accSer.getFromCacheById(target);
		BizBankLog fee = (BizBankLog) data;
		String ret = checkBankLogAmount(false, fee.getAmount(), new BigDecimal(300));// 检测：流水金额
		if (StringUtils.isNotBlank(ret))
			return ret;
		ret = checkBankLog(fee.getFromAccount(), fee.getId());// 检测：流水是否已经被处理
		if (StringUtils.isNotBlank(ret))
			return ret;
		BizSysErr err = sysErrSer.findById(errorId);// 检测：BizSysErr信息
		ret = checkError(err, operator);
		if (StringUtils.isNotBlank(ret))
			return ret;
		// 从系统余额中扣除手续费
		BigDecimal[] bs = storeHandler.setSysBal(accountingStringRedisTemplate, target, fee.getAmount(), null, true);
		// 保存操作记录
		Date d = new Date();
		ErrorOpp opp = genOpp(fee);
		Date occurTime = (Objects.isNull(fee.getCreateTime()) ? d : fee.getCreateTime());
		BizSysInvst actionFee = sysInvstSer.saveAndFlush(fee.getFromAccount(), fee.getId(), fee.getAmount(), bs[1],
				bs[0], opp.getOppId(), opp.getOppHandicap(), opp.getOppAccount(), opp.getOppOwner(), errorId,
				err.getBatchNo(), opp.getOrderId(), opp.getOrderNo(), SysInvstType.Fee.getType(), fee.getSummary(),
				remark, operator.getId(), d, occurTime, d.getTime() - occurTime.getTime());
		log.info("INVST{} [ FEE ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}", base.getId(),
				fee.getAmount(), operator.getUid(), remark, fee.getId(), actionFee.getId());
		saveErrorBankLog(fee, operator, SysInvstType.Fee.getMessage());
		return StringUtils.EMPTY;
	}
}
