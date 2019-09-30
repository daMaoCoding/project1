package com.xinbo.fundstransfer.chatPay.ptorder.services;
import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.chatPay.commons.config.AliOutConfig;
import com.xinbo.fundstransfer.chatPay.commons.enums.RedisKeyEnums;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import org.redisson.api.RLock;

import java.util.Map;


public interface RoomHandler {
    /**
     * 是否启用
     */
    boolean isUsable();



    /**
     * 房间策略类型
     */
    RedisKeyEnums getRoomType();



    /**
     * 是否能处理
     */
    boolean canDoOutMoney(BizOutwardRequest bizOutwardRequest, BizHandicap handicap, BizLevel level, String remarkNum, AliOutConfig aliOutConfig);



    /**
     * 处理出款单
     */
    void handleOutMoneyOrder(BizOutwardRequest bizOutwardRequest, BizHandicap handicap, BizLevel level, String remarkNum, AliOutConfig aliOutConfig);


    /**
     * 处理入款单
     */
    void handleInMoneyOrder(BizIncomeRequest bizIncomeRequest, BizHandicap handicap, BizLevel level, String remarkNum, AliIncomeConfig geAliIncomeConfig);




    /**
     * 会员入款找房间(已有出款单创建的房间)-支付宝
     */
    Map<String, RLock> findMemberOutMoneyZfbRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig aliIncomeConfig, long findMemberOutMoneyRoomTimeOut);

    /**
     * 会员入款找房间(已有出款单创建的房间)-微信
     */
    Map<String, RLock> findMemberOutMoneyWxRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig aliIncomeConfig, long findMemberOutMoneyRoomTimeOut);




    /**
     * 会员入款-找兼职创建房间-支付宝代收
     */
    Map<String, RLock> findCreateRebateUserZfbDaiShouRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig geAliIncomeConfig, long rebateUserRoomTimeOut);

    /**
     * 会员入款-找兼职创建房间-微信代收
     */
    Map<String, RLock> findCreateRebateUserWxDaiShouRoom(BizIncomeRequest bizIncomeRequest, AliIncomeConfig geAliIncomeConfig, long rebateUserRoomTimeOut);



}