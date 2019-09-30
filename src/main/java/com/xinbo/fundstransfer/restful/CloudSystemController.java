package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.CloudService;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/r/cloud/sys")
public class CloudSystemController extends BaseController {
	@Autowired
	private CloudService cloudService;

	/**
	 * 云端配置信息获取
	 */
	@RequestMapping("/findPro")
	public String findPro() throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			return mapper.writeValueAsString(cloudService.rSysFindPro());
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败 " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 云端配置信息保存
	 */
	@RequestMapping("/savePro")
	public String savePro() throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			return mapper.writeValueAsString(cloudService.rSysSavePro(buildParams()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败 " + e.getLocalizedMessage()));
		}
	}
}
