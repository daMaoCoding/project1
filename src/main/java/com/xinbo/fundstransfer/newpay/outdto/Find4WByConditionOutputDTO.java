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
public class Find4WByConditionOutputDTO implements Serializable {
	private Integer oid; // 业主oid
	private String ownerName; // 业主名称
	private Byte level; // 内外层，0：外层，1：中层，2：内层
	private String drawBankName; // 收款银行名称
	private String drawAccount; // 收款银行卡号
	private String drawBankOpen;// 收款银行开户行
	private String drawBankMan;// 收款银行开户人
	private String payBankName; // 付款银行名称
	private String payAccount; // 付款银行卡号
	private String payBankOpen; // 付款银行开户行
	private String payBankMan; // 付款银行开户人
	private Double money; // 金额
	private Byte type;// 转出类型，0：微信提现到银行卡，1：支付宝提现到银行卡，2：兼职人员银行卡转账到业主收款银行卡
	private String code; // 订单号
	private Byte status;// 状态：0：未匹配，1：已匹配
	private String createtime;// 2018-08-08 11:11:11”, // 创建时间
	private String admintime;// 2018-08-08 11:11:11” // 匹配时间（确认时间）
}
