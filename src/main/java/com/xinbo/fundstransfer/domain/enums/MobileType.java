package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by 000 on 2017/6/27.
 */
public enum MobileType {
	Customer(1, "客户"), Self(2, "自用");

	private Integer typeId = null;
	private String msg = null;

	MobileType(Integer typeId, String msg) {
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

	public static MobileType findByTypeId(Integer typeId) {
		if (typeId == null) {
			return null;
		}
		for (MobileType type : MobileType.values()) {
			if (typeId.equals(type.typeId)) {
				return type;
			}
		}
		return null;
	}


}
