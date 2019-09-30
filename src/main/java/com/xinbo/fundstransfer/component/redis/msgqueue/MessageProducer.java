package com.xinbo.fundstransfer.component.redis.msgqueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Administrator on 2018/10/20.
 */
@Slf4j
@Component
public class MessageProducer {
	@Autowired
	private RedisService redisService;
	@Autowired
	private ObjectMapper objectMapper;
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
	 * 把事件放入队列以待异步处理 比如： 把接单结束接单事件放入队列
	 *
	 * @param key
	 * @param messageModel
	 * @throws JsonProcessingException
	 */
	public void pushMessage(String key, MessageModel messageModel) {
		try {
			String message = objectMapper.writeValueAsString(messageModel);
			stringRedisTemplate.opsForList().leftPush(key, message);
		} catch (JsonProcessingException e) {
			log.error("MessageProducer.pushMessage error occured:", e);
		}
	}
}
