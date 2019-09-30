package com.xinbo.fundstransfer.report.acc.classify;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.SysInvstType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.ActionDeStruct;
import com.xinbo.fundstransfer.report.SysBalUtils;
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
 * 重复流水处理方式:只处理入款卡转入流水（系统余额相应减去 重复流水的金额）
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionDuplicateStatement)
public class ActionDuplicateStatement extends Error {

	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (errorId == null || data == null || StringUtils.isBlank(remark) || operator == null)
			return "参数为空";
		if (!(data instanceof BizBankLog) || operator.getId() == AppConstants.USER_ID_4_ADMIN)
			return "参数非法";
		AccountBaseInfo base = accSer.getFromCacheById(target);
		BizBankLog duplicate = (BizBankLog) data;
		String ret = checkBankLog(duplicate.getFromAccount(), duplicate.getId());// 检测：流水是否已经被处理
		if (StringUtils.isNotBlank(ret))
			return ret;
		BizSysErr err = sysErrSer.findById(errorId);
		ret = checkError(err, operator);// 检测：BizSysErr信息
		if (StringUtils.isNotBlank(ret))
			return ret;
		// 处理入款卡的入款流水
		BigDecimal[] bs;
		if (Objects.equals(AccountType.InBank.getTypeId(), base.getType())
				&& duplicate.getAmount().compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal amt = SysBalUtils.radix2(duplicate.getAmount());
			bs = storeHandler.setSysBal(accountingStringRedisTemplate, base.getId(), amt.abs(), null, true);
		} else {
			BizAccount acc = accSer.getById(duplicate.getFromAccount());
			if (Objects.isNull(acc))
				return "账号不存在";
			bs = new BigDecimal[] { SysBalUtils.radix2(acc.getBankBalance()), SysBalUtils.radix2(acc.getBalance()) };
		}
		BigDecimal bankBal = bs[0], sysBal = bs[1];
		Date d = new Date();
		ErrorOpp opp = genOpp(duplicate);
		Date occurTime = (Objects.isNull(duplicate.getCreateTime()) ? d : duplicate.getCreateTime());
		BizSysInvst actionFee = sysInvstSer.saveAndFlush(duplicate.getFromAccount(), duplicate.getId(),
				duplicate.getAmount(), sysBal, bankBal, null, opp.getOppHandicap(), opp.getOppAccount(),
				opp.getOppOwner(), errorId, err.getBatchNo(), opp.getOrderId(), opp.getOrderNo(),
				SysInvstType.DuplicateStatement.getType(), duplicate.getSummary(), remark, operator.getId(), d,
				occurTime, d.getTime() - occurTime.getTime());
		saveErrorBankLog(duplicate, operator, SysInvstType.DuplicateStatement.getMessage());
		log.info("INVST{} [ DUPLICATESTATEMENT ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
				base.getId(), duplicate.getAmount(), operator.getUid(), remark, duplicate.getId(), actionFee.getId());
		return StringUtils.EMPTY;
	}
}
