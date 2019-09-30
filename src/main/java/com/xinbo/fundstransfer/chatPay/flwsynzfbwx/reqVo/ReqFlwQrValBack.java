package com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo;

import lombok.Data;

/**
 * ************************
 * 出入款调返利网，异步通知二维码验证结果
 * @author tony
 */
@Data
public class ReqFlwQrValBack {


    /**
     * 参数签名  md5(qrId+status+密钥)
     */
    private String token;


    /**
     * 二维码ID(验证二维码有效性返回的二维码ID)
     */
    private String qrId;


    /**
     * 是否验证通过 （1:验证通过，0:验证失败）
     */
    private int status;


}
