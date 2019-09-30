package com.xinbo.fundstransfer.restful.v3.pojo;

import org.hibernate.validator.constraints.NotBlank;

public class ReqV3Level {
	@NotBlank
	private String oid;
	@NotBlank
	private String levelCode;
	@NotBlank
	private String levelName;
	@NotBlank
	private String token;

	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

	public String getLevelCode() {
		return levelCode;
	}

	public void setLevelCode(String levelCode) {
		this.levelCode = levelCode;
	}

	public String getLevelName() {
		return levelName;
	}

	public void setLevelName(String levelName) {
		this.levelName = levelName;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}
