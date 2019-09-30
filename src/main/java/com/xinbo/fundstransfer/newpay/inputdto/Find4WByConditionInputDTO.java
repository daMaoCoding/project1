package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Find4WByConditionInputDTO extends PageInputDTO implements Serializable {
	private Integer oid;// 盘口编码
	@NotNull
	private Byte status;// 状态：0：未匹配，1：已匹配
	private Long admintimeStart;// 确认时间开始值
	private Long admintimeEnd;// 确认时间结束值
	private String drawBankName; // 否 收款银行名称
	private String payBankName; // 付款银行名称
	@NotNull
	private Long timeStart;// 检索日期开始值
	@NotNull
	private Long timeEnd;// 检索日期结束值
	private Double moneyStart;// 检索金额开始值
	private Double moneyEnd;// 检索金额结束值
}
