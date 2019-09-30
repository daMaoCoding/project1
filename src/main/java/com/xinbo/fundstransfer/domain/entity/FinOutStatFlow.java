package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinOutStatFlow implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private String account;
	private String toaccount;
	private BigDecimal amount;
	private BigDecimal fee;
	private Integer status;
	private int type;
	private String transactionno;
	private String toaccountowner;
	private String tradingtime;
	private String createtime;
	private int id;
	private BigDecimal balance;
	private String remark;
	private String summary;

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
	 * @return the toaccount
	 */
	public String getToaccount() {
		return toaccount;
	}

	/**
	 * @param toaccount
	 *            the toaccount to set
	 */
	public void setToaccount(String toaccount) {
		this.toaccount = toaccount;
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

	

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
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
	 * @return the transactionno
	 */
	public String getTransactionno() {
		return transactionno;
	}

	/**
	 * @param transactionno
	 *            the transactionno to set
	 */
	public void setTransactionno(String transactionno) {
		this.transactionno = transactionno;
	}

	/**
	 * @return the toaccountowner
	 */
	public String getToaccountowner() {
		return toaccountowner;
	}

	/**
	 * @param toaccountowner
	 *            the toaccountowner to set
	 */
	public void setToaccountowner(String toaccountowner) {
		this.toaccountowner = toaccountowner;
	}

	/**
	 * @return the tradingtime
	 */
	public String getTradingtime() {
		return tradingtime;
	}

	/**
	 * @param tradingtime
	 *            the tradingtime to set
	 */
	public void setTradingtime(String tradingtime) {
		this.tradingtime = tradingtime;
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
	

	public String getCreatetime() {
		return createtime;
	}

	public void setCreatetime(String createtime) {
		this.createtime = createtime;
	}

	public FinOutStatFlow() {
		super();
	}

	/**
	 * @return the balance
	 */
	public BigDecimal getBalance() {
		return balance;
	}

	/**
	 * @param balance the balance to set
	 */
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	/**
	 * @return the remark
	 */
	public String getRemark() {
		return remark;
	}

	/**
	 * @param remark the remark to set
	 */
	public void setRemark(String remark) {
		this.remark = remark;
	}

	/**
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * @param summary the summary to set
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	public FinOutStatFlow(String account, String toaccount, BigDecimal amount, BigDecimal fee, Integer status, int type,
			String transactionno, String toaccountowner, String tradingtime, String createtime, int id,
			BigDecimal balance, String remark, String summary) {
		super();
		this.account = account;
		this.toaccount = toaccount;
		this.amount = amount;
		this.fee = fee;
		this.status = status;
		this.type = type;
		this.transactionno = transactionno;
		this.toaccountowner = toaccountowner;
		this.tradingtime = tradingtime;
		this.createtime = createtime;
		this.id = id;
		this.balance = balance;
		this.remark = remark;
		this.summary = summary;
	}
	
	

}
