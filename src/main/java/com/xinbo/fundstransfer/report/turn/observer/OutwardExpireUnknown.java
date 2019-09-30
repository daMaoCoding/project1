package com.xinbo.fundstransfer.report.turn.observer;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.report.SystemAccountConfiguration;
import com.xinbo.fundstransfer.report.turn.TurnHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * 自动转主管在一定时间内 {@code this#MILLIS_CHECK_POINT}，已上报但未确认的出款任务
 */
public class OutwardExpireUnknown implements Observer {
	protected static final Logger logger = LoggerFactory.getLogger(OutwardExpireAcknowledge.class);
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
			// 加载待处理数据
			List<BizOutwardTask> taskList = handler.getOTaskDao().getExpireUnknown(
					OutwardTaskStatus.Unknown.getStatus(), (System.currentTimeMillis() - MILLIS_CHECK_POINT) / 1000);
			if (CollectionUtils.isEmpty(taskList))
				return;
			SysUser operator = handler.getUserSer().findFromCacheById(AppConstants.USER_ID_4_ADMIN);
			for (BizOutwardTask task : taskList) {
				try {
					// 自动转主管在一定时间内 {@code this#MILLIS_CHECK_POINT}，已上报但未确认的出款任务
					handler.getAllocOSer().alterStatusToMgr(task, operator, null, task.getScreenshot());
					logger.info(
							"SB{} TURN{} [ OUTWARD EXPIRE UNKNOWN ] -> successfully . toAccount: {} toOwner: {} amount: {} orderNo: {}",
							task.getAccountId(), task.getId(), task.getToAccount(), task.getToAccountOwner(),
							task.getAmount(), task.getOrderNo());
				} catch (Exception e) {
					logger.info(
							"SB{} TURN{} [ OUTWARD EXPIRE UNKNOWN ] -> fail . toAccount: {} toOwner: {} amount: {} orderNo: {} e:",
							task.getAccountId(), task.getId(), task.getToAccount(), task.getToAccountOwner(),
							task.getAmount(), task.getOrderNo(), e);
				}
			}
		} catch (Exception e) {
			logger.error("TURN-ERROR [ OUTWARD EXPIRE UNKNOWN ] -> e:", e);
		}
	}
}
