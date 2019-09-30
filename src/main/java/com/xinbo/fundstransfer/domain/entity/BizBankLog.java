package com.xinbo.fundstransfer.domain.entity;
// Generated 2017-6-27 10:11:57 by Hibernate Tools 5.2.3.Final

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.*;

import com.xinbo.fundstransfer.domain.enums.BankLogMatchWay;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;

/**
 * BizBankLog generated by hbm2java
 */
@Entity
@Table(name = "biz_bank_log")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizBankLog {

	private Long id;
	private int fromAccount;
	private Date tradingTime;
	private Date createTime;
	private BigDecimal amount;
	private BigDecimal balance;
	private Integer status = BankLogStatus.Matching.getStatus();
	private String remark;
	/** 摘要信息，各银行不一样可能是支付方式/渠道等都放入此字段 */
	private String summary;
	private String toAccount;
	private Date updateTime;
	private BigDecimal commission;

	private String toAccountOwner;
	private Integer fromAccountType;
	private String fromAccountNO;
	private String fromAlias;
	private String fromOwner;
	private String fromBankType;
	private Integer fromCurrSysLevel;
	private String fromCurrSysLevelName;
	private String fromAccountTypeName;
	private String tradingTimeStr;
	private String createTimeStr;
	private String statusStr;
	private String handicapName;
	private String transFerBankName;
	private String transFerHandicap;

	private Long taskId;
	/**
	 * {@cdoe SysBalTrans#TASK_TYPE_INNER} 下发
	 * {@code SysBalTrans#TASK_TYPE_OUTMEMEBER} 出款
	 * {@code SysBalTrans#TASK_TYPE_OUTREBATE} 返利网
	 */
	private Integer taskType;

	private String orderNo;
	/**
	 * 匹配方式 0:流水找单;1:单找流水
	 */
	private int matchWay = BankLogMatchWay.FlowFindOrder.getWay();

	private Integer flag;
	private Integer serial;

	public BizBankLog() {
	}

	public BizBankLog(int fromAccount, Date tradingTime, BigDecimal amount, Integer status, String remark,
			String toAccount) {
		this.fromAccount = fromAccount;
		this.tradingTime = tradingTime;
		this.amount = amount;
		this.status = status;
		this.remark = remark;
		this.toAccount = toAccount;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "from_account")
	public int getFromAccount() {
		return this.fromAccount;
	}

	public void setFromAccount(int fromAccount) {
		this.fromAccount = fromAccount;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "trading_time", length = 19)
	public Date getTradingTime() {
		return this.tradingTime;
	}

	public void setTradingTime(Date tradingTime) {
		this.tradingTime = tradingTime;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "create_time", length = 19)
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Column(name = "amount", precision = 10)
	public BigDecimal getAmount() {
		return this.amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column(name = "status")
	public Integer getStatus() {
		return this.status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Column(name = "remark", length = 100)
	public String getRemark() {
		return this.remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "to_account", length = 32)
	public String getToAccount() {
		return this.toAccount;
	}

	public void setToAccount(String toAccount) {
		this.toAccount = toAccount;
	}

	@Column(name = "to_account_owner", length = 32)
	public String getToAccountOwner() {
		return toAccountOwner;
	}

	public void setToAccountOwner(String toAccountOwner) {
		this.toAccountOwner = toAccountOwner;
	}

	@Column(name = "summary", length = 45)
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Transient
	public String getTradingTimeStr() {
		return tradingTimeStr;
	}

	public void setTradingTimeStr(String tradingTimeStr) {
		this.tradingTimeStr = tradingTimeStr;
	}

	@Transient
	public String getCreateTimeStr() {
		return createTimeStr;
	}

	public void setCreateTimeStr(String createTimeStr) {
		this.createTimeStr = createTimeStr;
	}

	@Override
	public String toString() {
		return new ReflectionToStringBuilder(this).toString();
	}

	@Transient
	public String getStatusStr() {
		BankLogStatus temp = BankLogStatus.findByStatus(getStatus());
		if (null == temp) {
			return null;
		}
		this.statusStr = temp.getMsg();
		return statusStr;
	}

	public void setStatusStr(String statusStr) {
		this.statusStr = statusStr;
	}

	@Transient
	public String getFromAccountNO() {
		return fromAccountNO;
	}

	public void setFromAccountNO(String fromAccountNO) {
		this.fromAccountNO = fromAccountNO;
	}

	@Column(name = "balance", precision = 10)
	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Transient
	public String getFromAlias() {
		return fromAlias;
	}

	public void setFromAlias(String fromAlias) {
		this.fromAlias = fromAlias;
	}

	@Transient
	public String getFromOwner() {
		return fromOwner;
	}

	public void setFromOwner(String fromOwner) {
		this.fromOwner = fromOwner;
	}

	@Transient
	public String getFromBankType() {
		return fromBankType;
	}

	public void setFromBankType(String fromBankType) {
		this.fromBankType = fromBankType;
	}

	@Transient
	public Integer getFromAccountType() {
		return fromAccountType;
	}

	public void setFromAccountType(Integer fromAccountType) {
		this.fromAccountType = fromAccountType;
	}

	@Transient
	public Integer getFromCurrSysLevel() {
		return fromCurrSysLevel;
	}

	public void setFromCurrSysLevel(Integer fromCurrSysLevel) {
		this.fromCurrSysLevel = fromCurrSysLevel;
	}

	@Transient
	public String getFromCurrSysLevelName() {
		return fromCurrSysLevelName;
	}

	public void setFromCurrSysLevelName(String fromCurrSysLevelName) {
		this.fromCurrSysLevelName = fromCurrSysLevelName;
	}

	@Transient
	public String getFromAccountTypeName() {
		return fromAccountTypeName;
	}

	public void setFromAccountTypeName(String fromAccountTypeName) {
		this.fromAccountTypeName = fromAccountTypeName;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "update_time", length = 19)
	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Transient
	public String getHandicapName() {
		return handicapName;
	}

	public void setHandicapName(String handicapName) {
		this.handicapName = handicapName;
	}

	@Transient
	public String getTransFerBankName() {
		return transFerBankName;
	}

	public void setTransFerBankName(String transFerBankName) {
		this.transFerBankName = transFerBankName;
	}

	@Transient
	public String getTransFerHandicap() {
		return transFerHandicap;
	}

	public void setTransFerHandicap(String transFerHandicap) {
		this.transFerHandicap = transFerHandicap;
	}

	public BigDecimal getCommission() {
		return commission;
	}

	@Column(name = "commission", precision = 10)
	public void setCommission(BigDecimal commission) {
		this.commission = commission;
	}

	@Transient
	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	@Transient
	public Integer getTaskType() {
		return taskType;
	}

	public void setTaskType(Integer taskType) {
		this.taskType = taskType;
	}

	@Transient
	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	@Transient
	public int getMatchWay() {
		return matchWay;
	}

	public void setMatchWay(int matchWay) {
		this.matchWay = matchWay;
	}

	@Transient
	public Integer getFlag() {
		return flag;
	}

	public void setFlag(Integer flag) {
		this.flag = flag;
	}

	@Column(name = "serial")
	public Integer getSerial() {
		return serial;
	}

	public void setSerial(Integer serial) {
		this.serial = serial;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BizBankLog other = (BizBankLog) obj;
		if (id != other.getId())
			return false;
		return true;
	}
}
