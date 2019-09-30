package com.xinbo.fundstransfer.component.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.assign.AssignOutwardTask;
import com.xinbo.fundstransfer.component.net.socket.MinaMonitorServer;
import com.xinbo.fundstransfer.component.websocket.IncomeRequestWebSocketEndpoint;
import com.xinbo.fundstransfer.component.websocket.SystemWebSocketEndpoint;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.CabanaStatus;
import com.xinbo.fundstransfer.newinaccount.service.InAccountService;
import com.xinbo.fundstransfer.runtime.ConcurrentUtils;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.unionpay.ysf.service.YSFService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.TreeMap;

/**
 * 通用监听适配类，根据Topic处理，注意：此类仅处理不是很频繁的消息，过于频繁的消息请单独处理
 *
 * 
 */
@Component
public class GeneralListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(GeneralListenerAdapter.class);
	@Autowired
	IncomeRequestWebSocketEndpoint incomeRequestWebSocketEndpoint;
	@Autowired
	SystemWebSocketEndpoint systemWebSocketEndpoint;
	@Autowired
	AllocateIncomeAccountService incomeAccountAllocateService;
	@Autowired
	AllocateOutwardTaskService outwardTaskAllocateService;
	@Autowired
	SysUserService userService;
	@Autowired
	HandicapService handicapService;
	@Autowired
	LevelService levelService;
	@Autowired
	AccountService accountService;
	@Autowired
	MenuInitialService menuInitialService;
	@Autowired
	MinaMonitorServer minaMonitorServer;
	@Autowired
	AllocateOutwardTaskService allocateOutwardTaskService;
	@Autowired
	AllocateTransferService allocateTransferService;
	@Autowired
	FeedBackService feedBackService;
	ObjectMapper mapper = new ObjectMapper();
	@Autowired
	FinBalanceStatService finBalanceStatService;
	@Autowired
	Environment environment;
	@Autowired
	SysDataPermissionService sysDataPermissionService;
	@Autowired
	AccountChangeService accountChangeService;
	@Autowired
	AccountMoreService accountMoreService;
	@Autowired
	ProblemService problemService;
	@Autowired
	QuickPayService quickPayService;
	@Autowired
	InAccountService inAccountService;
	@Autowired
	RebateUserService rebateUserService;
	@Autowired
	YSFService ysfService;
	@Autowired
	AssignOutwardTask assignOutwardTask;
	@Autowired
	RebateUserActivityService rebateUserActivityService;

	/**
	 * 系统设置，全局文件上传目录
	 */
	// @Value("${spring.http.multipart.location}")
	// private String deletePath;
	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("Received topic: {}，msg: {}", new String(topic), msg);
		try {
			switch (new String(topic)) {
			case RedisTopics.FRESH_INACCOUNT_YSFLOGIN_CACHE:
				ysfService.freshCache(msg.toString());
				break;
			case RedisTopics.FRESH_INACCOUNT_CACHE:
				inAccountService.freshCache(Long.valueOf(msg.toString()));
				break;
			case RedisTopics.REFRESH_LEVEL:
				levelService.flushCache(mapper.readValue(msg.toString(), BizLevel.class));
				break;
			case RedisTopics.REFRESH_ACCOUNT:
				accountService.flushCache(mapper.readValue(msg.toString(), AccountBaseInfo.class));
				break;
			case RedisTopics.REFRESH_ACCOUNT_LIST:
				accountService.flushCache();
				break;
			case RedisTopics.BROADCAST:
				systemWebSocketEndpoint.sendMessage(msg.toString());
				break;
			case RedisTopics.PUSH_MESSAGE_TOOLS:
				minaMonitorServer.messageSend(msg.toString());
				break;
			case RedisTopics.REFRESH_USER:
				userService.invalidateInCache(Integer.valueOf(msg.toString()));
				break;
			case RedisTopics.REFRESH_MENUPERMISSION:
				menuInitialService.invalidCache(Integer.valueOf(msg.toString()));
				break;
			case RedisTopics.REFRESH_SYSTEM_PROFILE:
				SysUserProfile userProfile = mapper.readValue(msg.toString(), SysUserProfile.class);
				MemCacheUtils.getInstance().getSystemProfile().put(userProfile.getPropertyKey(),
						userProfile.getPropertyValue());
				String propKey = userProfile.getPropertyKey();
				if (StringUtils.equals(propKey, UserProfileKey.FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY.getValue())) {
					// incomeAccountAllocateService.reloadFreezeAccount();
				}
				if (StringUtils.equals(propKey, UserProfileKey.INCOME_ACCOUNTS_PERUSER.getValue())) {
					// incomeAccountAllocateService.allocate(0, false);
					// incomeAccountAllocateService.allocate(1, false);
				}
				if (StringUtils.equals(propKey, UserProfileKey.OUTDRA_HALT_ALLOC_START_TIME.getValue())) {
					allocateOutwardTaskService.alterHaltTimeToNull();
				}
				// 如果修改定时取消入款参数，重启定时任务
				if (StringUtils.equals(propKey, UserProfileKey.INCOME_LIMIT_REQUEST_CANCEL.getValue())) {
					ConcurrentUtils.getInstance().restartCancelIncomeRequestTask();
				}
				// 银行维护
				if (StringUtils.equals(propKey, UserProfileKey.OUTDRAW_SYS_MAINTAIN_BANKTYPE.getValue())) {
					allocateOutwardTaskService.alterMaintainBank();
				}
				break;
			case RedisTopics.SYS_REBOOT:
				incomeAccountAllocateService.reboot();
				break;
			case RedisTopics.DELETED_SCREENSHOTS:
				finBalanceStatService.delFolder(
						environment.getProperty("funds.transfer.multipart.location") + "/screenshot", msg.toString());
				break;
			case RedisTopics.DELETED_FEEDBACK_SCREENSHOTS:
				feedBackService.deleteFeedBackImgs(msg.toString());
				break;
			case RedisTopics.ALLOC_OUT_TASK_SUSPEND:
				if (msg != null) {
					String[] inf = msg.toString().split(":");
					allocateOutwardTaskService.suspend(Integer.parseInt(inf[0]), Objects.equals(inf[1], "1"),
							Integer.parseInt(inf[2]));
				}
				break;
			case RedisTopics.REFRESH_ALL_HANDICAP:
				handicapService.flushCache();
				break;
			case RedisTopics.REFRESH_ALL_SYS_SETTING:
				MemCacheUtils.getInstance().loadingPreferencesData();
				break;
			case RedisTopics.REFRESH_OTASK_MERGE_LEVEL:
				String[] inf = msg.toString().split(":");
				int zone = Integer.valueOf(inf[0]);
				boolean on = Integer.valueOf(inf[1]) == 1;
				int durTm = Integer.parseInt(inf[2]);
				outwardTaskAllocateService.setMergeLevel(false, zone, on, durTm);
				break;
			case RedisTopics.REFRESH_USER_HANDICAP_PERMISSION_TOPIC:
				sysDataPermissionService.flushUserHandicapCache(mapper.readValue(msg.toString(), Integer.class));
				break;
			case RedisTopics.ACCOUNT_CHANGE_BROADCAST:
				if (Objects.isNull(msg))
					break;
				TreeMap<String, String> params = mapper.readValue(msg.toString(), TreeMap.class);
				String id = params.get("id");
				String bankBalance = params.get("bankBalance");
				String rptTm = params.get("rptTm");
				String logTm = params.get("logTm");
				String error = params.get("error");
				CabanaStatus status = new CabanaStatus();
				status.setId(Integer.valueOf(id));
				status.setBalance(StringUtils.isNotBlank(bankBalance) ? Float.parseFloat(bankBalance) : null);
				status.setError(StringUtils.trimToNull(error));
				status.setLogtime(StringUtils.isNotBlank(logTm) ? Long.valueOf(logTm) : null);
				status.setTime(StringUtils.isNoneBlank(rptTm) ? Long.valueOf(rptTm) : null);
				accountChangeService.monitor(status);
				break;
			case RedisTopics.REFRESH_ACCOUNT_MORE:
				accountMoreService.flushCache(mapper.readValue(msg.toString(), BizAccountMore.class));
				break;
			case RedisTopics.REFRESH_REBATE_USER:
				rebateUserService.flushCache(mapper.readValue(msg.toString(), BizRebateUser.class));
				break;
			case RedisTopics.ACCOUNT_MORE_CLEAN:
				accountMoreService.cleanCache();
				break;
			case RedisTopics.OTHER_ACCOUNT_CLEAN:
				quickPayService.cleanCache();
				break;
			case RedisTopics.DEL_ALLOCATEDID_AFTER_TRANSACK:
				outwardTaskAllocateService.addOrRemoveAllocated(mapper.readValue(msg.toString(), Integer.class), false);
				assignOutwardTask.addOrRemoveAllocated(mapper.readValue(msg.toString(), Integer.class), false);
				break;
			case RedisTopics.REBATE_USER_CLEAN:
				rebateUserService.cleanCache();
				break;
			default:
				break;
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}
}