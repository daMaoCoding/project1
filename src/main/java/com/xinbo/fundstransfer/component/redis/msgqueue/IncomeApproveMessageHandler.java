package com.xinbo.fundstransfer.component.redis.msgqueue;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.enums.IncomeAuditWsEnum;
import com.xinbo.fundstransfer.domain.pojo.IncomeAuditWs;
import com.xinbo.fundstransfer.service.AssignAWInAccountService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Administrator on 2018/10/20.
 */
@Component
@Slf4j
public class IncomeApproveMessageHandler implements MessageHandler {
	@Autowired
	private AssignAWInAccountService service;
	@Autowired
	private RedisService redisService;
	private ObjectMapper objectMapper = new ObjectMapper();
	private StringRedisTemplate stringRedisTemplate;

	public StringRedisTemplate getTemplate() {
		return stringRedisTemplate;
	}

	@Autowired
	public void setTemplate(StringRedisTemplate stringRedisTemplate) {
		if (stringRedisTemplate == null) {
			stringRedisTemplate = redisService.getStringRedisTemplate();
		}
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/**
	 * 入款审核 开始接单 停止接单业务处理
	 *
	 * @param eventModel
	 */
	@Override
	public boolean doHandle(MessageModel eventModel) throws HandleException {
		boolean ret = false;
		String message = "FRESH_ALIWECHAT", msg = null;
		IncomeAuditWs incomeAuditWs = new IncomeAuditWs();
		incomeAuditWs.setIncomeAuditWsFrom(IncomeAuditWsEnum.FRESHPAGE_ALIWECHATASSGINED.ordinal());
		incomeAuditWs.setMessage(message);
		if (eventModel.getType() == MessageType.INCOME_APPROVE_START.getType()) {
			ret = service.assignOnStartByUser(eventModel.getOperatorId());
			if (!ret) {
				throw new HandleException(HandleErrorCodeEum.E_INCOME_START);
			}
			try {
				msg = objectMapper.writeValueAsString(incomeAuditWs);
			} catch (JsonProcessingException e) {
				log.error("IncomeApproveMessageHandler.doHandle error:", e);
			}
			stringRedisTemplate.convertAndSend(RedisTopics.ASSIGNED_INCOMEAWACCOUNT_TOPIC, msg);
		}
		if (eventModel.getType() == MessageType.INCOME_APPROVE_STOP.getType()) {
			ret = service.assignOnStopByUser(eventModel.getOperatorId());
			if (!ret) {
				throw new HandleException(HandleErrorCodeEum.E_INCOME_STOP);
			}
			try {
				msg = objectMapper.writeValueAsString(incomeAuditWs);
			} catch (JsonProcessingException e) {
				log.error("IncomeApproveMessageHandler.doHandle error:", e);
			}
			stringRedisTemplate.convertAndSend(RedisTopics.ASSIGNED_INCOMEAWACCOUNT_TOPIC, msg);
		}
		if (eventModel.getType() == MessageType.INCOME_APPROVE_WECHATACCUPDATE.getType()
				|| eventModel.getType() == MessageType.INCOME_APPROVE_ALIPAYACCUPDATE.getType()) {
			int userId = service.dealOnAccountUpdate(eventModel.getOperatorId(), eventModel.getType());
			if (userId == -1 && eventModel.getType() == MessageType.INCOME_APPROVE_WECHATACCUPDATE.getType()) {
				throw new HandleException(HandleErrorCodeEum.E_WECHAT_UPDATE);
			}
			if (userId == -1 && eventModel.getType() == MessageType.INCOME_APPROVE_ALIPAYACCUPDATE.getType()) {
				throw new HandleException(HandleErrorCodeEum.E_ALIPAY_UPDATE);
			}
			incomeAuditWs.setOwner(String.valueOf(userId));
			try {
				msg = objectMapper.writeValueAsString(incomeAuditWs);
			} catch (JsonProcessingException e) {
				log.error("IncomeApproveMessageHandler.doHandle error:", e);
			}
			stringRedisTemplate.convertAndSend(RedisTopics.ASSIGNED_INCOMEAWACCOUNT_TOPIC, msg);
		}
		return ret;
	}

	@Override
	public List<MessageType> getSupportedEvents() {
		return Arrays.asList(MessageType.INCOME_APPROVE_START, MessageType.INCOME_APPROVE_STOP,
				MessageType.INCOME_APPROVE_ALIPAYACCUPDATE, MessageType.INCOME_APPROVE_WECHATACCUPDATE);
	}
}
