package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyPWDInputDTO implements Serializable {
    @NotNull
    private Integer oid;// 盘口编码
    @NotNull
    private Long id;// id
    private String wechatLoginPassword;// 微信登陆密码
    private String wechatPaymentPassword;// 微信支付密码
    private String alipayLoginPassword;// 支付宝登陆密码
    private String alipayPaymentPassword;// 支付宝支付密码
    private String bankLoginPassword;// 银行卡登陆密码
    private String bankPaymentPassword;// 银行卡支付密码
    private String bankPassword;// 银行密码
    private String ushieldPassword;// U盾密码

    private String ysfLoginPassword;   //  云闪付登陆密码
    private String ysfPaymentPassword;// 	云闪付支付密码

}
