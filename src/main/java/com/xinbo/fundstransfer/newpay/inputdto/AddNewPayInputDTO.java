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
public class AddNewPayInputDTO implements Serializable {
    @NotNull
    private Integer oid;// 盘口编码
    @NotNull
    private String contactName;// 联系人名
    @NotNull
    private String tel;// 联系电话
    private Double credits;// 信用额度 type=0 时候必填
    @NotNull
    private Byte type;// 类型，0：客户，1：自用
    @NotNull
    private Byte level;// 内外层，0：外层，1：中层，2：内层

    private String commissionBankNum;// 返佣账号 type=0时，返佣信息都是必填
    private String commissionOpenMan;// 返佣开户人 type=0时，返佣信息都是必填
    private String commissionBankName;// 返佣开户行 type=0时，返佣信息都是必填
    private String wechatAccount;// 微信账号 如果填了微信账号，那微信所有信息都是必填，支付宝银行卡同理
    private String wechatName;// 微信姓名 如果填了微信账号，那微信所有信息都是必填，支付宝银行卡同理
    private Double wechatInLimit;// 微信入款限额 如果填了微信账号，那微信所有信息都是必填，支付宝银行卡同理
    private Double wechatBalanceAlarm;// 微信余额告警 如果填了微信账号，那微信所有信息都是必填，支付宝银行卡同理
    private String wechatLoginPassword;// 微信登陆密码 如果填了微信账号，那微信所有信息都是必填，支付宝银行卡同理
    private String wechatPaymentPassword;// 微信支付密码 如果填了微信账号，那微信所有信息都是必填，支付宝银行卡同理
    private Byte wechatQrDrawalMethod;// 微信提现方式：二维码提现（0：未使用，1：使用）
    private Byte wechatBankDrawalMethod;
    ;// 微信提现方式：银行卡提现（0：未使用，1：使用）
    private String alipayAccount;// 支付宝账号
    private String alipayName;// 支付宝姓名
    private Double alipayInLimit;// 支付宝入款限额
    private Double alipayBalanceAlarm;// 支付宝余额告警
    private String alipayLoginPassword;// 支付宝登陆密码
    private String alipayPaymentPassword;// 支付宝提现方式：二维码提现（0：未使用，1：使用）
    private Byte alipayQrDrawalMethod;// 支付宝提现方式：银行卡提现（0：未使用，1：使用）
    private Byte alipayBankDrawalMethod;// 支付宝提现方式：银行卡提现（0：未使用，1：使用）
    private Long bankId;// 银行id
    private String bankAccount;// 银行卡账号
    private String openMan;// 开户人
    private String bankName;// 银行名称（类型）
    private String bankOpen;// 开户支行名称
    private Double bankBalanceAlarm;// 银行卡余额告警
    private String bankLoginPassword;// 银行卡登陆密码
    private String bankPaymentPassword;// 银行卡支付密码
    private String bankPassword;// 银行密码
    private String ushieldPassword;// U盾密码
    private Double ylbThreshold;// 余利宝余额告警
    private Integer ylbInterval;// 余利宝余额查询时间间隔
    private String zfbAlias1;// 支付宝别名1
    private String zfbAlias2;// 支付宝别名2
    private String wxAlias1;// 微信别名1
    private String wxAlias2;// 微信别名2
    private String wxAlias3;// 微信别名3
    private String uid;// 支付宝专用UID
    private Byte isEpAlipay;// 否 是否是企业支付宝 0:否 1:是

    private String ysfAccount;//否	云闪付账号
    private String ysfName;//	否	云闪付姓名，ysfAccount不为空时必填
    private Double ysfInLimit;//	否	云闪付入款限额，ysfAccount不为空时必填
    private Double ysfBalanceAlarm;//	否	云闪付余额告警，ysfAccount不为空时必填
    private String ysfLoginpass;//	否	云闪付登录密码
    private String ysfTradepass;//	否	云闪付交易密码
    private Byte ysfQrDrawalMethod;//		否	支付宝提现方式：二维码提现（0：未使用，1：使用），ysfAccount不为空时必填
    private Byte ysfBankDrawalMethod;//	否	支付宝提现方式：银行卡提现（0：未使用，1：使用），ysfAccount不为空时必填

}
