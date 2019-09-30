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
import org.apache.shiro.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 人工内部转出 others[0] 对方账号流水号
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionManualTransOut)
public class ActionManualTransOut extends Error {

	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (errorId == null || data == null || StringUtils.isBlank(remark) || operator == null || others == null
				|| others.length < 1 || StringUtils.isBlank(others[0]))
			return "参数为空";
		if (!(data instanceof BizBankLog) || operator.getId() == AppConstants.USER_ID_4_ADMIN)
			return "参数非法";
		BizBankLog manual = (BizBankLog) data;
		String ret = checkBankLogAmount(false, manual.getAmount(), null);// 检测：流水金额
		if (StringUtils.isNotBlank(ret))
			return ret;
		ret = checkBankLog(manual.getFromAccount(), manual.getId());// 检测：流水是否已经被处理
		if (StringUtils.isNotBlank(ret))
			return ret;
		BizSysErr err = sysErrSer.findById(errorId);// 检测：BizSysErr信息
		ret = checkError(err, operator);
		if (StringUtils.isNotBlank(ret))
			return ret;
		List<BizAccount> oppAccList = accSer.findByAlias(others[0]);
		if (CollectionUtils.isEmpty(oppAccList))
			return "对方账号不存在";
		BizAccount oppAcc = oppAccList.get(0);
		AccountBaseInfo oppBase = accSer.getFromCacheById(oppAcc.getId());
		BigDecimal[] bs = storeHandler.setSysBal(accountingStringRedisTemplate, target, manual.getAmount(), null, true);
		BigDecimal bankBal = bs[0], sysBal = bs[1];
		Date d = new Date();
		String summary = StringUtils.trimToEmpty(manual.getSummary());
		ErrorOpp opp = genOpp(oppBase);
		Date occurTime = (Objects.isNull(manual.getCreateTime()) ? d : manual.getCreateTime());
		BizSysInvst actionMaual = sysInvstSer.saveAndFlush(manual.getFromAccount(), manual.getId(), manual.getAmount(),
				sysBal, bankBal, opp.getOppId(), opp.getOppHandicap(), opp.getOppAccount(), opp.getOppOwner(), errorId,
				err.getBatchNo(), opp.getOrderId(), opp.getOrderNo(), SysInvstType.ManualTransOut.getType(), summary,
				remark, operator.getId(), d, occurTime, d.getTime() - occurTime.getTime());
		saveErrorBankLog(manual, operator, SysInvstType.ManualTransOut.getMessage());
		log.info("INVST{} [ MANUALTRANSOUT ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}", target,
				manual.getAmount(), operator.getUid(), remark, manual.getId(), actionMaual.getId());
		return StringUtils.EMPTY;
	}
}
