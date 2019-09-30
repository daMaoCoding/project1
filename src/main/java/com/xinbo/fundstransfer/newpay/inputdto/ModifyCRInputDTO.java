package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyCRInputDTO implements Serializable {
	@NotNull
	private long id;
	@NotNull
	private Integer oid;// 业主oid
	@NotNull
	private Byte inType;// 0.wx 1.zfb 2.银行卡
	@NotNull
	private Double startMoney;// 该兼职当天所收金额起始值
	@NotNull
	private Double endMoney;// 该兼职当天所收金额结束值
	@NotNull
	private Float commissionPercent;// 佣金比列
	@NotNull
	private Float commissionMax;// 最高限额
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号
}
