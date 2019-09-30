package com.xinbo.fundstransfer.newpay.controller;

import javax.validation.Valid;

import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.newpay.inputdto.AckResultInputDTO;
import com.xinbo.fundstransfer.newpay.inputdto.ApplyMoneyDrawInputDTO;
import com.xinbo.fundstransfer.newpay.inputdto.ApplyMoneyDrawOutputDTO;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.service.AllocateTransService;

/**
 * Created by Administrator on 2018/7/23.
 */
@RestController
@RequestMapping("/newpay4plat")
public class NewPay4PlatController extends BaseController {

	private static final Logger LOGGER = LoggerFactory.getLogger(NewPay4PlatController.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AllocateTransService allocateTransService;

	@RequestMapping("/apply4Out")
	@ResponseBody
	public String apply(@Valid @RequestBody ApplyMoneyDrawInputDTO requestBody, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ApplyMoneyDrawOutputDTO> responseData;
		LOGGER.info("plat call apply method parameters :Account:{},Balance:{},HandicapCode:{},Level:{}",
				requestBody.getAccount(), requestBody.getBalance(), requestBody.getHandicapCode(),
				requestBody.getLevel());
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			TransferEntity transferEntity = allocateTransService.applyByCloud(requestBody.getAccountType(),
					requestBody.getAccount(), requestBody.getHandicapCode(), requestBody.getLevel(),
					requestBody.getBalance());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "请求成功");
			if (transferEntity != null) {
				ApplyMoneyDrawOutputDTO outputDTO = new ApplyMoneyDrawOutputDTO();
				outputDTO.setAccount(transferEntity.getAccount());
				outputDTO.setAcquireTime(transferEntity.getAcquireTime());
				outputDTO.setAmount(transferEntity.getAmount());
				outputDTO.setBankAddr(transferEntity.getBankAddr());
				outputDTO.setBankType(transferEntity.getBankType());
				outputDTO.setOwner(transferEntity.getOwner());
				outputDTO.setToAccountId(transferEntity.getToAccountId());
				responseData.setData(outputDTO);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("plat call apply method  fail:{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping("/reportResult")
	@ResponseBody
	public String reportResult(@Valid @RequestBody AckResultInputDTO requestBody, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ResponseDataNewPay> responseData;
		LOGGER.info(
				"plat call reportResult method parameters :Code:{},Amount:{},Account:{},ToAccountId:{},Flag:{},AcquireTime:{},Oid:{}",
				requestBody.getCode(), requestBody.getAmount(), requestBody.getAccount(), requestBody.getToAccountId(),
				requestBody.getFlag(), requestBody.getAckTime(), requestBody.getOid());
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			allocateTransService.ackByCloud(requestBody.getCode(), requestBody.getOid().toString(),
					requestBody.getAccount(), requestBody.getToAccountId(), requestBody.getAmount(),
					requestBody.getAckTime(), requestBody.getFlag());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "上报成功");
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			LOGGER.info("plat call reportResult method fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "上报失败");
			return mapper.writeValueAsString(responseData);
		}
	}
}
