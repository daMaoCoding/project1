package com.xinbo.fundstransfer.domain.pojo;

import java.io.Serializable;

public class ProblemInfo implements Serializable {
	private static final long serialVersionUID = 7926875020078477841L;
	private Integer id;
	private Integer flag;// 0 pc 1 手机 2 返利网
	private String error;
	private Integer offLine;
	private Integer logTmOut;
	private Integer balTmOut;
	private Integer taskTmOut;
	private Long logTm;
	private Long balTm;
	private Long taskTm;
	private Integer handicap;
	private String handicapName;
	private Integer type;
	private String account;
	private String bankType;
	private String owner;
	private Integer peakBalance;
	private String alias;
	private String mobile;

	public Integer getId() {
		return id;
	}

	public String getError() {
		return error;
	}

	public Integer getOffLine() {
		return offLine;
	}

	public Integer getLogTmOut() {
		return logTmOut;
	}

	public Integer getBalTmOut() {
		return balTmOut;
	}

	public Long getLogTm() {
		return logTm;
	}

	public Long getBalTm() {
		return balTm;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setError(String error) {
		this.error = error;
	}

	public void setLogTmOut(Integer logTmOut) {
		this.logTmOut = logTmOut;
	}

	public void setBalTmOut(Integer balTmOut) {
		this.balTmOut = balTmOut;
	}

	public void setLogTm(Long logTm) {
		this.logTm = logTm;
	}

	public void setBalTm(Long balTm) {
		this.balTm = balTm;
	}

	public Integer getTaskTmOut() {
		return taskTmOut;
	}

	public void setTaskTmOut(Integer taskTmOut) {
		this.taskTmOut = taskTmOut;
	}

	public Long getTaskTm() {
		return taskTm;
	}

	public void setTaskTm(Long taskTm) {
		this.taskTm = taskTm;
	}

	public void setFlag(Integer flag) {
		this.flag = flag;
	}

	public Integer getFlag() {
		return flag;
	}

	public void setHandicap(Integer handicap) {
		this.handicap = handicap;
	}

	public Integer getHandicap() {
		return this.handicap;
	}

	public void setHandicapName(String handicapName) {
		this.handicapName = handicapName;
	}

	public String getHandicapName() {
		return this.handicapName;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public void setOffLine(Integer offLine) {
		this.offLine = offLine;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Integer getPeakBalance() {
		return peakBalance;
	}

	public void setPeakBalance(Integer peakBalance) {
		this.peakBalance = peakBalance;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
