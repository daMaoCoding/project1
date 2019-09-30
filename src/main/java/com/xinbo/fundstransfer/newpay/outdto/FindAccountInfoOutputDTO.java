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
public class FindAccountInfoOutputDTO implements Serializable {
    private String ownerName;// 业主名称
    private String name; // 微信支付宝姓名 or 银行开户人  or 云闪付
    private String account;// 微信支付宝账号 or 银行卡号   or 云闪付
    private Byte level;// 内外层，0：外层，1：中层，2：内层
    private Byte status; // 0.停用 1.启用
    private Double balance; // 余额
    private Double inLimit; // 入款限额
    private String createtime;// 2018-08-08 11:11:11”, // 创建时间
    private String uptime;// 2018-08-08 11:11:11”, // 最后更新时间
    private Double balanceAlarm; // 余额告警
    private String contactName; // 联系人名
    private String tel; // 联系电话
    private Byte qrDrawalMethod; // 提现方式：二维码提现（0：未使用，1：使用）
    private Byte bankDrawalMethod; // 提现方式：银行卡提现（0：未使用，1：使用）
    private long bankId; // bankName对应的id
    private String bankName; // 银行名称（类型）
    private String bankOpen; // 开户支行名称
    private String remark; // 备注
    private Long accountId;// 微信支付宝账号id or 银行卡号id
    private Double ylbThreshold;// 余利宝余额告警
    private Integer ylbInterval;// 余利宝余额查询时间间隔
    private String zfbAlias1;// 支付宝别名1
    private String zfbAlias2;// 支付宝别名2
    private String wxAlias1;// 微信别名1
    private String wxAlias2;// 微信别名2
    private String wxAlias3;// 微信别名3
    private String uid;// 支付宝专用UID
    private Byte isEpAlipay;// 否 是否是企业支付宝 0:否 1:是

    private Double ysfInLimit; // 云闪付入款限额
    private Double ysfBalanceAlarm;// 云闪付余额告警（提现限额）

}
