package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinFrostLessStat implements java.io.Serializable {

	private int id;
	private String alias;
	private String handicap;
	private String level;
	private int type;
	private String account;
	private String bankname;
	private BigDecimal bankbalance;
	private String owner;
	private String banktype;
	private String remark;
	private String time;
	private String operator;
	private int currSysLeval;
	private BigDecimal balance;
	private String cause;
	private String createTime;
	private String pendingRemark;
	private int pendingId;
	private BigDecimal totalAmount;
	private int status;
	private String confirmor;
	private BigDecimal jdTotalAmount;
	private BigDecimal amount;
	private String showPassword;
	private String pendingStatus;
	private String defrostType;
	private String sbuType;
	private String flag;

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
	 * @return the handicap
	 */
	public String getHandicap() {
		return handicap;
	}

	/**
	 * @param handicap
	 *            the handicap to set
	 */
	public void setHandicap(String handicap) {
		this.handicap = handicap;
	}

	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * @param level
	 *            the level to set
	 */
	public void setLevel(String level) {
		this.level = level;
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

	public FinFrostLessStat() {
		super();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((account == null) ? 0 : account.hashCode());
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((bankbalance == null) ? 0 : bankbalance.hashCode());
		result = prime * result + ((bankname == null) ? 0 : bankname.hashCode());
		result = prime * result + ((banktype == null) ? 0 : banktype.hashCode());
		result = prime * result + ((handicap == null) ? 0 : handicap.hashCode());
		result = prime * result + id;
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((remark == null) ? 0 : remark.hashCode());
		result = prime * result + type;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
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
		FinFrostLessStat other = (FinFrostLessStat) obj;
		if (account == null) {
			if (other.account != null)
				return false;
		} else if (!account.equals(other.account))
			return false;
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
		if (bankbalance == null) {
			if (other.bankbalance != null)
				return false;
		} else if (!bankbalance.equals(other.bankbalance))
			return false;
		if (bankname == null) {
			if (other.bankname != null)
				return false;
		} else if (!bankname.equals(other.bankname))
			return false;
		if (banktype == null) {
			if (other.banktype != null)
				return false;
		} else if (!banktype.equals(other.banktype))
			return false;
		if (handicap == null) {
			if (other.handicap != null)
				return false;
		} else if (!handicap.equals(other.handicap))
			return false;
		if (id != other.id)
			return false;
		if (level == null) {
			if (other.level != null)
				return false;
		} else if (!level.equals(other.level))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (remark == null) {
			if (other.remark != null)
				return false;
		} else if (!remark.equals(other.remark))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	/**
	 * @return the time
	 */
	public String getTime() {
		return time;
	}

	/**
	 * @param time
	 *            the time to set
	 */
	public void setTime(String time) {
		this.time = time;
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
	 * @return the currSysLeval
	 */
	public int getCurrSysLeval() {
		return currSysLeval;
	}

	/**
	 * @param currSysLeval
	 *            the currSysLeval to set
	 */
	public void setCurrSysLeval(int currSysLeval) {
		this.currSysLeval = currSysLeval;
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
	 * @return the cause
	 */
	public String getCause() {
		return cause;
	}

	/**
	 * @param cause
	 *            the cause to set
	 */
	public void setCause(String cause) {
		this.cause = cause;
	}

	/**
	 * @return the createTime
	 */
	public String getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime
	 *            the createTime to set
	 */
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the pendingRemark
	 */
	public String getPendingRemark() {
		return pendingRemark;
	}

	/**
	 * @param pendingRemark
	 *            the pendingRemark to set
	 */
	public void setPendingRemark(String pendingRemark) {
		this.pendingRemark = pendingRemark;
	}

	/**
	 * @return the pendingId
	 */
	public int getPendingId() {
		return pendingId;
	}

	/**
	 * @param pendingId
	 *            the pendingId to set
	 */
	public void setPendingId(int pendingId) {
		this.pendingId = pendingId;
	}

	/**
	 * @return the totalAmount
	 */
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	/**
	 * @param totalAmount
	 *            the totalAmount to set
	 */
	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
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

	/**
	 * @return the jdTotalAmount
	 */
	public BigDecimal getJdTotalAmount() {
		return jdTotalAmount;
	}

	/**
	 * @param jdTotalAmount
	 *            the jdTotalAmount to set
	 */
	public void setJdTotalAmount(BigDecimal jdTotalAmount) {
		this.jdTotalAmount = jdTotalAmount;
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
	 * @return the showPassword
	 */
	public String getShowPassword() {
		return showPassword;
	}

	/**
	 * @param showPassword
	 *            the showPassword to set
	 */
	public void setShowPassword(String showPassword) {
		this.showPassword = showPassword;
	}

	/**
	 * @return the pendingStatus
	 */
	public String getPendingStatus() {
		return pendingStatus;
	}

	/**
	 * @param pendingStatus
	 *            the pendingStatus to set
	 */
	public void setPendingStatus(String pendingStatus) {
		this.pendingStatus = pendingStatus;
	}

	/**
	 * @return the defrostType
	 */
	public String getDefrostType() {
		return defrostType;
	}

	/**
	 * @param defrostType
	 *            the defrostType to set
	 */
	public void setDefrostType(String defrostType) {
		this.defrostType = defrostType;
	}

	/**
	 * @return the sbuType
	 */
	public String getSbuType() {
		return sbuType;
	}

	/**
	 * @param sbuType
	 *            the sbuType to set
	 */
	public void setSbuType(String sbuType) {
		this.sbuType = sbuType;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public FinFrostLessStat(int id, String alias, String handicap, String level, int type, String account,
			String bankname, BigDecimal bankbalance, String owner, String banktype, String remark, String time,
			String operator, int currSysLeval, BigDecimal balance, String cause, String createTime,
			String pendingRemark, int pendingId, BigDecimal totalAmount, int status, String confirmor,
			BigDecimal jdTotalAmount, BigDecimal amount, String showPassword, String pendingStatus, String defrostType,
			String sbuType, String flag) {
		super();
		this.id = id;
		this.alias = alias;
		this.handicap = handicap;
		this.level = level;
		this.type = type;
		this.account = account;
		this.bankname = bankname;
		this.bankbalance = bankbalance;
		this.owner = owner;
		this.banktype = banktype;
		this.remark = remark;
		this.time = time;
		this.operator = operator;
		this.currSysLeval = currSysLeval;
		this.balance = balance;
		this.cause = cause;
		this.createTime = createTime;
		this.pendingRemark = pendingRemark;
		this.pendingId = pendingId;
		this.totalAmount = totalAmount;
		this.status = status;
		this.confirmor = confirmor;
		this.jdTotalAmount = jdTotalAmount;
		this.amount = amount;
		this.showPassword = showPassword;
		this.pendingStatus = pendingStatus;
		this.defrostType = defrostType;
		this.sbuType = sbuType;
		this.flag = flag;
	}

}
