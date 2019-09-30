package com.xinbo.fundstransfer.component.redis.message;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.util.CollectionUtils;
import java.util.Map;


/**
 * ************************
 * Topic消息处理
 * @author tony
 */
@Slf4j
@Configuration
public class RedisTopicHandlerMapping  implements RedisTopicHandler, BeanPostProcessor {
    private static Map<String, MessageDelegate> mappers = Maps.newConcurrentMap();
    private static final String DEFAULTRECIVEMESSAGE = "onMessage";

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        try {
            if(bean instanceof MessageDelegate  && AnnotationUtils.isAnnotationDeclaredLocally(RedisTopicProcess.class, bean.getClass()) ){
                String  topic = AnnotationUtils.findAnnotation(bean.getClass(), RedisTopicProcess.class).value();
                if(StringUtils.isBlank(topic)) throw new RuntimeException("Topic处理器消息名称为空："+bean.getClass());
                mappers.put(topic,(MessageDelegate)bean);
            }
        }catch (Exception e){
            log.error("创建RedisTopic消息处理器出错,{},程序退出。",e.getMessage(),e);
            System.exit(1);
        }
        return bean;
    }


    @Override
    public void initContainer(RedisMessageListenerContainer container) {
        if (!CollectionUtils.isEmpty(mappers) && container != null) {
            mappers.forEach((k, v) -> {
                container.addMessageListener( new MessageListenerAdapter(new RedisMsgReceiver(v), DEFAULTRECIVEMESSAGE),new PatternTopic(k) );
            });
        }
    }
}
