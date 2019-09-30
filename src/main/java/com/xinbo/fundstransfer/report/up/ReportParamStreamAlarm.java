package com.xinbo.fundstransfer.report.up;

import java.math.BigDecimal;

/**
 * 小铃铛处理
 */
public class ReportParamStreamAlarm {
	private int targetId;
	private long bankLogId;
	private int bankLogStatus;
	private BigDecimal bankLogAmount;
	private Integer operator;
	private String remark;

	public ReportParamStreamAlarm() {
	}

	public ReportParamStreamAlarm(int targetId, long bankLogId, BigDecimal bankLogAmount, int bankLogStatus,
			Integer operator, String remark) {
		this.targetId = targetId;
		this.bankLogId = bankLogId;
		this.bankLogAmount = bankLogAmount;
		this.bankLogStatus = bankLogStatus;
		this.operator = operator;
		this.remark = remark;
	}

	public int getTargetId() {
		return targetId;
	}

	public void setTargetId(int targetId) {
		this.targetId = targetId;
	}

	public long getBankLogId() {
		return bankLogId;
	}

	public void setBankLogId(long bankLogId) {
		this.bankLogId = bankLogId;
	}

	public BigDecimal getBankLogAmount() {
		return bankLogAmount;
	}

	public void setBankLogAmount(BigDecimal bankLogAmount) {
		this.bankLogAmount = bankLogAmount;
	}

	public int getBankLogStatus() {
		return bankLogStatus;
	}

	public void setBankLogStatus(int bankLogStatus) {
		this.bankLogStatus = bankLogStatus;
	}

	public Integer getOperator() {
		return operator;
	}

	public void setOperator(Integer operator) {
		this.operator = operator;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}
