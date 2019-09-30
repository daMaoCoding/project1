package com.xinbo.fundstransfer.report.success;

import java.lang.annotation.*;

import org.springframework.stereotype.Component;

/**
 * 订单转账成功确认分类注解
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface SuccessAnnotation {

	String value();
}
