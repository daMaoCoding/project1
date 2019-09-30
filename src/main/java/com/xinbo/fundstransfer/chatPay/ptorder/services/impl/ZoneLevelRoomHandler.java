package com.xinbo.fundstransfer.chatPay.ptorder.services.impl;

import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.chatPay.commons.config.AliOutConfig;
import com.xinbo.fundstransfer.chatPay.commons.enums.RedisKeyEnums;
import com.xinbo.fundstransfer.chatPay.commons.services.AbstractRoomHandlerBaseServer;
import com.xinbo.fundstransfer.chatPay.ptorder.services.RoomHandler;
import com.xinbo.fundstransfer.domain.entity.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ************************
 *  区域层级-类型房间处理
 * @author tony
 */
@Data
@Slf4j
@Service
public class ZoneLevelRoomHandler extends AbstractRoomHandlerBaseServer implements RoomHandler {
    boolean usable = true;
    RedisKeyEnums roomType = RedisKeyEnums.CHATPAY_ZONE_LEVEL_ROOM;


    @Override
    public boolean isUsable() {
        return usable;
    }

    @Override
    public boolean canDoOutMoney(BizOutwardRequest bizOutwardRequest, BizHandicap handicap, BizLevel level, String remarkNum, AliOutConfig aliOutConfig) {
        if(handicap!=null && level!=null)
            return true;
        return false;
    }


    /**
     * 保存出款单
     */
    @Override
    public void handleOutMoneyOrder(BizOutwardRequest bizOutwardRequest, BizHandicap handicap, BizLevel level, String remarkNum, AliOutConfig aliOutConfig) {
        bizOutwardRequest = handleOutMoneyOrder(bizOutwardRequest); //保存出款单

        //todo:// 缓存房间信息，等待入款单查找
        //测试
        for (int i = 0; i < 100; i++) {
            String zoneS = String.valueOf( RandomUtils.nextInt(1,3));
            String levelS = String.valueOf( RandomUtils.nextInt(5,10));
            HashOperations<String, String, Object> hashOperations = redisService.getHashOperations();
            String value = RandomStringUtils.randomAlphabetic(100);
            hashOperations.put(roomType.concat(zoneS),levelS,value);
        }

    }


    /**
     * 保存入款单
     */
    @Override
    public void handleInMoneyOrder(BizIncomeRequest bizIncomeRequest, BizHandicap handicap, BizLevel level, String remarkNum, AliIncomeConfig geAliIncomeConfig) {
        bizIncomeRequest =  handleInMoneyOrder(bizIncomeRequest);
    }



    /**
     * 会员入款找房间(已有出款单创建的房间)-(支付宝)
     */
    @Override
    public Map<String, RLock> findMemberOutMoneyZfbRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig aliIncomeConfig, long findMemberOutMoneyRoomTimeOut) {
        long startTime = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted() &&  System.currentTimeMillis()-startTime < findMemberOutMoneyRoomTimeOut+timeOutAddsOnFindRoom ){
            System.out.println("寻找会员出款房间：-------"+System.currentTimeMillis());
            sleep(100);
        }
        return null;
    }





    /**
     * 会员入款-找兼职创建房间-(支付宝)
     */
    @Override
    public Map<String, RLock> findCreateRebateUserZfbDaiShouRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig geAliIncomeConfig, long rebateUserRoomTimeOut) {
        long startTime = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis()-startTime < rebateUserRoomTimeOut+timeOutAddsOnFindCreateRebateUser){
            System.out.println("创建兼职代收房间：-------"+System.currentTimeMillis());
             sleep(100);
        }
        return null;
    }

















    /**
     * 会员入款找房间(已有出款单创建的房间)-(微信)
     */
    @Override
    public Map<String, RLock> findMemberOutMoneyWxRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig aliIncomeConfig, long findMemberOutMoneyRoomTimeOut) {
        System.out.println(findMemberOutMoneyRoomTimeOut);
        throw new RuntimeException("暂不支持微信");
    }


    /**
     * 会员入款-找兼职创建房间-(微信)
     */
    @Override
    public Map<String, RLock> findCreateRebateUserWxDaiShouRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig geAliIncomeConfig, long rebateUserRoomTimeOut) {
        throw new RuntimeException("暂不支持微信");
    }


}
