package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindByConditionOutputDTO implements Serializable {
    private Long id; // id
    private Integer oid; // 业主oid
    private String ownerName; // 业主名称
    private String contactName; // 联系人名
    private String tel; // 联系电话
    private Byte status;// 0.停用 1.启用
    private Byte type;// 0.客户 1.自用
    private Long bankAccountId; // bankAccount对应id
    private String bankAccount; // 银行卡账号
    private String bankDevice;// 设备号
    private Byte bankStatus;// 银行设备状态 0 ： 可用 1：繁忙 2：离线
    private Long wechatAccountId; // wechatAccount对应id
    private String wechatAccount; // 微信账号
    private String wechatDevice;// 设备号
    private Byte wechatStatus;// 微信设备状态 0 ： 可用 1：繁忙 2：离线
    private Long alipayAccountId; // alipayAccount对应id
    private String alipayAccount; // 支付宝账号
    private String alipayDevice;// 设备号
    private Byte alipayStatus;// 支付宝设备状态 0 ： 可用 1：繁忙 2：离线
    private Long credits; // 信用额度
    private Double balance;// 总余额
    private Double todayInCount;// 今日收款(已匹配上的)
    private Double commission;// 累计已获得的确认佣金
    private Double sysBalance;// 设备余额
    private String prefix;// 收款理由前缀
    private String suffix;// 收款理由后缀
    private Byte wapStatus;// 支付宝wap工具设备状态 0 ： 可用 1：繁忙 2：离线
    private Double todayOutCount;// 今日出款
    private Double ylbBalance;// 余利宝余额

    private Double availableCredits; // 可用额度
    private Double zfbBalance; // 支付宝余额 - 设备余额
    private Double bankBalance; // 银行卡余额 - 设备余额
    private Double wxBalance;// 微信余额 - 设备余额
    private Double sysZfbBalance; // 支付宝余额 - 系统余额
    private Double sysYlbBalance; // 余利宝余额 - 系统余额
    private Double sysBankBalance; // 银行卡余额 - 系统余额
    private Double sysWxBalance; // 微信余额 - 系统余额

    private Double unconfirmOutmoney;// 待确认信用额度

    private Byte zfbStatus;// 支付宝账号 0.停用 1.启用
    private Byte wxStatus;// 微信账号 0.停用 1.启用
    private Byte bkStatus;// 银行卡 0.停用 1.启用
    private Byte uoFlag;// 未确认出款金额开关，1-开，0-关，默认0

    private Byte mcmStatus;// 支付宝是否有成功被点击过 0.否 1.是
    private Byte wechatMcmStatus;// 微信是否有成功被点击过 0.否 1.是
    private Byte isEpAlipay;// 否 是否是企业支付宝 0:否 1:是

    private String ysfDevice; // 云闪付设备号
    private String ysfAccount;// 云闪付账号
    private Long ysfAccountId; // 云闪付账号ID
    private Double ysfBalance;// 云闪付余额
    private Double sysYsfBalance;// 云闪付系统余额
    private Byte ysfStatus; // 云闪付 0.停用 1.启用
}
