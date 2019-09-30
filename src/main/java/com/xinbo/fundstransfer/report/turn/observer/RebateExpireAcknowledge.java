package com.xinbo.fundstransfer.report.turn.observer;

import java.util.*;

import com.xinbo.fundstransfer.service.AllocateTransService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.report.fail.Common;
import com.xinbo.fundstransfer.report.turn.TurnHandler;

/**
 * 自动转出一定时间内 {@code this#MILLIS_CHECK_POINT}，未确认上报的返利提现任务
 */
public class RebateExpireAcknowledge implements Observer {
	protected static final Logger logger = LoggerFactory.getLogger(RebateExpireAcknowledge.class);
	/**
	 * 7分钟
	 */
	private static final long MILLIS_CHECK_POINT = 420000;

	@Override
	public void update(Observable o, Object arg) {
		try {
			// 加载引用数据
			TurnHandler handler = (TurnHandler) o;
			String checkTm = Common.yyyyMMddHHmmss.get()
					.format(new Date(System.currentTimeMillis() - MILLIS_CHECK_POINT));
			List<BizAccountRebate> rebateList = handler.getAccRebateDao().getExpireRebateTask(checkTm);
			if (CollectionUtils.isEmpty(rebateList))
				return;
			SysUser operator = handler.getUserSer().findFromCacheById(AppConstants.USER_ID_4_ADMIN);
			Set<String> lastKeys = handler.getRedisSer().getStringRedisTemplate()
					.boundSetOps(RedisKeys.TASK_REPORT_LAST_STEP).members();
			AllocateTransService allocateTransService = handler.getAllocateTransService();
			for (BizAccountRebate task : rebateList) {
				try {
					if (lastKeys != null && lastKeys.contains(task.getId() + "") || !allocateTransService.isOnline(task.getAccountId())) {
						handler.getAccRebateDao().updRemarkOrStatus(task.getId(), "超过7分钟，并且完成最后一步，转待排查",
								OutwardTaskStatus.Failure.getStatus(), task.getStatus(), null);
						handler.getRedisSer().increment(RedisKeys.COUNT_FAILURE_TRANS, String.valueOf(task.getAccountId()), 1);
						logger.info("turnExpireOutwardTask>>待排查,taskid {} accid {}", task.getId(), task.getAccountId());
					} else {
						handler.getAccRebateSer().reAssignDrawing(operator, task.getId(),
								StringUtils.trimToEmpty(task.getRemark()) + "超过7分钟,无结果机器转出");
						handler.getRedisSer().increment(RedisKeys.COUNT_FAILURE_TRANS, String.valueOf(task.getAccountId()), 1);
						logger.info("turnExpireOutwardTask>>重新分配,taskid {} accid {}", task.getId(),
								task.getAccountId());
					}
				} catch (Exception e) {
					logger.error("turnExpireOutwardTask>>rebate task turn out error,taskid {} accid {}", task.getId(),
							task.getAccountId());
				}
			}
		} catch (Exception e) {
			logger.error("TURN-ERROR [ REBATE EXPIRE ACKNOWLEDGE ] -> e:", e);
		}
	}
}
