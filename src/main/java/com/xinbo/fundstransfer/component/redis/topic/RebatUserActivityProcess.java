package com.xinbo.fundstransfer.component.redis.topic;

import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.redis.message.MessageDelegate;
import com.xinbo.fundstransfer.component.redis.message.RedisTopicProcess;
import com.xinbo.fundstransfer.service.RebateUserActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ************************
 *
 * @author tony
 */

@Slf4j
@RedisTopicProcess(RedisTopics.REBAT_USER_ACTIVITY_TOPIC)
public class RebatUserActivityProcess extends MessageDelegate<String> {

    @Autowired
    RebateUserActivityService rebateUserActivityService;

    @Override
    public int handleMessage(String messageChannel,String message) {
        rebateUserActivityService.onMessage(messageChannel,message);
        return  1;
    }


}
