package com.xinbo.fundstransfer.newpay.outdto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Find2ByConditionOutputDTO implements Serializable {
	private Integer oid; // 业主oid
	private String ownerName; // 业主名称
	private Byte level; // 0：外层，1：中层，2：内层
	private String account;// 收款账号
	private Double money; // 金额
	private String code;// 订单号
	private Byte status; // 1：已匹配，2：未认领（无法匹配），3：已取消（已驳回），4：正在匹配
	private String createtime; // 创建时间
	private String admintime; // 匹配时间（确认时间）
}
