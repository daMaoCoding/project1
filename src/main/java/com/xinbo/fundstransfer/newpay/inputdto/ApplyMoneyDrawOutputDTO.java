package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/23.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ApplyMoneyDrawOutputDTO implements Serializable {
	private Integer toAccountId;// 收款账号id
	private String account;// 收款账号
	/** 开户人 */
	private String owner;// 开户人
	/** 银行类别 */
	private String bankType;// 银行类别
	/** 开户地址 */
	private String bankAddr;// 开户地址
	private Long acquireTime;// 时间
	private Float amount;// 金额
}
