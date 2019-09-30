package com.xinbo.fundstransfer.report.fail;

import java.lang.annotation.*;

import org.springframework.stereotype.Component;

/**
 * 失败确认分类注解
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface FailAnnotation {

	String value();
}
