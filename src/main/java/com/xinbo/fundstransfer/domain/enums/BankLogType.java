package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by 000 on 2017/6/27.
 */
public enum BankLogType {

	/**
	 * 类型：1-支付宝，2-微信，3-银行卡，4-第三方，5-人工补录
	 */
	ALIPAY(1, "支付宝流水"), WECHAT(2, "微信流水"), BANK(3, "银行流水"), THIRD(4, "第三方流水"),
	/**
	 * 类型：1-支付宝，2-微信，3-银行卡，4-第三方，5-人工补录
	 */
	MANUAL(5, "人工补录");

	private Integer type = null;
	private String msg = null;

	BankLogType(Integer type, String msg) {
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

	public static BankLogType findByType(Integer type) {
		if (type == null) {
			return null;
		}
		for (BankLogType bankLogType : BankLogType.values()) {
			if (type.equals(bankLogType.type)) {
				return bankLogType;
			}
		}
		return null;
	}
}
