package com.xinbo.fundstransfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class SystemConfiguration extends WebMvcConfigurerAdapter {
	@Autowired
	private RateLimitInterceptor rateLimitInterceptor;
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 流量控制
		registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/v2/trans/**");
	}
}
