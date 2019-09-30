package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/12.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindAWB3OutByConditionInputDTO extends PageInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 0：微信，1：支付宝，2：银行卡
	@NotNull
	private Long accountId;// /ownerNewpayLog/findByCondition返回的id
	@NotNull
	private Byte inoutType;// 0.入款 1.出款
	@NotNull
	private Byte status;// 1：已匹配，2：未认领（无法匹配），3：已取消（已驳回），4：正在匹配

	private String inAccount;// 收款账号
	private String code;// 订单号
	private Byte level;// 0：外层，1：中层，2：内层
	private String userName;// 会员名
	private Double moneyStart;// 检索金额开始值
	private Double moneyEnd;// 检索金额结束值
	@NotNull
	private Long timeStart;// 检索日期开始值
	@NotNull
	private Long timeEnd;// 检索日期结束值
}
