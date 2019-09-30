package com.xinbo.fundstransfer.restful;

import java.util.*;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AliInSummaryInputDTO;
import com.xinbo.fundstransfer.service.AliWechatInSummaryService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;

/**
 * Created by Administrator on 2018/10/5.
 */
@RequestMapping("/r/aliIn")
@RestController
public class AliWechatInSummaryController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AliWechatInSummaryController.class);
	private AliWechatInSummaryService service;
	private SysDataPermissionService sysDataPermissionService;

	@Autowired
	public AliWechatInSummaryController(AliWechatInSummaryService service,
			SysDataPermissionService sysDataPermissionService) {
		this.service = service;
		this.sysDataPermissionService = sysDataPermissionService;
	}

	private static ObjectMapper mapper = new ObjectMapper();

	@RequestMapping(value = "/findPage", method = RequestMethod.POST, consumes = "application/json")
	public String findPage(@Valid @RequestBody AliInSummaryInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		List<Map<String, Object>> ret;
		try {
			if (result.hasErrors()) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
				return mapper.writeValueAsString(responseData);
			}
			String check = commonCheck(inputDTO);
			if (StringUtils.isNotBlank(check)) {
				return check;
			}
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<String> handicapCodeList = sysDataPermissionService.handicapIdsList(inputDTO.getHandicapId(),
					operator);
			List<String> levelList = sysDataPermissionService.levelIdsList(inputDTO.getLevel(), operator);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
			// id,handicap,
			// level,to_account,order_no,member_user_name,amount,create_time,update_time,remark
			List<Object[]> list = service.findPage(inputDTO, handicapCodeList, levelList);
			if (CollectionUtils.isEmpty(list)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无数据");
				responseData.setPage(new Paging());
				responseData.setData(null);
				return mapper.writeValueAsString(responseData);
			}
			ret = new ArrayList<>();
			for (Iterator<Object[]> it = list.iterator(); it.hasNext();) {
				Object[] obj = it.next();
				Map<String, Object> map = new HashMap<>();
				map.put("handicapId", obj[1]);
				map.put("level", obj[2]);
				map.put("account", obj[3]);
				map.put("accountId", obj[10]);
				map.put("orderNo", obj[4]);
				map.put("member", obj[5]);
				map.put("amount", obj[6]);
				map.put("createTime", obj[7]);
				map.put("updateTime", obj[8]);
				map.put("remark", obj[9]);
				ret.add(map);
			}
			responseData.setData(ret);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			LOGGER.error("AliInSummaryController.findPage error:", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询异常");
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping(value = "/count", method = RequestMethod.POST, consumes = "application/json")
	public String count(@Valid @RequestBody AliInSummaryInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		try {
			if (result.hasErrors()) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
				return mapper.writeValueAsString(responseData);
			}
			String check = commonCheck(inputDTO);
			if (StringUtils.isNotBlank(check)) {
				return check;
			}
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<String> handicapCodeList = sysDataPermissionService.handicapIdsList(inputDTO.getHandicapId(),
					operator);
			List<String> levelList = sysDataPermissionService.levelIdsList(inputDTO.getLevel(), operator);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
			Long count = service.count(inputDTO, handicapCodeList, levelList);
			Paging page;
			if (count != null) {
				page = CommonUtils.getPage(inputDTO.getPageNo() + 1,
						inputDTO.getPageSize() != null ? inputDTO.getPageSize() : AppConstants.PAGE_SIZE,
						String.valueOf(count));
			} else {
				page = CommonUtils.getPage(0,
						inputDTO.getPageSize() != null ? inputDTO.getPageSize() : AppConstants.PAGE_SIZE, "0");
			}
			responseData.setPage(page);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			LOGGER.error("AliInSummaryController.count error :", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询异常");
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping(value = "/sum", method = RequestMethod.POST, consumes = "application/json")
	public String sum(@Valid @RequestBody AliInSummaryInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<Object> responseData;
		try {
			if (result.hasErrors()) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
				return mapper.writeValueAsString(responseData);
			}
			String check = commonCheck(inputDTO);
			if (StringUtils.isNotBlank(check)) {
				return check;
			}
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<String> handicapCodeList = sysDataPermissionService.handicapIdsList(inputDTO.getHandicapId(),
					operator);
			List<String> levelList = sysDataPermissionService.levelIdsList(inputDTO.getLevel(), operator);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
			Double sum = service.sum(inputDTO, handicapCodeList, levelList);
			responseData.setData(sum);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			LOGGER.error("AliInSummaryController.sum error :", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询异常");
			return mapper.writeValueAsString(responseData);
		}
	}

	private String commonCheck(AliInSummaryInputDTO inputDTO) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		// 用户盘口权限
		List<String> handicapCodeList = sysDataPermissionService.handicapIdsList(inputDTO.getHandicapId(), operator);
		if (handicapCodeList == null || handicapCodeList.size() == 0) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无盘口数据权限");
			responseData.setPage(new Paging());
			responseData.setData(null);
			return mapper.writeValueAsString(responseData);
		}
		// 用户层级权限
		List<String> levelList = sysDataPermissionService.levelIdsList(inputDTO.getLevel(), operator);
		if (levelList == null || levelList.size() == 0) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无层级数据权限");
			responseData.setPage(new Paging());
			responseData.setData(null);
			return mapper.writeValueAsString(responseData);
		}
		return null;
	}
}
