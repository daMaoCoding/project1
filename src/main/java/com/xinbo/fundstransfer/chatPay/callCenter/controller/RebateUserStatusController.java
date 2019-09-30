package com.xinbo.fundstransfer.chatPay.callCenter.controller;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqRebateUserOut;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqRebateUserReady;
import com.xinbo.fundstransfer.chatPay.callCenter.services.RebateUserStatusServices;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.ChangeUserChatPayBlacklistBack;
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
 * 客服系统 调用出入款，通知出入款兼职状态相关
 * @author tony
 */
@Slf4j
@RestController
@RequestMapping("/api/callcenter/v1")
@RequireHeader(HeaderCondition.chatPayHeader)
public class RebateUserStatusController {

    @Autowired  RebateUserStatusServices rebateUserStatusServices;


    /**
     * 兼职上线通知
     */
    @RequestMapping(value = "/p2pRebateUserReady",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> p2pRebateUserReady(@Validated @RequestBody ReqRebateUserReady reqRebateUserReady, Errors errors) {

        //自动参数验证
        log.info("[兼职上线通知]->p2pRebateUserReady,参数：{}", JSON.toJSONString(reqRebateUserReady));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        try {
            if(rebateUserStatusServices.p2pRebateUserReady(reqRebateUserReady))
                return SimpleResponseData.getSuccessMapResult();
            throw  new RuntimeException("上线失败");
        }catch (Exception e){
            log.error("[兼职上线通知]->p2pRebateUserReady,异常：{}",e.getMessage(),e);
            return SimpleResponseData.getErrorMapResult(StringUtils.isEmpty(e.getMessage())?"内部错误,"+System.currentTimeMillis():e.getMessage());
        }
    }





    /**
     * 兼职离线通知
     */
    @RequestMapping(value = "/p2pRebateUserOut",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> p2pRebateUserOut(@Validated @RequestBody ReqRebateUserOut reqRebateUserOut, Errors errors) {

        //自动参数验证
        log.info("[兼职离线通知]->p2pRebateUserOut,参数：{}", JSON.toJSONString(reqRebateUserOut));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        try {
            if(rebateUserStatusServices.p2pRebateUserOut(reqRebateUserOut))
                return SimpleResponseData.getSuccessMapResult();
            throw  new RuntimeException("离线通知失败");
        }catch (Exception e){
            log.error("[兼职离线通知]->p2pRebateUserOut,异常：{}",e.getMessage(),e);
            return SimpleResponseData.getErrorMapResult(StringUtils.isEmpty(e.getMessage())?"内部错误,"+System.currentTimeMillis():e.getMessage());
        }
    }


    /**
     * 修改会员聊天室通道黑名单状态[拉黑/解除拉黑]
     * 客服系统调用 出入款，出入款转发消息至平台
     */
    @RequestMapping(value = "/changeUserChatPayBlacklist",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> changeUserChatPayBlacklist(@Validated @RequestBody ChangeUserChatPayBlacklistBack changeUserChatPayBlacklistBack, Errors errors) {

        //自动参数验证
        log.info("[修改会员聊天室通道黑名单状态[拉黑/解除拉黑]]->changeUserChatPayBlacklist,参数：{}", JSON.toJSONString(changeUserChatPayBlacklistBack));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }

        try {
            if(rebateUserStatusServices.changeUserChatPayBlacklist(changeUserChatPayBlacklistBack))
                return SimpleResponseData.getSuccessMapResult();
            throw  new RuntimeException("拉黑/解除拉黑 通知失败");
        }catch (Exception e){
            log.error("[拉黑/解除拉黑]->changeUserChatPayBlacklist,异常：{}",e.getMessage(),e);
            return SimpleResponseData.getErrorMapResult(StringUtils.isEmpty(e.getMessage())?"内部错误,"+System.currentTimeMillis():e.getMessage());
        }
    }




}
