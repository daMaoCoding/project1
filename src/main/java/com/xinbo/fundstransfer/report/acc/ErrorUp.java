package com.xinbo.fundstransfer.report.acc;

import java.lang.annotation.*;

import org.springframework.stereotype.Component;

/**
 * 处理上报分类注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ErrorUp {

    String value();
}
