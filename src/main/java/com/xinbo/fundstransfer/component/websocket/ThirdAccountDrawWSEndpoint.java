package com.xinbo.fundstransfer.component.websocket;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.service.OutwardTaskService;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.CopyOnWriteMap;
import org.springframework.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Objects;

/**
 * Created by Administrator on 2018/8/30.
 */
@Component
@ServerEndpoint(value = "/ws/thirdInAccountDraw", configurator = GetHttpSessionConfigurator.class)
public class ThirdAccountDrawWSEndpoint {
	private static final Logger LOG = LoggerFactory.getLogger(ThirdAccountDrawWSEndpoint.class);
	private static final CopyOnWriteMap<String, Session> sessionMap = new CopyOnWriteMap<>();

	@OnOpen
	public void open(Session session, EndpointConfig conf) throws Exception {
		// 该用户的sessionId
		String jSessionId = (String) conf.getUserProperties().get(AppConstants.JSESSIONID);
		LOG.info("Opened ThirdAccountDrawWSEndpoint  jSessionId: {} ", jSessionId);
		if (!CollectionUtils.isEmpty(sessionMap)) {
			sessionMap.forEach((k, o) -> {
				if (Objects.equals(k, jSessionId)) {
					sessionMap.remove(k);
				}
			});
		}
		sessionMap.put(jSessionId, session);
	}

	@OnClose
	public void close(Session session, CloseReason reason) throws Exception {
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
		LOG.info("close ThirdAccountDrawWSEndpoint  jSessionId: {} ", jSessionId);
	}

	@OnError
	public void error(Session session, Throwable error) {
		try {
			session.close();
		} catch (Exception e) {
			LOG.error("", error);
		}
	}

	// 给页面发消息
	public void sendMessage(String message) throws IOException {
		LOG.debug("ThirdAccountDrawWSEndpoint sendMessage: {}", message);
		if (CollectionUtils.isEmpty(sessionMap)) {
			return;
		}
		sessionMap.forEach((k, session) -> {
			try {
				if (session == null) {
					return;
				}
				session.getBasicRemote().sendText("FRESH_PAGE_THIRD_ACCOUNT");
			} catch (IOException e) {
				LOG.error("ThirdAccountDrawWSEndpoint 发送消息", e);
			}
		});
	}
}
