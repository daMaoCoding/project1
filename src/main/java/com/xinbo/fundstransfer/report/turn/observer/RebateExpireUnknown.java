package com.xinbo.fundstransfer.report.turn.observer;

import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.SystemAccountConfiguration;
import com.xinbo.fundstransfer.report.fail.Common;
import com.xinbo.fundstransfer.report.turn.TurnHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 自动转待排查在一定时间内 {@code this#MILLIS_CHECK_POINT}，已上报但未确认的返利提现任务
 */
public class RebateExpireUnknown implements Observer {
	protected static final Logger logger = LoggerFactory.getLogger(RebateExpireUnknown.class);
	/**
	 * 15分钟
	 */
	private static final long MILLIS_CHECK_POINT = 900000;

	@Override
	public void update(Observable o, Object arg) {
		// if (!SystemAccountConfiguration.needAutoAssignIfTransactionFail())
		// return;
		try {
			// 加载引用数据
			TurnHandler handler = (TurnHandler) o;
			String checkTm = Common.yyyyMMddHHmmss.get()
					.format(new Date(System.currentTimeMillis() - MILLIS_CHECK_POINT));
			List<BizAccountRebate> rebateList = handler.getAccRebateDao()
					.getExpireUnknown(OutwardTaskStatus.Unknown.getStatus(), checkTm);
			if (CollectionUtils.isEmpty(rebateList))
				return;
			for (BizAccountRebate task : rebateList) {
				try {
					// 自动转待排查在一定时间内 {@code
					// this#MILLIS_CHECK_POINT}，已上报但未确认的返利提现任务
					handler.getRebateApiSer().failByRobot(buildTransfer(task));
					logger.info(
							"SB{} TURN{} [ REBATE EXPIRE UNKNOWN ] -> successfully . toAccount: {} toOwner: {} amount: {} tid: {}",
							task.getAccountId(), task.getId(), task.getToAccount(), task.getToHolder(),
							task.getAmount(), task.getTid());
				} catch (Exception e) {
					logger.info(
							"SB{} TURN{} [ REBATE EXPIRE UNKNOWN ] -> fail . toAccount: {} toOwner: {} amount: {} tid: {} e:",
							task.getAccountId(), task.getId(), task.getToAccount(), task.getToHolder(),
							task.getAmount(), task.getTid(), e);
				}
			}
		} catch (Exception e) {
			logger.error("TURN-ERROR [ REBATE EXPIRE UNKNOWN ] -> e:", e);
		}
	}

	private TransferEntity buildTransfer(BizAccountRebate task) {
		TransferEntity ret = new TransferEntity();
		ret.setFromAccountId(task.getAccountId());
		ret.setAccount(StringUtils.trimToNull(task.getToAccount()));
		ret.setTaskId(task.getId());
		ret.setAmount(task.getAmount().floatValue());
		ret.setOwner(StringUtils.trimToNull(task.getToHolder()));
		ret.setBankType(StringUtils.trimToNull(task.getToAccountType()));// Type对应的是ToAccountBank、BankAddr对应的是ToAccountName
		ret.setBankAddr(StringUtils.trimToNull(task.getToAccountInfo()));
		ret.setAcquireTime(System.currentTimeMillis());
		return ret;
	}
}
