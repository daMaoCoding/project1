/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.controller;

import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuInfoFindColInputDTO;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuInfoDTO;
import com.xinbo.fundstransfer.daifucomponent.service.impl.DaifuInfoServiceImpl;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.restful.BaseController;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author blake
 *
 */
@Slf4j
@RestController
@RequestMapping("/daifuInfo")
public class DaifuInfoController extends BaseController  {

	@Autowired
	private DaifuInfoServiceImpl daifuInfoService;
	
	@RequestMapping(value ="/findByOutConfigId",method=RequestMethod.POST)
	public String findByOutConfigId(@Validated @RequestBody DaifuInfoFindColInputDTO inputDTO) throws JsonProcessingException {
		String params = mapper.writeValueAsString(inputDTO);
		log.info("DaifuInfoController.findByOutConfigId 参数：{}",params);
		GeneralResponseData<List<DaifuInfoDTO>> result = new GeneralResponseData<>(-1);
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				result = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(result);
			}
			GeneralResponseData<List<DaifuInfoDTO>> daifuInfoList = daifuInfoService.findByOutConfigId(inputDTO);
			return mapper.writeValueAsString(daifuInfoList);
		}catch (Exception e) {
			log.error("查询通道的代付订单信息异常findByOutConfigId异常",e);
			log.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
		
	}
	
}
