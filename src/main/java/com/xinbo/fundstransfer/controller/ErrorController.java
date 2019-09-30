package com.xinbo.fundstransfer.controller;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;

import com.xinbo.fundstransfer.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.restful.BaseController;

@Controller
public class ErrorController extends BaseController {
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;

	@RequestMapping(value = "/error401", produces = "text/html")
	public String error401() {
		return MessageFormat.format("redirect:{0}/{1}/401.html", HttpUtils.getRealDomain(), appProperties.getVersion());
	}

	@RequestMapping(value = "/error401")
	@ResponseBody
	public String error401(HttpServletRequest request) throws JsonProcessingException {
		return mapper.writeValueAsString(new SimpleResponseData(401, getMessage("response.error.401")));
	}

	@RequestMapping(value = "/error404", produces = "text/html")
	public String error404() {
		return MessageFormat.format("redirect:{0}/{1}/404.html", HttpUtils.getRealDomain(), appProperties.getVersion());
	}

	@RequestMapping(value = "/error404")
	@ResponseBody
	public String error404(HttpServletRequest request) throws JsonProcessingException {
		return mapper.writeValueAsString(new SimpleResponseData(404, getMessage("response.error.404")));
	}

	@RequestMapping(value = "/error500", produces = "text/html")
	public String error500() {
		return MessageFormat.format("redirect:{0}/{1}/500.html",  HttpUtils.getRealDomain(),appProperties.getVersion());
	}

	@RequestMapping(value = "/error500")
	@ResponseBody
	public String error500(HttpServletRequest request) throws JsonProcessingException {
		return mapper.writeValueAsString(new SimpleResponseData(500, getMessage("response.error.500")));
	}
}
