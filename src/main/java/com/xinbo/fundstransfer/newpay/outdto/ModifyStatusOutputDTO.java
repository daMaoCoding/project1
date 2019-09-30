package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/12.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyStatusOutputDTO implements Serializable {
    private long id;
    private Integer oid;// 业主oid
    private String ownerName;// 业主名称
    private String contactName; // 联系人名
    private String tel; // 联系电话
    private Byte status; // 0.停用 1.启用
    private Byte type;// 0.客户 1.自用
    private String bankAccount; // 银行卡账号
    private String wechatAccount; // 微信账号
    private String alipayAccount;// 支付宝账号
    private Double credits; // 信用额度
    private Double balance; // 总余额
    private Double todayInCount; // 今日收款(已匹配上的)
    private Double commission; // 累计已获得的确认佣金
    private Double sysBalance;// 设备余额
    private Double todayOutCount;// 今日出款
    private Double ylbBalance;// 余利宝余额

    private String ysfDevice; // 云闪付设备号
    private String ysfAccount;// 云闪付账号
    private Long ysfAccountId; // 云闪付账号ID
    private Double ysfBalance;// 云闪付余额
    private Double sysYsfBalance;// 云闪付系统余额
    private Byte ysfStatus; // 云闪付 0.停用 1.启用
}
