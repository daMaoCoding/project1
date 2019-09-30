package com.xinbo.fundstransfer.chatPay.callCenter.services;

import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqRebateUserOut;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqRebateUserReady;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.ChangeUserChatPayBlacklistBack;

/**
 * ************************
 * 聊天室支付 兼职状态
 * @author tony
 */
public interface RebateUserStatusServices {

    /**
     * 聊天室支付 返利网兼职开始准备接收任务
     */
    boolean p2pRebateUserReady(ReqRebateUserReady reqRebateUserReady);

    /**
     * 聊天室支付 返利网兼职
     */
    boolean p2pRebateUserOut(ReqRebateUserOut reqRebateUserOut);


    /**
     * 修改会员聊天室通道黑名单状态[拉黑/解除拉黑]
     */
    boolean changeUserChatPayBlacklist(ChangeUserChatPayBlacklistBack changeUserChatPayBlacklistBack);
}
