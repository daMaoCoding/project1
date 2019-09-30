package com.xinbo.fundstransfer.chatPay.inmoney.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.params.P2pIncomAckDto;
import com.xinbo.fundstransfer.chatPay.inmoney.params.P2pIncomReadyDto;
import com.xinbo.fundstransfer.chatPay.inmoney.params.P2pKillIncomDto;
import com.xinbo.fundstransfer.chatPay.inmoney.services.ChatpayLogService;
import com.xinbo.fundstransfer.chatPay.inmoney.services.InmoneyService;
import com.xinbo.fundstransfer.utils.SimpleResponseData;

import lombok.extern.slf4j.Slf4j;

/**
 * 入款支付时的操作接口
 * 
 * @author ERIC
 *
 */

@Slf4j
@RestController
@RequestMapping("/api/callcenter/v1")
public class InmoneyController {

	private static String TAG = "入款支付操作";

	@Autowired
	@Qualifier("ParttimeInmoneyServiceImpl")
	private InmoneyService parttimeInmoneyService;
	@Autowired
	@Qualifier("MemberInmoneyServiceImpl")
	private InmoneyService memberInmoneyService;
	@Autowired
	private ChatpayLogService chatpayLogService;
	
	/**
	 * 入款方开始转账
	 * 
	 * @param p2pIncomReadyDto
	 * @param errors
	 * @return
	 */
	@RequestMapping(value = "/p2pIncomReady", method = RequestMethod.POST, consumes = "application/json")
	public Map<String, Object> p2pIncomReady(@Validated @RequestBody P2pIncomReadyDto p2pIncomReadyDto, Errors errors) {
		// 自动参数验证
		log.info("{}->p2pIncomReady,参数：{}", TAG, JSON.toJSONString(p2pIncomReadyDto));
		if (errors.hasErrors()) {
			return SimpleResponseData.getErrorMapResult(errors.getAllErrors().get(0).getDefaultMessage());
		}
		try {
			BizChatpayLog chatpayLog = chatpayLogService.findByIncomeOrderNoAndStatus(p2pIncomReadyDto.getOrderNumber());
			if(chatpayLog.incomeIsMember()) {			
				memberInmoneyService.appendChatpayLogRemark(chatpayLog, "入款方" + p2pIncomReadyDto.getOrderNumber() + "开始转账");
			} else {
				parttimeInmoneyService.appendChatpayLogRemark(chatpayLog, "入款方" + p2pIncomReadyDto.getOrderNumber() + "开始转账");
			}			
			Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
			return successMapResult;
		} catch (Exception e) {
			log.error("入款方开始转账报错 {}", p2pIncomReadyDto.toString(), e);
			return SimpleResponseData.getErrorMapResult(e.getMessage());
		}
	}

	/**
	 * 入款方请求出款方确认
	 * 
	 * @param p2pIncomAckDto
	 * @param errors
	 * @return
	 */
	@RequestMapping(value = "/p2pIncomReqAck", method = RequestMethod.POST, consumes = "application/json")
	public Map<String, Object> p2pIncomReqAck(@Validated @RequestBody P2pIncomAckDto p2pIncomAckDto, Errors errors) {
		// 自动参数验证
		log.info("{}->p2pIncomReqAck,参数：{}", TAG, JSON.toJSONString(p2pIncomAckDto));
		if (errors.hasErrors()) {
			return SimpleResponseData.getErrorMapResult(errors.getAllErrors().get(0).getDefaultMessage());
		}
		try {
			BizChatpayLog chatpayLog = chatpayLogService.findByIncomeOrderNoAndStatus(p2pIncomAckDto.getOrderNumber());
			if(chatpayLog.incomeIsMember()) {			
				memberInmoneyService.appendChatpayLogRemark(chatpayLog, "入款方" + p2pIncomAckDto.getOrderNumber() + "请求出款方确认");
			} else {
				parttimeInmoneyService.appendChatpayLogRemark(chatpayLog, "入款方" + p2pIncomAckDto.getOrderNumber() + "请求出款方确认");
			}
			Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
			return successMapResult;
		} catch (Exception e) {
			log.error("入款方请求出款方确认报错 {}", p2pIncomAckDto.toString(), e);
			return SimpleResponseData.getErrorMapResult(e.getMessage());
		}
	}

	/**
	 * 入款方不支付超时被客服踢出
	 * 
	 * @param p2pKillIncomDto
	 * @param errors
	 * @return
	 */
	@RequestMapping(value = "/p2pKillIncom", method = RequestMethod.POST, consumes = "application/json")
	public Map<String, Object> p2pKillIncom(@Validated @RequestBody P2pKillIncomDto p2pKillIncomDto, Errors errors) {
		// 自动参数验证
		log.info("{}->p2pKillIncom,参数：{}", TAG, JSON.toJSONString(p2pKillIncomDto));
		if (errors.hasErrors()) {
			return SimpleResponseData.getErrorMapResult(errors.getAllErrors().get(0).getDefaultMessage());
		}
		try {
			BizChatpayLog chatpayLog = chatpayLogService.findByIncomeOrderNoAndStatus(p2pKillIncomDto.getOrderNumber());
			if(chatpayLog.incomeIsMember()) {
				memberInmoneyService.killIncomeOrder(p2pKillIncomDto, chatpayLog);
			} else {
				parttimeInmoneyService.killIncomeOrder(p2pKillIncomDto, chatpayLog);
			}
			Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
			return successMapResult;
		} catch (Exception e) {
			log.error("入款方不支付超时被客服踢出报错 {}", p2pKillIncomDto.toString(), e);
			return SimpleResponseData.getErrorMapResult(e.getMessage());
		}		
	}
}
