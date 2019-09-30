package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "biz_sys_invst")
public class BizSysInvst {

	private Long id;
	private Integer accountId;
	private Long bankLogId;
	private BigDecimal amount;
	private BigDecimal balance;
	private BigDecimal bankBalance;
	private Integer oppId;
	private Integer oppHandicap;
	private String oppAccount;
	private String oppOwner;
	private Long errorId;
	private String batchNo;
	private Long orderId;
	private String orderNo;
	private Integer type;
	private String summary;
	private String remark;
	private Integer confirmer;
	private Date createTime;
	private Date occurTime;
	private Long consumeMillis;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	@Column(name = "balance")
	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Column(name = "bank_balance")
	public BigDecimal getBankBalance() {
		return bankBalance;
	}

	public void setBankBalance(BigDecimal bankBalance) {
		this.bankBalance = bankBalance;
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

	@Column(name = "error_id")
	public Long getErrorId() {
		return errorId;
	}

	public void setErrorId(Long errorId) {
		this.errorId = errorId;
	}

	@Column(name = "batch_no")
	public String getBatchNo() {
		return batchNo;
	}

	public void setBatchNo(String batchNo) {
		this.batchNo = batchNo;
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

	@Column(name = "confirmer")
	public Integer getConfirmer() {
		return confirmer;
	}

	public void setConfirmer(Integer confirmer) {
		this.confirmer = confirmer;
	}

	@Column(name = "create_time")
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Column(name = "occur_time")
	public Date getOccurTime() {
		return occurTime;
	}

	public void setOccurTime(Date occurTime) {
		this.occurTime = occurTime;
	}

	@Column(name = "consume_millis")
	public Long getConsumeMillis() {
		return consumeMillis;
	}

	public void setConsumeMillis(Long consumeMillis) {
		this.consumeMillis = consumeMillis;
	}

	private String confirmerName;
	private String consumeStr;

	@Transient
	public String getConfirmerName() {
		return confirmerName;
	}

	public void setConfirmerName(String confirmerName) {
		this.confirmerName = confirmerName;
	}

	@Transient
	public String getConsumeStr() {
		return consumeStr;
	}

	public void setConsumeStr(String consumeStr) {
		this.consumeStr = consumeStr;
	}
}
