package com.xinbo.fundstransfer.component.redis.topic;

import com.xinbo.fundstransfer.component.redis.message.MessageDelegate;
import com.xinbo.fundstransfer.component.redis.message.RedisTopicProcess;
import lombok.extern.slf4j.Slf4j;

/**
 * ************************
 * 可自定义泛型消息类型
 * @author tony
 */

@Slf4j
@RedisTopicProcess(value = "Hello")
public class HelloWorld extends MessageDelegate<String> {

    @Override
    public int handleMessage(String messageChannel,String message) {
        System.out.println("具体Process-Hello收到消息："+message+":线程："+Thread.currentThread());
        return 1;
    }


}
