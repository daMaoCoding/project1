package com.xinbo.fundstransfer.component.redis.message;

import org.springframework.data.redis.listener.RedisMessageListenerContainer;


/**
 * ************************
 * Topic消息处理
 * @author tony
 */
public interface RedisTopicHandler {

    void initContainer(RedisMessageListenerContainer container);
}
