package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "biz_bank_log_commission")
public class BizBankLogCommission {
	private Long id;
	private Long bankLogId;
	private Long returnSummaryId;
	private Integer accountId;
	private String uid;
	private String calcTime;
	private String commission;
	private BigDecimal amount;
	private Integer flwActivity;

	public BizBankLogCommission() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "bank_log_id")
	public Long getBankLogId() {
		return bankLogId;
	}

	public void setBankLogId(Long bankLogId) {
		this.bankLogId = bankLogId;
	}

	@Column(name = "return_summary_id")
	public Long getReturnSummaryId() {
		return returnSummaryId;
	}

	public void setReturnSummaryId(Long returnSummaryId) {
		this.returnSummaryId = returnSummaryId;
	}

	@Column(name = "account_id")
	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	@Column(name = "uid")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Column(name = "calc_time")
	public String getCalcTime() {
		return calcTime;
	}

	public void setCalcTime(String calcTime) {
		this.calcTime = calcTime;
	}

	@Column(name = "commission")
	public String getCommission() {
		return commission;
	}

	public void setCommission(String commission) {
		this.commission = commission;
	}

	@Column(name = "amount")
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column(name = "flw_activity")
	public Integer getFlwActivity() {
		return flwActivity;
	}

	public void setFlwActivity(Integer flwActivity) {
		this.flwActivity = flwActivity;
	}
}
