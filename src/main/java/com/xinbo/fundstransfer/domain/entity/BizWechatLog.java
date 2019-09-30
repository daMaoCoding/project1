package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Entity
@Table(name = "biz_wechat_log")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizWechatLog {

	/**
	 * 
	 */
	private int id;
	private int fromAccount;
	private Date tradingTime;
	private BigDecimal amount;
	private BigDecimal balance;
	private String remark;
	private String summary;
	private String depositor;
	private Date createTime;
	private int status;
	private int wechatRequestId;
	private int handicapId;
	private String handicapName;
	private String account;
	private int counts;
	private String trTime;
	private String crTime;
	private String owner;
	private int accountStatus;

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
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

	/**
	 * @return the fromAccount
	 */
	@Column(name = "from_account")
	public int getFromAccount() {
		return fromAccount;
	}

	/**
	 * @param fromAccount
	 *            the fromAccount to set
	 */
	public void setFromAccount(int fromAccount) {
		this.fromAccount = fromAccount;
	}

	/**
	 * @return the amount
	 */
	@Column(name = "amount", precision = 10)
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
	 * @return the balance
	 */
	@Column(name = "balance", precision = 10)
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
	 * @return the remark
	 */
	@Column(name = "remark", precision = 10)
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
	 * @return the depositor
	 */
	public String getDepositor() {
		return depositor;
	}

	/**
	 * @param depositor
	 *            the depositor to set
	 */
	public void setDepositor(String depositor) {
		this.depositor = depositor;
	}

	/**
	 * @return the handicapId
	 */
	@Transient
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
	@Transient
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
	 * @return the account
	 */
	@Transient
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
	 * @return the counts
	 */
	@Transient
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
	 * @return the status
	 */
	@Column(name = "status")
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
	 * @return the wechatRequestId
	 */
	@Column(name = "wechat_request_id")
	public int getWechatRequestId() {
		return wechatRequestId;
	}

	/**
	 * @param wechatRequestId
	 *            the wechatRequestId to set
	 */
	public void setWechatRequestId(int wechatRequestId) {
		this.wechatRequestId = wechatRequestId;
	}

	/**
	 * @return the tradingTime
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "trading_time", length = 19)
	public Date getTradingTime() {
		return tradingTime;
	}

	/**
	 * @param tradingTime
	 *            the tradingTime to set
	 */
	public void setTradingTime(Date tradingTime) {
		this.tradingTime = tradingTime;
	}

	/**
	 * @return the createTime
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "create_time", length = 19)
	public Date getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime
	 *            the createTime to set
	 */
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the trTime
	 */
	@Transient
	public String getTrTime() {
		return trTime;
	}

	/**
	 * @param trTime
	 *            the trTime to set
	 */
	public void setTrTime(String trTime) {
		this.trTime = trTime;
	}

	/**
	 * @return the crTIme
	 */
	@Transient
	public String getCrTime() {
		return crTime;
	}

	/**
	 * @param crTIme
	 *            the crTIme to set
	 */
	public void setCrTime(String crTime) {
		this.crTime = crTime;
	}

	/**
	 * @return the owner
	 */
	@Transient
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
	 * @return the accountStatus
	 */
	@Transient
	public int getAccountStatus() {
		return accountStatus;
	}

	/**
	 * @param accountStatus
	 *            the accountStatus to set
	 */
	public void setAccountStatus(int accountStatus) {
		this.accountStatus = accountStatus;
	}

	public BizWechatLog(int id, int fromAccount, Date tradingTime, BigDecimal amount, BigDecimal balance, String remark,
			String summary, String depositor, Date createTime, int status, int wechatRequestId) {
		this.id = id;
		this.fromAccount = fromAccount;
		this.tradingTime = tradingTime;
		this.amount = amount;
		this.balance = balance;
		this.remark = remark;
		this.summary = summary;
		this.depositor = depositor;
		this.createTime = createTime;
		this.status = status;
		this.wechatRequestId = wechatRequestId;
	}

	public BizWechatLog() {

	}

}
