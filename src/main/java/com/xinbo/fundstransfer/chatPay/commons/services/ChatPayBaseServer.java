package com.xinbo.fundstransfer.chatPay.commons.services;

import com.xinbo.fundstransfer.chatPay.commons.config.ChatPayConfigService;
import com.xinbo.fundstransfer.service.IncomeRequestService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.utils.randomUtil.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * ************************
 *
 * @author tony
 */
public abstract class ChatPayBaseServer {
    @Autowired   protected OutwardRequestService outwardRequestService;
    @Autowired   protected IncomeRequestService incomeRequestService;
    @Autowired   protected RedisService redisService;
    @Autowired   protected RandomUtil randomUtil;
    @Autowired   protected OidAndLevelServices oidAndLevelServices;
    @Autowired   protected ChatPayConfigService chatPayConfigService;



    /**
     * 暂停
     */
    protected  void sleep(long milliseconds){
        try {  TimeUnit.MILLISECONDS.sleep(milliseconds); }catch (Exception e){}
    }


}
