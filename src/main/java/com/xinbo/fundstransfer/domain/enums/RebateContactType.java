package com.xinbo.fundstransfer.domain.enums;

import java.util.Objects;

public enum RebateContactType {
	Mobile(1,"手机"),WeChat(2,"微信"),QQ(3,"QQ"),Email(4,"Email"),Others(5,"其他");
	private int type;
	private String desc;

	RebateContactType(int type, String desc) {
		this.type = type;
		this.desc = desc;
	}

	public static RebateContactType findByTypeId(Integer typeId) {
		if (Objects.isNull(typeId))
			return null;
		for (RebateContactType type : RebateContactType.values()) {
			if (type.getType() == typeId) {
				return type;
			}
		}
		return null;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}