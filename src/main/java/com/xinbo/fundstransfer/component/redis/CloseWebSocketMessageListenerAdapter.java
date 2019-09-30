package com.xinbo.fundstransfer.component.redis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.websocket.IncomeRequestWebSocketEndpoint;
import com.xinbo.fundstransfer.component.websocket.SystemWebSocketEndpoint;
import com.xinbo.fundstransfer.component.websocket.TaskReviewWSEndpoint;

@Component
public class CloseWebSocketMessageListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(CloseWebSocketMessageListenerAdapter.class);
	@Autowired
	IncomeRequestWebSocketEndpoint incomeRequestWebSocketEndpoint;
	@Autowired
	SystemWebSocketEndpoint systemWebSocketEndpoint;
	@Autowired
	TaskReviewWSEndpoint taskReviewWSEndpoint;

	// 接收redis消息事件
	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("Received msg: {}", msg);
		if (null == msg) {
			return;
		}
		String[] strArray = msg.toString().split(":");
		Integer userId = Integer.valueOf(strArray[0]);
		String excIp = strArray.length > 1 ? StringUtils.trimToNull(strArray[1]) : null;
		String inclusiveWsEndpoint = strArray.length > 2 ? StringUtils.trimToNull(strArray[2]) : null;
		String wsType = strArray.length > 3 ? StringUtils.trimToNull(strArray[3]) : StringUtils.EMPTY;
		String jSessionId = strArray.length > 4 ? StringUtils.trimToNull(strArray[4]) : null;
		String localIp = CommonUtils.getInternalIp();
		boolean validIp = excIp == null || !StringUtils.equals(excIp, localIp);
		if (validIp && (inclusiveWsEndpoint == null
				|| StringUtils.equals(inclusiveWsEndpoint, SystemWebSocketEndpoint.class.getName()))) {
			if (AppConstants.LOGOUT_WS.equals(wsType)) {
				systemWebSocketEndpoint.logout(userId);
			} else {
				systemWebSocketEndpoint.closeBySessionId(wsType, jSessionId);
			}
		}
		if (validIp && (inclusiveWsEndpoint == null
				|| StringUtils.equals(inclusiveWsEndpoint, IncomeRequestWebSocketEndpoint.class.getName()))) {
			if (AppConstants.LOGOUT_WS.equals(wsType)) {
				incomeRequestWebSocketEndpoint.logout(userId);
			} else {
				incomeRequestWebSocketEndpoint.closeBySessionId(wsType, jSessionId);
			}
		}
		if (validIp && (inclusiveWsEndpoint == null
				|| StringUtils.equals(inclusiveWsEndpoint, TaskReviewWSEndpoint.class.getName()))) {
			if (AppConstants.LOGOUT_WS.equals(wsType)) {
				taskReviewWSEndpoint.logout(userId);
			} else {
				taskReviewWSEndpoint.closeBySessionId(wsType, jSessionId);
			}
		}
	}
}
