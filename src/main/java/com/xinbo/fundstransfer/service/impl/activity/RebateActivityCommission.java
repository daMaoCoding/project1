package com.xinbo.fundstransfer.service.impl.activity;

import java.math.BigDecimal;

public class RebateActivityCommission {
	private Integer accId;
	private String acc;
	private BigDecimal total;
	private BigDecimal amount;
	private BigDecimal balance;
	private BigDecimal creditValue;
	private BigDecimal realTimeBalance;
	private String time;

	private BigDecimal activityAmount;

	private BigDecimal agentAmount;

	private BigDecimal totalAgentAmount;

	public Integer getAccId() {
		return accId;
	}

	public void setAccId(Integer accId) {
		this.accId = accId;
	}

	public String getAcc() {
		return acc;
	}

	public void setAcc(String acc) {
		this.acc = acc;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BigDecimal getCreditValue() {
		return creditValue;
	}

	public void setCreditValue(BigDecimal creditValue) {
		this.creditValue = creditValue;
	}

	public BigDecimal getRealTimeBalance() {
		return realTimeBalance;
	}

	public void setRealTimeBalance(BigDecimal realTimeBalance) {
		this.realTimeBalance = realTimeBalance;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public BigDecimal getActivityAmount() {
		return activityAmount;
	}

	public void setActivityAmount(BigDecimal activityAmount) {
		this.activityAmount = activityAmount;
	}

	public BigDecimal getAgentAmount() {
		return agentAmount;
	}

	public void setAgentAmount(BigDecimal agentAmount) {
		this.agentAmount = agentAmount;
	}

	public BigDecimal getTotalAgentAmount() {
		return totalAgentAmount;
	}

	public void setTotalAgentAmount(BigDecimal totalAgentAmount) {
		this.totalAgentAmount = totalAgentAmount;
	}

	public RebateActivityCommission(Integer accId, String acc, BigDecimal total, BigDecimal amount, BigDecimal balance,
			BigDecimal creditValue, BigDecimal realTimeBalance, String time, BigDecimal activityAmount,
			BigDecimal agentAmount, BigDecimal totalAgentAmount) {
		super();
		this.accId = accId;
		this.acc = acc;
		this.total = total;
		this.amount = amount;
		this.balance = balance;
		this.creditValue = creditValue;
		this.realTimeBalance = realTimeBalance;
		this.time = time;
		this.activityAmount = activityAmount;
		this.agentAmount = agentAmount;
		this.totalAgentAmount = totalAgentAmount;
	}

}
