package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.ThirdAccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/r/thirdInfo")
@Slf4j
public class ThirdAccountController {
	@Autowired
	private ThirdAccountService service;
	@Autowired
	private AccountService accountService;
	@Autowired
	private HandicapService handicapService;

	private ObjectMapper mapper = new ObjectMapper();

	@GetMapping("/findById")
	public String findById(ThirdAccountInputDTO inputDTO) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}

		GeneralResponseData<BizThirdAccountOutputDTO> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
		BizThirdAccountOutputDTO outputDTO = service.findById(inputDTO);
		res.setData(outputDTO);
		return mapper.writeValueAsString(res);
	}

	@GetMapping("/unBindThirdAccount")
	public String findByUnBindThirdAccount(ThirdAccountInputDTO inputDTO) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}
		log.debug("查询 参数:{}", inputDTO.toString());
		if (CollectionUtils.isEmpty(inputDTO.getHandicapId())) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有盘口权限!"));
		}
		GeneralResponseData<List<UnBindThirdAccountOutputDTO>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
		List<UnBindThirdAccountOutputDTO> outputDTO = service.unbindThirdAccount(inputDTO);
		res.setData(outputDTO);
		return mapper.writeValueAsString(res);
	}

	@PostMapping("/edit")
	public String edit(@Valid @RequestBody BizThirdAccountInputDTO inputDTO, Errors errors)
			throws JsonProcessingException, InvocationTargetException, IllegalAccessException {
		if (errors.hasErrors()) {
			StringBuilder mes = new StringBuilder();
			for (ObjectError e : errors.getAllErrors()) {
				mes.append(e.getDefaultMessage()).append(",");
			}
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					mes.toString().substring(0, mes.length() - 1)));
		}
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}
		inputDTO.setSysUser(operator);
		AccountBaseInfo baseInfo = accountService.getFromCacheById(inputDTO.getAccountId());
		if (baseInfo == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "关联的账号不存在"));
		}

		BizThirdAccountOutputDTO outputDTO1 = service.findByAccountId(baseInfo.getId());
		if (outputDTO1 != null && (inputDTO.getId() == null || !outputDTO1.getId().equals(inputDTO.getId()))) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "三方账号已被其他三方资料关联"));
		}
		BizHandicap handicap = handicapService.findFromCacheById(baseInfo.getHandicapId());
		if (handicap == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "盘口不存在!"));
		}
		GeneralResponseData<BizThirdAccountOutputDTO> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!");
		// 保存第三方账号的真实状态
		inputDTO.setStatus(baseInfo.getStatus() == null ? AccountStatus.StopTemp.getStatus().byteValue()
				: baseInfo.getStatus().byteValue());
		BizThirdAccountOutputDTO outputDTO = service.edit(inputDTO);
		res.setData(outputDTO);
		return mapper.writeValueAsString(res);
	}

	@GetMapping("/list")
	public String page(ThirdAccountInputDTO inputDTO) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}
		if (StringUtils.isBlank(inputDTO.getType())) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "业务类型必传"));
		}
		GeneralResponseData<List<BizThirdAccountOutputDTO>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!");
		boolean resetPageNo = inputDTO.getPageNo() == null || StringUtils.isNotBlank(inputDTO.getThirdName());
		if (resetPageNo) {
			inputDTO.setPageNo(0);
		}
		if (CollectionUtils.isEmpty(inputDTO.getHandicapId())) {
			List<Integer> handicapIdList = handicapService.handicapIdList(sysUser);
			if (CollectionUtils.isEmpty(handicapIdList))
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "用户所属区域没有盘口权限!"));
			inputDTO.setHandicapId(handicapIdList);
		}
		inputDTO.setSysUser(sysUser);
		if (inputDTO.getPageSize() == null) {
			inputDTO.setPageSize(AppConstants.PAGE_SIZE);
		}
		int pageSize = inputDTO.getPageSize();
		PageRequest pageRequest = PageRequest.of(inputDTO.getPageNo(), pageSize, Sort.Direction.DESC, "createTime");
		Map<String, Object> map = service.pageBySql(inputDTO, pageRequest);// service.page(inputDTO, pageRequest);
		res.setData(Lists.newArrayList());
		res.setPage(new Paging());
		if (!CollectionUtils.isEmpty(map)) {
			res.setData((List<BizThirdAccountOutputDTO>) map.get("data"));
			res.setPage((Paging) map.get("page"));
		}
		return mapper.writeValueAsString(res);
	}
}
