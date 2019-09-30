package com.xinbo.fundstransfer.component.redis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageConsumer;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageType;
import com.xinbo.fundstransfer.component.websocket.TaskReviewWSEndpoint;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;

/**
 * Created by Administrator on 2018/7/9.
 */
@Component
public class TaskReviewWSEndpointMsgListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(TaskReviewWSEndpointMsgListenerAdapter.class);
	@Autowired
	private TaskReviewWSEndpoint taskReviewWSEndpoint;
	@Autowired
	private AllocateIncomeAccountService allocateIncomeAccountService;
	@Autowired
	private MessageConsumer messageConsumer;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void onMessage(Message message, byte[] pattern) {
		try {
			boolean host = allocateIncomeAccountService.checkHostRunRight();
			if (!host) {
				log.info("主机ip:{} 不能执行分配", CommonUtils.getInternalIp());
				return;
			}
			// 测试用,上线需要删除
			// if (!CommonUtils.getInternalIp().equals("192.168.12.6")) {
			// return;
			// }
			log.debug("订单排查 适配器 消息:{}", message.toString());
			if (StringUtils.isNotBlank(message.toString())) {
				if (message.toString().contains("FRESH_PAGE")) {
					// 页面刷新
					taskReviewWSEndpoint.sendMessage(message.toString());
				}
				if (message.toString().contains("ASIGNFAILEDTASK")) {
					// 分配待排查任务
					String[] msg = message.toString().split(":");
					switch (msg[1]) {
					case "START":
						messageConsumer.consumeMessage(MessageType.TASK_REVIEW_START, msg[2]);
						break;
					case "PAUSE":
						messageConsumer.consumeMessage(MessageType.TASK_REVIEW_PAUSE, msg[2]);
						break;
					case "STOP":
						messageConsumer.consumeMessage(MessageType.TASK_REVIEW_STOP, msg[2]);
						break;
					case "UPDATE":
						messageConsumer.consumeMessage(MessageType.TASK_REVIEW_UPDATE, msg[2]);
						break;
					default:
						break;
					}
				}
			}
		} catch (Exception e) {
			log.error(" TaskReviewWSEndpointMsgListenerAdapter deal   error : ", e);
		}
	}
}
