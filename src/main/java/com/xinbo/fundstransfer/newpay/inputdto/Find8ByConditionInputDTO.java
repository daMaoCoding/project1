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
public class Find8ByConditionInputDTO extends PageInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 0.wx 1.zfb
	@NotNull
	private Long mobileId;// /ownerNewpayConfig/findByCondition返回的id

	private Double moneyStart;// 否 检索金额开始值

	private Double moneyEnd;// 否 检索金额结束值
	@NotNull
	private Long timeStart;// 是 检索日期开始值
	@NotNull
	private Long timeEnd;// 是 检索日期结束值
	@NotNull
	private Byte inoutType;// 0.入款 1.出款
}
