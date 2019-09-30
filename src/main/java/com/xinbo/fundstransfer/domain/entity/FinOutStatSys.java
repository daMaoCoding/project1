package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinOutStatSys implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private String handicapname;
	private String levelname;
	private String member;
	private String accountname;
	private String toaccount;
	private BigDecimal amount;
	private BigDecimal fee;
	private String operatorname;
	private String asigntime;
	private String orderno;
	private String accountowner;
	private int restatus;
	private int tastatus;

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


	public FinOutStatSys() {
		super();
	}

	/**
	 * @return the orderno
	 */
	public String getOrderno() {
		return orderno;
	}

	/**
	 * @param orderno the orderno to set
	 */
	public void setOrderno(String orderno) {
		this.orderno = orderno;
	}

	/**
	 * @return the accountowner
	 */
	public String getAccountowner() {
		return accountowner;
	}

	/**
	 * @param accountowner the accountowner to set
	 */
	public void setAccountowner(String accountowner) {
		this.accountowner = accountowner;
	}

	/**
	 * @return the restatus
	 */
	public int getRestatus() {
		return restatus;
	}

	/**
	 * @param restatus the restatus to set
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
	 * @param tastatus the tastatus to set
	 */
	public void setTastatus(int tastatus) {
		this.tastatus = tastatus;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accountname == null) ? 0 : accountname.hashCode());
		result = prime * result + ((accountowner == null) ? 0 : accountowner.hashCode());
		result = prime * result + ((amount == null) ? 0 : amount.hashCode());
		result = prime * result + ((asigntime == null) ? 0 : asigntime.hashCode());
		result = prime * result + ((fee == null) ? 0 : fee.hashCode());
		result = prime * result + ((handicapname == null) ? 0 : handicapname.hashCode());
		result = prime * result + ((levelname == null) ? 0 : levelname.hashCode());
		result = prime * result + ((member == null) ? 0 : member.hashCode());
		result = prime * result + ((operatorname == null) ? 0 : operatorname.hashCode());
		result = prime * result + ((orderno == null) ? 0 : orderno.hashCode());
		result = prime * result + restatus;
		result = prime * result + tastatus;
		result = prime * result + ((toaccount == null) ? 0 : toaccount.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FinOutStatSys other = (FinOutStatSys) obj;
		if (accountname == null) {
			if (other.accountname != null)
				return false;
		} else if (!accountname.equals(other.accountname))
			return false;
		if (accountowner == null) {
			if (other.accountowner != null)
				return false;
		} else if (!accountowner.equals(other.accountowner))
			return false;
		if (amount == null) {
			if (other.amount != null)
				return false;
		} else if (!amount.equals(other.amount))
			return false;
		if (asigntime == null) {
			if (other.asigntime != null)
				return false;
		} else if (!asigntime.equals(other.asigntime))
			return false;
		if (fee == null) {
			if (other.fee != null)
				return false;
		} else if (!fee.equals(other.fee))
			return false;
		if (handicapname == null) {
			if (other.handicapname != null)
				return false;
		} else if (!handicapname.equals(other.handicapname))
			return false;
		if (levelname == null) {
			if (other.levelname != null)
				return false;
		} else if (!levelname.equals(other.levelname))
			return false;
		if (member == null) {
			if (other.member != null)
				return false;
		} else if (!member.equals(other.member))
			return false;
		if (operatorname == null) {
			if (other.operatorname != null)
				return false;
		} else if (!operatorname.equals(other.operatorname))
			return false;
		if (orderno == null) {
			if (other.orderno != null)
				return false;
		} else if (!orderno.equals(other.orderno))
			return false;
		if (restatus != other.restatus)
			return false;
		if (tastatus != other.tastatus)
			return false;
		if (toaccount == null) {
			if (other.toaccount != null)
				return false;
		} else if (!toaccount.equals(other.toaccount))
			return false;
		return true;
	}

	

}
