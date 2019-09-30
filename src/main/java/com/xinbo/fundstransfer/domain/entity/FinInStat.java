package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinInStat implements java.io.Serializable {

	private String handicapname;
	private String levelname;
	private String account;
	private BigDecimal amount;
	private BigDecimal fee;
	private BigDecimal bankBalance;
	private int counts;
	private int id;
	private String alias;
	private String owner;
	private String banktype;
	private int status;
	private String time;

	private BigDecimal bankUse;
	private BigDecimal bankStop;
	private BigDecimal bankCanUse;
	private BigDecimal sysUse;
	private BigDecimal sysStop;
	private BigDecimal sysCanUse;

	/**
	 * @return the handicapname
	 */
	public String getHandicapname() {
		return handicapname;
	}

	/**
	 * @param handicapname
	 *            the handicapname to set
	 */
	public void setHandicapname(String handicapname) {
		this.handicapname = handicapname;
	}

	/**
	 * @return the levelname
	 */
	public String getLevelname() {
		return levelname;
	}

	/**
	 * @param levelname
	 *            the levelname to set
	 */
	public void setLevelname(String levelname) {
		this.levelname = levelname;
	}

	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * @param account
	 *            the account to set
	 */
	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * @return the amount
	 */
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * @param amount
	 *            the amount to set
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	/**
	 * @return the fee
	 */
	public BigDecimal getFee() {
		return fee;
	}

	/**
	 * @param fee
	 *            the fee to set
	 */
	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	/**
	 * @return the counts
	 */
	public int getCounts() {
		return counts;
	}

	/**
	 * @param counts
	 *            the counts to set
	 */
	public void setCounts(int counts) {
		this.counts = counts;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the bankBalance
	 */
	public BigDecimal getBankBalance() {
		return bankBalance;
	}

	/**
	 * @param bankBalance
	 *            the bankBalance to set
	 */
	public void setBankBalance(BigDecimal bankBalance) {
		this.bankBalance = bankBalance;
	}

	/**
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @param alias
	 *            the alias to set
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the banktype
	 */
	public String getBanktype() {
		return banktype;
	}

	/**
	 * @param banktype
	 *            the banktype to set
	 */
	public void setBanktype(String banktype) {
		this.banktype = banktype;
	}

	public FinInStat() {
		super();
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public BigDecimal getBankUse() {
		return bankUse;
	}

	public void setBankUse(BigDecimal bankUse) {
		this.bankUse = bankUse;
	}

	public BigDecimal getBankStop() {
		return bankStop;
	}

	public void setBankStop(BigDecimal bankStop) {
		this.bankStop = bankStop;
	}

	public BigDecimal getBankCanUse() {
		return bankCanUse;
	}

	public void setBankCanUse(BigDecimal bankCanUse) {
		this.bankCanUse = bankCanUse;
	}

	public BigDecimal getSysUse() {
		return sysUse;
	}

	public void setSysUse(BigDecimal sysUse) {
		this.sysUse = sysUse;
	}

	public BigDecimal getSysStop() {
		return sysStop;
	}

	public void setSysStop(BigDecimal sysStop) {
		this.sysStop = sysStop;
	}

	public BigDecimal getSysCanUse() {
		return sysCanUse;
	}

	public void setSysCanUse(BigDecimal sysCanUse) {
		this.sysCanUse = sysCanUse;
	}

	public FinInStat(String handicapname, String levelname, String account, BigDecimal amount, BigDecimal fee,
			BigDecimal bankBalance, int counts, int id, String alias, String owner, String banktype, int status,
			String time, BigDecimal bankUse, BigDecimal bankStop, BigDecimal bankCanUse, BigDecimal sysUse,
			BigDecimal sysStop, BigDecimal sysCanUse) {
		super();
		this.handicapname = handicapname;
		this.levelname = levelname;
		this.account = account;
		this.amount = amount;
		this.fee = fee;
		this.bankBalance = bankBalance;
		this.counts = counts;
		this.id = id;
		this.alias = alias;
		this.owner = owner;
		this.banktype = banktype;
		this.status = status;
		this.time = time;
		this.bankUse = bankUse;
		this.bankStop = bankStop;
		this.bankCanUse = bankCanUse;
		this.sysUse = sysUse;
		this.sysStop = sysStop;
		this.sysCanUse = sysCanUse;
	}

}
