package com.xinbo.fundstransfer.chatPay.ptorder.services;


import com.xinbo.fundstransfer.chatPay.ptorder.reqVo.ReqInMoney;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import org.redisson.api.RLock;

import java.util.Map;

/**
 * ************************
 *  聊天室支付  平台入款
 * @author tony
 */
public interface ReqInMoneyServer{

    /**
     * 请求入款
     */
    Map<String, String> reqInMoney(ReqInMoney reqInMoney,long startTimeStamp);


    /**
     * 出入款入款单找房间
     * @return
     */
    Map<String, RLock> findMemberOutMoneyRoom(BizIncomeRequest bizIncomeRequest);
}
