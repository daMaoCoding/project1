package com.xinbo.fundstransfer.component.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import com.xinbo.fundstransfer.component.websocket.IncomeRequestWebSocketEndpoint;

@Component
public class IncomeAuditAccountAllocatedMessageListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(IncomeAuditAccountAllocatedMessageListenerAdapter.class);
	@Autowired
	IncomeRequestWebSocketEndpoint incomeRequestWebSocketEndpoint;

	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("Received msg: {}", msg);
		try {
			incomeRequestWebSocketEndpoint.sendAllocated(msg.toString());
		} catch (Exception e) {
			log.error("", e);
		}
	}
}
