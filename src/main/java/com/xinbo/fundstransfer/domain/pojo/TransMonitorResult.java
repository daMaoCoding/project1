package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("unused")
public class TransMonitorResult<T> {

	private int accId;
	private String acc;
	private String owner;
	private String alias;
	private String bankType;

	private boolean ret;
	private Long riskMillis;
	private BigDecimal lastRealBal;
	private BigDecimal thisRealBal;
	private List<T> retList;

	public TransMonitorResult() {
	}

	public TransMonitorResult(AccountBaseInfo acc, boolean ret, Long riskMillis, BigDecimal lastRealBal,
			BigDecimal thisRealBal, List<T> retList) {
		this.accId = acc.getId();
		this.acc = acc.getAccount();
		this.owner = acc.getOwner();
		this.alias = acc.getAlias();
		this.bankType = acc.getBankType();
		this.ret = ret;
		this.riskMillis = riskMillis;
		this.lastRealBal = lastRealBal;
		this.thisRealBal = thisRealBal;
		this.retList = retList;
	}

	public int getAccId() {
		return accId;
	}

	public void setAccId(int accId) {
		this.accId = accId;
	}

	public String getAcc() {
		return acc;
	}

	public void setAcc(String acc) {
		this.acc = acc;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public boolean isRet() {
		return ret;
	}

	public void setRet(boolean ret) {
		this.ret = ret;
	}

	public Long getRiskMillis() {
		return riskMillis;
	}

	public void setRiskMillis(Long riskMillis) {
		this.riskMillis = riskMillis;
	}

	public BigDecimal getLastRealBal() {
		return lastRealBal;
	}

	public void setLastRealBal(BigDecimal lastRealBal) {
		this.lastRealBal = lastRealBal;
	}

	public BigDecimal getThisRealBal() {
		return thisRealBal;
	}

	public void setThisRealBal(BigDecimal thisRealBal) {
		this.thisRealBal = thisRealBal;
	}

	public List<T> getRetList() {
		return retList;
	}

	public void setRetList(List<T> retList) {
		this.retList = retList;
	}
}
