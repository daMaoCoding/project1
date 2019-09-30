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
public class Find8ByConditionOutputDTO implements Serializable {
	private String inAccount; // 微信、支付宝账号
	private Double money;// 金额
	private String createtime;// 2018-01-01 11:11:11”, // 交易时间
	private String reporttime;// “2018-01-01 11:11:11”, // 上报时间
	private String summary; // 摘要
	private String remark; // 备注
	private String chkRemark; // 收款理由
	private String tradeCode; // 支付宝/微信/银行订单号
}
