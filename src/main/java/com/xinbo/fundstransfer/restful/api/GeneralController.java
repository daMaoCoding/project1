package com.xinbo.fundstransfer.restful.api;

import java.util.UUID;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.codec.SaltPassword;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.SysUserService;

/**
 * 入款请求接口
 *
 *
 */
@RestController
@RequestMapping("/api")
public class GeneralController {
	static final Logger log = LoggerFactory.getLogger(GeneralController.class);
	@Autowired
	private SysUserService sysUserService;
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * 获取TOKEN
	 */
	@RequestMapping(value = "/auth", method = { RequestMethod.GET, RequestMethod.POST })
	public String auth(@RequestParam(value = "username", required = true) String username,
			@RequestParam(value = "passwd", required = true) String passwd) throws JsonProcessingException {
		try {
			SysUser user = sysUserService.findByUid(username);
			if (null == user) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  ,用户不存在"));
			}
			if (!SaltPassword.checkPassword(passwd, user.getPassword(), user.getSalt())) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  ,密码错误"));
			}
			String token = UUID.randomUUID().toString();
			StringBuilder json = new StringBuilder();
			json.append("{\"status\":1,\"message\":\"操作成功\",\"data\":{\"token\":\"");
			json.append(token);
			json.append("\",\"expires\":\"");
			json.append(DateFormatUtils.format(System.currentTimeMillis() + 1800000, "yyyy-MM-dd HH:ss:mm"));
			json.append("\"}}");
			log.info("Authorization success. uid:{}", username);
			return json.toString();
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败." + e.getLocalizedMessage()));
		}
	}
}
