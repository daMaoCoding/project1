package com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * ************************
 * 出入款同步兼职微信/支付宝账号  参数信息
 * @author tony
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqRebateUserWxZfbAccount {

    /**
     * token : 123123
     * uid : abcabc
     * uname : zhangsan
     * qrType : 1
     * account : zhangsan@163.com
     * name : 张三
     * qrContent : HTTPS://QR.ALIPAY.COM/FKX06574IICWHYVHKT5WAF?t=1567417015551
     * timestamp : 1567417165990
     */

    /**
     * 参数签名
     */
    @NotBlank(message = "token不能为空")
    @Size(min = 4, message = "token错误")
    private String token;


    /**
     * 兼职UID
     */
    @NotNull(message = "UID不能为空")
    private String uid;


    /**
     * 兼职账号
     */
    @NotNull(message = "账号不能为空")
    private String uname;


    /**
     * 类型(0微信收款码，1支付宝收款码)
     */
    @NotNull(message = "二维码类型不能为空")
    private int qrType;


    /**
     * 	支付宝或微信账号
     */
    @NotNull(message = "账号不能为空")
    private String account;

    /**
     * 	支付宝或微信-真实姓名
     */
    @NotNull(message = "真实姓名不能为空")
    private String name;

    /**
     * 二维码内容(解析后的内容)
     */
    @NotNull(message = "二维码不能为空")
    private String qrContent;

    /**
     * 返利网时间戳
     */
    @NotNull(message = "时间戳不能为空")
    private long timestamp;


    /**
     * 交易密码
     */
    @NotNull(message = "交易密码不能空")
    private String tpwd;


}
