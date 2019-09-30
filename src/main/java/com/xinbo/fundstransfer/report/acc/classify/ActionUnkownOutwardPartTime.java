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
 * 盗刷流水处理方式:兼职所为
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionUnkownOutwardPartTime)
public class ActionUnkownOutwardPartTime extends Error {

	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (errorId == null || data == null || StringUtils.isBlank(remark) || operator == null)
			return "参数为空";
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
				err.getBatchNo(), null, null, SysInvstType.UnkownOutwardByPartTime.getType(), unkown.getSummary(),
				remark, operator.getId(), d, occurTime, d.getTime() - occurTime.getTime());
		if (Objects.nonNull(actionDeStructList)) {
			// 降低兼职人员信用额度//延迟执行
			ActionDeStruct deStruct = () -> {
				String ret_ = reduceCredit(err.getTarget(), unkown.getAmount(), operator.getUid(), remark);
				if (StringUtils.isBlank(ret_)) {// 降低信用额度成功
					log.info(
							"INVST{} [ UNKOWNOUTWARD-PARTTIME SUCCESS ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
							base.getId(), unkown.getAmount(), operator.getUid(), remark, unkown.getId(),
							actionFee.getId());
				} else {// 降低信用额度失败
					log.info(
							"INVST{} [ UNKOWNOUTWARD-PARTTIME ERROR ] >> {}  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
							base.getId(), ret_, unkown.getAmount(), operator.getUid(), remark, unkown.getId(),
							actionFee.getId());
				}
				return StringUtils.isBlank(ret_);
			};
			actionDeStructList.add(deStruct);
		} else {
			ret = reduceCredit(err.getTarget(), unkown.getAmount(), operator.getUid(), remark);
			if (StringUtils.isBlank(ret)) {// 降低信用额度成功
				log.info(
						"INVST{} [ UNKOWNOUTWARD-PARTTIME SUCCESS ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
						base.getId(), unkown.getAmount(), operator.getUid(), remark, unkown.getId(), actionFee.getId());
			} else {// 降低信用额度失败
				log.info(
						"INVST{} [ UNKOWNOUTWARD-PARTTIME ERROR ] >> {}  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
						base.getId(), ret, unkown.getAmount(), operator.getUid(), remark, unkown.getId(),
						actionFee.getId());
			}
		}
		saveErrorBankLog(unkown, operator, SysInvstType.UnkownOutwardByPartTime.getMessage() + "(降额)");
		return StringUtils.trimToEmpty(ret);
	}
}
