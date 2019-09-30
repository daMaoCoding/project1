package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyAccountInputDTO implements Serializable {
    @NotNull
    private Integer oid;// 盘口编码
    @NotNull
    private Long mobileId;// ownerNewpayConfig/findByCondition返回的id

    private Long accountId;/// ownerNewpayConfig/findByCondition返回的bankAccountId或wechatAccountId或alipayAccountId
    @NotNull
    private Byte type;// 0.wx 1.zfb 2.银行卡 3.云闪付
    @NotNull
    private String account;// 账号
    @NotNull
    private String name;// 姓名/开户人
    private Double inLimit;// 入款限额（wx/zfb必填）
    private Double balanceAlarm;// 余额告警
    private Byte qrDrawalMethod;// 提现方式：二维码提现（0：未使用，1：使用）（wx/zfb必填）
    private Byte bankDrawalMethod;// 提现方式：银行卡提现（0：未使用，1：使用）（wx/zfb必填）
    private Long bankId;// 银行id（银行卡必填）
    private String openMan;// 开户人（银行卡必填）
    private String bankName;// 银行名称（类型）（银行卡必填）
    private String bankOpen;// 开户支行名称（银行卡必填）
    private Double ylbThreshold;// 余利宝余额告警
    private Integer ylbInterval;// 余利宝余额查询时间间隔
    private String zfbAlias1;// 支付宝别名1
    private String zfbAlias2;// 支付宝别名2
    private String wxAlias1;// 微信别名1
    private String wxAlias2;// 微信别名2
    private String wxAlias3;// 微信别名3
    private String uid;// 支付宝专用UID
    private Byte isEpAlipay;// 否 是否是企业支付宝 0:否 1:是

    private Double ysfInLimit;//否	云闪付入款限额,type=3必填
    private Double ysfBalanceAlarm;//	否	云闪付提现限额,type=3必填
    private String ysfLoginpass;//否	云闪付登录密码
    private String ysfTradepass;//否	云闪付交易密码

}
