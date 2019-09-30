package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "biz_account_return_summary")
public class BizAccountReturnSummary {

	private Long id;
	private Integer account;
	private BigDecimal totalAmount;
	private BigDecimal amount;
	private Date createTime;
	private String calcTime;
	private String remark;
	private int status;
	private BigDecimal activityAmount;
	private BigDecimal balance;
	private String uid;
	private BigDecimal agentAmount;
	private BigDecimal totalAgentAmount;
	private List<BizAccountReturnSummary> returnSummary;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "account")
	public Integer getAccount() {
		return account;
	}

	public void setAccount(Integer account) {
		this.account = account;
	}

	@Column(name = "total_amount")
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	@Column(name = "amount")
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column(name = "create_time")
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Column(name = "calc_time")
	public String getCalcTime() {
		return calcTime;
	}

	public void setCalcTime(String calcTime) {
		this.calcTime = calcTime;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "status")
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@Column(name = "activity_amount")
	public BigDecimal getActivityAmount() {
		return activityAmount;
	}

	public void setActivityAmount(BigDecimal activityAmount) {
		this.activityAmount = activityAmount;
	}

	@Column(name = "balance")
	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Column(name = "uid")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Column(name = "agent_amount")
	public BigDecimal getAgentAmount() {
		return agentAmount;
	}

	public void setAgentAmount(BigDecimal agentAmount) {
		this.agentAmount = agentAmount;
	}

	@Transient
	public List<BizAccountReturnSummary> getReturnSummary() {
		return returnSummary;
	}

	public void setReturnSummary(List<BizAccountReturnSummary> returnSummary) {
		this.returnSummary = returnSummary;
	}

	@Column(name = "total_agent_amount")
	public BigDecimal getTotalAgentAmount() {
		return totalAgentAmount;
	}

	public void setTotalAgentAmount(BigDecimal totalAgentAmount) {
		this.totalAgentAmount = totalAgentAmount;
	}

}
