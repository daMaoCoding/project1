package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindCRByConditionInputDTO extends PageInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 业主oid
	private Byte inType;// 0.wx 1.zfb 2.银行卡，不传或为空表示全部
	private String adminName;// 操作人
	private Float commissionPercentStart;// 佣金比列开始值
	private Float commissionPercentEnd;// 佣金比列结束值
	private Integer commissionMaxStart;// 最高限额开始值
	private Integer commissionMaxEnd;// 最高限额结束值
}
