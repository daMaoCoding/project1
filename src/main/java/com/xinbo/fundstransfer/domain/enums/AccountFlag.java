package com.xinbo.fundstransfer.domain.enums;

public enum AccountFlag {
	PC(0, "PC"), SPARETIME(1, "兼职"), REFUND(2, "返利网");

	private Integer typeId = null;
	private String msg = null;

	AccountFlag(Integer typeId, String msg) {
		this.typeId = typeId;
		this.msg = msg;
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public static AccountFlag findByTypeId(Integer typeId) {
		if (typeId == null) {
			return null;
		}
		for (AccountFlag type : AccountFlag.values()) {
			if (typeId.equals(type.typeId)) {
				return type;
			}
		}
		return null;
	}
}
