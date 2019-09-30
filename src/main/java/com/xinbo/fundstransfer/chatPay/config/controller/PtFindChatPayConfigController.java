package com.xinbo.fundstransfer.chatPay.config.controller;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.chatPay.commons.config.ChatPayConfigService;
import com.xinbo.fundstransfer.chatPay.commons.enums.BizQrInfoEnums;
import com.xinbo.fundstransfer.chatPay.config.reqVo.ReqWxZfbIncomeTimeOut;
import com.xinbo.fundstransfer.component.mvc.RequireHeader;
import com.xinbo.fundstransfer.utils.SimpleResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.xinbo.fundstransfer.component.mvc.*;

import java.util.Map;

/**
 * ************************
 *  平台调用出入款查询 聊天室支付相关参数配置
 * @author tony
 */
@Slf4j
@RestController
@RequestMapping({"/api/v2"})
@RequireHeader(HeaderCondition.ptCrkHeader)
public class PtFindChatPayConfigController{


    @Autowired  ChatPayConfigService chatPaySettingService;



    /**
     * 平台查询会员微信支付宝入款时超时时间
     */
    @RequestMapping(value = "/reqChatPayIncomeTimeOut",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> reqChatPayIncomeTimeOut(@Validated @RequestBody ReqWxZfbIncomeTimeOut reqWxZfbIncomeTimeOut, Errors errors){
        //自动参数验证
        log.info("[平台查询会员微信支付宝入款时超时时间]->reqChatPayIncomeTimeOut,参数：{}", JSON.toJSONString(reqWxZfbIncomeTimeOut));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        int timeOut = 60;
        int type = reqWxZfbIncomeTimeOut.getQrType();

        try {
            if(type==BizQrInfoEnums.QrType.ZFB.getNum()){
                AliIncomeConfig aliIncomeConfig = chatPaySettingService.getAliIncomeConfig();
                timeOut = aliIncomeConfig.getTotalMatchSecondForAliIn();
            }else if(type==BizQrInfoEnums.QrType.WX.getNum()){
                //todo://微信timeout
            }
        }catch (Exception e){
            log.error("[平台获取聊天室支付支付宝/微信 入款等待超时时间]，出错：{}",e.getMessage(),e);
        }
        Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
        successMapResult.put("timeOut",timeOut+1);
        return successMapResult;
    }

}
