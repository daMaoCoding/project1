package com.xinbo.fundstransfer.domain.enums;

public enum AccountTraceType {
	MerchantByCard(1, "卡商赔付抵卡"), merchantByTransfer(2, "卡商赔付转账"), fundsCollecTion(3, "资金归集"), fastPayment(4,
			"快捷支付"), posFunds(5, "POS机刷卡"), atmfunds(6, "取款机提现"), other(7, "其它");

	private Integer typeId = null;
	private String msg = null;

	AccountTraceType(Integer typeId, String msg) {
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

	public static AccountTraceType findByTypeId(Integer typeId) {
		if (typeId == null) {
			return null;
		}
		for (AccountTraceType type : AccountTraceType.values()) {
			if (typeId.equals(type.typeId)) {
				return type;
			}
		}
		return null;
	}

}
