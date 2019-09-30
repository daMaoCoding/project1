package com.xinbo.fundstransfer.daifucomponent.controller;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuSurpportBankTypeInputDTO;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuSynSurpportBankTypeInputDTO;
import com.xinbo.fundstransfer.daifucomponent.service.DaifuSurpportBankTypeService;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizDaifuSurpportBanktypeEntity;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
@Slf4j
@RestController
public class DaifuSurpportBankTypeController {

	@Autowired
	private DaifuSurpportBankTypeService daifuSurpportBankTypeService;

	/**
	 * 接口 2.1.0 页面同步 平台代付供应商支持银行类型到出入款
	 *
	 * @param inputDTO
	 * @return
	 */
	@PostMapping("/daifu/synBankType")
	public GeneralResponseData<Map<String, String>> synBankType(
			@Validated @RequestBody DaifuSynSurpportBankTypeInputDTO inputDTO, Errors errors) {
		GeneralResponseData<Map<String, String>> ret;
		try {
			if (errors.hasErrors()) {
				return new GeneralResponseData<>(-1, "同步代付支持的银行类型参数必传!");
			}
			log.debug("页面同步 平台代付供应商支持银行类型到出入款 参数:{}", inputDTO);
			String res = daifuSurpportBankTypeService.querSurpportBankType(inputDTO);
			daifuSurpportBankTypeService.freshBankTypeCache();
			log.debug("页面同步 平台代付供应商支持银行类型到出入款 结果:{}", res);
			ret = new GeneralResponseData<>(1, "更新结果:" + res);
		} catch (Exception e) {
			ret = new GeneralResponseData<>(-1, "更新失败:" + e.getLocalizedMessage());
			log.error("前端页面同步代付供应商支持银行类型到出入款系统异常:", e);
		}
		log.debug("页面同步 平台代付供应商支持银行类型到出入款 结果:{}", ObjectMapperUtils.serialize(ret));
		return ret;
	}

	/**
	 * 接口 2.1.1 平台同步 代付供应商支持银行类型到出入款
	 * 
	 * @param map
	 * @return
	 */
	@PostMapping("/daifu/bankTypeSyn")
	public GeneralResponseData<Map<String, String>> bankTypeSyn(@RequestBody Map<String, String> map) {
		GeneralResponseData<Map<String, String>> ret;
		try {
			if (CollectionUtils.isEmpty(map)) {
				return new GeneralResponseData<>(-1, "同步代付支持的银行类型参数必传!");
			}
			log.debug("平台同步 代付供应商支持银行类型到出入款 参数:{}", map);
			ret = new GeneralResponseData<>(1, "更新成功!");
			Map<String, String> res = daifuSurpportBankTypeService.saveAllBankType(map);
			daifuSurpportBankTypeService.freshBankTypeCache();
			ret.setData(res);
		} catch (Exception e) {
			ret = new GeneralResponseData<>(-1, "更新失败:" + e.getLocalizedMessage());
			log.error("同步代付供应商支持银行类型到出入款系统异常:", e);
		}
		log.debug("平台同步 代付供应商支持银行类型到出入款 结果:{}", ret);
		return ret;
	}

	/**
	 * 前端查询
	 * 
	 * @param inputDTO
	 * @param errors
	 * @return
	 */
	@PostMapping("/daifu/surpportBankType")
	public GeneralResponseData<List<BizDaifuSurpportBanktypeEntity>> list(
			@Validated @RequestBody DaifuSurpportBankTypeInputDTO inputDTO, Errors errors) {
		if (errors.hasErrors()) {
			return new GeneralResponseData<>(-1, "参数必填");
		}
		try {
			log.debug("查询支持银行类型 参数:{}", inputDTO);
			PageRequest pageRequest = new PageRequest(inputDTO.getPageNo() - 1,
					inputDTO.getPageSize() == null ? AppConstants.PAGE_SIZE : inputDTO.getPageSize());
			Page<BizDaifuSurpportBanktypeEntity> page = daifuSurpportBankTypeService.list(inputDTO, pageRequest);
			GeneralResponseData<List<BizDaifuSurpportBanktypeEntity>> responseData = new GeneralResponseData<>(1,
					"查询成功!");
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			log.debug("查询支持银行类型 结果:{}", responseData);
			return responseData;
		} catch (Exception e) {
			log.error("查询支持的银行卡类型 异常:", e);
			return new GeneralResponseData<>(-1, "查询支持的银行卡类型异常!");
		}
	}

	/**
	 * 前端页面 更新支持的银行类型 选中 和取消
	 * 
	 * @param inputDTO
	 * @return
	 */
	@PostMapping("/daifu/updateSurpport")
	public GeneralResponseData<BizDaifuSurpportBanktypeEntity> updateSurpport(
			@Validated @RequestBody DaifuSynSurpportBankTypeInputDTO inputDTO, Errors errors) {
		if (errors.hasErrors()) {
			return new GeneralResponseData<>(-1, "参数不能为空");
		}
		try {
			log.debug("更新 支持银行类型 参数:{}", inputDTO);
			BizDaifuSurpportBanktypeEntity res = daifuSurpportBankTypeService.updateSurpport(inputDTO);
			GeneralResponseData<BizDaifuSurpportBanktypeEntity> responseData = new GeneralResponseData<>(1, "更新成功!");
			responseData.setData(res);
			log.debug("更新 支持银行类型 结果:{}", responseData);
			return responseData;
		} catch (Exception e) {
			log.error("更新 异常:", e);
			return new GeneralResponseData<>(-1, "更新异常!");
		}
	}
}
