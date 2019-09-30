package com.xinbo.fundstransfer.domain.enums;

public enum AccountRebateReturnSummaryStatus {
	Auditing(0, "审核中"), Success(1, "审核通过"), Fail(4, "审核不通过");

	private Integer status = null;
	private String msg = null;

	AccountRebateReturnSummaryStatus(Integer status, String msg) {
		this.status = status;
		this.msg = msg;
	}

	public Integer getStatus() {
		return status;
	}

	public String getMsg() {
		return msg;
	}

}
