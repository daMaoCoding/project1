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
public class FindAWLOG3ByConditionInputDTO extends PageInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 0：微信，1：支付宝
	@NotNull
	private Long accountId;// ownerNewpayLog/findByCondition返回的id
	private String statuses;// 0：未匹配，1：已匹配，多选以“,”隔开
	private Double moneyStart;// 检索金额开始值
	private Double moneyEnd;// 检索金额结束值
	@NotNull
	private Long timeStart;// 检索日期开始值
	@NotNull
	private Long timeEnd;// 检索日期结束值
}
