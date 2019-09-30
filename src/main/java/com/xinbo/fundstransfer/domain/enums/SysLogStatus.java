package com.xinbo.fundstransfer.domain.enums;

public enum SysLogStatus {
	Invalid(-1, "无效"), Valid(1, "完成"), NoOwner(20, "未确认");

	public static SysLogStatus findByStatusId(Integer statusId) {
		if (statusId == null) {
			return null;
		}
		for (SysLogStatus status : SysLogStatus.values()) {
			if (statusId.equals(status.statusId)) {
				return status;
			}
		}
		return null;
	}

	SysLogStatus() {
	}

	private SysLogStatus(int statusId, String msg) {
		this.statusId = statusId;
		this.msg = msg;
	}

	private int statusId;
	private String msg;

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public int getStatusId() {
		return statusId;
	}

	public void setStatusId(int statusId) {
		this.statusId = statusId;
	}
}
