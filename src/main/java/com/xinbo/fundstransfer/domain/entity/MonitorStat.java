package com.xinbo.fundstransfer.domain.entity;

public class MonitorStat {
	private Integer accType;
	private Integer currSysLevel;
	private Float amount = 0F;
	private Integer accNum = 0;
	private Integer zone;
	private Integer totalNum = 0;
	private Float totalBal = 0F;
	private String accTypeAndStatus;

	public Integer getAccType() {
		return accType;
	}

	public Integer getCurrSysLevel() {
		return currSysLevel;
	}

	public Float getAmount() {
		return amount;
	}

	public Integer getAccNum() {
		return accNum;
	}

	public Integer getZone() {
		return zone;
	}

	public void setAccType(Integer accoType) {
		this.accType = accoType;
	}

	public void setCurrSysLevel(Integer currSysLevel) {
		this.currSysLevel = currSysLevel;
	}

	public void setAmount(Float amount) {
		this.amount = amount;
	}

	public void setAccNum(Integer accNum) {
		this.accNum = accNum;
	}

	public void setZone(Integer zone) {
		this.zone = zone;
	}

	public Integer getTotalNum() {
		return totalNum;
	}

	public Float getTotalBal() {
		return totalBal;
	}

	public void setTotalNum(Integer totalNum) {
		this.totalNum = totalNum;
	}

	public void setTotalBal(Float totalBal) {
		this.totalBal = totalBal;
	}
	
	public String getAccTypeAndStatus() {
		return accTypeAndStatus;
	}

	public void setAccTypeAndStatus(String accTypeAndStatus) {
		this.accTypeAndStatus = accTypeAndStatus;
	}
}
