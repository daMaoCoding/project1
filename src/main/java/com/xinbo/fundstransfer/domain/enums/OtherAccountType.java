package com.xinbo.fundstransfer.domain.enums;

import java.util.Objects;

public enum OtherAccountType {
	YSF(1, "云闪付");
	private Integer type;
	private String typeDesc;

	OtherAccountType(Integer type, String typeDesc) {
		this.type = type;
		this.typeDesc = typeDesc;
	}

	OtherAccountType(Integer type) {
		this.type = type;
	}

	public static OtherAccountType getByType(Byte type) {
		if (Objects.isNull(type))
			return null;
		for (OtherAccountType accountType : OtherAccountType.values()) {
			if (type.equals(accountType.getType().byteValue()))
				return accountType;
		}
		return null;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getTypeDesc() {
		return typeDesc;
	}

	public void setTypeDesc(String typeDesc) {
		this.typeDesc = typeDesc;
	}
}
