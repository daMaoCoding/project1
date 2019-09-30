package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;

public class AccountReturnSummary {
	private String calcTime;
	private BigDecimal bankAmounts;
	private BigDecimal rebateAmount;
	private BigDecimal rebateBalanc;
	private int counts;
	private int status;
	private int isLock;
	private int isLockMyself;

	private String rebateUser;
	private int accountId;
	private int totalCards;
	private int useCards;
	private String bankType;
	private String owner;
	private String account;

	private String remark;

	public String getCalcTime() {
		return calcTime;
	}

	public void setCalcTime(String calcTime) {
		this.calcTime = calcTime;
	}

	public BigDecimal getBankAmounts() {
		return bankAmounts;
	}

	public void setBankAmounts(BigDecimal bankAmounts) {
		this.bankAmounts = bankAmounts;
	}

	public BigDecimal getRebateAmount() {
		return rebateAmount;
	}

	public void setRebateAmount(BigDecimal rebateAmount) {
		this.rebateAmount = rebateAmount;
	}

	public int getCounts() {
		return counts;
	}

	public void setCounts(int counts) {
		this.counts = counts;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public AccountReturnSummary() {
		super();
	}

	public int getIsLock() {
		return isLock;
	}

	public void setIsLock(int isLock) {
		this.isLock = isLock;
	}

	public int getIsLockMyself() {
		return isLockMyself;
	}

	public void setIsLockMyself(int isLockMyself) {
		this.isLockMyself = isLockMyself;
	}

	public String getRebateUser() {
		return rebateUser;
	}

	public void setRebateUser(String rebateUser) {
		this.rebateUser = rebateUser;
	}

	public int getAccountId() {
		return accountId;
	}

	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}

	public int getTotalCards() {
		return totalCards;
	}

	public void setTotalCards(int totalCards) {
		this.totalCards = totalCards;
	}

	public int getUseCards() {
		return useCards;
	}

	public void setUseCards(int useCards) {
		this.useCards = useCards;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public BigDecimal getRebateBalanc() {
		return rebateBalanc;
	}

	public void setRebateBalanc(BigDecimal rebateBalanc) {
		this.rebateBalanc = rebateBalanc;
	}

	public AccountReturnSummary(String calcTime, BigDecimal bankAmounts, BigDecimal rebateAmount,
			BigDecimal rebateBalanc, int counts, int status, int isLock, int isLockMyself, String rebateUser,
			int accountId, int totalCards, int useCards, String bankType, String owner, String account, String remark) {
		super();
		this.calcTime = calcTime;
		this.bankAmounts = bankAmounts;
		this.rebateAmount = rebateAmount;
		this.rebateBalanc = rebateBalanc;
		this.counts = counts;
		this.status = status;
		this.isLock = isLock;
		this.isLockMyself = isLockMyself;
		this.rebateUser = rebateUser;
		this.accountId = accountId;
		this.totalCards = totalCards;
		this.useCards = useCards;
		this.bankType = bankType;
		this.owner = owner;
		this.account = account;
		this.remark = remark;
	}

}
