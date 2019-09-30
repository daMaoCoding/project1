package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/12.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindAWLOG3ByConditionOutputDTO implements Serializable {
	private Byte inoutType; // 0.入款 1.出款
	private String inoutTypeDesc;
	private String inAccount; // 收款账号
	private Double money; // 金额
	private Byte status; // 0：未匹配，1：已匹配
	private String createtime; // 创建时间
	private String admintime; // 匹配时间（确认时间）
	private String summary; // 摘要
	private String remark; // 备注
	private String chkRemark;// 收款理由
	private String tradeCode; // 支付宝/微信/银行订单号
	private String reporttime;// 抓取时间

	public void setInoutType(Byte inoutType) {
		String inoutTypeDesc = "";
		if (inoutType != null) {
			switch (inoutType) {
			case 0:
				inoutTypeDesc = "入款";
				break;
			case 1:
				inoutTypeDesc = "出款";
				break;
			default:
				inoutTypeDesc = "";
				break;
			}
		}
		this.inoutTypeDesc = inoutTypeDesc;
		this.inoutType = inoutType;
	}
}
