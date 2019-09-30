
package com.xinbo.fundstransfer.component.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.IncomeAuditWsEnum;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.IncomeAuditWs;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.CopyOnWriteMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@ServerEndpoint(value = "/ws/income", configurator = GetHttpSessionConfigurator.class)
public class IncomeRequestWebSocketEndpoint {
	private static Logger LOG = LoggerFactory.getLogger(IncomeRequestWebSocketEndpoint.class);
	private static CopyOnWriteMap<String, Session> sessionMap = new CopyOnWriteMap<>();// Key->jSessionId
	private static Timer timer = new Timer();
	ObjectMapper mapper = new ObjectMapper();
	private static RateLimiter RATE_LIMITER = RateLimiter.create(0.5);
	public final static String HANDICAP_ZONE_MANILA0_TAIWAN1 = "HANDICAP_ZONE_MANILA0_TAIWAN1";

	@OnOpen
	public void open(Session session, EndpointConfig conf) {
		RATE_LIMITER.acquire();
		String ORIGIN_FROM_AUDIT_COMP = "Audit=Comp";
		String jSessionId = (String) conf.getUserProperties().get(AppConstants.JSESSIONID);
		LOG.info("Opened new incomeRequest session. jSessionId: {} ", jSessionId);
		String QueryString = StringUtils.trimToEmpty(session.getQueryString());
		if (QueryString.contains(ORIGIN_FROM_AUDIT_COMP)) {
			closeOtherWSByJSessionId(true, AppConstants.HISTORY_WS, jSessionId);
			sessionMap.put(jSessionId, session);
			AllocateIncomeAccountService allocateService = SpringContextUtils
					.getBean(AllocateIncomeAccountService.class);
			SysUserProfileService profileService = SpringContextUtils.getBean(SysUserProfileService.class);
			// 注册
			int userId = CommonUtils.genFactorByJSessionId(jSessionId);
			SysUserService userService = SpringContextUtils.getBean(SysUserService.class);
			SysUser user = userService.findFromCacheById(userId);
			if (user == null) {
				sendInvalidMessage("非法用户,无法接单!");
				return;
			}
			SysUserProfile profile = profileService.findByUserIdAndPropertyKey(userId, HANDICAP_ZONE_MANILA0_TAIWAN1);
			if (Objects.nonNull(profile) && Objects.nonNull(profile.getPropertyValue())) {
				allocateService.registOrCancel(userId, true);
				// 入款审核人员分配：广播
				allocateService.allocate(Integer.valueOf(profile.getPropertyValue()), true);
				AssignAWInAccountService service = SpringContextUtils.getBean(AssignAWInAccountService.class);
				service.saveUserToRedis(userId, profile.getPropertyValue());
			} else {
				sendInvalidMessage("您的账号没有区域分类无法分配账号接单!");
				return;
			}
		}
	}

