package com.xinbo.fundstransfer.chatPay.valtools.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.services.BizQrInfoServer;
import com.xinbo.fundstransfer.utils.TokenCheckUtil;
import com.xinbo.fundstransfer.chatPay.valtools.reqVo.ReqBackValQrJob;
import com.xinbo.fundstransfer.chatPay.valtools.reqVo.ReqGetValQrJob;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.utils.SimpleResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ************************
 * 二维码验证工具-验证二维码
 * @author tony
 */
@Slf4j
@RestController
@RequestMapping("/api/qrtools/v1")
public class QrToolsController {

    private static String TAG = "二维码验证工具-验证二维码";
    private static Feature[] baseFeatures = {Feature.AllowUnQuotedFieldNames,Feature.IgnoreNotMatch};

    @Autowired    BizQrInfoServer bizQrInfoServer;
    @Autowired    AppProperties appProperties;


    /**
     * 二维码验证工具-定时获取待验证的二维码（每分钟）
     */
    @RequestMapping(value = "/getValQrJob",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> getValQrJob(@Validated @RequestBody ReqGetValQrJob reqGetValQrJob, Errors errors){

        //自动参数验证
        log.info("{}->getValQrJob,参数：{}",TAG, JSON.toJSONString(reqGetValQrJob));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        //验证token
        Object[] tokenCheckArgs = new Object []{reqGetValQrJob.getDeviceId(),appProperties.getQrtoolssalt()};
        if(!TokenCheckUtil.reqTokenCheck(reqGetValQrJob.getToken(),tokenCheckArgs))  return SimpleResponseData.getErrorMapResult("token验证错误。");


        //获取要验证二维码任务
        BizQrInfo bizQrInfo  = bizQrInfoServer.findValQrJob();
        if(null==bizQrInfo)  return SimpleResponseData.getErrorMapResult("没有验证任务，下次再来 ："+System.currentTimeMillis());
        bizQrInfo = bizQrInfoServer.valQrJobAccept(reqGetValQrJob,bizQrInfo); //更新任务状态

        Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
        successMapResult.put("qrID",bizQrInfo.getQrId());
        successMapResult.put("qrType",bizQrInfo.getQrType());
        successMapResult.put("account",bizQrInfo.getAccount());
        successMapResult.put("name",bizQrInfo.getName());
        successMapResult.put("qrContent",bizQrInfo.getQrContent());
        return successMapResult;
    }








    /**
     * 二维码验证工具-上报验证结果。
     */
    @RequestMapping(value = "/backValQrJob",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> backValQrJob(@Validated @RequestBody ReqBackValQrJob reqBackValQrJob, Errors errors){

        //自动参数验证
        log.info("{}->backValQrJob,参数：{}",TAG, JSON.toJSONString(reqBackValQrJob));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        //验证token
        Object[] tokenCheckArgs = new Object []{reqBackValQrJob.getDeviceId(),reqBackValQrJob.getQrId(),reqBackValQrJob.getStatus(),appProperties.getQrtoolssalt()};
        if(!TokenCheckUtil.reqTokenCheck(reqBackValQrJob.getToken(),tokenCheckArgs))  return SimpleResponseData.getErrorMapResult("token验证错误。");


        //上报二维码信息
        BizQrInfo bizQrInfo  = bizQrInfoServer.findQrInfoByQrId(reqBackValQrJob.getQrId());
        if(null==bizQrInfo)  return SimpleResponseData.getErrorMapResult("二维码id不正确。："+System.currentTimeMillis());
        bizQrInfoServer.backValQrJobAccept(reqBackValQrJob,bizQrInfo); //更新任务状态


        return SimpleResponseData.getSuccessMapResult();
    }







}
