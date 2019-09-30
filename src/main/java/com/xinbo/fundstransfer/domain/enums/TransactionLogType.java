package com.xinbo.fundstransfer.domain.enums;

/**
 * 系统流水类型（出款/入款）：0-出款,其它值参考入款请求类型（1-100预留给平台，101-200为系统中转类型）
 * 【注意】此类型是包含关系，包含入款请求的类型
 */
public enum TransactionLogType {

	/** 类型：0-出款 */
	OUTWARD(0, "出款"),
	/** 类型：1-平账 */
	FLAT(999, "平账");

	private Integer type = null;
	private String msg = null;

	TransactionLogType(Integer type, String msg) {
		this.type = type;
		this.msg = msg;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public static TransactionLogType findByType(Integer type) {
		if (type == null) {
			return null;
		}
		for (TransactionLogType bankLogType : TransactionLogType.values()) {
			if (type.equals(bankLogType.type)) {
				return bankLogType;
			}
		}
		return null;
	}
}
