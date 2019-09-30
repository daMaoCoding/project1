package com.xinbo.fundstransfer.domain.enums;

public enum AccountTraceStatus {
	CashflowDispose(0, "金流处理"), HandicapDispose(1, "盘口处理"), FinanceDispose(2, "财务处理"), ContinueToFreeze(3,
			"持续冻结"), ThawRecovery(4, "解冻恢复使用"), shiftDelete(5, "永久删除");

	private Integer statusId = null;
	private String msg = null;

	AccountTraceStatus(Integer statusId, String msg) {
		this.statusId = statusId;
		this.msg = msg;
	}

	public Integer getStatusId() {
		return statusId;
	}

	public void setStatusId(Integer statusId) {
		this.statusId = statusId;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

}
