package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;
import java.util.Date;

public class TransRec {
	private Long id;
	private int req0OTask1;
	private Integer handicapId;
	private String handicapCode;
	private String capture;
	private BigDecimal amount;
	private String orderNo;
	private String member;
	private String remark;
	private Date transTime;
	private Integer fromId;
	private String fromBankType;
	private String fromAccount;
	private String fromOwner;
	private String fromAlias;
	private Integer fromCurrSysLevel;
	private String fromCurrSysLevelName;
	private Integer fromType;
	private String fromTypeName;
	private Integer toId;
	private String toBankType;
	private String toAccount;
	private String toOwner;
	private String toAlias;
	private String operator;

	public TransRec() {
	}

	public TransRec(Long id, int req0OTask1, Integer handicapId, String handicapCode, String capture, BigDecimal amount,
			String orderNo, String remark, Date transTime, Integer fromId, String fromBankType, String fromAccount,
			String fromOwner, String fromAlias, Integer toId, String toBankType, String toAccount, String toOwner,
			String toAlias, String member, String operator) {
		this.id = id;
		this.req0OTask1 = req0OTask1;
		this.handicapId = handicapId;
		this.handicapCode = handicapCode;
		this.capture = capture;
		this.amount = amount;
		this.orderNo = orderNo;
		this.remark = remark;
		this.transTime = transTime;
		this.fromId = fromId;
		this.fromBankType = fromBankType;
		this.fromAccount = fromAccount;
		this.fromOwner = fromOwner;
		this.fromAlias = fromAlias;
		this.toId = toId;
		this.toBankType = toBankType;
		this.toAccount = toAccount;
		this.toOwner = toOwner;
		this.toAlias = toAlias;
		this.member = member;
		this.operator = operator;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getReq0OTask1() {
		return req0OTask1;
	}

	public void setReq0OTask1(int req0OTask1) {
		this.req0OTask1 = req0OTask1;
	}

	public Integer getHandicapId() {
		return handicapId;
	}

	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}

	public String getHandicapCode() {
		return handicapCode;
	}

	public void setHandicapCode(String handicapCode) {
		this.handicapCode = handicapCode;
	}

	public String getCapture() {
		return capture;
	}

	public void setCapture(String capture) {
		this.capture = capture;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Date getTransTime() {
		return transTime;
	}

	public void setTransTime(Date transTime) {
		this.transTime = transTime;
	}

	public Integer getFromId() {
		return fromId;
	}

	public void setFromId(Integer fromId) {
		this.fromId = fromId;
	}

	public String getFromBankType() {
		return fromBankType;
	}

	public void setFromBankType(String fromBankType) {
		this.fromBankType = fromBankType;
	}

	public String getFromAccount() {
		return fromAccount;
	}

	public void setFromAccount(String fromAccount) {
		this.fromAccount = fromAccount;
	}

	public String getFromOwner() {
		return fromOwner;
	}

	public void setFromOwner(String fromOwner) {
		this.fromOwner = fromOwner;
	}

	public String getFromAlias() {
		return fromAlias;
	}

	public void setFromAlias(String fromAlias) {
		this.fromAlias = fromAlias;
	}

	public Integer getToId() {
		return toId;
	}

	public void setToId(Integer toId) {
		this.toId = toId;
	}

	public String getToBankType() {
		return toBankType;
	}

	public void setToBankType(String toBankType) {
		this.toBankType = toBankType;
	}

	public String getToAccount() {
		return toAccount;
	}

	public void setToAccount(String toAccount) {
		this.toAccount = toAccount;
	}

	public String getToOwner() {
		return toOwner;
	}

	public void setToOwner(String toOwner) {
		this.toOwner = toOwner;
	}

	public String getToAlias() {
		return toAlias;
	}

	public void setToAlias(String toAlias) {
		this.toAlias = toAlias;
	}

	public String getMember() {
		return member;
	}

	public void setMember(String member) {
		this.member = member;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public Integer getFromCurrSysLevel() {
		return fromCurrSysLevel;
	}

	public void setFromCurrSysLevel(Integer fromCurrSysLevel) {
		this.fromCurrSysLevel = fromCurrSysLevel;
	}

	public String getFromCurrSysLevelName() {
		return fromCurrSysLevelName;
	}

	public void setFromCurrSysLevelName(String fromCurrSysLevelName) {
		this.fromCurrSysLevelName = fromCurrSysLevelName;
	}

	public Integer getFromType() {
		return fromType;
	}

	public void setFromType(Integer fromType) {
		this.fromType = fromType;
	}

	public String getFromTypeName() {
		return fromTypeName;
	}

	public void setFromTypeName(String fromTypeName) {
		this.fromTypeName = fromTypeName;
	}
}
