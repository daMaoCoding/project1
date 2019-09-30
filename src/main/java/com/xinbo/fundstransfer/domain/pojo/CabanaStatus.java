package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CabanaStatus {
	private Integer id;
	private String account;
	private Long time;
	private Integer status;
	private Integer mode;
	private Long logtime;
	private Float balance;
	private String error;
	/**
	 * 1-simulator, 0-phone
	 */
	int flag;
	/**
	 * 最后一次对账时间
	 */
	Long checkLogTime;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getMode() {
		return mode;
	}

	public void setMode(Integer mode) {
		this.mode = mode;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Long getLogtime() {
		return logtime;
	}

	public void setLogtime(Long logtime) {
		this.logtime = logtime;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public Float getBalance() {
		return balance;
	}

	public void setBalance(Float balance) {
		this.balance = balance;
	}

	public Long getCheckLogTime() {
		return checkLogTime;
	}

	public void setCheckLogTime(Long checkLogTime) {
		this.checkLogTime = checkLogTime;
	}
}
