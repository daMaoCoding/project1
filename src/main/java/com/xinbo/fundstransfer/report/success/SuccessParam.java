package com.xinbo.fundstransfer.report.success;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;

import java.math.BigDecimal;

public class SuccessParam {

	BizBankLog bankLog;
	BizBankLog lastBankLog;
	TransferEntity entity;
	BigDecimal balance;
	BigDecimal fee;
	BigDecimal benchmark;
	BizAccount acc;

	SuccessParam(BizBankLog bankLog, BizBankLog lastBankLog, TransferEntity entity, BigDecimal balance, BigDecimal fee,
			BigDecimal benchmark, BizAccount acc) {
		this.bankLog = bankLog;
		this.lastBankLog = lastBankLog;
		this.entity = entity;
		this.balance = balance;
		this.fee = fee;
		this.benchmark = benchmark;
		this.acc = acc;

	}

	public BizBankLog getBankLog() {
		return bankLog;
	}

	public void setBankLog(BizBankLog bankLog) {
		this.bankLog = bankLog;
	}

	public BizBankLog getLastBankLog() {
		return lastBankLog;
	}

	public void setLastBankLog(BizBankLog lastBankLog) {
		this.lastBankLog = lastBankLog;
	}

	public TransferEntity getEntity() {
		return entity;
	}

	public void setEntity(TransferEntity entity) {
		this.entity = entity;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	public BigDecimal getBenchmark() {
		return benchmark;
	}

	public void setBenchmark(BigDecimal benchmark) {
		this.benchmark = benchmark;
	}

	public BizAccount getAcc() {
		return acc;
	}

	public void setAcc(BizAccount acc) {
		this.acc = acc;
	}
}
