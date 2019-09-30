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
public class Find5ByConditionOutputDTO implements Serializable {
	private long id; // id
	private Integer oid;// 业主oid
	private String ownerName; // 业主名称
	private String level; // 会员层级
	private String userName; // 会员账号
	private String code; // 订单号
	private Double money;// 金额
	private String inAccount;//
	private String createtime; // 创建时间
	private String admintime;// 匹配时间（确认时间）
	private String remark;// 备注
	private String chkRemark;// 收款理由
}
