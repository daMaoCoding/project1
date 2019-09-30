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
public class FindBankOutputDTO implements Serializable {
	private long id;
	private String oid;// 业主oid 如99
	private String bankAccount; // 银行卡账号
	private String openMan; // 开户人
	private String bankOpen; // 开户支行名称
	private Double todayInCount; // 今日收款(已匹配上的)
	private Double bankBalance;// 银行余额
}
