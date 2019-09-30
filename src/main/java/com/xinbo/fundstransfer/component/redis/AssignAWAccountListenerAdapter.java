package com.xinbo.fundstransfer.component.redis;

import com.xinbo.fundstransfer.service.RedisService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageConsumer;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageType;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Administrator on 2018/10/18.
 */
@Slf4j
@Component
public class AssignAWAccountListenerAdapter extends MessageListenerAdapter {
	@Autowired
	private AllocateIncomeAccountService allocateIncomeAccountService;
	@Autowired
	private MessageConsumer messageConsumer;
	@Autowired
	private RedisService redisService;

	@Override
	@Transactional
	public void onMessage(Message message, byte[] pattern) {
		if (StringUtils.isBlank(message.toString())) {
			return;
		}
		boolean hasRight = allocateIncomeAccountService.checkHostRunRight();
		if (!hasRight) {
			log.info("host:{},AssignAWAccountListenerAdapter receive message :{},but has no execute right!",
					CommonUtils.getInternalIp(), message.toString());
			return;
		}
		try {
			if (StringUtils.isNotBlank(message.toString())) {
				String[] msg = message.toString().split(":");
				if (msg.length == 0) {
					return;
				}
				switch (msg[0]) {
				case "ASSIGNAWACCOUNTSTART":
					messageConsumer.consumeMessage(MessageType.INCOME_APPROVE_START, msg[1]);
					break;
				case "ASSIGNAWACCOUNTSTOP":
					messageConsumer.consumeMessage(MessageType.INCOME_APPROVE_STOP, msg[1]);
					break;

				case "ASSIGNALIACCOUNT":
					messageConsumer.consumeMessage(MessageType.INCOME_APPROVE_ALIPAYACCUPDATE, msg[1]);
					break;
				case "ASSIGNWECHATACCOUNT":
					messageConsumer.consumeMessage(MessageType.INCOME_APPROVE_WECHATACCUPDATE, msg[1]);
					break;
				default:
					break;
				}
			}
		} catch (Exception e) {
			log.error("AssignAWAccountListenerAdapter.onMessage error:", e);
			redisService.convertAndSend(RedisTopics.ASSIGN_INCOMEAWACCOUNT_TOPIC, message.toString());
		}
	}
}
