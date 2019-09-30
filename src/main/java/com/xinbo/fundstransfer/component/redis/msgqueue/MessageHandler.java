package com.xinbo.fundstransfer.component.redis.msgqueue;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Administrator on 2018/10/20. 用于定义每一种操作事件模型处理接口
 * 如，入款,出款,任务排查等，每一种模型可以处理多种事件:如 入款:开始接单，结束接单
 */
@Component
public interface MessageHandler {

	boolean doHandle(MessageModel eventModel) throws HandleException;

	/*
	 * 一个Handler可以处理多种事件
	 **/
	List<MessageType> getSupportedEvents();

}
