package com.xinbo.fundstransfer.component.redis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;

@Component
public class IncomeAuditAccountAllocatingMessageListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(IncomeAuditAccountAllocatingMessageListenerAdapter.class);
	@Autowired
	AllocateIncomeAccountService incomeAccountAllocateService;

	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("Received msg: {}", msg);
		try {
			if (StringUtils.isNumeric(msg.toString())) {
				incomeAccountAllocateService.allocate(Integer.valueOf(msg.toString()), false);
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

}
