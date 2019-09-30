package com.xinbo.fundstransfer.domain.enums;

public enum FeedBackStatus {
	Untreated(1, "未处理"), Processing(2, "处理中"), Treated(3, "已处理");

	private Integer status = null;
	private String msg = null;

	FeedBackStatus(Integer status, String msg) {
		this.status = status;
		this.msg = msg;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public static FeedBackStatus findByStatus(Integer status) {
		if (status == null) {
			return null;
		}
		for (FeedBackStatus feedBackStatus : FeedBackStatus.values()) {
			if (status.equals(feedBackStatus.status)) {
				return feedBackStatus;
			}
		}
		return null;
	}
}
