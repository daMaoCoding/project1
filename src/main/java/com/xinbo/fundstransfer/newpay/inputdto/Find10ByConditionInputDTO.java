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
public class Find10ByConditionInputDTO extends PageInputDTO implements Serializable {
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 0：微信，1：支付宝
	private String userName;// 会员名
	private String code;// 订单号
	private String inAccount;// 微信、支付宝账号
	@NotNull
	private Long timeStart;// 检索日期开始值
	@NotNull
	private Long timeEnd;// 检索日期结束值
	private Double moneyStart;// 检索金额开始值
	private Double moneyEnd;// 检索金额结束值
	@NotNull
	private String device;// 设备号
}
