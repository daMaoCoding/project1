package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinOutStatFlowDeTaiIs implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String handicapname;
	private String levelname;
	private String member;
	private String toaccount;
	private String toaccountname;
	private String toaccountowner;
	private String atoaccount;
	private BigDecimal amount;
	private BigDecimal fee;
	private String tradingtime;
	private int type;
	private BigDecimal damount;
	private BigDecimal bfee;
	private String asigntime;
	private String operatorname;
	private String comfirmor;
	private String accountname;
	private String bankname;
	private String owner;

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
	 * @return the member
	 */
	public String getMember() {
		return member;
	}

	/**
	 * @param member
	 *            the member to set
	 */
	public void setMember(String member) {
		this.member = member;
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
	 * @return the toaccountname
	 */
	public String getToaccountname() {
		return toaccountname;
	}

	/**
	 * @param toaccountname
	 *            the toaccountname to set
	 */
	public void setToaccountname(String toaccountname) {
		this.toaccountname = toaccountname;
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
	 * @return the atoaccount
	 */
	public String getAtoaccount() {
		return atoaccount;
	}

	/**
	 * @param atoaccount
	 *            the atoaccount to set
	 */
	public void setAtoaccount(String atoaccount) {
		this.atoaccount = atoaccount;
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
	 * @return the damount
	 */
	public BigDecimal getDamount() {
		return damount;
	}

	/**
	 * @param damount
	 *            the damount to set
	 */
	public void setDamount(BigDecimal damount) {
		this.damount = damount;
	}

	/**
	 * @return the bfee
	 */
	public BigDecimal getBfee() {
		return bfee;
	}

	/**
	 * @param bfee
	 *            the bfee to set
	 */
	public void setBfee(BigDecimal bfee) {
		this.bfee = bfee;
	}

	/**
	 * @return the asigntime
	 */
	public String getAsigntime() {
		return asigntime;
	}

	/**
	 * @param asigntime
	 *            the asigntime to set
	 */
	public void setAsigntime(String asigntime) {
		this.asigntime = asigntime;
	}

	/**
	 * @return the operatorname
	 */
	public String getOperatorname() {
		return operatorname;
	}

	/**
	 * @param operatorname
	 *            the operatorname to set
	 */
	public void setOperatorname(String operatorname) {
		this.operatorname = operatorname;
	}

	/**
	 * @return the comfirmor
	 */
	public String getComfirmor() {
		return comfirmor;
	}

	/**
	 * @param comfirmor
	 *            the comfirmor to set
	 */
	public void setComfirmor(String comfirmor) {
		this.comfirmor = comfirmor;
	}

	/**
	 * @return the accountname
	 */
	public String getAccountname() {
		return accountname;
	}

	/**
	 * @param accountname
	 *            the accountname to set
	 */
	public void setAccountname(String accountname) {
		this.accountname = accountname;
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

	public FinOutStatFlowDeTaiIs(String handicapname, String levelname, String member, String toaccount,
			String toaccountname, String toaccountowner, String atoaccount, BigDecimal amount, BigDecimal fee,
			String tradingtime, int type, BigDecimal damount, BigDecimal bfee, String asigntime, String operatorname,
			String comfirmor, String accountname, String bankname, String owner) {
		super();
		this.handicapname = handicapname;
		this.levelname = levelname;
		this.member = member;
		this.toaccount = toaccount;
		this.toaccountname = toaccountname;
		this.toaccountowner = toaccountowner;
		this.atoaccount = atoaccount;
		this.amount = amount;
		this.fee = fee;
		this.tradingtime = tradingtime;
		this.type = type;
		this.damount = damount;
		this.bfee = bfee;
		this.asigntime = asigntime;
		this.operatorname = operatorname;
		this.comfirmor = comfirmor;
		this.accountname = accountname;
		this.bankname = bankname;
		this.owner = owner;
	}

	public FinOutStatFlowDeTaiIs() {
		super();
	}

}
