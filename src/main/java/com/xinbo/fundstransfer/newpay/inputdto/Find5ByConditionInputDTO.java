package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Find5ByConditionInputDTO extends PageInputDTO implements Serializable {
	// private String oids;// 盘口编码
	@NotNull
	private Integer oid;// 否 盘口编码
	@NotNull
	private Byte type;// 0：微信，1：支付宝
	@NotNull
	private Byte status;// 0：未匹配，1：已匹配
	private String userName;// 会员名
	private String code;// 订单号
	private String inAccount;// 微信、支付宝账号
	@NotNull
	private Long timeStart;// 检索日期开始值
	@NotNull
	private Long timeEnd;// 检索日期结束值
	private Double moneyStart;// 检索金额开始值
	private Double moneyEnd;// 检索金额结束值
	private String chkRemark;// 收款理由
	private String[] deviceCol;// device数组
}
