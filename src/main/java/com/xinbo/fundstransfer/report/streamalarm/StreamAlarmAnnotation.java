package com.xinbo.fundstransfer.report.streamalarm;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface StreamAlarmAnnotation {
	String value();
}