	private final void sendInvalidMessage(String message) {
		try {
			RedisService redisService = SpringContextUtils.getBean(RedisService.class);
			IncomeAuditWs noticeEntity = new IncomeAuditWs();
			noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.NOTASSIGNACCOUNT.ordinal());
			noticeEntity.setMessage(message);
			redisService.convertAndSend(RedisTopics.INCOME_REQUEST, mapper.writeValueAsString(noticeEntity));
		} catch (IOException e) {
			LOG.error("IncomeRequestWebSocketEndpoint.open profile null send message error:", e);
		}
	}

	@OnError
	public void error(Session session, Throwable error) {
		try {
			session.close();
		} catch (Exception e) {
			LOG.error("", error);
		}
	}

	@OnClose
	public void close(Session session, CloseReason reason) {
		try {
			if (reason != null && (StringUtils.equals(AppConstants.HISTORY_WS, reason.getReasonPhrase())
					|| StringUtils.equals(AppConstants.LOGOUT_WS, reason.getReasonPhrase()))) {
				return;
			}
			String jSessionId = null;
			for (String obj : sessionMap.keySet()) {
				jSessionId = sessionMap.get(obj).equals(session) ? obj : jSessionId;
			}
			if (jSessionId == null) {
				return;
			}
			sessionMap.remove(jSessionId);
			Integer userId = CommonUtils.genFactorByJSessionId(jSessionId);
			StringRedisTemplate redisTemplate = SpringContextUtils.getBean(RedisService.class).getStringRedisTemplate();
			String pattern4UserId = RedisKeys.genPattern4IncomeAuditAccountAllocateByUserId(userId);
			Set<String> keys = redisTemplate.keys(pattern4UserId);
			if (CollectionUtils.isEmpty(keys)) {
				return;
			}
			keys.forEach((p) -> redisTemplate.boundValueOps(p).expire(500, TimeUnit.MILLISECONDS));
			timer.schedule(new TimerTask() {
				public void run() {
					try {
						if (CollectionUtils.isEmpty(redisTemplate.keys(pattern4UserId))
								|| !SpringContextUtils.getBean(SysUserService.class).online(userId)) {
							SysUserProfileService profileService = SpringContextUtils
									.getBean(SysUserProfileService.class);
							// 注册
							SysUserProfile profile = profileService.findByUserIdAndPropertyKey(userId,
									HANDICAP_ZONE_MANILA0_TAIWAN1);
							if (Objects.nonNull(profile) && Objects.nonNull(profile.getPropertyValue())) {
								AllocateIncomeAccountService allocateService = SpringContextUtils
										.getBean(AllocateIncomeAccountService.class);
								allocateService.registOrCancel(userId, false);
								allocateService.allocate(Integer.valueOf(profile.getPropertyValue()), true);
							}
						}
					} catch (Exception e) {
						LOG.error("" + e);
					}
				}
			}, 640);
			LOG.debug("IncomeRequestWebSocketEndpoint.close userId  :{}", userId);
			AssignAWInAccountService service = SpringContextUtils.getBean(AssignAWInAccountService.class);
			service.deleteUserOnRedis(userId);
			LOG.debug("IncomeRequestWebSocketEndpoint.close AssignAWInAccountService.deleteUserOnRedis  userId  :{}",
					userId);
		} catch (Exception e) {
			LOG.error("IncomeRequestWebSocketEndpoint.close error  :", e);
		}
	}

	public void closeBySessionId(String wsType, String sessionId) {
		closeOtherWSByJSessionId(false, wsType, sessionId);
	}

	public void logout(Integer userId) {
		try {
			LOG.debug("IncomeRequestWebSocketEndpoint.logout userId:{}", userId);
			AllocateIncomeAccountService allocateService = SpringContextUtils
					.getBean(AllocateIncomeAccountService.class);
			// 注销
			boolean resultOfRegistOrCancel = allocateService.registOrCancel(userId, false);
			LOG.debug("IncomeRequestWebSocketEndpoint.logout userId:{},allocateService.allocate", userId);
			// 过滤userId的Key
			List<String> keys = sessionMap.keySet().stream()
					.filter((p) -> CommonUtils.genFactorByJSessionId(p) == userId).collect(Collectors.toList());
			// 关闭会话
			if (!CollectionUtils.isEmpty(keys)) {
				keys.forEach((p -> {
					try {
						sessionMap.remove(p)
								.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, AppConstants.LOGOUT_WS));
					} catch (Exception e) {
						LOG.error("关闭Session失败." + e);
					}
				}));
				// 重新分配
				if (resultOfRegistOrCancel) {
					SysUserProfileService profileService = SpringContextUtils.getBean(SysUserProfileService.class);
					SysUserProfile profile = profileService.findByUserIdAndPropertyKey(userId,
							HANDICAP_ZONE_MANILA0_TAIWAN1);
					if (Objects.nonNull(profile) && Objects.nonNull(profile.getPropertyValue())) {

						allocateService.registOrCancel(userId, true);
						allocateService.allocate(Integer.valueOf(profile.getPropertyValue()), true);
					}
				}

			}
			LOG.debug("IncomeRequestWebSocketEndpoint.logout userId:{},allocateService.allocate finish", userId);
			AssignAWInAccountService service = SpringContextUtils.getBean(AssignAWInAccountService.class);
			service.deleteUserOnRedis(userId);
			LOG.debug(
					"IncomeRequestWebSocketEndpoint.logout userId:{},AssignAWInAccountService.deleteUserOnRedis finish",
					userId);

		} catch (Exception e) {
			LOG.error("IncomeRequestWebSocketEndpoint.logout error  :", e);
		}
	}

	public void sendMessage(String message) throws IOException {
		LOG.debug("Income sendMessage: {}", message);
		sessionMap.forEach((k, o) -> {
			synchronized (k) {
				try {
					o.getBasicRemote().sendText(message);
				} catch (IOException e) {
					LOG.error("发送消息", e);
				}
			}
		});
	}

	public void sendAllocated(String msg) throws Exception {
		if (StringUtils.isBlank(msg)) {
			return;
		}
		String[] inf = msg.split(":");
		Integer userId = Integer.valueOf(inf[1]);
		sessionMap.forEach((k, o) -> {
			if (CommonUtils.genFactorByJSessionId(k) == userId) {
				List<IncomeAuditWs> wsList = new ArrayList<>();
				if (inf.length > 2) {
					AccountService accountService = SpringContextUtils.getBean(AccountService.class);
					for (int index = 2; index < inf.length; index++) {
						if (!StringUtils.isNumeric(inf[index])) {
							continue;
						}
						AccountBaseInfo base = accountService.getFromCacheById(Integer.valueOf(inf[index]));
						if (base == null) {
							continue;
						}
						String owner = base.getOwner();
						if (StringUtils.isNotBlank(owner)) {
							int len = owner.length();
							if (len <= 2) {
								owner = "*" + owner.substring(len - 1);
							}
							if (len >= 3) {
								owner = owner.substring(0, 1) + "*" + owner.substring(len - 1);
							}
						}
						wsList.add(new IncomeAuditWs(base.getId(), base.getAccount(), base.getStatus(),
								IncomeAuditWsEnum.FromAllocate, null, base.getBankType(), base.getBankName(),
								base.getAlias(), owner, ""));
					}
				}
				try {
					String sendMsg = mapper.writeValueAsString(wsList);
					synchronized (k) {
						try {
							LOG.debug("Income sendMessage,userId : {} sendMsg : {}", userId, sendMsg);
							o.getBasicRemote().sendText(sendMsg);
						} catch (IOException e) {
							LOG.error("发送消息", e);
						}
					}
				} catch (Exception e) {

				}
			}
		});
	}

	private void closeOtherWSByJSessionId(boolean broadCast, String wsType, String sessionId) {
		Session sessionHis = sessionMap.remove(sessionId);
		if (sessionHis != null) {
			try {
				sessionHis.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, wsType));
			} catch (Exception e) {
				LOG.error("关闭Session失败." + e);
			}
		}
		if (broadCast) {
			int userId = CommonUtils.genFactorByJSessionId(sessionId);
			String closeMsg = CommonUtils.genCloseMsg4WS(userId, CommonUtils.getInternalIp(),
					IncomeRequestWebSocketEndpoint.class, wsType, sessionId);
			SpringContextUtils.getBean(RedisService.class).convertAndSend(RedisTopics.CLOSE_WEBSOCKET, closeMsg);
		}
	}

	// 支付宝 微信入款账号分配之后 发送socket消息至页面刷新
	public void sendMessage4AliWechatAccountAssigned(String message) {
		if (StringUtils.isBlank(message)) {
			return;
		}
		if (CollectionUtils.isEmpty(sessionMap)) {
			return;
		}
		AssignAWInAccountService awInAccountService = SpringContextUtils.getBean(AssignAWInAccountService.class);

		IncomeAuditWs incomeAuditWs = null;
		try {
			incomeAuditWs = mapper.readValue(message, IncomeAuditWs.class);
		} catch (IOException e) {
			LOG.error("IncomeRequestWebSocketEndpoint.sendMessage4AliWechatAccountAssigned error:", e);
		}
		if (incomeAuditWs != null) {
			if (StringUtils.isNotBlank(incomeAuditWs.getOwner())) {
				String userId = incomeAuditWs.getOwner();
				HandicapService handicapService = SpringContextUtils.getBean(HandicapService.class);
				int zone = handicapService.findZoneByUserId(Integer.valueOf(userId));
				Set<String> onlineUsers = awInAccountService.getUsersOnLine(Integer.valueOf(zone));
				if (CollectionUtils.isEmpty(onlineUsers)) {
					return;
				}
				String messages = incomeAuditWs.getMessage();
				if (!onlineUsers.contains(userId)) {
					return;
				}
				sessionMap.forEach((k, o) -> {
					synchronized (k) {
						try {
							if (CommonUtils.genFactorByJSessionId(k) == Integer.valueOf(userId)) {
								o.getBasicRemote().sendText(messages);
							}
						} catch (IOException e) {
							LOG.error("发送消息", e);
						}
					}
				});
			} else {
				sessionMap.forEach((k, o) -> {
					synchronized (k) {
						try {
							o.getBasicRemote().sendText(message);
						} catch (IOException e) {
							LOG.error("IncomeRequestWebSocketEndpoint.sendMessage4AliWechatAccountAssigned error:", e);
						}
					}
				});
			}
		}
	}
}
