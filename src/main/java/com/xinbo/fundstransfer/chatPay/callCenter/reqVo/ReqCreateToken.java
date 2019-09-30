package com.xinbo.fundstransfer.chatPay.callCenter.reqVo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ************************
 * 出入款系统 调用客服系统创建 获取房间token
 * @author tony
 */
@Data
@NoArgsConstructor
public class ReqCreateToken {


    /**
     * （必传）房间号
     */
    private String roomId;

    /**
     *（必传）用户ID
     */
    private String userId;


    /**
     * 业务ID
     */
    private String bizId;

    public ReqCreateToken(String roomId, String userId, String bizId) {
        this.roomId = roomId;
        this.userId = userId;
        this.bizId = bizId;
    }
}
