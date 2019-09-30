package com.xinbo.fundstransfer.chatPay.ptorder.services;


import com.xinbo.fundstransfer.chatPay.ptorder.reqVo.ReqOutMoney;

import java.util.Map;

/**
 * ************************
 * 聊天室支付 出款
 * @author tony
 */
public interface ReqOutMoneyServer{


    /**
     * 聊天室支付 出款
     */
    Map<String, String> reqOutMoney(ReqOutMoney reqOutMoney);
}
