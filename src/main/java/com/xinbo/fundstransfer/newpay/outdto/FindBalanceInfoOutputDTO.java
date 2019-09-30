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
public class FindBalanceInfoOutputDTO implements Serializable {
	private Byte type; // 0：微信，1：支付宝，2：银行卡
	private String account; // 微信支付宝账号 or 银行卡号
	private Double balance; // 余额
	private Double inMoney; // 当日转入
	private Double outMoney; // 当日转出
}
