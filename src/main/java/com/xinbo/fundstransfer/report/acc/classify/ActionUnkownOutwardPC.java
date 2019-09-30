package com.xinbo.fundstransfer.report.acc.classify;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.xinbo.fundstransfer.report.ActionDeStruct;
import org.apache.commons.lang3.StringUtils;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysErr;
import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.SysErrStatus;
import com.xinbo.fundstransfer.domain.enums.SysInvstType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.acc.Error;
import com.xinbo.fundstransfer.report.acc.ErrorUp;
import com.xinbo.fundstransfer.report.acc.SysAccPush;

/**
 * 盗刷流水处理方式:卡商所为
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionUnkownOutwardPC)
public class ActionUnkownOutwardPC extends Error {

	/**
	 * others[2]: 处理结果
	 */
	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (errorId == null || data == null || StringUtils.isBlank(remark) || operator == null || others == null
				|| others.length < 3)
			return "参数为空";
		if (Objects.equals(others[2], SysErrStatus.FinishedNormarl.getStatus().toString()))
			return "卡商盗刷:需永久冻结(走冻结卡流水)";
		if (!(data instanceof BizBankLog) || operator.getId() == AppConstants.USER_ID_4_ADMIN)
			return "参数非法";
		AccountBaseInfo base = accSer.getFromCacheById(target);
		BizBankLog unkown = (BizBankLog) data;
		String ret = checkBankLogAmount(false, unkown.getAmount(), null);
		if (StringUtils.isNotBlank(ret))
			return ret;
		ret = checkBankLog(unkown.getFromAccount(), unkown.getId());// 检测：流水是否已经被处理
		if (StringUtils.isNotBlank(ret))
			return ret;
		BizSysErr err = sysErrSer.findById(errorId);// 检测：BizSysErr信息
		ret = checkError(err, operator);
		if (StringUtils.isNotBlank(ret))
			return ret;
		// 从系统余额中扣除盗刷金额
		BigDecimal[] bs = storeHandler.setSysBal(accountingStringRedisTemplate, target, unkown.getAmount(), null, true);
		// 保存操作记录
		Date d = new Date();
		Date occurTime = (Objects.isNull(unkown.getCreateTime()) ? d : unkown.getCreateTime());
		BizSysInvst actionFee = sysInvstSer.saveAndFlush(unkown.getFromAccount(), unkown.getId(), unkown.getAmount(),
				bs[1], bs[0], null, base.getHandicapId(), unkown.getToAccount(), unkown.getToAccountOwner(), errorId,
				err.getBatchNo(), null, null, SysInvstType.UnkownOutwardByPC.getType(), unkown.getSummary(), remark,
				operator.getId(), d, occurTime, d.getTime() - occurTime.getTime());
		saveErrorBankLog(unkown, operator, SysInvstType.UnkownOutwardByPC.getMessage());
		log.info("INVST{} [ UNKOWNOUTWARD-PC ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
				base.getId(), unkown.getAmount(), operator.getUid(), remark, unkown.getId(), actionFee.getId());
		return StringUtils.EMPTY;
	}
}
