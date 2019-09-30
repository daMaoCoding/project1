package com.xinbo.fundstransfer.chatPay.ptqrval.reqVo;

import lombok.Data;

/**
 * ************************
 * 出入款调用平台，异步通知二维码验证结果
 * @author tony
 */
@Data
public class ReqPtQrValBack {

    /**
     * token : 参数签名
     * oid : 66
     * uid : 77
     * qr_id : 155db
     * status : 1
     * opt_time : 1567738767
     */



    /**
     * 盘口编码
     */
    private int oid;

    /**
     * 会员UID
     */
    private Long uid;

    /**
     * 二维码ID(验证二维码有效性返回的二维码ID)
     */
    private String qr_id;

    /**
     * 是否验证通过 （1:验证通过，0:验证失败）
     */
    private int status;


    /**
     * 验证时间
     */
    private Long opt_time;


}
