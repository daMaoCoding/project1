package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class ClearDateReport implements java.io.Serializable {

	private String account;
	private String bankName;
	private String owner;
	private int handicapId;
	private String handicapName;
	private String alias;
	private String bankType;
	private int accountId;
	private BigDecimal income;
	private BigDecimal outward;
	private BigDecimal fee;
	private int incomeCount;
	private BigDecimal balance;
	private int outwardCount;
	private int outwardPerson;
	private int incomePerson;
	private String time;
	private int feeCount;
	private BigDecimal los;
	private int losCount;
	private BigDecimal incomeSys;
	private int incomeSysCount;
	private BigDecimal outwardSys;
	private int outwardSysCount;
	private String levels;
	private int accountType;
	private String status;
	private int freezeCardCount;
	private BigDecimal freezeAmounts;
	private BigDecimal incomeFee;
	private int thirdIncomeCount;
	private BigDecimal companyIncome;
	private BigDecimal thirdIncome;

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
	 * @return the bankName
	 */
	public String getBankName() {
		return bankName;
	}

	/**
	 * @param bankName
	 *            the bankName to set
	 */
	public void setBankName(String bankName) {
		this.bankName = bankName;
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
	 * @return the handicapId
	 */
	public int getHandicapId() {
		return handicapId;
	}

	/**
	 * @param handicapId
	 *            the handicapId to set
	 */
	public void setHandicapId(int handicapId) {
		this.handicapId = handicapId;
	}

	/**
	 * @return the handicapName
	 */
	public String getHandicapName() {
		return handicapName;
	}

	/**
	 * @param handicapName
	 *            the handicapName to set
	 */
	public void setHandicapName(String handicapName) {
		this.handicapName = handicapName;
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
	 * @return the bankType
	 */
	public String getBankType() {
		return bankType;
	}

	/**
	 * @param bankType
	 *            the bankType to set
	 */
	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	/**
	 * @return the accountId
	 */
	public int getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId
	 *            the accountId to set
	 */
	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the income
	 */
	public BigDecimal getIncome() {
		return income;
	}

	/**
	 * @param income
	 *            the income to set
	 */
	public void setIncome(BigDecimal income) {
		this.income = income;
	}

	/**
	 * @return the outward
	 */
	public BigDecimal getOutward() {
		return outward;
	}

	/**
	 * @param outward
	 *            the outward to set
	 */
	public void setOutward(BigDecimal outward) {
		this.outward = outward;
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
	 * @return the incomeCount
	 */
	public int getIncomeCount() {
		return incomeCount;
	}

	/**
	 * @param incomeCount
	 *            the incomeCount to set
	 */
	public void setIncomeCount(int incomeCount) {
		this.incomeCount = incomeCount;
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
	 * @return the outwardCount
	 */
	public int getOutwardCount() {
		return outwardCount;
	}

	/**
	 * @param outwardCount
	 *            the outwardCount to set
	 */
	public void setOutwardCount(int outwardCount) {
		this.outwardCount = outwardCount;
	}

	/**
	 * @return the outwardPerson
	 */
	public int getOutwardPerson() {
		return outwardPerson;
	}

	/**
	 * @param outwardPerson
	 *            the outwardPerson to set
	 */
	public void setOutwardPerson(int outwardPerson) {
		this.outwardPerson = outwardPerson;
	}

	/**
	 * @return the incomePerson
	 */
	public int getIncomePerson() {
		return incomePerson;
	}

	/**
	 * @param incomePerson
	 *            the incomePerson to set
	 */
	public void setIncomePerson(int incomePerson) {
		this.incomePerson = incomePerson;
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
	 * @return the feeCount
	 */
	public int getFeeCount() {
		return feeCount;
	}

	/**
	 * @param feeCount
	 *            the feeCount to set
	 */
	public void setFeeCount(int feeCount) {
		this.feeCount = feeCount;
	}

	/**
	 * @return the los
	 */
	public BigDecimal getLos() {
		return los;
	}

	/**
	 * @param los
	 *            the los to set
	 */
	public void setLos(BigDecimal los) {
		this.los = los;
	}

	/**
	 * @return the losCount
	 */
	public int getLosCount() {
		return losCount;
	}

	/**
	 * @param losCount
	 *            the losCount to set
	 */
	public void setLosCount(int losCount) {
		this.losCount = losCount;
	}

	/**
	 * @return the incomeSys
	 */
	public BigDecimal getIncomeSys() {
		return incomeSys;
	}

	/**
	 * @param incomeSys
	 *            the incomeSys to set
	 */
	public void setIncomeSys(BigDecimal incomeSys) {
		this.incomeSys = incomeSys;
	}

	/**
	 * @return the incomeSysCount
	 */
	public int getIncomeSysCount() {
		return incomeSysCount;
	}

	/**
	 * @param incomeSysCount
	 *            the incomeSysCount to set
	 */
	public void setIncomeSysCount(int incomeSysCount) {
		this.incomeSysCount = incomeSysCount;
	}

	/**
	 * @return the outwardSys
	 */
	public BigDecimal getOutwardSys() {
		return outwardSys;
	}

	/**
	 * @param outwardSys
	 *            the outwardSys to set
	 */
	public void setOutwardSys(BigDecimal outwardSys) {
		this.outwardSys = outwardSys;
	}

	/**
	 * @return the outwardSysCount
	 */
	public int getOutwardSysCount() {
		return outwardSysCount;
	}

	/**
	 * @param outwardSysCount
	 *            the outwardSysCount to set
	 */
	public void setOutwardSysCount(int outwardSysCount) {
		this.outwardSysCount = outwardSysCount;
	}

	public ClearDateReport() {
		super();
	}

	/**
	 * @return the levels
	 */
	public String getLevels() {
		return levels;
	}

	/**
	 * @param levels
	 *            the levels to set
	 */
	public void setLevels(String levels) {
		this.levels = levels;
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
		result = prime * result + accountId;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((balance == null) ? 0 : balance.hashCode());
		result = prime * result + ((bankName == null) ? 0 : bankName.hashCode());
		result = prime * result + ((bankType == null) ? 0 : bankType.hashCode());
		result = prime * result + ((fee == null) ? 0 : fee.hashCode());
		result = prime * result + feeCount;
		result = prime * result + handicapId;
		result = prime * result + ((handicapName == null) ? 0 : handicapName.hashCode());
		result = prime * result + ((income == null) ? 0 : income.hashCode());
		result = prime * result + incomeCount;
		result = prime * result + incomePerson;
		result = prime * result + ((incomeSys == null) ? 0 : incomeSys.hashCode());
		result = prime * result + incomeSysCount;
		result = prime * result + ((levels == null) ? 0 : levels.hashCode());
		result = prime * result + ((los == null) ? 0 : los.hashCode());
		result = prime * result + losCount;
		result = prime * result + ((outward == null) ? 0 : outward.hashCode());
		result = prime * result + outwardCount;
		result = prime * result + outwardPerson;
		result = prime * result + ((outwardSys == null) ? 0 : outwardSys.hashCode());
		result = prime * result + outwardSysCount;
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
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
		ClearDateReport other = (ClearDateReport) obj;
		if (account == null) {
			if (other.account != null)
				return false;
		} else if (!account.equals(other.account))
			return false;
		if (accountId != other.accountId)
			return false;
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
		if (balance == null) {
			if (other.balance != null)
				return false;
		} else if (!balance.equals(other.balance))
			return false;
		if (bankName == null) {
			if (other.bankName != null)
				return false;
		} else if (!bankName.equals(other.bankName))
			return false;
		if (bankType == null) {
			if (other.bankType != null)
				return false;
		} else if (!bankType.equals(other.bankType))
			return false;
		if (fee == null) {
			if (other.fee != null)
				return false;
		} else if (!fee.equals(other.fee))
			return false;
		if (feeCount != other.feeCount)
			return false;
		if (handicapId != other.handicapId)
			return false;
		if (handicapName == null) {
			if (other.handicapName != null)
				return false;
		} else if (!handicapName.equals(other.handicapName))
			return false;
		if (income == null) {
			if (other.income != null)
				return false;
		} else if (!income.equals(other.income))
			return false;
		if (incomeCount != other.incomeCount)
			return false;
		if (incomePerson != other.incomePerson)
			return false;
		if (incomeSys == null) {
			if (other.incomeSys != null)
				return false;
		} else if (!incomeSys.equals(other.incomeSys))
			return false;
		if (incomeSysCount != other.incomeSysCount)
			return false;
		if (levels == null) {
			if (other.levels != null)
				return false;
		} else if (!levels.equals(other.levels))
			return false;
		if (los == null) {
			if (other.los != null)
				return false;
		} else if (!los.equals(other.los))
			return false;
		if (losCount != other.losCount)
			return false;
		if (outward == null) {
			if (other.outward != null)
				return false;
		} else if (!outward.equals(other.outward))
			return false;
		if (outwardCount != other.outwardCount)
			return false;
		if (outwardPerson != other.outwardPerson)
			return false;
		if (outwardSys == null) {
			if (other.outwardSys != null)
				return false;
		} else if (!outwardSys.equals(other.outwardSys))
			return false;
		if (outwardSysCount != other.outwardSysCount)
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (time == null) {
			if (other.time != null)
				return false;
		} else if (!time.equals(other.time))
			return false;
		return true;
	}

	/**
	 * @return the accountType
	 */
	public int getAccountType() {
		return accountType;
	}

	/**
	 * @param accountType
	 *            the accountType to set
	 */
	public void setAccountType(int accountType) {
		this.accountType = accountType;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the freezeCardCount
	 */
	public int getFreezeCardCount() {
		return freezeCardCount;
	}

	/**
	 * @param freezeCardCount
	 *            the freezeCardCount to set
	 */
	public void setFreezeCardCount(int freezeCardCount) {
		this.freezeCardCount = freezeCardCount;
	}

	/**
	 * @return the freezeAmounts
	 */
	public BigDecimal getFreezeAmounts() {
		return freezeAmounts;
	}

	/**
	 * @param freezeAmounts
	 *            the freezeAmounts to set
	 */
	public void setFreezeAmounts(BigDecimal freezeAmounts) {
		this.freezeAmounts = freezeAmounts;
	}

	/**
	 * @return the incomeFee
	 */
	public BigDecimal getIncomeFee() {
		return incomeFee;
	}

	/**
	 * @param incomeFee
	 *            the incomeFee to set
	 */
	public void setIncomeFee(BigDecimal incomeFee) {
		this.incomeFee = incomeFee;
	}

	/**
	 * @return the thirdIncomeCount
	 */
	public int getThirdIncomeCount() {
		return thirdIncomeCount;
	}

	/**
	 * @param thirdIncomeCount
	 *            the thirdIncomeCount to set
	 */
	public void setThirdIncomeCount(int thirdIncomeCount) {
		this.thirdIncomeCount = thirdIncomeCount;
	}

	public BigDecimal getCompanyIncome() {
		return companyIncome;
	}

	public void setCompanyIncome(BigDecimal companyIncome) {
		this.companyIncome = companyIncome;
	}

	public BigDecimal getThirdIncome() {
		return thirdIncome;
	}

	public void setThirdIncome(BigDecimal thirdIncome) {
		this.thirdIncome = thirdIncome;
	}

	public ClearDateReport(String account, String bankName, String owner, int handicapId, String handicapName,
			String alias, String bankType, int accountId, BigDecimal income, BigDecimal outward, BigDecimal fee,
			int incomeCount, BigDecimal balance, int outwardCount, int outwardPerson, int incomePerson, String time,
			int feeCount, BigDecimal los, int losCount, BigDecimal incomeSys, int incomeSysCount, BigDecimal outwardSys,
			int outwardSysCount, String levels, int accountType, String status, int freezeCardCount,
			BigDecimal freezeAmounts, BigDecimal incomeFee, int thirdIncomeCount, BigDecimal companyIncome,
			BigDecimal thirdIncome) {
		super();
		this.account = account;
		this.bankName = bankName;
		this.owner = owner;
		this.handicapId = handicapId;
		this.handicapName = handicapName;
		this.alias = alias;
		this.bankType = bankType;
		this.accountId = accountId;
		this.income = income;
		this.outward = outward;
		this.fee = fee;
		this.incomeCount = incomeCount;
		this.balance = balance;
		this.outwardCount = outwardCount;
		this.outwardPerson = outwardPerson;
		this.incomePerson = incomePerson;
		this.time = time;
		this.feeCount = feeCount;
		this.los = los;
		this.losCount = losCount;
		this.incomeSys = incomeSys;
		this.incomeSysCount = incomeSysCount;
		this.outwardSys = outwardSys;
		this.outwardSysCount = outwardSysCount;
		this.levels = levels;
		this.accountType = accountType;
		this.status = status;
		this.freezeCardCount = freezeCardCount;
		this.freezeAmounts = freezeAmounts;
		this.incomeFee = incomeFee;
		this.thirdIncomeCount = thirdIncomeCount;
		this.companyIncome = companyIncome;
		this.thirdIncome = thirdIncome;
	}

}
