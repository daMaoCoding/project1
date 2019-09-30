package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountIncome {
	/**
	 * 入款卡账号
	 */
	Integer id;
	/**
	 * 收款时间
	 */
	long incomeTime;
	/**
	 * 收款金额
	 */
	private BigDecimal amount;

	public AccountIncome(Integer id, long incomeTime, BigDecimal amount) {
		this.incomeTime = incomeTime;
		this.amount = amount;
		this.id = id;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	public long getIncomeTime() {
		return incomeTime;
	}

	public void setIncomeTime(long incomeTime) {
		this.incomeTime = incomeTime;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
