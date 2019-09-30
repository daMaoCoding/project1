package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinOutMatch implements java.io.Serializable {

	private String handicapname;
	private int handicappno;
	private String member;
	private String orderno;
	private String createtime;
	private BigDecimal amounts;
	private BigDecimal amount;
	private BigDecimal fee;
	private String operator;
	private String toaccount;
	private String toaccountowner;
	private String operatorczr;
	private String confirmor;
	private String fromaccount;
	private int fromaccountid;
	private BigDecimal bankamount;
	private int restatus;
	private int tastatus;
	private String updatetime;
	private String owner;
	private String banktype;
	private String alias;
	private String bankcreatime;

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
	 * @return the handicappno
	 */
	public int getHandicappno() {
		return handicappno;
	}

	/**
	 * @param handicappno
	 *            the handicappno to set
	 */
	public void setHandicappno(int handicappno) {
		this.handicappno = handicappno;
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
	 * @return the amounts
	 */
	public BigDecimal getAmounts() {
		return amounts;
	}

	/**
	 * @param amounts
	 *            the amounts to set
	 */
	public void setAmounts(BigDecimal amounts) {
		this.amounts = amounts;
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
	 * @return the operatorczr
	 */
	public String getOperatorczr() {
		return operatorczr;
	}

	/**
	 * @param operatorczr
	 *            the operatorczr to set
	 */
	public void setOperatorczr(String operatorczr) {
		this.operatorczr = operatorczr;
	}

	/**
	 * @return the confirmor
	 */
	public String getConfirmor() {
		return confirmor;
	}

	/**
	 * @param confirmor
	 *            the confirmor to set
	 */
	public void setConfirmor(String confirmor) {
		this.confirmor = confirmor;
	}

	public FinOutMatch() {
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
	 * @return the fromaccountid
	 */
	public int getFromaccountid() {
		return fromaccountid;
	}

	/**
	 * @param fromaccountid
	 *            the fromaccountid to set
	 */
	public void setFromaccountid(int fromaccountid) {
		this.fromaccountid = fromaccountid;
	}

	/**
	 * @return the bankamount
	 */
	public BigDecimal getBankamount() {
		return bankamount;
	}

	/**
	 * @param bankamount
	 *            the bankamount to set
	 */
	public void setBankamount(BigDecimal bankamount) {
		this.bankamount = bankamount;
	}

	/**
	 * @return the restatus
	 */
	public int getRestatus() {
		return restatus;
	}

	/**
	 * @param restatus
	 *            the restatus to set
	 */
	public void setRestatus(int restatus) {
		this.restatus = restatus;
	}

	/**
	 * @return the tastatus
	 */
	public int getTastatus() {
		return tastatus;
	}

	/**
	 * @param tastatus
	 *            the tastatus to set
	 */
	public void setTastatus(int tastatus) {
		this.tastatus = tastatus;
	}

	/**
	 * @return the updatetime
	 */
	public String getUpdatetime() {
		return updatetime;
	}

	/**
	 * @param updatetime
	 *            the updatetime to set
	 */
	public void setUpdatetime(String updatetime) {
		this.updatetime = updatetime;
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

	/**
	 * @return the bankcreatime
	 */
	public String getBankcreatime() {
		return bankcreatime;
	}

	/**
	 * @param bankcreatime the bankcreatime to set
	 */
	public void setBankcreatime(String bankcreatime) {
		this.bankcreatime = bankcreatime;
	}

	public FinOutMatch(String handicapname, int handicappno, String member, String orderno, String createtime,
			BigDecimal amounts, BigDecimal amount, BigDecimal fee, String operator, String toaccount,
			String toaccountowner, String operatorczr, String confirmor, String fromaccount, int fromaccountid,
			BigDecimal bankamount, int restatus, int tastatus, String updatetime, String owner, String banktype,
			String alias, String bankcreatime) {
		super();
		this.handicapname = handicapname;
		this.handicappno = handicappno;
		this.member = member;
		this.orderno = orderno;
		this.createtime = createtime;
		this.amounts = amounts;
		this.amount = amount;
		this.fee = fee;
		this.operator = operator;
		this.toaccount = toaccount;
		this.toaccountowner = toaccountowner;
		this.operatorczr = operatorczr;
		this.confirmor = confirmor;
		this.fromaccount = fromaccount;
		this.fromaccountid = fromaccountid;
		this.bankamount = bankamount;
		this.restatus = restatus;
		this.tastatus = tastatus;
		this.updatetime = updatetime;
		this.owner = owner;
		this.banktype = banktype;
		this.alias = alias;
		this.bankcreatime = bankcreatime;
	}

	

	

}
