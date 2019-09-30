package com.xinbo.fundstransfer.report.patch;

import java.lang.annotation.*;

import org.springframework.stereotype.Component;


@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface PatchAnnotation {

	String value();
}
