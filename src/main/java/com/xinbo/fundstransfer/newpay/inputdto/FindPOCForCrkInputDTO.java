package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

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
public class FindPOCForCrkInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Integer type;// 类型，0：微信，1：支付宝

	private Byte isEpAlipay;// 否 是否是企业支付宝 0:否 1:是

	public Object getMoneyEnd() {
		// TODO Auto-generated method stub
		return null;
	}
}
