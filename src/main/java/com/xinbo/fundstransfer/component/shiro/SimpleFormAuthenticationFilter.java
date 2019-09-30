package com.xinbo.fundstransfer.component.shiro;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xinbo.fundstransfer.utils.HttpUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.beans.factory.annotation.Autowired;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysUserService;

public class SimpleFormAuthenticationFilter extends FormAuthenticationFilter {
	@Autowired
	private SysUserService userService;
	@Autowired
	private RedisService redisService;

	@Override
	protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
		String username = getUsername(request);
		String password = getPassword(request);
		boolean rememberMe = isRememberMe(request);
		String host = getHost(request);
		SysUser user = userService.findByUid(username);
		if (user != null) {
			AppConstants.THREADLOCAL_USER_ID.set(user.getId());
		}
		return new UsernamePasswordToken(username, password, rememberMe, host);
	}

	/**
	 * 覆盖默认实现
	 * 
	 * @param subject
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@Override
	protected boolean onLoginSuccess(AuthenticationToken authenticationToken, Subject subject, ServletRequest request,
			ServletResponse response) throws Exception {

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;
		// String uid = String.valueOf(subject.getPrincipal());
		// String token = redisService.generateToken(uid);

		if ("XMLHttpRequest".equalsIgnoreCase(httpServletRequest.getHeader("X-Requested-With"))) {
			String ip =request.getParameter("ip");
			if(StringUtils.isNotBlank(ip)) {
				SimpleCookie cookieUid = new SimpleCookie("JIP");
				cookieUid.setValue(ip);
				cookieUid.setHttpOnly(false);
				cookieUid.saveTo(httpServletRequest, httpServletResponse);
			}
			httpServletResponse.sendRedirect(HttpUtils.getRealDomain()  + "/auth/login/success");
		} else {
			httpServletResponse.sendRedirect(HttpUtils.getRealDomain()  + this.getSuccessUrl());
		}

		// WebUtils.getAndClearSavedRequest(httpServletRequest);
		// WebUtils.redirectToSavedRequest(httpServletRequest,
		// httpServletResponse, "/login");

		return false;
	}

}
