package com.xinbo.fundstransfer.component.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.AccountService;

@Component
public class AccountLevelMessageListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(AccountLevelMessageListenerAdapter.class);
	@Autowired
	private AccountService accountService;
	private static ObjectMapper mapper = new ObjectMapper();

	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("Received msg: {}", msg);
		try {
			AccountBaseInfo baseInfo = mapper.readValue(msg.toString(), AccountBaseInfo.class);
			accountService.flushCache(baseInfo);
		} catch (Exception e) {
			log.error("", e);
		}
	}

}
