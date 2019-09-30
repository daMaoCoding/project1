package com.xinbo.fundstransfer.accountfee.pojo;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class AccountFee4PlatReqBase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5251877143678930626L;

	@NotNull
	private String handicap;
	
	@NotNull
	private String bankType;
	
	@NotNull
	private String account;
	
	public String getHandicap() {
		return handicap;
	}

	public void setHandicap(String handicap) {
		this.handicap = handicap;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

}
