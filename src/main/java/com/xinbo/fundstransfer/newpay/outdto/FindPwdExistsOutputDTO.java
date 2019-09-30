package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindPwdExistsOutputDTO implements Serializable {
    private Byte wechatLoginPwdExists; // 微信登陆密码是否已被设置 0.否 1.是
    private Byte wechatPaymentPwdExists;// 微信支付密码是否已被设置 0.否 1.是
    private Byte alipayLoginPwdExists; // 支付宝登陆密码是否已被设置 0.否 1.是
    private Byte alipayPaymentPwdExists; // 支付宝支付密码是否已被设置 0.否 1.是
    private Byte bankLoginPwdExists;// 银行卡登陆密码是否已被设置 0.否 1.是
    private Byte bankPaymentPwdExists;// 银行卡支付密码是否已被设置 0.否 1.是
    private Byte bankPwdExists;// 银行密码是否已被设置 0.否 1.是
    private Byte uShieldPwdExists;// U盾密码是否已被设置 0.否 1.是

    private Byte ysfLoginPwdExists; // 云闪付登陆密码 0.否 1.是
    private Byte ysfPaymentPwdExists;// 云闪付支付密码 0.否 1.是

}
