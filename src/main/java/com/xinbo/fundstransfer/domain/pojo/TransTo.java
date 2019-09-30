package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;
import java.util.Date;

public class TransTo {
	private int STATUS_ALLOC = TransLock.STATUS_ALLOC;
	private int STATUS_CLAIM = TransLock.STATUS_CLAIM;
	private int STATUS_ACK = TransLock.STATUS_ACK;
	private int STATUS_DEL = TransLock.STATUS_DEL;

	private Integer frId;
	private Integer frHandicapId;
	private String frHandicapName;
	private Integer frType;
	private Integer frCsl;
	private String frAlias;
	private String frAcc;
	private String frBankType;
	private String frOwner;

	private Integer toId;
	private Integer toHandicapId;
	private String toHandiCapName;
	private Integer toType;
	private Integer toCsl;
	private String toAlias;
	private String toAcc;
	private String toBankType;
	private String toOwner;

	private String orderNo;

	private BigDecimal transAmt;
	private Date ltTime;
	private Integer status;
	private String statusMsg;

	private Date createTime;
	private Integer priority;

	private String remark;

	private String timeConsume;
	
	private String username;

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getCreateTime() {
		return createTime;
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

	public Integer getFrHandicapId() {
		return frHandicapId;
	}

	public void setFrHandicapId(Integer frHandicapId) {
		this.frHandicapId = frHandicapId;
	}

	public String getFrHandicapName() {
		return frHandicapName;
	}

	public void setFrHandicapName(String frHandicapName) {
		this.frHandicapName = frHandicapName;
	}

	public Integer getToHandicapId() {
		return toHandicapId;
	}

	public void setToHandicapId(Integer toHandicapId) {
		this.toHandicapId = toHandicapId;
	}

	public String getToHandiCapName() {
		return toHandiCapName;
	}

	public void setToHandiCapName(String toHandiCapName) {
		this.toHandiCapName = toHandiCapName;
	}

	public void setFrAlias(String frAlias) {
		this.frAlias = frAlias;
	}

	public void setToAlias(String toAlias) {
		this.toAlias = toAlias;
	}

	public void setFrBankType(String frBankType) {
		this.frBankType = frBankType;
	}

	public void setToBankType(String toBankType) {
		this.toBankType = toBankType;
	}

	public String getFrAlias() {
		return frAlias;
	}

	public String getToAlias() {
		return toAlias;
	}

	public String getFrBankType() {
		return frBankType;
	}

	public String getToBankType() {
		return toBankType;
	}

	public TransTo() {
	}

	public Integer getFrId() {
		return frId;
	}

	public void setFrId(Integer frId) {
		this.frId = frId;
	}

	public String getFrAcc() {
		return frAcc;
	}

	public void setFrAcc(String frAcc) {
		this.frAcc = frAcc;
	}

	public Integer getToId() {
		return toId;
	}

	public void setToId(Integer toId) {
		this.toId = toId;
	}

	public BigDecimal getTransAmt() {
		return transAmt;
	}

	public void setTransAmt(BigDecimal transAmt) {
		this.transAmt = transAmt;
	}

	public Date getLtTime() {
		return ltTime;
	}

	public void setLtTime(Date ltTime) {
		this.ltTime = ltTime;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getFrType() {
		return frType;
	}

	public void setFrType(Integer frType) {
		this.frType = frType;
	}

	public Integer getFrCsl() {
		return frCsl;
	}

	public void setFrCsl(Integer frCsl) {
		this.frCsl = frCsl;
	}

	public Integer getToCsl() {
		return toCsl;
	}

	public void setToCsl(Integer toCsl) {
		this.toCsl = toCsl;
	}

	public Integer getToType() {
		return toType;
	}

	public void setToType(Integer toType) {
		this.toType = toType;
	}

	public String getStatusMsg() {
		return statusMsg;
	}

	public String getToAcc() {
		return toAcc;
	}

	public void setToAcc(String toAcc) {
		this.toAcc = toAcc;
	}

	public void setStatusMsg(String statusMsg) {
		this.statusMsg = statusMsg;
	}

	public String getFrOwner() {
		return frOwner;
	}

	public void setFrOwner(String frOwner) {
		this.frOwner = frOwner;
	}

	public String getToOwner() {
		return toOwner;
	}

	public void setToOwner(String toOwner) {
		this.toOwner = toOwner;
	}

	public String getTimeConsume() {
		return timeConsume;
	}

	public void setTimeConsume(String timeConsume) {
		this.timeConsume = timeConsume;
	}

	public int getSTATUS_ALLOC() {
		return STATUS_ALLOC;
	}

	public int getSTATUS_CLAIM() {
		return STATUS_CLAIM;
	}

	public int getSTATUS_ACK() {
		return STATUS_ACK;
	}

	public int getSTATUS_DEL() {
		return STATUS_DEL;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
}
