package com.xinbo.fundstransfer.domain.enums;

public enum FeedBackLevel {
	Advice(1, "建议"), General(2, "一般"), Serious(3, "严重");

	private Integer status = null;
	private String msg = null;

	FeedBackLevel(Integer status, String msg) {
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

	public static FeedBackLevel findByStatus(Integer status) {
		if (status == null) {
			return null;
		}
		for (FeedBackLevel feedBackLevel : FeedBackLevel.values()) {
			if (status.equals(feedBackLevel.status)) {
				return feedBackLevel;
			}
		}
		return null;
	}
}
