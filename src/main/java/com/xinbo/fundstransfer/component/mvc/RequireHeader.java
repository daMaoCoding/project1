package com.xinbo.fundstransfer.component.mvc;


import java.lang.annotation.*;

/**
 * ************************
 * 请求头及请求头绑定ip限制。
 * http调用的请求头限制，不支持{@PathVariable}  参数的url
 * 放到类上,表示这个类全部Mapping都应用Header限制
 * 绑定IP：com.xinbo.fundstransfer.AppProperties#PT_CRK_IPS
 * 增加Ip限制：com.xinbo.fundstransfer.component.mvc.RequireHeaderMapping#checkIp(java.lang.String, java.lang.String, java.lang.String)
 * @author tony
 */
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireHeader {
    String value() ;  // default ""
}
