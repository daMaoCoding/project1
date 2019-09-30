package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.service.RedisService;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller抽象类
 *
 * 
 *
 */
public abstract class BaseController {
	Logger log = LoggerFactory.getLogger(BaseController.class);
	public ObjectMapper mapper = new ObjectMapper();
	@Autowired
	public HttpServletRequest request;

	@Autowired
	public HttpServletResponse response;

	@Autowired
	public ResourceBundleMessageSource resourceBundleMessageSource;

	@Autowired
	public LocaleResolver localeResolver;
	@Autowired
	public RedisService redisService;

	public String getMessage(String key) {
		return getMessage(key, null);
	}

	public String getMessage(String key, Object[] args) {
		try {
			return resourceBundleMessageSource.getMessage(key, args, localeResolver.resolveLocale(request));
		} catch (NoSuchMessageException e) {
			return key;
		}
	}

	@ExceptionHandler(value = Exception.class)
	@ResponseBody
	public String jsonExceptionHandler(HttpServletRequest request, Exception e) throws Exception {
		log.error("Request URI: {}", request.getRequestURI());
		log.error("RestfulApiExceptionHandler", e);
		if (e instanceof org.apache.shiro.authz.UnauthorizedException) {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			String info = CommonUtils.genSysMsg4WS(operator.getId(), SystemWebSocketCategory.System, "unauthorized");
			redisService.convertAndSend(RedisTopics.BROADCAST, info);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					getMessage("response.error") + ", 未授权，请联系管理员"));
		}
		if (e instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(), "参数格式有误"));
		}
		return mapper.writeValueAsString(new SimpleResponseData(500, getMessage("response.error.500")));
	}

	public Map<String, Object> buildParams() {
		Map<String, Object> result = new HashMap();
		Enumeration em = request.getParameterNames();
		while (em.hasMoreElements()) {
			String param = (String) em.nextElement();
			result.put(param, request.getParameter(param));
		}
		return result;
	}

}
