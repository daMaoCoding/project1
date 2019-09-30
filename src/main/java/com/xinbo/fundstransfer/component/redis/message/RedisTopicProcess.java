package com.xinbo.fundstransfer.component.redis.message;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * ************************
 * Topic消息处理
 * @author tony
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RedisTopicProcess {
    String value() ;  // default ""
}
