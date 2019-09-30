package com.xinbo.fundstransfer.component.redis;

import com.xinbo.fundstransfer.component.websocket.ThirdAccountDrawWSEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * Created by Administrator on 2018/8/30.
 */
@Component
public class ThirdAccountDrawWSEndpointMsgListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(ThirdAccountDrawWSEndpointMsgListenerAdapter.class);
	@Autowired
	private ThirdAccountDrawWSEndpoint thirdAccountDrawWSEndpoint;

	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("ThirdAccountDrawWSEndpointMsgListenerAdapter Received msg: {}", msg);
		try {
			return;
			// thirdAccountDrawWSEndpoint.sendMessage(msg.toString());
		} catch (Exception e) {
			log.error("ThirdAccountDrawWSEndpointMsgListenerAdapter send msg error :", e);
		}
	}
}
