/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.controller;

import javax.validation.Valid;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeLevelAddInputDto;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeLevelDelInputDto;
import com.xinbo.fundstransfer.accountfee.service.AccountFeeService;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.AccountService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Blake
 *
 */
@Slf4j
@RestController
@RequestMapping("/r/account/fee")
public class AccountFeeController {
	
	@Autowired
	private AccountService accountService;
	
	@Autowired
	private AccountFeeService accountFeeService;

	@RequestMapping(value = "/add", method = RequestMethod.POST, consumes = "application/json")
	public SimpleResponseData add(@Valid @RequestBody AccountFeeLevelAddInputDto vo) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return new SimpleResponseData(-1, "请重新登陆!");
		}
		BizAccount bizAccount = accountService.findById(operator, vo.getAccountId());
		if (bizAccount == null) {
			return new SimpleResponseData(0, "未知的accountId!");
		}
		SimpleResponseData result = new SimpleResponseData();
		try {
			accountFeeService.calFeeLevelAdd(bizAccount, operator.getUsername(), vo.getCalFeeLevelType(), vo.getMoneyBegin(), vo.getMoneyEnd(), vo.getFeeMoney(), vo.getFeePercent());
			result.setStatus(1);
		}catch (Exception e) {
			log.error(String.format("AccountFeeController.add时产生异常，异常信息%s,参数%s",e.getMessage(),ObjectMapperUtils.serialize(vo)));
			result.setStatus(0);
			result.setMessage(e.getMessage());
		}
		return result;
	}
	
	@RequestMapping(value = "/del", method = RequestMethod.POST, consumes = "application/json")
	public SimpleResponseData del(@Valid @RequestBody AccountFeeLevelDelInputDto vo) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return new SimpleResponseData(-1, "请重新登陆!");
		}
		BizAccount bizAccount = accountService.findById(operator, vo.getAccountId());
		if (bizAccount == null) {
			return new SimpleResponseData(0, "未知的accountId!");
		}
		SimpleResponseData result = new SimpleResponseData();
		try {
			accountFeeService.calFeeLevelDel(bizAccount, operator.getUsername(), vo.getCalFeeLevelType(), vo.getIndex());
			result.setStatus(1);
		}catch (Exception e) {
			log.error(String.format("AccountFeeController.del时产生异常，异常信息%s,参数%s",e.getMessage(),ObjectMapperUtils.serialize(vo)));
			result.setStatus(0);
			result.setMessage(e.getMessage());
		}
		return result;
	}
	
}
