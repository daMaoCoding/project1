package com.xinbo.fundstransfer.restful.v3.pojo;

import org.hibernate.validator.constraints.NotBlank;

public class ReqV3CancelLimit {
	private float amount;
	@NotBlank
	private String tid;
	private int type;
	private String token;

	public float getAmount() {
		return amount;
	}

	public void setAmount(float amount) {
		this.amount = amount;
	}

	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}
