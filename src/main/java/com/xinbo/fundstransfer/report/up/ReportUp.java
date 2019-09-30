package com.xinbo.fundstransfer.report.up;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 处理上报分类注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ReportUp {

    String value();
}
