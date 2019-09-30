package com.xinbo.fundstransfer.ali4enterprise.controller;

import java.util.List;

import javax.validation.Valid;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.ali4enterprise.inputdto.*;
import com.xinbo.fundstransfer.ali4enterprise.outputdto.*;
import com.xinbo.fundstransfer.ali4enterprise.service.Ali4EnterpriseService;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.SysUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/ali4enterprise")
public class Ali4EnterpriseController {
	@Autowired
	private Ali4EnterpriseService service;

	// 1.1.1 企业支付宝-新增通道资料
	@RequestMapping(value = "/addEpAisleDetail", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> addEpAisleDetail(
			@Valid @RequestBody AddEpAisleDetailInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay responseDataNewPay = service.addEpAisleDetail(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "新增成功");

			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"新增失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("addEpAisleDetail   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"新增失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.2企业支付宝-通道资料-列表查询
	@RequestMapping(value = "/findEpDetailList", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<FindEpDetailListOutputDTO> findEpDetailList(@Valid @RequestBody CommonInputDTO inputDTO,
			BindingResult result) {
		GeneralResponseData<FindEpDetailListOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay<FindEpDetailListOutputDTO> responseDataNewPay = service.findEpDetailList(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"列表查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"列表查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findEpDetailList   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"列表查询失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.3 企业支付宝-通道资料-单条通道资料查询
	@RequestMapping(value = "/findEpDetail", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<FindEpDetailOutputDTO> findEpDetail(@Valid @RequestBody CommonInputDTO inputDTO,
			BindingResult result) {
		GeneralResponseData<FindEpDetailOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay<FindEpDetailOutputDTO> responseDataNewPay = service.findEpDetail(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"单条通道资料查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findEpDetail   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"单条通道资料查询失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.11 企业支付宝-通道资料-列表分页查询
	@RequestMapping(value = "/findEpByCondition", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<List<FindEpByConditionOutputDTO>> findEpByCondition(
			@Valid @RequestBody FindEpByConditionInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<List<FindEpByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay<PageOutPutDTO<FindEpByConditionOutputDTO>> responseDataNewPay = service
					.findEpByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"列表分页查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"列表分页查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findEpDetail   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"列表分页查询失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.10 企业支付宝-通道资料-通道资料绑定支付通道-支付通道查询
	@RequestMapping(value = "/findEpAisle", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<List<FindEpAisleOutputDTO>> findEpAisle(@Valid @RequestBody FindEpAisleInputDTO inputDTO,
			BindingResult result) {
		GeneralResponseData<List<FindEpAisleOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			ResponseDataNewPay<List<FindEpAisleOutputDTO>> responseDataNewPay = service.findEpAisle(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"支付通道查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"支付通道查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findEpAisle   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"支付通道查询失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.9 企业支付宝-通道资料-通道资料绑定支付通道
	@RequestMapping(value = "/bindEpAisle", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> bindEpAisle(@Valid @RequestBody BindEpAisleInputDTO inputDTO,
			BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
				return responseData;
			}
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = service.bindEpAisle(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "绑定成功");
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"绑定失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("bindEpAisle   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"绑定失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.8 企业支付宝-通道资料-修改
	@RequestMapping(value = "/modifyEpAisleDetail", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> modifyEpAisleDetail(
			@Valid @RequestBody ModifyEpAisleDetailInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay responseDataNewPay = service.modifyEpAisleDetail(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "修改成功");

			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("modifyEpAisleDetail   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.7 企业支付宝-通道资料-清空累计金额
	@RequestMapping(value = "/cleanEpInData", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> cleanEpInData(@Valid @RequestBody CommonInputDTO inputDTO,
			BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay responseDataNewPay = service.cleanEpInData(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"清空累计金额成功");

			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"清空累计金额失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("cleanEpInData   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"清空累计金额失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.5 企业支付宝-通道资料-修改停用金额告警
	@RequestMapping(value = "/modifyEpStopAlarm", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> modifyEpStopAlarm(
			@Valid @RequestBody ModifyEpStopAlarmInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay responseDataNewPay = service.modifyEpStopAlarm(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"修改停用金额告警成功");

			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改停用金额告警失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("modifyEpStopAlarm   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改停用金额告警失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.6 企业支付宝-通道资料-查询停用金额告警
	@RequestMapping(value = "/findEpStopAlarm", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<FindEpStopAlarmOutputDTO> findEpStopAlarm(
			@Valid @RequestBody FindEpStopAlarmInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<FindEpStopAlarmOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay<FindEpStopAlarmOutputDTO> responseDataNewPay = service.findEpStopAlarm(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"查询停用金额告警成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询停用金额告警失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findEpStopAlarm   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询停用金额告警失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.1.4 企业支付宝-通道资料-修改状态
	@RequestMapping(value = "/modifyEpStatus", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ModifyEpStatusOutputDTO> modifyEpStatus(
			@Valid @RequestBody ModifyEpStatusInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<ModifyEpStatusOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay<ModifyEpStatusOutputDTO> responseDataNewPay = service.modifyEpStatus(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"修改状态成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改状态失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("modifyEpStatus   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改状态失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.1 企业支付宝-商品说明库-列表查询
	@RequestMapping(value = "/findPayOwnerWordList", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<List<FindPayOwnerWordListOutputDTO>> findPayOwnerWordList(
			@Valid @RequestBody FindPayOwnerWordListInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<List<FindPayOwnerWordListOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordListOutputDTO>> responseDataNewPay = service
					.findPayOwnerWordList(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"列表查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"列表查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findPayOwnerWordList   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"列表查询失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.2 企业支付宝-商品说明库-新增商品说明
	@RequestMapping(value = "/addPayOwnerWord", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> addPayOwnerWord(@Valid @RequestBody AddPayOwnerWordInputDTO inputDTO,
			BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return responseData;
		}
		inputDTO.setOperationAdminId(sysUser.getId().longValue());
		inputDTO.setOperationAdminName(sysUser.getUid());
		try {
			ResponseDataNewPay responseDataNewPay = service.addPayOwnerWord(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"新增商品说明成功");

			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"新增商品说明失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("addPayOwnerWord   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"新增商品说明失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.3 企业支付宝-商品说明库-修改商品说明
	@RequestMapping(value = "/modifyPayOwnerWord", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ModifyPayOwnerWordOutputDTO> modifyPayOwnerWord(
			@Valid @RequestBody ModifyPayOwnerWordInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<ModifyPayOwnerWordOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return responseData;
		}
		inputDTO.setOperationAdminId(sysUser.getId().longValue());
		inputDTO.setOperationAdminName(sysUser.getUid());
		try {
			ResponseDataNewPay<ModifyPayOwnerWordOutputDTO> responseDataNewPay = service.modifyPayOwnerWord(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"修改商品说明成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改商品说明失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("modifyPayOwnerWord   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改商品说明失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.4 企业支付宝-商品说明库-删除商品说明
	@RequestMapping(value = "/removePayOwnerWord", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> removePayOwnerWord(
			@Valid @RequestBody RemovePayOwnerWordInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			ResponseDataNewPay responseDataNewPay = service.removePayOwnerWord(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"删除商品说明成功");

			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"删除商品说明失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("removePayOwnerWord   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"删除商品说明失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.5 企业支付宝-商品说明库-统计
	@RequestMapping(value = "/findPayOwnerWordSta", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<String> findPayOwnerWordSta(@Valid @RequestBody FindPayOwnerWordStaInputDTO inputDTO,
			BindingResult result) {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			ResponseDataNewPay responseDataNewPay = service.findPayOwnerWordSta(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "统计成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"统计失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findPayOwnerWordSta   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"统计失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.6 企业支付宝-商品说明库-类型管理-类型列表查询
	@RequestMapping(value = "/findPayOwnerWordTypeList", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<List<FindPayOwnerWordTypeListOutputDTO>> findPayOwnerWordTypeList(
			@Valid @RequestBody FindPayOwnerWordTypeListInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<List<FindPayOwnerWordTypeListOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}

		try {
			ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordTypeListOutputDTO>> responseDataNewPay = service
					.findPayOwnerWordTypeList(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"类型列表查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"类型列表查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findPayOwnerWordTypeList   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"类型列表查询失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.7 企业支付宝-商品说明库-类型管理-类型名称列表查询
	@RequestMapping(value = "/findPayOwnerWordTypeNameList", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<List<FindPayOwnerWordTypeNameListOutputDTO>> findPayOwnerWordTypeNameList(
			@Valid @RequestBody FindPayOwnerWordTypeNameListInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<List<FindPayOwnerWordTypeNameListOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			ResponseDataNewPay<List<FindPayOwnerWordTypeNameListOutputDTO>> responseDataNewPay = service
					.findPayOwnerWordTypeNameList(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"类型名称列表查询成功");
				responseData.setData(responseDataNewPay.getData());

			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"类型名称列表查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findPayOwnerWordTypeNameList   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"类型名称列表查询失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.8 企业支付宝-商品说明库-类型管理-新增类型
	@RequestMapping(value = "/addPayOwnerWordType", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<AddPayOwnerWordTypeOutputDTO> addPayOwnerWordType(
			@Valid @RequestBody AddPayOwnerWordTypeInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<AddPayOwnerWordTypeOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return responseData;
		}
		inputDTO.setOperationAdminId(sysUser.getId().longValue());
		inputDTO.setOperationAdminName(sysUser.getUid());
		try {
			ResponseDataNewPay<AddPayOwnerWordTypeOutputDTO> responseDataNewPay = service.addPayOwnerWordType(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"新增类型成功");

			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"新增类型失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("addPayOwnerWordType   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"新增类型失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.9 企业支付宝-商品说明库-类型管理-删除类型
	@RequestMapping(value = "/removePayOwnerWordType", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> removePayOwnerWordType(
			@Valid @RequestBody RemovePayOwnerWordTypeInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			ResponseDataNewPay responseDataNewPay = service.removePayOwnerWordType(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"删除类型成功");
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"删除类型失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("removePayOwnerWordType   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"删除类型失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.10 企业支付宝-商品说明库-绑定商品类型-查询
	@RequestMapping(value = "/findPayOwnerWordBindList", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<List<FindPayOwnerWordBindListOutputDTO>> findPayOwnerWordBindList(
			@Valid @RequestBody CommonInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<List<FindPayOwnerWordBindListOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			ResponseDataNewPay<List<FindPayOwnerWordBindListOutputDTO>> responseDataNewPay = service
					.findPayOwnerWordBindList(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"绑定商品类型-查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"绑定商品类型-查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("findPayOwnerWordBindList   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"绑定商品类型-查询失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 1.2.11 企业支付宝-商品说明库-绑定商品类型-保存
	@RequestMapping(value = "/savePayOwnerWordBind", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<ResponseDataNewPay> savePayOwnerWordBind(
			@Valid @RequestBody SavePayOwnerWordBindInputDTO inputDTO, BindingResult result) {
		GeneralResponseData<ResponseDataNewPay> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "本系统参数校验不通过");
			return responseData;
		}
		try {
			ResponseDataNewPay responseDataNewPay = service.savePayOwnerWordBind(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"绑定商品类型-保存成功");
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"绑定商品类型-保存失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return responseData;
		} catch (Exception e) {
			log.error("savePayOwnerWordBind   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"绑定商品类型-保存失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	// 包装分页返回页面 count 总记录数
	private Paging wrapPage(Integer pageSize, int pageNo, Integer count) {
		Paging page;
		if (count != null) {
			page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					String.valueOf(count));
		} else {
			page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
		}
		return page;
	}
}
