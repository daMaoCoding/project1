package com.xinbo.fundstransfer.component.net.http.restTemplate;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.CancelInOrderBack;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.ChangeUserChatPayBlacklistBack;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.SureInOrderBack;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.SureNewInOrderBack;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.SureNewOutOrderBack;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.SureOrCancelOutOrderBack;
import com.xinbo.fundstransfer.chatPay.ptqrval.reqVo.ReqPtQrValBack;


/**
 * ************************
 *  与平台http交互
 * @author tony
 */

@Component
public class PlatformServiceApiStatic extends  BaseServiceApiStatic{

    private static final String notifyQrCheckResult ="/crk/v2/notifyQrCheckResult";
    private static final String sureInOrder ="/crk/v2/sureInOrder";
    private static final String cancelInOrder ="/crk/v2/cancelInOrder";
    private static final String sureNewInOrder ="/crk/v2/sureNewInOrder";
    private static final String sureOrCancelOutOrder ="/crk/v2/sureOrCancelOutOrder";
    private static final String sureNewOutOrder ="/crk/v2/sureNewOutOrder";
    private static final String changeUserChatPayBlacklist ="/crk/v2/changeUserChatPayBlacklist";


    /**
     * 通知平台二维码验证状态
     */
    public int notifyQrCheckResult(ReqPtQrValBack reqPtQrValBack) {
        return basePostJsonPt(notifyQrCheckResult,reqPtQrValBack,platformCrkBaseHeader).getIntValue("status"); //失败返回0
    }
    /**
     * 会员入款单确认                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
     */
    public int sureInOrder(SureInOrderBack sureInOrderBack) {
        return basePostJsonPt(sureInOrder,sureInOrderBack,platformCrkBaseHeader).getIntValue("status"); //失败返回0
    }
    /**
     * 会员入款单取消 
     */
    public int cancelInOrder(CancelInOrderBack cancelInOrderBack) {
        return basePostJsonPt(cancelInOrder,cancelInOrderBack,platformCrkBaseHeader).getIntValue("status"); //失败返回0
    }
    /**
     * 会员入款单部分确认(入款补提单) 
     */
    public int sureNewInOrder(SureNewInOrderBack sureNewInOrderBack) {
        return basePostJsonPt(sureNewInOrder,sureNewInOrderBack,platformCrkBaseHeader).getIntValue("status"); //失败返回0
    }
    /**
     * 会员出款单确认或取消 
     */
    public int sureOrCancelOutOrder(SureOrCancelOutOrderBack sureOrCancelOutOrderBack) {
        return basePostJsonPt(sureOrCancelOutOrder,sureOrCancelOutOrderBack,platformCrkBaseHeader).getIntValue("status"); //失败返回0
    }
    /**
     * 会员出款部分确认(出款补单)  
     */
    public JSONObject sureNewOutOrder(SureNewOutOrderBack sureNewOutOrderBack) {
        return basePostJsonPt(sureNewOutOrder,sureNewOutOrderBack,platformCrkBaseHeader); //失败返回0
    }
    /**
     * 修改会员聊天室通道黑名单状态[拉黑/解除拉黑]  
     */
    public int changeUserChatPayBlacklist(ChangeUserChatPayBlacklistBack changeUserChatPayBlacklistBack) {
        return basePostJsonPt(changeUserChatPayBlacklist,changeUserChatPayBlacklistBack,platformCrkBaseHeader).getIntValue("status"); //失败返回0
    }
   
}
