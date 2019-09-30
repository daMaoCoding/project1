package com.xinbo.fundstransfer.domain.enums;

import java.util.Objects;

public enum SysInvstType {

	Matched(0, "匹配"), UnknowIncome(1, "额外收入"), DuplicateOutward(2, "重复出款"), Fee(3,
			"费用"), UnkownOutwardByPartTime(4, "盗刷-兼职所为"), Refund(5, "回冲"), UnkownOutwardByNonePartTime(6,
					"盗刷-非兼职所为"), UnkownOutwardByPC(7, "盗刷-卡商"), DuplicateStatement(-1, "重复流水"), InvalidTransfer(
							-2, "无效订单"), ManualTransOut(8, "人工内部转出"), ManualTransIn(9, "人工内部转入");

	private int type;
	private String message;

	SysInvstType(int type, String message) {
		this.type = type;
		this.message = message;
	}

	public static SysInvstType findByTypeId(Integer typeId) {
		if (Objects.isNull(typeId))
			return null;
		for (SysInvstType invst : SysInvstType.values()) {
			if (invst.getType() == typeId) {
				return invst;
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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
