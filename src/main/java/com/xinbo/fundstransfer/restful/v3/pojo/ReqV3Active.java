package com.xinbo.fundstransfer.restful.v3.pojo;

import org.hibernate.validator.constraints.NotBlank;

public class ReqV3Active {
	@NotBlank
	private String acc;
	private String token;

	public String getAcc() {
		return acc;
	}

	public void setAcc(String acc) {
		this.acc = acc;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
