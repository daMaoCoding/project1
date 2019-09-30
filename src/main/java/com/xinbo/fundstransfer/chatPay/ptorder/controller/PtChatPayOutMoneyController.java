package com.xinbo.fundstransfer.chatPay.ptorder.controller;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.ptorder.reqVo.ReqOutMoney;
import com.xinbo.fundstransfer.chatPay.ptorder.services.ReqOutMoneyServer;
import com.xinbo.fundstransfer.component.mvc.HeaderCondition;
import com.xinbo.fundstransfer.component.mvc.RequireHeader;
import com.xinbo.fundstransfer.utils.SimpleResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ************************
 * 平台请求出入款 会员出款提单(同步出款单)
 * @author tony
 */
@Slf4j
@RestController
@RequestMapping("/api/v2")
@RequireHeader(HeaderCondition.ptCrkHeader)
public class PtChatPayOutMoneyController {

    @Autowired    ReqOutMoneyServer reqOutMoneyServer;


    /**
     * 聊天室支付 平台 出款
     * 返回：
     *     "roomUrl":"http://abc.com/?token=200"   //聊天室房间url
     *     "roomNum":"100" //房间号
     *     "token"："111111" //进入房间的token
     *     "bizId":"653" //业务id,通过业务id可查到入款单信息，聊天室端保存
     *
     */

    @RequestMapping(value = "/reqOutward",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> reqOutward(@Validated @RequestBody ReqOutMoney reqOutMoney, Errors errors){
        //自动参数验证
        log.info("[聊天室支付-会员出款]->reqOutward,参数：{}", JSON.toJSONString(reqOutMoney));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            Map<String,String> outMoneyRoomAndTokenInfo =  reqOutMoneyServer.reqOutMoney(reqOutMoney);
            Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
            successMapResult.putAll(outMoneyRoomAndTokenInfo);
            return successMapResult;
        }catch (Exception e){
            log.error("[聊天室支付/出款],出错:{}",e.getMessage(),e);
            return SimpleResponseData.getErrorMapResult(StringUtils.isEmpty(e.getMessage())?"内部错误,"+System.currentTimeMillis():e.getMessage());
        }

    }






}
