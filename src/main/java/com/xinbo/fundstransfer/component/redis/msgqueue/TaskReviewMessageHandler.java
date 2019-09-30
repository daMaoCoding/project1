package com.xinbo.fundstransfer.component.redis.msgqueue;

import java.util.Arrays;
import java.util.List;

import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.service.AsignFailedTaskService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TaskReviewMessageHandler implements MessageHandler {
	@Autowired
	private AsignFailedTaskService asignFailedTaskService;
	@Autowired
	private RedisService redisService;
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

	@Override
	public boolean doHandle(MessageModel eventModel) throws HandleException {
		boolean ret = false;
		log.debug("处理消息:{}", ObjectMapperUtils.serialize(eventModel));
		if (eventModel.getType() == MessageType.TASK_REVIEW_START.getType()) {
			log.debug("开始接单 处理 :{}", ObjectMapperUtils.serialize(eventModel));
			ret = asignFailedTaskService.asignTaskOnSocketOpen(eventModel.getOperatorId());
			log.debug("  处理 结果:{}", ret);
			if (!ret) {
				throw new HandleException(HandleErrorCodeEum.E_REVIEWTASK_START);
			}
		}
		if (eventModel.getType() == MessageType.TASK_REVIEW_PAUSE.getType()) {
			log.debug("暂停接单 处理 :{}", ObjectMapperUtils.serialize(eventModel));
			ret = asignFailedTaskService.dealOnpauseReviewTask(eventModel.getOperatorId());
			log.debug("  处理 结果:{}", ret);
			if (!ret) {
				throw new HandleException(HandleErrorCodeEum.E_REVIEWTASK_PAUSE);
			}
		}
		if (eventModel.getType() == MessageType.TASK_REVIEW_UPDATE.getType()) {
			log.debug("订单状态变更  处理 :{}", ObjectMapperUtils.serialize(eventModel));
			ret = asignFailedTaskService.asignReviewTaskOnTurnToFail(eventModel.getOperatorId());
			log.debug("  处理 结果:{}", ret);
			if (!ret) {
				throw new HandleException(HandleErrorCodeEum.E_REVIEWTASK_UPDATE);
			}
		}
		if (eventModel.getType() == MessageType.TASK_REVIEW_STOP.getType()) {
			log.debug("结束接单  处理 :{}", ObjectMapperUtils.serialize(eventModel));
			ret = asignFailedTaskService.dealOnstopReviewTask(eventModel.getOperatorId());
			log.debug("  处理 结果:{}", ret);
			if (!ret) {
				throw new HandleException(HandleErrorCodeEum.E_REVIEWTASK_STOP);
			}
		}
		if (ret) {
			log.debug("  处理 之后 发socket消息 刷新页面:{}", ret);
			stringRedisTemplate.convertAndSend(RedisTopics.ASIGN_REVIEWTASK_TOPIC, "FRESH_PAGE");
		}
		return ret;
	}

	@Override
	public List<MessageType> getSupportedEvents() {
		return Arrays.asList(MessageType.TASK_REVIEW_START, MessageType.TASK_REVIEW_PAUSE,
				MessageType.TASK_REVIEW_STOP);
	}
}
