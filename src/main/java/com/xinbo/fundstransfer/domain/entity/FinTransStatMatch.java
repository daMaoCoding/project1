package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinTransStatMatch implements java.io.Serializable {

	private String orderno;
	private String fromaccountname;
	private String toaccountname;
	private BigDecimal amount;
	private BigDecimal fee;
	private String tradingtime;
	private String createtime;
	private String remark;
	private String operator;
	private int toid;
	private int status;
	private String alias;
	private String handicapname;

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
	 * @return the fromaccountname
	 */
	public String getFromaccountname() {
		return fromaccountname;
	}

	/**
	 * @param fromaccountname
	 *            the fromaccountname to set
	 */
	public void setFromaccountname(String fromaccountname) {
		this.fromaccountname = fromaccountname;
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
	 * @return the operator
	 */
	public String getOperator() {
		return operator;
	}

	/**
	 * @param operator
	 *            the operator to set
	 */
	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getTradingtime() {
		return tradingtime;
	}

	public void setTradingtime(String tradingtime) {
		this.tradingtime = tradingtime;
	}

	public FinTransStatMatch() {
		super();
	}

	/**
	 * @return the toid
	 */
	public int getToid() {
		return toid;
	}

	/**
	 * @param toid
	 *            the toid to set
	 */
	public void setToid(int toid) {
		this.toid = toid;
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

	public FinTransStatMatch(String orderno, String fromaccountname, String toaccountname, BigDecimal amount,
			BigDecimal fee, String tradingtime, String createtime, String remark, String operator, int toid, int status,
			String alias, String handicapname) {
		super();
		this.orderno = orderno;
		this.fromaccountname = fromaccountname;
		this.toaccountname = toaccountname;
		this.amount = amount;
		this.fee = fee;
		this.tradingtime = tradingtime;
		this.createtime = createtime;
		this.remark = remark;
		this.operator = operator;
		this.toid = toid;
		this.status = status;
		this.alias = alias;
		this.handicapname = handicapname;
	}

}
