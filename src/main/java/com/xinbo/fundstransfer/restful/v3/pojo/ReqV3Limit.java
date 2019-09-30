package com.xinbo.fundstransfer.restful.v3.pojo;

import org.hibernate.validator.constraints.NotBlank;

public class ReqV3Limit {
	private float amount;
	@NotBlank
	private String acc;
	@NotBlank
	private String tid;
	private String token;

	public float getAmount() {
		return amount;
	}

	public void setAmount(float amount) {
		this.amount = amount;
	}

	public String getAcc() {
		return acc;
	}

	public void setAcc(String acc) {
		this.acc = acc;
	}

	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
