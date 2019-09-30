package com.xinbo.fundstransfer.restful.v3.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqV3Withdrawal {
	@NotBlank
	private String uid;
	@NotBlank
	private String toacc;
	@NotBlank
	private String holder;
	@NotBlank
	private String acctype;
	@NotBlank
	private String accinfo;
	private float amount;
	private float balance;
	@NotBlank
	private String tid;
	@NotBlank
	private String token;
	private int type;// 1返利提现、2降额

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getToacc() {
		return toacc;
	}

	public void setToacc(String toacc) {
		this.toacc = toacc;
	}

	public String getHolder() {
		return holder;
	}

	public void setHolder(String holder) {
		this.holder = holder;
	}

	public String getAcctype() {
		return acctype;
	}

	public void setAcctype(String acctype) {
		this.acctype = acctype;
	}

	public String getAccinfo() {
		return accinfo;
	}

	public void setAccinfo(String accinfo) {
		this.accinfo = accinfo;
	}

	public float getAmount() {
		return amount;
	}

	public void setAmount(float amount) {
		this.amount = amount;
	}

	public float getBalance() {
		return balance;
	}

	public void setBalance(float balance) {
		this.balance = balance;
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

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
