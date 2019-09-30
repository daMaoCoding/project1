package com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo;

import lombok.Data;

/**
 * ************************
 * 修改会员聊天室通道黑名单状态[拉黑/解除拉黑] 
 * @author wira
 */
@Data
public class ChangeUserChatPayBlacklistBack {

    /**
     * token : 参数签名
     * oid : 盘口编码
     * type : 操作类型[1：拉黑，2：解除拉黑]
     * uid : 会员UID
     * user_name : 会员账号
     * admin_type : 操作人类型（1：客服，2：出入款）
     * admin_name : 操作人账号
     * channel_type : 通道类型（1支付宝入款，2支付宝出款，3微信入款，4微信出款）
     * opt_time : 操作时间
     */

    /**
     * 参数签名
     */
    private String token;

    /**
     * 盘口编码
     */
    private int oid;

    /**
     * 操作类型[1：拉黑，2：解除拉黑]
     */
    private byte type;

    /**
     * 会员UID
     */
    private Long uid;

    /**
     * 会员账号
     */
    private String user_name;

    /**
     * 操作人类型（1：客服，2：出入款）
     */
    private byte admin_type;

    /**
     *  操作人账号
     */
    private String admin_name;
    
    /**
     *   通道类型（1支付宝入款，2支付宝出款，3微信入款，4微信出款）
     */
    private byte channel_type;
    
    /**
     * 验证时间
     */
    private Long opt_time;


}
