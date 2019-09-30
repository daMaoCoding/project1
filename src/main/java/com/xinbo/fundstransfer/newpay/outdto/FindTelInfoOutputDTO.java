package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindTelInfoOutputDTO implements Serializable {
    private String ownerName;// 业主名称
    private String contactName;// 联系人名
    private String tel; // 联系电话
    private Byte level; // 内外层，0：外层，1：中层，2：内层
    private Double credits; // 信用额度
    private String bankAccount; // 银行卡账号
    private String wechatAccount; // 微信账号
    private String alipayAccount;// 支付宝账号
    private String[] wechatDeviceCol;// [“”], // 微信设备号
    private String[] alipayDeviceCol;// [“”], // 支付宝设备号
    private String[] bankDeviceCol;// 银行卡设备号
    private Byte status;// 0.停用 1.启用
    private Byte type;// 类型，0：客户，1：自用
    private String commissionBankNum; // 返佣账号
    private String commissionOpenMan; // 返佣开户人
    private String commissionBankName; // 返佣开户行
    private String remark; // 备注
    private String prefix;// 收款理由前缀
    private String suffix;// 收款理由后缀
    private Byte chkType;// 收款理由类型，0：前后缀形式，1：形容词-名词形式
    private Byte uoFlag;// 未确认出款金额开关，1-开，0-关，默认0

    private String ysfAccount;// 云闪付账号
    private String[] ysfDeviceCol; // 云闪付设备号

}
