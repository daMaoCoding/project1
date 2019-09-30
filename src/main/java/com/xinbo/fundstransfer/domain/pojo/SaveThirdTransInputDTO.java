package com.xinbo.fundstransfer.domain.pojo;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SaveThirdTransInputDTO implements Serializable {
	/**
	 * {@link com.xinbo.fundstransfer.domain.enums.IncomeRequestType}
	 */
	/*** 入款请求类型 103 */
	@NotNull
	private Integer type;
	/*** 提现金额 **/
	@NotNull
	private BigDecimal[] amountToArray;
	/*** 提现目标账号 **/
	@NotNull
	private String[] toAccountToArray;
	/*** 提现源账号 **/
	@NotNull
	private String fromAccount;
	/** 操作人id */
	@NotNull
	private String operator;
	/*** 手续费 */
	@NotNull
	private String[] feeToArray;
	/*** 提现源账号id */
	@NotNull
	private Integer fromId;
	/*** 提现目标账号id */
	@NotNull
	private Integer[] toIdArray;
	/*** 订单号 */
	@NotNull
	private String[] orderNoArray;
	/**
	 * 标识 下发任务 1 其他地方下发 2
	 */
	private Byte drawTask;
}
