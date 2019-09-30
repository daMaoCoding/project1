package com.xinbo.fundstransfer.chatPay.ptqrval.controller;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.services.BizQrInfoServer;
import com.xinbo.fundstransfer.chatPay.ptqrval.reqVo.ReqPtQrVal;
import com.xinbo.fundstransfer.component.mvc.RequireHeader;
import com.xinbo.fundstransfer.utils.TokenCheckUtil;
import com.xinbo.fundstransfer.utils.randomUtil.RandomUtil;
import com.xinbo.fundstransfer.AppProperties;
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
 * 平台 会员 微信/支付宝账号 绑定验证
 * @author tony
 */
@Slf4j
@RestController
@RequestMapping({"/api/v2"})
@RequireHeader(HeaderCondition.ptCrkHeader)
public class PtWxZfbAccountValController {

    private static String TAG = "平台会员二维码验证";

    @Autowired    BizQrInfoServer bizQrInfoServer;
    @Autowired    RandomUtil randomUtil;
    @Autowired    AppProperties appProperties;


    /**
     * 平台会员二维码验证
     */
    @RequestMapping(value = "/reqQrVal",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> reqQrVal(@Validated @RequestBody ReqPtQrVal reqPtQrVal, Errors errors){

        //自动参数验证
        log.info("{}->reqQrVal,参数：{}",TAG, JSON.toJSONString(reqPtQrVal));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        //验证token(不验证token,验证HttpHeader+IP绑定)
        //Object[] tokenCheckArgs = new Object []{reqPtQrVal.getOid(),reqPtQrVal.getUid(),reqPtQrVal.getUname(),reqPtQrVal.getQrType(),reqPtQrVal.getAccount(),reqPtQrVal.getName(),reqPtQrVal.getQrContent(),appProperties.getKeystore()};
        //if(!TokenCheckUtil.reqTokenCheck(reqPtQrVal.getToken(),tokenCheckArgs))  return SimpleResponseData.getErrorMapResult("token验证错误。");

        //保存二维码验证信息
        Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
        BizQrInfo bizQrInfo  = bizQrInfoServer.convertAndCheck(reqPtQrVal);
        if(null==bizQrInfo)  return SimpleResponseData.getErrorMapResult("内部错误："+System.currentTimeMillis());
        bizQrInfo = bizQrInfoServer.saveAndFlush(bizQrInfo);
        successMapResult.put("qrId",bizQrInfo.getQrId());

        return successMapResult;
    }

}
