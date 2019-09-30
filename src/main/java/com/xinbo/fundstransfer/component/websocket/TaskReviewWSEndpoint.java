package com.xinbo.fundstransfer.component.websocket;

/**
 * Created by Administrator on 2018/7/7.
 */

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.CopyOnWriteMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.service.AsignFailedTaskService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

@Component
@ServerEndpoint(value = "/ws/taskReview", configurator = GetHttpSessionConfigurator.class)
@Slf4j
public class TaskReviewWSEndpoint {
	private static final CopyOnWriteMap<String, Session> sessionMap = new CopyOnWriteMap<>();// Key->jSessionId

	@OnOpen
	public void open(Session session, EndpointConfig conf) {
		try {
			// 该用户的sessionId
			String jSessionId = (String) conf.getUserProperties().get(AppConstants.JSESSIONID);
			if (!CollectionUtils.isEmpty(sessionMap)) {
				sessionMap.forEach((k, o) -> {
					if (Objects.equals(k, jSessionId)) {
						sessionMap.remove(k);
					}
				});
			}
			sessionMap.put(jSessionId, session);
			int userId = CommonUtils.genFactorByJSessionId(jSessionId);
			// session.getBasicRemote().sendText(String.valueOf(userId));
			log.info("订单排查接单 userId: {} ", userId);
		} catch (Exception e) {
			log.error("open error:", e);
		}
	}

	// 刷新的时候 关闭socket 点击退出的时候 也会调用
	@OnClose
	public void close(Session session, CloseReason reason) {
		try {
			if (reason != null && (StringUtils.equals(AppConstants.HISTORY_WS, reason.getReasonPhrase())
					|| StringUtils.equals(AppConstants.LOGOUT_WS, reason.getReasonPhrase()))) {
				return;
			}
			String jSessionId = null;
			if (sessionMap != null && sessionMap.size() > 0) {
				for (String obj : sessionMap.keySet()) {
					jSessionId = sessionMap.get(obj).equals(session) ? obj : jSessionId;
				}
			}
			if (jSessionId == null) {
				return;
			}
			sessionMap.remove(jSessionId);
			// 结束接单的时候
			if (reason.getCloseCode().getCode() == 4888) {
				log.debug("结束接单");
				return;
			}
		} catch (Exception e) {
			log.error("close socket error:", e);
		}

	}

	@OnError
	public void error(Session session, Throwable error) {
		try {
			if (session != null)
				session.close();
		} catch (Exception e) {
			log.error("", error);
		}
	}

	@OnMessage
	public void receiveMessage(Session session, String message) {
		try {
			if (StringUtils.isNotBlank(message)) {
				if ("STOP".equals(message)) {
					String jSessionId = null;
					for (String obj : sessionMap.keySet()) {
						jSessionId = sessionMap.get(obj).equals(session) ? obj : jSessionId;
					}
					if (jSessionId == null) {
						return;
					}
					sessionMap.remove(jSessionId);
				}
			}
		} catch (Exception e) {
			log.error("receive msg error :", e);
		}
	}

	// 给页面发消息
	public void sendMessage(String message) {
		try {
			sessionMap.forEach((k, session) -> {
				if (session != null) {
					synchronized (session) {
						try {
							session.getBasicRemote().sendText(message);
						} catch (IOException e) {
							log.error("TaskReviewWS 发送消息异常:", e);
						}
					}
				}
			});
		} catch (Exception e) {
			log.error("TaskReviewWS 发送消息异常:", e);
		}
	}

	private void closeOtherWSByJSessionId(boolean broadCast, String wsType, String sessionId) {
		Session sessionHis = sessionMap.remove(sessionId);
		if (sessionHis != null) {
			try {
				sessionHis.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, wsType));
			} catch (Exception e) {
				log.error("关闭Session失败." + e);
			}
		}
		if (broadCast) {
			int userId = CommonUtils.genFactorByJSessionId(sessionId);
			String closeMsg = CommonUtils.genCloseMsg4WS(userId, CommonUtils.getInternalIp(),
					TaskReviewWSEndpoint.class, wsType, sessionId);
			SpringContextUtils.getBean(RedisService.class).convertAndSend(RedisTopics.CLOSE_WEBSOCKET, closeMsg);
		}
	}

	public void logout(Integer userId) {
		try {
			// 如果退出登录
			if (userId != null) {
				AsignFailedTaskService asignFailedTaskService = SpringContextUtils
						.getBean(AsignFailedTaskService.class);
				RedisService redisService = SpringContextUtils.getBean(RedisService.class);
				StringRedisTemplate template = redisService.getStringRedisTemplate();
				asignFailedTaskService.removeUserAndTasksInRedis(userId, 3);
				template.convertAndSend(RedisTopics.ASIGN_REVIEWTASK_TOPIC, "ASIGNFAILEDTASK:STOP:" + userId);
			}
			// 过滤userId的Key 以下代码走不到 因为退出的时候会调用 onclose
			List<String> keys = sessionMap.keySet().stream()
					.filter((p) -> CommonUtils.genFactorByJSessionId(p) == userId).collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(keys)) {
				keys.forEach((p -> {
					try {
						sessionMap.remove(p)
								.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, AppConstants.LOGOUT_WS));
					} catch (Exception e) {
						log.error("关闭 Session 失败." + e);
					}
				}));
			}
		} catch (Exception e) {
			log.error("logout error: ", e);
		}
	}

	public void closeBySessionId(String wsType, String sessionId) {
		closeOtherWSByJSessionId(false, wsType, sessionId);
	}
}
