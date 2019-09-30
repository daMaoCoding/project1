package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindBankCardByConditionInputDTO extends PageInputDTO implements Serializable {
	@NotNull
	private Long timeStart;// 是 检索日期开始值
	@NotNull
	private Long timeEnd;// 是 检索日期结束值
	@NotNull
	private Integer oid;// 否 盘口编码
	// private String oids;// 否 盘口编码
	private String statuses;// 否 0.停用 1.启用，多选以“,”隔开
	private String levels;// 否 0：外层，1：中层，2：内层，多选以“,”隔开
	private String configTypes;// 否 0：客户，1：自用，多选以“,”隔开
	private String account;// 否 银行卡号
	private String ip;
}
