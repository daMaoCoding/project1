package com.xinbo.fundstransfer.chatPay.outmoney.controller;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.services.ChatpayLogService;
import com.xinbo.fundstransfer.chatPay.inmoney.services.InmoneyService;
import com.xinbo.fundstransfer.chatPay.outmoney.params.P2pKillOutwardDto;
import com.xinbo.fundstransfer.chatPay.outmoney.params.P2pOutwardConfimDto;
import com.xinbo.fundstransfer.chatPay.outmoney.services.OutmoneyService;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.utils.SimpleResponseData;

import lombok.extern.slf4j.Slf4j;

/**
 * 出款支付时的操作接口
 * 
 * @author ERIC
 *
 */

@Slf4j
@RestController
@RequestMapping("/api/callcenter/v1")
public class OutmoneyController {
	private static String TAG = "出款支付操作";
	@Autowired
	@Qualifier("ParttimeInmoneyServiceImpl")
	private InmoneyService parttimeInmoneyService;
	@Autowired
	@Qualifier("MemberInmoneyServiceImpl")
	private InmoneyService memberInmoneyService;
	
	@Autowired
	@Qualifier("ParttimeOutmoneyServiceImpl")
	private OutmoneyService parttimeOutmoneyService;

	@Autowired
	@Qualifier("MemberOutmoneyServiceImpl")
	private OutmoneyService memberOutmoneyService;	
	
	@Autowired
	private ChatpayLogService chatpayLogService;
	
	/**
	 * 收款方主动确认收款
	 * 
	 * @param p2pOutwardConfimDto
	 * @param errors
	 * @return
	 */
	@RequestMapping(value = "/p2pOutwardConfim", method = RequestMethod.POST, consumes = "application/json")
	public Map<String, Object> p2pOutwardConfim(@Validated @RequestBody P2pOutwardConfimDto p2pOutwardConfimDto, Errors errors) {
		// 自动参数验证
		log.info("{}->p2pOutwardConfim,参数：{}", TAG, JSON.toJSONString(p2pOutwardConfimDto));
		if (errors.hasErrors()) {
			return SimpleResponseData.getErrorMapResult(errors.getAllErrors().get(0).getDefaultMessage());
		}
		try {
			BizChatpayLog chatpayLog = chatpayLogService.findByIncomeOrderNoAndStatus(p2pOutwardConfimDto.getOrderNumber());
			if(chatpayLog.incomeIsMember()) {			
				memberInmoneyService.outwardSideInmoneyConfirm(p2pOutwardConfimDto, chatpayLog);
			} else {
				parttimeInmoneyService.outwardSideInmoneyConfirm(p2pOutwardConfimDto, chatpayLog);
			}				
			Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
			return successMapResult;
		} catch (Exception e) {
			log.error("收款方主动确认收款报错 {}", p2pOutwardConfimDto.toString(), e);
			return SimpleResponseData.getErrorMapResult(e.getMessage());
		}
	}

	/**
	 * 无法完成出款(超时)客服结束出款任务 / 出款人员主动结束出款任务
	 * 
	 * @param p2pKillOutwardDto
	 * @param errors
	 * @return
	 */
	@RequestMapping(value = "/p2pKillOutward", method = RequestMethod.POST, consumes = "application/json")
	public Map<String, Object> p2pKillOutward(@Validated @RequestBody P2pKillOutwardDto p2pKillOutwardDto, Errors errors) {
		// 自动参数验证
		log.info("{}->p2pKillOutward,参数：{}", TAG, JSON.toJSONString(p2pKillOutwardDto));
		if (errors.hasErrors()) {
			return SimpleResponseData.getErrorMapResult(errors.getAllErrors().get(0).getDefaultMessage());
		}
		try {
			Pair<BizOutwardRequest, BizAccountTranslog> entity = memberOutmoneyService.getOutmoneyEntity(p2pKillOutwardDto.getOrderNumber());
			if(entity.getLeft() != null) {
				memberOutmoneyService.outmoneyOver(p2pKillOutwardDto.getOrderNumber(), p2pKillOutwardDto.getTimestamp(), entity);
			} else {
				parttimeOutmoneyService.outmoneyOver(p2pKillOutwardDto.getOrderNumber(), p2pKillOutwardDto.getTimestamp(), entity);
			}
			Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
			return successMapResult;
		} catch (Exception e) {
			log.error("出款人员主动结束出款任务报错 {}", p2pKillOutwardDto.toString(), e);
			return SimpleResponseData.getErrorMapResult(e.getMessage());
		}
	}
}
