package com.xinbo.fundstransfer.chatPay.flwsynzfbwx.controller;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo.ReqRebateUserWxZfbAccount;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo.ReqRebateUserWxZfbAccountStatus;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.services.BizQrInfoServer;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.utils.TokenCheckUtil;
import com.xinbo.fundstransfer.utils.randomUtil.RandomUtil;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.utils.SimpleResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ************************
 * 返利网同步 兼职微信/支付宝账号
 * @author tony
 */
@Slf4j
@RestController
@RequestMapping("/api/v3")
public class FlwWxZfbAccountController {

    private static String TAG = "返利网兼职微信/支付宝账号同步";

    @Autowired    BizQrInfoServer bizQrInfoServer;
    @Autowired    RandomUtil randomUtil;
    @Autowired    AppProperties appProperties;

    /**
     * 返利网兼职支付宝账号同步（每个账号只有1个收款码）
     */
    @RequestMapping(value = "/synwxzfb",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> synwxzfb(@Validated @RequestBody ReqRebateUserWxZfbAccount reqRebateUserWxZfbAccount, Errors errors){

        //自动参数验证
        log.info("{}->synwxzfb,参数：{}",TAG, JSON.toJSONString(reqRebateUserWxZfbAccount));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        try {
            //验证token
            Object[] tokenCheckArgs = new Object []{reqRebateUserWxZfbAccount.getUid(),reqRebateUserWxZfbAccount.getUname(),reqRebateUserWxZfbAccount.getQrType(),reqRebateUserWxZfbAccount.getAccount(),reqRebateUserWxZfbAccount.getName(),reqRebateUserWxZfbAccount.getQrContent(),reqRebateUserWxZfbAccount.getTimestamp(),appProperties.getRebatesalt()};
            if(!TokenCheckUtil.reqTokenCheck(reqRebateUserWxZfbAccount.getToken(),tokenCheckArgs)) throw new RuntimeException("token验证错误。");

            //保存兼职支付宝/微信 二维码Account&AccountMore
            BizAccount bizAccountDb =   bizQrInfoServer.saveOrUpdateAccountAndAccountMore(reqRebateUserWxZfbAccount);
            if(null==bizAccountDb)   throw new RuntimeException("内部错误："+System.currentTimeMillis());

            //保存二维码验证信息-二维码验证表
            BizQrInfo bizQrInfo  = bizQrInfoServer.convertAndCheck(reqRebateUserWxZfbAccount);
            if(null==bizQrInfo)  throw new RuntimeException("内部错误："+System.currentTimeMillis());
            bizQrInfo = bizQrInfoServer.saveAndFlush(bizQrInfo);

            Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
            successMapResult.put("qrId",bizQrInfo.getQrId());
            return successMapResult;

        }catch (Exception e){
            log.error("[返利网兼职微信/支付宝账号同步],出错:{}",e.getMessage(),e);
            return SimpleResponseData.getErrorMapResult(StringUtils.isEmpty(e.getMessage())?"内部错误,"+System.currentTimeMillis():e.getMessage());
        }
    }





    /**
     * 兼职 启用/停用  微信/支付宝 收款二维码（每个账号只有1个收款码）
     * 1.兼职在返利网后台可以停用自己的二维码-->修改qrinfo表的qrStatus
     * 2.出入款运营人员可以在出入款后台停用账号account-->修改account status
     */
    @RequestMapping(value = "/synwxzfbStatus",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> synwxzfbStatus(@Validated @RequestBody ReqRebateUserWxZfbAccountStatus reqRebateUserWxZfbAccountStatus, Errors errors){

        //自动参数验证
        log.info("{}->synwxzfbStatus,参数：{}",TAG, JSON.toJSONString(reqRebateUserWxZfbAccountStatus));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        try {
            //验证token
            Object[] tokenCheckArgs = new Object []{reqRebateUserWxZfbAccountStatus.getUid(),reqRebateUserWxZfbAccountStatus.getQrId(),reqRebateUserWxZfbAccountStatus.getTimestamp(),appProperties.getRebatesalt()};
            if(!TokenCheckUtil.reqTokenCheck(reqRebateUserWxZfbAccountStatus.getToken(),tokenCheckArgs)) throw new RuntimeException("token验证错误。");

            //保存二维码状态
            BizQrInfo bizQrInfo  = bizQrInfoServer.findQrInfoByQrId(reqRebateUserWxZfbAccountStatus.getQrId());
            if(null==bizQrInfo)  throw new RuntimeException("二维码ID错误："+reqRebateUserWxZfbAccountStatus.getQrId()+"，"+System.currentTimeMillis());
            bizQrInfo.setQrStatus(reqRebateUserWxZfbAccountStatus.getStatus());
            bizQrInfoServer.saveAndFlush(bizQrInfo);
            return SimpleResponseData.getSuccessMapResult();
        }catch (Exception e){
            log.error("[返利网兼职微信/支付宝账号状态同步],出错:{}",e.getMessage(),e);
            return SimpleResponseData.getErrorMapResult(StringUtils.isEmpty(e.getMessage())?"内部错误,"+System.currentTimeMillis():e.getMessage());
        }
    }





}
