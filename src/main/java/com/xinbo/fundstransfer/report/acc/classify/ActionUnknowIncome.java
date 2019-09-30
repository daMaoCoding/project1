package com.xinbo.fundstransfer.report.acc.classify;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysErr;
import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.SysInvstType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.ActionDeStruct;
import com.xinbo.fundstransfer.report.acc.Error;
import com.xinbo.fundstransfer.report.acc.ErrorUp;
import com.xinbo.fundstransfer.report.acc.SysAccPush;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 额外收入处理方式
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionUnknowIncome)
public class ActionUnknowIncome extends Error {

	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (errorId == null || data == null || StringUtils.isBlank(remark) || operator == null)
			return "参数为空";
		if (!(data instanceof BizBankLog) || operator.getId() == AppConstants.USER_ID_4_ADMIN)
			return "参数非法";
		AccountBaseInfo base = accSer.getFromCacheById(target);
		BizBankLog unknow = (BizBankLog) data;
		String ret = checkBankLogAmount(true, unknow.getAmount(), null);// 检测：流水金额
		if (StringUtils.isNotBlank(ret))
			return ret;
		ret = checkBankLog(unknow.getFromAccount(), unknow.getId());// 检测：流水是否已经被处理
		if (StringUtils.isNotBlank(ret))
			return ret;
		BizSysErr err = sysErrSer.findById(errorId);// 检测：BizSysErr信息
		ret = checkError(err, operator);
		if (StringUtils.isNotBlank(ret))
			return ret;
		// 系统余额加上额外收入
		BigDecimal[] bs = storeHandler.setSysBal(accountingStringRedisTemplate, target, unknow.getAmount(), null,
				false);
		// 保存操作记录
		Date d = new Date();
		Date occurTime = (Objects.isNull(unknow.getCreateTime()) ? d : unknow.getCreateTime());
		BizSysInvst actionFee = sysInvstSer.saveAndFlush(unknow.getFromAccount(), unknow.getId(), unknow.getAmount(),
				bs[1], bs[0], null, base.getHandicapId(), unknow.getToAccount(), unknow.getToAccountOwner(), errorId,
				err.getBatchNo(), null, null, SysInvstType.UnknowIncome.getType(), unknow.getSummary(), remark,
				operator.getId(), d, occurTime, d.getTime() - occurTime.getTime());
		saveErrorBankLog(unknow, operator, SysInvstType.UnknowIncome.getMessage());
		log.info("INVST{} [ UNKOWNINCOME ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
				base.getId(), unknow.getAmount(), operator.getUid(), remark, unknow.getId(), actionFee.getId());
		return StringUtils.EMPTY;
	}
}
