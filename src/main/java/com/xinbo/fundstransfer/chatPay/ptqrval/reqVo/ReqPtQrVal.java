package com.xinbo.fundstransfer.chatPay.ptqrval.reqVo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * ************************
 * 平台请求验证二维码
 * @author tony
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqPtQrVal {


    /**
     * token : abcd
     * oid : 66
     * uid : 20000
     * uname : ptlisi
     * qrType : 1
     * account : lisi@163.com
     * name : 李四
     * qrContent : HTTPS://QR.ALIPAY.COM/FKX06574IICWHYVHKT5WAF?t=1567417015551
     */


    /**
     * 盘口
     */
    @NotNull(message = "Oid不能空。")
    private int oid;

    /**
     * 会员UID
     */
    @NotNull(message = "Uid不能空")
    private String uid;

    /**
     * 会员账号
     */
    @NotNull(message = "uname不能空")
    private String uname;

    /**
     * 	类型(0微信收款码，1支付宝收款码)
     */
    @NotNull(message = "qrType不能空")
    private int qrType;

    /**
     * 	支付宝或微信账号
     */
    @NotNull(message = "账号不能空")
    private String account;

    /**
     * 	支付宝或微信-真实姓名
     */
    @NotNull(message = "真实姓名不能空")
    private String name;

    /**
     * 	二维码内容(解析后的内容)
     */
    @NotNull(message = "二维码不能空")
    private String qrContent;

}
