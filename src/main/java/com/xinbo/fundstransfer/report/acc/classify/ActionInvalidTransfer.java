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
 * 系统订单处理：失败
 */
@ErrorUp(Error.ERROR_ACC + SysAccPush.ActionInvalidTransfer)
public class ActionInvalidTransfer extends Error {

	public String deal(Long errorId, int target, Object data, String remark, SysUser operator, String[] others,
			List<ActionDeStruct> actionDeStructList) {
		if (Objects.isNull(data) || !(data instanceof SysBalTrans))
			return "信息有误";
		SysBalTrans ts = (SysBalTrans) data;
		try {
			saveAutoInvstRec(ts);
			if (SysBalTrans.REGIST_WAY_MAN == ts.getRegistWay()
					|| SysBalTrans.REGIST_WAY_MAN_MGR == ts.getRegistWay()) {
				return "人工出款任务不能进行自动排查";
			}
			if (Objects.nonNull(ts.getReslt()) && Objects.equals(1, ts.getReslt())) {
				return "当APP上报转账状态为1时不能进行自动排查";
			}
			if (StringUtils.isBlank(remark)) {
				remark = "无效转账";
			}
			BizBankLog frBank = ts.getFrBankLog();
			Date bkTdTm = Objects.isNull(frBank) ? null : frBank.getTradingTime();
			AccountBaseInfo base = accSer.getFromCacheById(target);
			if (SysBalTrans.TASK_TYPE_OUTMEMEBER == ts.getTaskType()) {
				deal4Outward(base, ts.getTaskId(), bkTdTm, remark, ts);
			} else if (SysBalTrans.TASK_TYPE_OUTREBATE == ts.getTaskType()) {
				deal4Rebate(base, ts.getTaskId(), bkTdTm, remark, ts);
			}
			return StringUtils.EMPTY;
		} catch (Exception e) {
			log.info("SB{} ERROR [ INVALID-TRANSFER ] >>  . msg: {} error: ", target, ((SysBalTrans) data).getMsg(),
					e.getLocalizedMessage());
			return "程序内部错误";
		}
	}

	private BizSysInvst saveAutoInvstRec(SysBalTrans ts) {
		int accountId = ts.getFrId();
		ErrorOpp opp = null;
		String summary = null;
		BigDecimal amount = ts.getAmt().abs().negate(), balance = null, bankBalance = null;
		// 第三方下发记录
		boolean flag3Th = false;
		if (ts.getFrId() != 0) {
			AccountBaseInfo frAcc = accSer.getFromCacheById(ts.getFrId());
			if (Objects.nonNull(frAcc) && Objects.equals(InThird, frAcc.getType())) {
				flag3Th = true;
				accountId = ts.getToId();
				amount = ts.getAmt().abs();
				summary = genSummaryTh3(frAcc.getBankName(), frAcc.getAccount());
				BizAccount toAcc = accSer.getById(ts.getToId());
				balance = Objects.isNull(toAcc) ? null : toAcc.getBalance();
				bankBalance = Objects.isNull(toAcc) ? null : toAcc.getBankBalance();
				opp = genOpp(frAcc);
			}
		}
		if (!flag3Th) {
			BizAccount frAcc = accSer.getById(ts.getFrId());
			balance = Objects.isNull(frAcc) ? null : frAcc.getBalance();
			bankBalance = Objects.isNull(frAcc) ? null : frAcc.getBankBalance();
			if (ts.getToId() != 0) {// 下发|内部中转|一键下发|测试转账
				opp = genOpp(accSer.getFromCacheById(ts.getToId()));
			} else if (SysBalTrans.TASK_TYPE_OUTREBATE == ts.getTaskType()) {// 返利提现
				opp = genOpp(accRebateDao.findById2(ts.getTaskId()));
			} else if (SysBalTrans.TASK_TYPE_OUTMEMEBER == ts.getTaskType()) {// 会员出款
				opp = genOpp(outwardTaskDao.findById2(ts.getTaskId()));
			} else {
				opp = new ErrorOpp();
			}
		}
		return sysInvstSer.saveAndFlush(accountId, null, amount, balance, bankBalance, opp.getOppId(),
				opp.getOppHandicap(), opp.getOppAccount(), opp.getOppOwner(), null, null,
				ts.getTaskId() == 0 ? null : opp.getOrderId(), opp.getOrderNo(), SysInvstType.InvalidTransfer.getType(),
				summary, StringUtils.EMPTY, null, new Date(), new Date(ts.getGetTm()),
				System.currentTimeMillis() - ts.getGetTm());
	}

	private String genSummaryTh3(String bankName, String acc) {
		acc = StringUtils.trimToEmpty(acc);
		String accSimp = String.format("...%s", (acc.length() <= 5 ? acc : acc.substring(acc.length() - 5)));
		return String.format("%s%s", StringUtils.trimToEmpty(bankName), accSimp);
	}
}
