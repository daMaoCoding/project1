package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.BizCommonRemarkInputDTO;
import com.xinbo.fundstransfer.domain.pojo.BizCommonRemarkOutputDTO;
import com.xinbo.fundstransfer.service.CommonRemarkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/r/commonRemark")
public class CommonRemarkController {

	@Autowired
	private CommonRemarkService service;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public String add(@RequestBody BizCommonRemarkInputDTO inputDTO) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}
		if (StringUtils.isBlank(StringUtils.trimToEmpty(inputDTO.getRemark()))) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "备注必填"));
		}
		inputDTO.setSysUser(operator);
		BizCommonRemarkOutputDTO outputDTO = service.add(inputDTO);
		GeneralResponseData<BizCommonRemarkOutputDTO> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "添加成功!");
		res.setData(outputDTO);
		return mapper.writeValueAsString(res);
	}

	@GetMapping(value = "/delete")
	public String delete(BizCommonRemarkInputDTO inputDTO) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}
		inputDTO.setSysUser(operator);
		BizCommonRemarkOutputDTO outputDTO = service.delete(inputDTO);
		GeneralResponseData<BizCommonRemarkOutputDTO> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!");
		res.setData(outputDTO);
		return mapper.writeValueAsString(res);
	}

	@GetMapping("/list")
	public String list(BizCommonRemarkInputDTO inputDTO) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}
		if (StringUtils.isBlank(inputDTO.getType())) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "业务类型必传"));
		}
		if (null == inputDTO.getBusinessId()) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "业务Id必传"));
		}
		int pageSize = inputDTO.getPageSize() == null ? AppConstants.PAGE_SIZE : inputDTO.getPageSize();
		int pageNo = inputDTO.getPageNo() == null ? 0 : inputDTO.getPageNo();
		// Sort.by(Sort.Direction.DESC, "createTime")
		PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.Direction.DESC, "createTime");
		Map<String, Object> map = service.list(inputDTO, pageRequest);
		GeneralResponseData<List<BizCommonRemarkOutputDTO>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
		res.setData(Lists.newArrayList());
		res.setPage(new Paging());
		if (!CollectionUtils.isEmpty(map)) {
			res.setData((List<BizCommonRemarkOutputDTO>) map.get("data"));
			res.setPage((Paging) map.get("page"));
		}
		return mapper.writeValueAsString(res);

	}
}
