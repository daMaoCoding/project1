package com.xinbo.fundstransfer.report.up;

import java.math.BigDecimal;

public class ReportInvstError {
	private int accId;
	private Integer operator;
	private Long errorId;
	private String remark;
	private BigDecimal diff;
	private BigDecimal sysBal;
	private BigDecimal bankBal;

	public ReportInvstError() {
	}

	public ReportInvstError(int accId, Integer operator, Long errorId, String remark, BigDecimal diff,
			BigDecimal sysBal, BigDecimal bankBal) {
		this.accId = accId;
		this.operator = operator;
		this.errorId = errorId;
		this.remark = remark;
		this.diff = diff;
		this.sysBal = sysBal;
		this.bankBal = bankBal;
	}

	public int getAccId() {
		return accId;
	}

	public Integer getOperator() {
		return operator;
	}

	public Long getErrorId() {
		return errorId;
	}

	public String getRemark() {
		return remark;
	}

	public BigDecimal getDiff() {
		return diff;
	}

	public BigDecimal getSysBal() {
		return sysBal;
	}

	public BigDecimal getBankBal() {
		return bankBal;
	}
}
