package com.xinbo.fundstransfer.chatPay.inmoney.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.inmoney.params.BillLogDto;
import com.xinbo.fundstransfer.chatPay.inmoney.services.MemberToolServices;
import com.xinbo.fundstransfer.utils.SimpleResponseData;

import lombok.extern.slf4j.Slf4j;

/**
 *  工具上报流水接口
 * @author ERIC
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/memberTools")
public class MemberToolController {
	private static String TAG = "工具上报流水";
	@Autowired
	private MemberToolServices memberToolServices;
	/**
	 * 	接收工具上报流水-(支付宝流水)
	 * @param p2pIncomReadyDto
	 * @param errors
	 * @return
	 */
    @RequestMapping(value = "/repotZfbBillsLog",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> repotZfbBillsLog(@Validated @RequestBody BillLogDto billLogDto, Errors errors) {
        //自动参数验证
        log.info("{}->repotZfbBillsLog,参数：{}",TAG, JSON.toJSONString(billLogDto));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }
        try {
	        memberToolServices.repotZfbBillsLog(billLogDto);
	        Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
	        return successMapResult;        
        } catch (Exception e) {
			log.error("接收工具上报支付宝流水报错 {}", billLogDto.toString(), e);
			return SimpleResponseData.getErrorMapResult(e.getMessage());
		}	      
    }
    
	/**
	 * 	接收工具上报流水-(微信流水)
	 * @param p2pIncomReadyDto
	 * @param errors
	 * @return
	 */
    @RequestMapping(value = "/repotWxBillsLog",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> repotWxBillsLog(@Validated @RequestBody BillLogDto billLogDto, Errors errors) {
        //自动参数验证
        log.info("{}->repotZfbBillsLog,参数：{}",TAG, JSON.toJSONString(billLogDto));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }
        try {        
	        memberToolServices.repotWxBillsLog(billLogDto);
	        Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
	        return successMapResult;   
        } catch (Exception e) {
			log.error("接收工具上报微信流水报错 {}", billLogDto.toString(), e);
			return SimpleResponseData.getErrorMapResult(e.getMessage());
		}	        
    }	    
}
