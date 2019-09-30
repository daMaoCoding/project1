package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by 007 on 2018/7/11.
 */
public enum AccountRateType {
	Fixed(0, "固定费率"), Ladder(1, "阶梯式");

	private Integer typeId = null;
	private String msg = null;

	AccountRateType(Integer typeId, String msg) {
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

}
