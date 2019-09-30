package com.xinbo.fundstransfer.component.shiro;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import org.apache.shiro.web.filter.authc.UserFilter;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;

public class RestfulUserFilter extends UserFilter {

	@Override
	protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
		SimpleCookie cookieVersion = new SimpleCookie("JVERSION");
		cookieVersion.setValue(SpringContextUtils.getBean(AppProperties.class).getVersion());
		cookieVersion.setHttpOnly(false);
		cookieVersion.saveTo(WebUtils.toHttp(request), WebUtils.toHttp(response));
		return true;
	}

	@Override
	protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String xrw = httpServletRequest.getHeader("X-Requested-With");
		if (xrw != null && xrw.equalsIgnoreCase("XMLHttpRequest")) {
			Map<String, String> queryParams = new HashMap<String, String>();
			queryParams.put("restful", "true");
			WebUtils.issueRedirect(request, response, getLoginUrl(), queryParams);
		} else {
			super.redirectToLogin(request, response);
		}
	}
}
