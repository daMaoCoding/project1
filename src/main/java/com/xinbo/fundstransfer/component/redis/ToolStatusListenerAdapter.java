package com.xinbo.fundstransfer.component.redis;

import com.xinbo.fundstransfer.component.websocket.SystemWebSocketEndpoint;
import com.xinbo.fundstransfer.service.ProblemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * 工具状态上报
 * 
 *
 *
 */
@Component
public class ToolStatusListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(ToolStatusListenerAdapter.class);
	@Autowired
	SystemWebSocketEndpoint systemWebSocketEndpoint;
	@Autowired
	ProblemService problemService;

	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("Received msg: {}", msg);
		try {
			systemWebSocketEndpoint.sendMessage(msg.toString());
		} catch (Exception e) {
			log.error("", e);
		}
	}

}
