package com.xinbo.fundstransfer.newpay.outdto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Created by Administrator on 2018/7/12.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindAWIn2ByConditionOutputDTO implements Serializable {
	private Integer oid; // 业主oid
	private String ownerName; // 业主名称
	private String levelCode; // 层级编码
	private String userName; // 会员名
	private String inAccount;// 收款账号
	private Long accountId;// 微信支付宝账号id or 银行卡号id
	private Double money; // 金额
	private Byte type;// 类型，0：客户，1：自用
	private String code;// 订单号
	private Byte status; // 1：已匹配，2：未认领（无法匹配），3：已取消（已驳回），4：正在匹配
	private String createtime; // 创建时间
	private String admintime; // 匹配时间（确认时间）
	private String remark; // 备注
	private String levelName;// 层级名称
}
