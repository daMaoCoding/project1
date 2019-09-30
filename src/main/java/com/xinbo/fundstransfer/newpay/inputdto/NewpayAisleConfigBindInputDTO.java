package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Anastasia on 2018/8/14.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NewpayAisleConfigBindInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Long mobileId;// 手机号ID
	@NotNull
	private Byte type;// 类型，0：微信，1：支付宝
	@NotNull
	private List<Long> ocIdCol;// 通道id数组
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号
}
