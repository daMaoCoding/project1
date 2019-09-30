package com.xinbo.fundstransfer.component.redis;

import com.xinbo.fundstransfer.component.websocket.IncomeRequestWebSocketEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IncomeApproveAWMessageListener extends MessageListenerAdapter {
	@Autowired
	IncomeRequestWebSocketEndpoint incomeRequestWebSocketEndpoint;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			incomeRequestWebSocketEndpoint.sendMessage4AliWechatAccountAssigned(message.toString());
		} catch (Exception e) {
			log.error("IncomeApproveAWMessageListener.onMessage error occured:", e);
		}
	}
}
