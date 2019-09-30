package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Find6ByConditionOutputDTO implements Serializable {
	private long id; // id
	private Integer oid;// 业主oid
	private String ownerName; // 业主名称
	private Double money;// 金额
	private String inAccount;//
	private String createtime; // 创建时间
	private String reporttime;// 匹配时间（确认时间）
	private String remark;// 备注
	private String tradeCode; // 支付宝/微信/银行订单号
	private String chkRemark;// 收款理由
}
