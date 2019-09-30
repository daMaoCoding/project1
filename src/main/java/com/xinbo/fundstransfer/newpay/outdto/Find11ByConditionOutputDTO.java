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
public class Find11ByConditionOutputDTO implements Serializable {
	private long id; // id
	private Integer oid;// 业主oid
	private String uid;// uid
	private String userName; // 会员账号
	private Byte type;// 0：微信，1：支付宝
	private Double money;// 金额
	private String inAccount;//
	private String createtime; // 创建时间
	private String reporttime;// 最新抓取时间
	private String summary;// 摘要
	private String remark;// 备注
	private String tradeCode; // 支付宝/微信/银行订单号
	private String chkRemark;// 收款理由
}
