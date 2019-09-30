package com.xinbo.fundstransfer.chatPay.ptorder.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.chatPay.commons.config.AliOutConfig;
import com.xinbo.fundstransfer.chatPay.ptorder.services.RoomHandler;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * ************************
 *
 * @author tony
 */
@Slf4j
@Component
public class RoomHandlerAdapter implements ApplicationContextAware {

    ApplicationContext       applicationContext;
    List<RoomHandler>        roomHandlers = Lists.newLinkedList();
    Map<String, RoomHandler> roomHandlerMap = Maps.newLinkedHashMap();
    RuntimeException         cannotFindRoomHandler =   new RuntimeException("内部错误，无法找到出款房间处理器");


    @PostConstruct
    public void initRoomHandler(){
        if(null!=applicationContext){
            roomHandlerMap = applicationContext.getBeansOfType(RoomHandler.class);
            roomHandlerMap.forEach((k,v)->{
                this.roomHandlers.add(v);
            });

        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(this.applicationContext==null){
            this.applicationContext = applicationContext;
        }
    }



    /**
     * 随机选择房间处理器
     */
    private   RoomHandler findAnyOneRoomHandler(){
        for (RoomHandler roomHandler : roomHandlers) {
            if(null!=roomHandler && roomHandler.isUsable())
                return roomHandler;
        }
        throw cannotFindRoomHandler;
    }


    /**
     * 处理出款单,使用可用的房间处理器
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleOutMoneyOrder(BizOutwardRequest bizOutwardRequest, BizHandicap handicap, BizLevel level, String remarkNum, AliOutConfig aliOutConfig){
        for (RoomHandler roomHandler : roomHandlers) {
            if(null!=roomHandler && roomHandler.isUsable() && roomHandler.canDoOutMoney(bizOutwardRequest,handicap,level,remarkNum,aliOutConfig)){
                roomHandler.handleOutMoneyOrder(bizOutwardRequest,handicap,level,remarkNum,aliOutConfig);
                break;
            }else{
                log.error("[聊天室支付]-出入款-无法找到出款房间处理器");
                throw cannotFindRoomHandler;
            }
        }

    }


    /**
     * 处理入款单，遍历全部处理器，防止遗留
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleInMoneyOrder(BizIncomeRequest bizIncomeRequest, BizHandicap handicap, BizLevel level, String remarkNum, AliIncomeConfig geAliIncomeConfig) {
        //遍历之前的room处理类，发现遗留的redisKEY
        RoomHandler anyOneRoomHandler = findAnyOneRoomHandler();
        anyOneRoomHandler.handleInMoneyOrder(bizIncomeRequest, handicap, level, remarkNum, geAliIncomeConfig);
    }




    /**
     * 会员入款找房间(已有出款单创建的房间)
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, RLock> findMemberOutMoneyRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig aliIncomeConfig, long findMemberOutMoneyRoomTimeOut)  {
        RoomHandler anyOneRoomHandler = findAnyOneRoomHandler();
        switch (bizIncomeRequest.getIncomeRequestType()){
            case PlatFromAli:
                return anyOneRoomHandler.findMemberOutMoneyZfbRoom( bizIncomeRequest, aliIncomeConfig,findMemberOutMoneyRoomTimeOut);
            case PlatFromWechat:
                return anyOneRoomHandler.findMemberOutMoneyWxRoom( bizIncomeRequest, aliIncomeConfig,findMemberOutMoneyRoomTimeOut);
        }
        return null;
    }



    /**
     * 会员入款-找兼职创建房间
     */
    @Transactional(rollbackFor = Exception.class)
    public  Map<String, RLock > findCreateRebateUserDaiShouRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig geAliIncomeConfig, long rebateUserRoomTimeOut) {
        RoomHandler anyOneRoomHandler = findAnyOneRoomHandler();
        switch (bizIncomeRequest.getIncomeRequestType()){
            case PlatFromAli:
                return anyOneRoomHandler.findCreateRebateUserZfbDaiShouRoom( bizIncomeRequest, geAliIncomeConfig,rebateUserRoomTimeOut);
            case PlatFromWechat:
                return anyOneRoomHandler.findCreateRebateUserWxDaiShouRoom( bizIncomeRequest, geAliIncomeConfig,rebateUserRoomTimeOut);
        }
        return null;
    }






}
