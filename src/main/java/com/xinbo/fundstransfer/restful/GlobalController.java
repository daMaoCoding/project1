package com.xinbo.fundstransfer.restful;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用接口
 *
 * 
 *
 */
@RestController
@RequestMapping("/global/")
public class GlobalController {
	@Value("${funds.transfer.version}")
	private String version;

	private String systemVersion = "";

	/**
	 * 获取系统静态资源版本信息
	 */
	@RequestMapping("/version")
	public String version() {
		if ("".equals(systemVersion)) {
			systemVersion = String.format("{\"status\":1,\"data\":\"%s\"}", version);
		}
		return systemVersion;
	}

}
