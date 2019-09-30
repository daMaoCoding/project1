package com.xinbo.fundstransfer.report.init;

import java.lang.annotation.*;

import org.springframework.stereotype.Component;

/**
 * 初始化操作分类注解
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface InitAnnotation {

	String value();
}
