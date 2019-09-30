package com.xinbo.fundstransfer.report.up;

public class ReportInitParam {
	private int accId;
	private Integer operator;
	private String remark;

	public ReportInitParam() {
	}

	public ReportInitParam(int accId, Integer operator, String remark) {
		this.accId = accId;
		this.operator = operator;
		this.remark = remark;
	}

	public int getAccId() {
		return accId;
	}

	public void setAccId(int accId) {
		this.accId = accId;
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
