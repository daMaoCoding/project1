package com.xinbo.fundstransfer.accountfee.pojo;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class AccountFee4PlatOperationReq extends AccountFee4PlatReqBase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5251877143678930626L;

	@NotNull
	private String adminName;

	public String getAdminName() {
		return adminName;
	}

	public void setAdminName(String adminName) {
		this.adminName = adminName;
	}
	
}
