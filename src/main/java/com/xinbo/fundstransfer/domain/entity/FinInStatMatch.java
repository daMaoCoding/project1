package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinInStatMatch implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String handicapname;
	private int type;
	private String memberrealname;
	private String orderno;
	private BigDecimal amount;
	private BigDecimal fee;
	private String createtime;
	private String remark;
	private int counts;
	private int id;
	private String fromaccount;
	private String bankname;

	private String tradingtime;
	private int status;
	private String toaccount;
	private String toaccountowner;
	private String summary;
	private BigDecimal balance;

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
	 * @return the memberrealname
	 */
	public String getMemberrealname() {
		return memberrealname;
	}

	/**
	 * @param memberrealname
	 *            the memberrealname to set
	 */
	public void setMemberrealname(String memberrealname) {
		this.memberrealname = memberrealname;
	}

	/**
	 * @return the orderno
	 */
	public String getOrderno() {
		return orderno;
	}

	/**
	 * @param orderno
	 *            the orderno to set
	 */
	public void setOrderno(String orderno) {
		this.orderno = orderno;
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
	 * @return the createtime
	 */
	public String getCreatetime() {
		return createtime;
	}

	/**
	 * @param createtime
	 *            the createtime to set
	 */
	public void setCreatetime(String createtime) {
		this.createtime = createtime;
	}

	/**
	 * @return the remark
	 */
	public String getRemark() {
		return remark;
	}

	/**
	 * @param remark
	 *            the remark to set
	 */
	public void setRemark(String remark) {
		this.remark = remark;
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

	public FinInStatMatch() {
		super();
	}

	/**
	 * @return the fromaccount
	 */
	public String getFromaccount() {
		return fromaccount;
	}

	/**
	 * @param fromaccount
	 *            the fromaccount to set
	 */
	public void setFromaccount(String fromaccount) {
		this.fromaccount = fromaccount;
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
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(int status) {
		this.status = status;
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
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * @param summary
	 *            the summary to set
	 */
	public void setSummary(String summary) {
		this.summary = summary;
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

	public FinInStatMatch(String handicapname, int type, String memberrealname, String orderno, BigDecimal amount,
			BigDecimal fee, String createtime, String remark, int counts, int id, String fromaccount, String bankname,
			String tradingtime, int status, String toaccount, String toaccountowner, String summary,
			BigDecimal balance) {
		super();
		this.handicapname = handicapname;
		this.type = type;
		this.memberrealname = memberrealname;
		this.orderno = orderno;
		this.amount = amount;
		this.fee = fee;
		this.createtime = createtime;
		this.remark = remark;
		this.counts = counts;
		this.id = id;
		this.fromaccount = fromaccount;
		this.bankname = bankname;
		this.tradingtime = tradingtime;
		this.status = status;
		this.toaccount = toaccount;
		this.toaccountowner = toaccountowner;
		this.summary = summary;
		this.balance = balance;
	}

}
