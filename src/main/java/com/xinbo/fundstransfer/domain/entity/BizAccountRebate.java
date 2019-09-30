package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "biz_account_rebate")
public class BizAccountRebate {
	private Long id;
	private String uid;
	private String tid;
	private Integer accountId;
	private String toAccount;
	private String toHolder;
	private String toAccountType;
	private String toAccountInfo;
	private BigDecimal amount;
	private BigDecimal balance;
	private Integer status;
	private Date createTime;
	private Date updateTime;
	private String remark;
	private String outAccount;
	private String outPerson;
	private Integer handicap;
	private Integer operator;
	private String screenshot;
	private String createTimeStr;
	private String updateTimeStr;
	private Date asignTime;
	private String asignTimeStr;
	private String differenceTime;
	private String handicapName;
	private Integer differenceMinutes;
	private String accountAlias;
	private Integer type;
	private String uName;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "uid")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Column(name = "tid")
	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}

	@Column(name = "to_account")
	public String getToAccount() {
		return toAccount;
	}

	public void setToAccount(String toAccount) {
		this.toAccount = toAccount;
	}

	@Column(name = "to_holder")
	public String getToHolder() {
		return toHolder;
	}

	public void setToHolder(String toHolder) {
		this.toHolder = toHolder;
	}

	@Column(name = "to_account_type")
	public String getToAccountType() {
		return toAccountType;
	}

	public void setToAccountType(String toAccountType) {
		this.toAccountType = toAccountType;
	}

	@Column(name = "to_account_info")
	public String getToAccountInfo() {
		return toAccountInfo;
	}

	public void setToAccountInfo(String toAccountInfo) {
		this.toAccountInfo = toAccountInfo;
	}

	@Column(name = "amount")
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column(name = "balance")
	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Column(name = "status")
	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Column(name = "create_time")
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Column(name = "update_time")
	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "account_id")
	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	@Transient
	public String getOutAccount() {
		return outAccount;
	}

	public void setOutAccount(String outAccount) {
		this.outAccount = outAccount;
	}

	@Transient
	public String getOutPerson() {
		return outPerson;
	}

	public void setOutPerson(String outPerson) {
		this.outPerson = outPerson;
	}

	@Column(name = "handicap")
	public Integer getHandicap() {
		return handicap;
	}

	public void setHandicap(Integer handicap) {
		this.handicap = handicap;
	}

	@Column(name = "operator")
	public Integer getOperator() {
		return operator;
	}

	public void setOperator(Integer operator) {
		this.operator = operator;
	}

	@Column(name = "screenshot", length = 100)
	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	@Transient
	public String getCreateTimeStr() {
		return createTimeStr;
	}

	public void setCreateTimeStr(String createTimeStr) {
		this.createTimeStr = createTimeStr;
	}

	@Transient
	public String getUpdateTimeStr() {
		return updateTimeStr;
	}

	public void setUpdateTimeStr(String updateTimeStr) {
		this.updateTimeStr = updateTimeStr;
	}

	@Column(name = "asign_time")
	public Date getAsignTime() {
		return asignTime;
	}

	public void setAsignTime(Date asignTime) {
		this.asignTime = asignTime;
	}

	@Transient
	public String getAsignTimeStr() {
		return asignTimeStr;
	}

	public void setAsignTimeStr(String asignTimeStr) {
		this.asignTimeStr = asignTimeStr;
	}

	@Transient
	public String getDifferenceTime() {
		return differenceTime;
	}

	public void setDifferenceTime(String differenceTime) {
		this.differenceTime = differenceTime;
	}

	@Transient
	public String getHandicapName() {
		return handicapName;
	}

	public void setHandicapName(String handicapName) {
		this.handicapName = handicapName;
	}

	@Transient
	public Integer getDifferenceMinutes() {
		return differenceMinutes;
	}

	public void setDifferenceMinutes(Integer differenceMinutes) {
		this.differenceMinutes = differenceMinutes;
	}

	@Transient
	public String getAccountAlias() {
		return accountAlias;
	}

	public void setAccountAlias(String accountAlias) {
		this.accountAlias = accountAlias;
	}

	@Column(name = "type")
	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	@Transient
	public String getuName() {
		return uName;
	}

	public void setuName(String uName) {
		this.uName = uName;
	}

}
