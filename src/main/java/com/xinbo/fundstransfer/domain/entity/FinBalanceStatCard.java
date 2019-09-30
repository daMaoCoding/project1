package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinBalanceStatCard implements java.io.Serializable {

	private String account;
	private int type;
	private String bankname;
	private BigDecimal balance;
	private BigDecimal bankbalance;
	private int id;
	private String alias;
	private String owner;
	private String banktype;

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
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * @return the bankname
	 */
	public String getBankname() {
		return bankname;
	}

	/**
	 * @param bankname
	 *            the bankname to set
	 */
	public void setBankname(String bankname) {
		this.bankname = bankname;
	}

	/**
	 * @return the balance
	 */
	public BigDecimal getBalance() {
		return balance;
	}

	/**
	 * @param balance
	 *            the balance to set
	 */
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	/**
	 * @return the bankbalance
	 */
	public BigDecimal getBankbalance() {
		return bankbalance;
	}

	/**
	 * @param bankbalance
	 *            the bankbalance to set
	 */
	public void setBankbalance(BigDecimal bankbalance) {
		this.bankbalance = bankbalance;
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


	public FinBalanceStatCard() {
		super();
	}


	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
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
	 * @param banktype the banktype to set
	 */
	public void setBanktype(String banktype) {
		this.banktype = banktype;
	}

	/**
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @param alias the alias to set
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	public FinBalanceStatCard(String account, int type, String bankname, BigDecimal balance, BigDecimal bankbalance,
			int id, String alias, String owner, String banktype) {
		super();
		this.account = account;
		this.type = type;
		this.bankname = bankname;
		this.balance = balance;
		this.bankbalance = bankbalance;
		this.id = id;
		this.alias = alias;
		this.owner = owner;
		this.banktype = banktype;
	}

	
	
}
