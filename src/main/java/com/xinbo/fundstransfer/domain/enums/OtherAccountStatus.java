package com.xinbo.fundstransfer.domain.enums;

import java.util.Objects;

public enum OtherAccountStatus {
	NORMAL(1, "在用"), STOP(2, "停用"), Inactivated(3, "未激活");
	private Integer status;
	private String statusDesc;

	OtherAccountStatus(Integer status) {
		this.status = status;
	}

	OtherAccountStatus(Integer status, String statusDesc) {
		this.status = status;
		this.statusDesc = statusDesc;
	}

	public static OtherAccountStatus getByStatus(Byte status) {
		if (Objects.isNull(status))
			return null;
		for (OtherAccountStatus otherAccountStatus : OtherAccountStatus.values()) {
			if (status.equals(otherAccountStatus.getStatus().byteValue()))
				return otherAccountStatus;
		}
		return null;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getStatusDesc() {
		return statusDesc;
	}

	public void setStatusDesc(String statusDesc) {
		this.statusDesc = statusDesc;
	}
}
