package com.xinbo.fundstransfer.chatPay.commons.services;

import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import lombok.extern.slf4j.Slf4j;


/**
 * ************************
 * 聊天室支付- 房间类型处理基类
 * @author tony
 */
@Slf4j
public abstract class AbstractRoomHandlerBaseServer extends ChatPayBaseServer {


    /**
     * 会员入款时， 匹配会员出款单  预留时间
     */
    protected  long timeOutAddsOnFindRoom = 1000L;

    /**
     * 会员入款时，拉兼职创建代收/付 房间 预留时间
     */
    protected  long timeOutAddsOnFindCreateRebateUser = 1000L;




    /**
     * 聊天室支付，保存会员出款单
     */
    public BizOutwardRequest handleOutMoneyOrder(BizOutwardRequest bizOutwardRequest) {
         return outwardRequestService.save(bizOutwardRequest);
    }


    /**
     * 聊天室支付，保存会员出款单
     */
    public BizIncomeRequest handleInMoneyOrder(BizIncomeRequest bizIncomeRequest) {
        return incomeRequestService.saveAndFlush(bizIncomeRequest);//保存入款单
    }



}
