/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFee4PlatFindReq;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeConfig;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFee4PlatLevelAddReq;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFee4PlatLevelDelReq;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFee4PlatUpdateReq;
import com.xinbo.fundstransfer.accountfee.service.AccountFee4PlatService;
import com.xinbo.fundstransfer.component.redis.msgqueue.HandleException;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Blake
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/accountFee")
public class AccountFee4PlatController {
	
	@Autowired
	private AccountFee4PlatService accountFee4PlatService;
	
	/**
	 * 查询手续费规则
	 * <br> apiUrl->confluence/pages/viewpage.action?pageId=21758353
	 * @throws HandleException 
	 */
	@RequestMapping(value = "/find", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody GeneralResponseData<AccountFeeConfig> find(@Valid @RequestBody AccountFee4PlatFindReq requestBody)
			throws JsonProcessingException, HandleException {
		GeneralResponseData<AccountFeeConfig> result = new GeneralResponseData<AccountFeeConfig>();
		try {
			AccountFeeConfig data = accountFee4PlatService.findByPlat(requestBody.getHandicap(),requestBody.getBankType(),requestBody.getAccount());
			data.setHandicap(requestBody.getHandicap());
			result.setData(data);
			result.setStatus(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		}catch (Exception e) {
			log.error("AccountFee2Controller.find时异常",e);
			result.setStatus(0);
			result.setMessage(e.getMessage());
		}
		return result;
	}
	
	/**
	 * 修改手续费规则
	 * <br> apiUrl->confluence/pages/viewpage.action?pageId=21758351
	 */
	@RequestMapping(value = "/update", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody SimpleResponseData update(@Valid @RequestBody AccountFee4PlatUpdateReq requestBody)
			throws JsonProcessingException {
		log.debug("平台修改手续费规则AccountFee2Controller.update,参数：{}",ObjectMapperUtils.serialize(requestBody));
		SimpleResponseData result = new SimpleResponseData(1);
		try {
			accountFee4PlatService.updateByPlat(requestBody);
		}catch (Exception e) {
			log.error("平台修改手续费规则AccountFee2Controller.update时产生异常，异常信息{}",e.getMessage());
			result.setStatus(0);
			result.setMessage(e.getMessage());
		}
		return result;
	}
	
	/**
	 * 新增计费阶梯
	 * <br> apiUrl->confluence/pages/viewpage.action?pageId=21758355
	 */
	@RequestMapping(value = "/levelAdd", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody SimpleResponseData calFeeLevelAdd(@Valid @RequestBody AccountFee4PlatLevelAddReq requestBody)
			throws JsonProcessingException {
		log.debug("平台修改新增计费阶梯AccountFee2Controller.calFeeLevelAdd,参数：{}",ObjectMapperUtils.serialize(requestBody));
		SimpleResponseData result = new SimpleResponseData(1);
		try {
			accountFee4PlatService.calFeeLevelAddByPlat(requestBody);
		}catch (Exception e) {
			log.error("平台修改手续费规则AccountFee2Controller.calFeeLevelAdd时产生异常，异常信息{}",e.getMessage());
			result.setStatus(0);
			result.setMessage(e.getMessage());
		}
		return result;
	}
	
	/**
	 * 删除计费阶梯
	 * <br> apiUrl->confluence/pages/viewpage.action?pageId=21758357
	 */
	@RequestMapping(value = "/levelDel", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody SimpleResponseData calFeeLevelDel(@Valid @RequestBody AccountFee4PlatLevelDelReq requestBody)
			throws JsonProcessingException {
		log.debug("平台修改删除计费阶梯AccountFee2Controller.calFeeLevelDel,参数：{}",ObjectMapperUtils.serialize(requestBody));
		SimpleResponseData result = new SimpleResponseData(1);
		try {
			accountFee4PlatService.calFeeLevelDelByPlat(requestBody);
		}catch (Exception e) {
			log.error("平台修改手续费规则AccountFee2Controller.calFeeLevelDel时产生异常，异常信息{}",e.getMessage());
			result.setStatus(0);
			result.setMessage(e.getMessage());
		}
		return result;
	}
	
}
