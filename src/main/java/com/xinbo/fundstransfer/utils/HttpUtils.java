package com.xinbo.fundstransfer.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class HttpUtils {

	public static HttpServletRequest request;
	private static String domainPort;

	public static String getRealDomain() {
		try {
			String address = IpUtils.getIpAddress(request);
			if (address.equals("127.0.0.1") || address.equals("0:0:0:0:0:0:0:1")) {
				return "";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		StringBuffer url = request.getRequestURL();
		String realUrl = url.delete(url.length() - request.getRequestURI().length(), url.length())
				.append(request.getServletContext().getContextPath()).append(":").append(domainPort).toString();
		return realUrl;
	}

	@Autowired
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	@Value("${funds.transfer.application.domain.port}")
	public void setDomainPort(String domainPort) {
		this.domainPort = domainPort;
	}
}
