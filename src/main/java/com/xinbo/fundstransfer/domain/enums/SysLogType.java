package com.xinbo.fundstransfer.domain.enums;

public enum SysLogType {
	Income(1, "入款"), Transfer(2, "下发"), Outward(3, "出款"), Rebate(4, "返利"), Init(5, "初始化"),

	Interest(-1, "结息"), UnknowIncome(-2, "不明来源"), DupliOutward(-3, "重复出款"), Fee(-4, "费用"), Steal(-5, "盗刷"), Refund(-6,
			"冲正"), InvstError(-7, "人工排查");

	public static SysLogType findByTypeId(Integer typeId) {
		if (typeId == null) {
			return null;
		}
		for (SysLogType type : SysLogType.values()) {
			if (typeId.equals(type.typeId)) {
				return type;
			}
		}
		return null;
	}

	SysLogType(int typeId, String msg) {
		this.typeId = typeId;
		this.msg = msg;

	}

	private int typeId;
	private String msg;

	public int getTypeId() {
		return typeId;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
