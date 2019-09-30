package com.xinbo.fundstransfer.domain.entity;
// Generated 2017-6-27 10:11:57 by Hibernate Tools 5.2.3.Final

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * BizOutwardTask generated by hbm2java
 */
@Entity
@Table(name = "biz_outward_task")
@JsonInclude(Include.NON_NULL)
public class BizOutwardTask {

	private Long id;
	private Long outwardRequestId;
	private BigDecimal amount;
	private Date asignTime;
	private Integer timeConsuming;
	private Integer operator;
	private Integer status;
	private Integer accountId;
	private String remark;
	private String screenshot;
	private String toAccount;
	private String toAccountOwner;

	private String handicap;
	private String level;
	private String member;
	private String orderNo;

	private String statusStr;
	private String operatorUid;
	private Integer currSysLevel;
	private Integer outwardPayType;
	/**
	 * 永久人工出款标志
	 */

	private Boolean manualForever;
	/**
	 * 第三方代付
	 */

	private Integer thirdInsteadPay;

	public BizOutwardTask() {
	}

	public void updProperties(Long id, BigDecimal amount, Date asignTime, Integer operator, Integer accountId,
			String remark, int status) {
		this.id = id;
		this.amount = amount;
		this.asignTime = asignTime;
		this.operator = operator;
		this.accountId = accountId;
		this.remark = remark;
		this.status = status;
	}

	// public BizOutwardTask(Long id, Long outwardRequestId, BigDecimal amount,
	// Date
	// asignTime, Integer timeConsuming,
	// Integer operator, int status, Integer accountId, String remark, String
	// screenshot, String toAccount,
	// String toAccountOwner, String handicap, String level, String member,
	// String
	// orderNo) {
	// this.id = id;
	// this.outwardRequestId = outwardRequestId;
	// this.amount = amount;
	// this.asignTime = asignTime;
	// this.timeConsuming = timeConsuming;
	// this.operator = operator;
	// this.status = status;
	// this.accountId = accountId;
	// this.remark = remark;
	// this.screenshot = screenshot;
	// this.toAccount = toAccount;
	// this.toAccountOwner = toAccountOwner;
	// this.handicap = handicap;
	// this.level = level;
	// this.member = member;
	// this.orderNo = orderNo;
	// }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "outward_request_id")
	public Long getOutwardRequestId() {
		return this.outwardRequestId;
	}

	public void setOutwardRequestId(Long outwardRequestId) {
		this.outwardRequestId = outwardRequestId;
	}

	@Column(name = "amount", precision = 10)
	public BigDecimal getAmount() {
		return this.amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "asign_time", length = 19)
	public Date getAsignTime() {
		return this.asignTime;
	}

	public void setAsignTime(Date asignTime) {
		this.asignTime = asignTime;
	}

	@Column(name = "time_consuming")
	public Integer getTimeConsuming() {
		return this.timeConsuming;
	}

	public void setTimeConsuming(Integer timeConsuming) {
		this.timeConsuming = timeConsuming;
	}

	@Column(name = "operator")
	public Integer getOperator() {
		return this.operator;
	}

	public void setOperator(Integer operator) {
		this.operator = operator;
	}

	@Column(name = "status")
	public Integer getStatus() {
		return this.status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return new ReflectionToStringBuilder(this).toString();
	}

	@Column(name = "account_id")
	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	@Column(name = "remark", length = 1000)
	public String getRemark() {
		return this.remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "screenshot", length = 100)
	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	@Column(name = "to_account")
	public String getToAccount() {
		return toAccount;
	}

	public void setToAccount(String toAccount) {
		this.toAccount = toAccount;
	}

	@Column(name = "to_account_owner")
	public String getToAccountOwner() {
		return toAccountOwner;
	}

	public void setToAccountOwner(String toAccountOwner) {
		this.toAccountOwner = toAccountOwner;
	}

	@Column(name = "handicap")
	public String getHandicap() {
		return handicap;
	}

	public void setHandicap(String handicap) {
		this.handicap = handicap;
	}

	@Column(name = "level")
	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	@Column(name = "member")
	public String getMember() {
		return member;
	}

	public void setMember(String member) {
		this.member = member;
	}

	@Column(name = "order_no")
	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	@Column(name = "third_instead_pay")
	public Integer getThirdInsteadPay() {
		return thirdInsteadPay;
	}

	public void setThirdInsteadPay(Integer thirdInsteadPay) {
		this.thirdInsteadPay = thirdInsteadPay;
	}

	@Column(name = "curr_sys_level")
	public Integer getCurrSysLevel() {
		return currSysLevel;
	}

	public void setCurrSysLevel(Integer currSysLevel){
		this.currSysLevel = currSysLevel;
	}

	@Column(name = "outward_pay_type")
	public Integer getOutwardPayType() {
		return outwardPayType;
	}

	public void setOutwardPayType(Integer outwardPayType){
		this.outwardPayType = outwardPayType;
	}

	@Transient
	public String getStatusStr() {
		return statusStr;
	}

	public void setStatusStr(String statusStr) {
		this.statusStr = statusStr;
	}

	@Transient
	public String getOperatorUid() {
		return operatorUid;
	}

	public void setOperatorUid(String operatorUid) {
		this.operatorUid = operatorUid;
	}

	@Transient
	public Boolean getManualForever() {
		return manualForever;
	}

	public void setManualForever(Boolean manualForever) {
		this.manualForever = manualForever;
	}
}
