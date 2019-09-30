package com.xinbo.fundstransfer.component.redis.msgqueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.AsignFailedTaskService;
import com.xinbo.fundstransfer.service.AssignAWInAccountService;
import com.xinbo.fundstransfer.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by Administrator on 2018/10/20.
 */
@Slf4j
@Component
public class MessageConsumer {
	// 注册事件与Handler映射
	// （一个事件类型可以对应多个handler处理，一个handler可以处理多个类型事件）
	// private Map<MessageType, List<MessageHandler>> eventConfig = new HashMap<>();
	private final ObjectMapper objectMapper;
	private RedisService redisService;
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private AssignAWInAccountService awInAccountService;
	@Autowired
	private AsignFailedTaskService asignFailedTaskService;
	private static final String INCOME_APPROVE = "INCOME_APPROVE";
	private static final String TASK_REVIEW = "TASK_REVIEW";
	private static volatile boolean EXEC_INCOME_FLAG = false;// 正在消费入款消息标识
	private static volatile boolean EXEC_TASKREVIEW_FLAG = false;// 正在消费排查任务消息标识
	@Autowired
	private IncomeApproveMessageHandler incomeApproveMessageHandler;
	@Autowired
	private TaskReviewMessageHandler taskReviewMessageHandler;

	@Autowired
	public MessageConsumer(ObjectMapper objectMapper, RedisService redisService) {
		this.objectMapper = objectMapper;
		this.redisService = redisService;
	}

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
	 * 收到消息通知之后去获取消息队列异步执行业务逻辑
	 * 
	 * @param type
	 *            {@link MessageType }
	 * @param operatorId
	 *            用户id或者账号id
	 */
	public void consumeMessage(MessageType type, String operatorId) {
		try {
			if (type == null || StringUtils.isBlank(operatorId)) {
				log.info("  message type is null :{} ,or operatorId is null:{}", type, operatorId);
				return;
			}
			if (StringUtils.isNotBlank(MessageType.getMessageType(type))
					&& MessageType.getMessageType(type).equals(INCOME_APPROVE) && !EXEC_INCOME_FLAG) {

				boolean exist = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY);
				if (!exist) {
					return;
				}
				long len = stringRedisTemplate.boundListOps(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY).size();
				if (len == 0L) {
					return;
				}
				int lockFlag = awInAccountService.lock4AssignAW();
				if (lockFlag == 0) {
					log.info("MessageType type:{},MessageConsumer.consumeMessage thread :{} ,get lock  result:{}",
							type.getDesc(), Thread.currentThread().getName(), lockFlag);
					return;
				}
				EXEC_INCOME_FLAG = true;
				dealMessageIncomeApprove(type, operatorId);
			}
			if (StringUtils.isNotBlank(MessageType.getMessageType(type))
					&& MessageType.getMessageType(type).equals(TASK_REVIEW) && !EXEC_TASKREVIEW_FLAG) {
				boolean exist = stringRedisTemplate.hasKey(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY);
				if (!exist) {
					return;
				}
				long len = stringRedisTemplate.boundListOps(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY).size();
				if (len == 0L) {
					return;
				}
				int lockFlag = asignFailedTaskService.getLock4AsignTask();
				if (lockFlag == 0) {
					log.info("MessageType type:{},MessageConsumer.consumeMessage thread :{} ,get lock  result:{}",
							type.getDesc(), Thread.currentThread().getName(), lockFlag);
					return;
				}
				EXEC_TASKREVIEW_FLAG = true;
				dealMessageTaskReview(type, operatorId);
			}

		} catch (Exception e) {
			log.error(" 问题排查处理消息异常 :", e);
			if (MessageType.getMessageType(type).equals(INCOME_APPROVE)) {
				awInAccountService.unlock4AssignAW();
				EXEC_INCOME_FLAG = false;
			}
			if (MessageType.getMessageType(type).equals(TASK_REVIEW)) {
				asignFailedTaskService.releaseLock4AsignTask();
				EXEC_TASKREVIEW_FLAG = false;
			}
		}
	}

	/**
	 * 对队列里的事件type总数len个数逐个执行 如果执行抛出指定的异常则重新获取消息执行动作
	 * 
	 * @param type
	 * @param operatorId
	 *            用户id或者账号id
	 */
	private void dealMessageIncomeApprove(MessageType type, String operatorId) {
		try {
			boolean flag;
			do {
				flag = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_ASSIGN_LOCK);// 在锁过期之前会一直执行
				String message = stringRedisTemplate.opsForList().rightPopAndLeftPush(
						RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY, RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
				if (StringUtils.isBlank(message)) {
					// 没有操作消息
					EXEC_INCOME_FLAG = false;
					awInAccountService.unlock4AssignAW();
					break;
				}
				executeHandle(type, operatorId, message);
			} while (flag);
			boolean exists = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
			if (exists) {
				stringRedisTemplate.delete(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
			}
		} catch (HandleException e) {
			if (e.getErrorCode() == HandleErrorCodeEum.E_INCOME_START.getErrorCode()
					|| e.getErrorCode() == HandleErrorCodeEum.E_INCOME_STOP.getErrorCode()
					|| e.getErrorCode() == HandleErrorCodeEum.E_ALIPAY_UPDATE.getErrorCode()
					|| e.getErrorCode() == HandleErrorCodeEum.E_WECHAT_UPDATE.getErrorCode()) {
				dealMessagePostHandleException(type, operatorId);
			}
		} catch (Exception e) {
			log.error(" error :", e);
		} finally {
			boolean exists = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
			if (exists) {
				long len = stringRedisTemplate.opsForList().size(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
				if (len > 0L) {
					stringRedisTemplate.opsForList().rightPop(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
				}
			}
		}
	}

	private void dealMessageTaskReview(MessageType type, String operatorId) {
		log.debug("问题排查处理,参数:{},{}", type, operatorId);
		try {
			boolean flag;
			do {
				// 在锁过期之前会一直执行
				flag = stringRedisTemplate.hasKey(RedisKeys.TASK_REVIEW_ASSIGN_LOCK);
				log.debug("订单排查处理 校验 锁是否过期(false过期) :{}", flag);
				String message = stringRedisTemplate.opsForList().rightPopAndLeftPush(
						RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY, RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
				log.debug("获取队列消息  :{}", message);
				if (StringUtils.isBlank(message)) {
					// 如果操作消息为空
					EXEC_TASKREVIEW_FLAG = false;
					asignFailedTaskService.releaseLock4AsignTask();
					break;
				}
				log.debug("获取队列消息 处理:type {},operatorId {},message {} ", type, operatorId, message);
				executeHandle(type, operatorId, message);
			} while (flag);
			boolean exists = stringRedisTemplate.hasKey(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
			if (exists) {
				log.debug("删除消息备份");
				stringRedisTemplate.delete(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
			}
		} catch (HandleException e) {
			log.debug("问题排查接单/暂停/结束 处理消息异常 参数:{},{},HandleException异常:", operatorId, type, e.getStackTrace());
			if (e.getErrorCode() == HandleErrorCodeEum.E_REVIEWTASK_START.getErrorCode()
					|| e.getErrorCode() == HandleErrorCodeEum.E_REVIEWTASK_STOP.getErrorCode()
					|| e.getErrorCode() == HandleErrorCodeEum.E_REVIEWTASK_PAUSE.getErrorCode()
					|| e.getErrorCode() == HandleErrorCodeEum.E_REVIEWTASK_UPDATE.getErrorCode()) {
				dealMessagePostHandleException(type, operatorId);
			}
		} catch (Exception e) {
			log.error("问题排查接单/暂停/结束 处理消息异常 参数:{},{},Exception异常:", operatorId, type, e.getStackTrace());
		} finally {
			boolean exists = stringRedisTemplate.hasKey(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
			if (exists) {
				long len = stringRedisTemplate.opsForList().size(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
				if (len > 0L) {
					stringRedisTemplate.opsForList().rightPop(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
				}
			}
		}
	}

	// 从队列获取消息执行发生异常则执行备份消息队列的事件
	private void dealMessagePostHandleException(MessageType type, String operatorId) {
		log.info("MessageConsumer异常发生之后处理 参数 type:{},operatorId:{}", type.getType() + "--" + type.getDesc(),
				operatorId);
		try {
			String message = null;
			if (StringUtils.isNotBlank(MessageType.getMessageType(type))
					&& MessageType.getMessageType(type).equals(INCOME_APPROVE)) {
				message = stringRedisTemplate.opsForList().rightPop(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
			}
			if (StringUtils.isNotBlank(MessageType.getMessageType(type))
					&& MessageType.getMessageType(type).equals(TASK_REVIEW)) {
				message = stringRedisTemplate.opsForList().rightPop(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
			}
			if (StringUtils.isBlank(message)) {
				return;
			}
			try {
				executeHandle(type, operatorId, message);
			} catch (HandleException e1) {
				log.error(" HandleException error  :", e1);
			} catch (Exception e1) {
				log.error(" Exception error  :", e1);
			}
		} catch (Exception e) {
			log.error("处理消息dealMessagePostHandleException异常:", e);
		}
	}

	private void executeHandle(MessageType type, String operatorId, String message) throws HandleException {
		log.error("执行消息处理  参数,type: {}, operatorId: {},message: {}", type, operatorId, message);
		try {
			if (StringUtils.isBlank(message)) {
				log.info("  message is empty:{},type:{},operatorId:{}", StringUtils.isBlank(message), type, operatorId);
				return;
			}
			MessageModel messageModel = null;
			try {
				messageModel = objectMapper.readValue(message, MessageModel.class);
			} catch (IOException e) {
				log.error(" error:", e);
			}
			log.debug("消息实体:{}", ObjectMapperUtils.serialize(messageModel));
			if (StringUtils.isNotBlank(MessageType.getMessageType(type))
					&& MessageType.getMessageType(type).equals(INCOME_APPROVE)) {
				// 执行注册到该事件的每一个Handler
				boolean ret = incomeApproveMessageHandler.doHandle(messageModel);
				if (!ret) {
					log.info(" dealMessage result is false and failed ,messageType:{},operatorId:{}", type.getDesc(),
							operatorId);
				}
			}
			if (StringUtils.isNotBlank(MessageType.getMessageType(type))
					&& MessageType.getMessageType(type).equals(TASK_REVIEW)) {
				log.debug("订单排查 处理");
				// 执行注册到该事件的每一个Handler
				boolean ret = taskReviewMessageHandler.doHandle(messageModel);
				log.debug("订单排查 处理 结果 :{}", ret);
				if (!ret) {
					log.info("  订单排查 处理 结果 :{} 失败 ,messageType:{},operatorId:{}", ret, type.getDesc(), operatorId);
				}
			}
		} catch (HandleException e) {
			log.error("执行消息处理异常:", e);
		}
	}
}
