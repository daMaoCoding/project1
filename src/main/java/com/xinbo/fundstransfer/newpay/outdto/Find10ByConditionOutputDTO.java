package com.xinbo.fundstransfer.newpay.outdto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Find10ByConditionOutputDTO implements Serializable {
	private long inId; // id
	private Integer oid;// 业主oid
	private String uid; // 业主名称
	private String userName; // 会员账号
	private String code; // 订单号
	private Double money;// 金额
	private String createtime; // 创建时间
	private String remark;// 备注
	private String chkRemark;// 收款理由
}
