package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.*;

@Entity
@Table(name = "biz_sys_log")
public class BizSysLog implements java.io.Serializable, Cloneable {
	private long id;
	// 账号
	private Integer accountId;
	private Long bankLogId;
	private BigDecimal amount;
	private BigDecimal fee;
	private BigDecimal balance;
	// 对方账号
	private Integer oppId;
	private Integer oppHandicap;
	private String oppAccount;
	private String oppOwner;
	// 其它信息
	private Long orderId;
	private String orderNo;
	private Integer type;// SysLogType
	private Integer status;// SysLogStatus
	private Date createTime;
	private Date updateTime;
	private String summary;
	private String remark;
	private Integer operator;
	private BigDecimal bankBalance;
	private Date successTime;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(name = "account_id")
	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	@Column(name = "bank_log_id")
	public Long getBankLogId() {
		return bankLogId;
	}

	public void setBankLogId(Long bankLogId) {
		this.bankLogId = bankLogId;
	}

	@Column(name = "amount")
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column(name = "fee")
	public BigDecimal getFee() {
		return fee;
	}

	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	@Column(name = "balance")
	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Column(name = "opp_id")
	public Integer getOppId() {
		return oppId;
	}

	public void setOppId(Integer oppId) {
		this.oppId = oppId;
	}

	@Column(name = "opp_handicap")
	public Integer getOppHandicap() {
		return oppHandicap;
	}

	public void setOppHandicap(Integer oppHandicap) {
		this.oppHandicap = oppHandicap;
	}

	@Column(name = "opp_account")
	public String getOppAccount() {
		return oppAccount;
	}

	public void setOppAccount(String oppAccount) {
		this.oppAccount = oppAccount;
	}

	@Column(name = "opp_owner")
	public String getOppOwner() {
		return oppOwner;
	}

	public void setOppOwner(String oppOwner) {
		this.oppOwner = oppOwner;
	}

	@Column(name = "order_id")
	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	@Column(name = "order_no")
	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	@Column(name = "type")
	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
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

	@Column(name = "summary")
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "operator")
	public Integer getOperator() {
		return operator;
	}

	public void setOperator(Integer operator) {
		this.operator = operator;
	}

	@Column(name = "bank_balance")
	public BigDecimal getBankBalance() {
		return bankBalance;
	}

	public void setBankBalance(BigDecimal bankBalance) {
		this.bankBalance = bankBalance;
	}

	@Column(name = "success_time")
	public Date getSuccessTime() {
		return successTime;
	}

	public void setSuccessTime(Date successTime) {
		this.successTime = successTime;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BizSysLog other = (BizSysLog) obj;
		if (id != other.getId())
			return false;
		return true;
	}

	public BizSysLog clone() {
		BizSysLog o = null;
		try {
			o = (BizSysLog) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return o;
	}
}
