package com.xinbo.fundstransfer.domain.enums;

import java.util.Objects;

public enum OtherAccountOwnType {
	OWN(1, "自有"), PARTIMEOWN(2, "兼职");
	private Integer ownType;
	private String ownTypeDesc;

	OtherAccountOwnType(int ownType, String ownTypeDesc) {
		this.ownType = ownType;
		this.ownTypeDesc = ownTypeDesc;
	}

	public Integer getOwnType() {
		return ownType;
	}

	public void setOwnType(Integer ownType) {
		this.ownType = ownType;
	}

	public String getOwnTypeDesc() {
		return ownTypeDesc;
	}

	public void setOwnTypeDesc(String ownTypeDesc) {
		this.ownTypeDesc = ownTypeDesc;
	}

	public static OtherAccountOwnType getByOwnType(Byte ownType) {
		if (Objects.isNull(ownType))
			return null;
		for (OtherAccountOwnType otherAccountOwnType : OtherAccountOwnType.values()) {
			if (ownType.equals(otherAccountOwnType.getOwnType().byteValue()))
				return otherAccountOwnType;
		}
		return null;
	}
}
