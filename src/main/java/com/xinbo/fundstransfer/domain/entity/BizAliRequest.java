package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class BizAliRequest implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int id;
	private int aliPayid;
	private int level;
	private int handicap;
	private int status;
	private BigDecimal amount;
	private String createTime;
	private String remark;
	private String orderNo;
	private int operator;
	private String memberId;
	private String memberName;
	private String updateTime;
	private String depositor;
	private String handicapName;
	private String account;
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @return the aliPayid
	 */
	public int getAliPayid() {
		return aliPayid;
	}
	/**
	 * @param aliPayid the aliPayid to set
	 */
	public void setAliPayid(int aliPayid) {
		this.aliPayid = aliPayid;
	}
	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}
	/**
	 * @param level the level to set
	 */
	public void setLevel(int level) {
		this.level = level;
	}
	/**
	 * @return the handicap
	 */
	public int getHandicap() {
		return handicap;
	}
	/**
	 * @param handicap the handicap to set
	 */
	public void setHandicap(int handicap) {
		this.handicap = handicap;
	}
	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}
	/**
	 * @return the amount
	 */
	public BigDecimal getAmount() {
		return amount;
	}
	/**
	 * @param amount the amount to set
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	/**
	 * @return the createTime
	 */
	public String getCreateTime() {
		return createTime;
	}
	/**
	 * @param createTime the createTime to set
	 */
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
	/**
	 * @return the remark
	 */
	public String getRemark() {
		return remark;
	}
	/**
	 * @param remark the remark to set
	 */
	public void setRemark(String remark) {
		this.remark = remark;
	}
	/**
	 * @return the orderNo
	 */
	public String getOrderNo() {
		return orderNo;
	}
	/**
	 * @param orderNo the orderNo to set
	 */
	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}
	/**
	 * @return the operator
	 */
	public int getOperator() {
		return operator;
	}
	/**
	 * @param operator the operator to set
	 */
	public void setOperator(int operator) {
		this.operator = operator;
	}
	/**
	 * @return the memberId
	 */
	public String getMemberId() {
		return memberId;
	}
	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	/**
	 * @return the memberName
	 */
	public String getMemberName() {
		return memberName;
	}
	/**
	 * @param memberName the memberName to set
	 */
	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}
	/**
	 * @return the updateTime
	 */
	public String getUpdateTime() {
		return updateTime;
	}
	/**
	 * @param updateTime the updateTime to set
	 */
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}
	/**
	 * @return the depositor
	 */
	public String getDepositor() {
		return depositor;
	}
	/**
	 * @param depositor the depositor to set
	 */
	public void setDepositor(String depositor) {
		this.depositor = depositor;
	}
	/**
	 * @return the handicapName
	 */
	public String getHandicapName() {
		return handicapName;
	}
	/**
	 * @param handicapName the handicapName to set
	 */
	public void setHandicapName(String handicapName) {
		this.handicapName = handicapName;
	}
	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}
	/**
	 * @param account the account to set
	 */
	public void setAccount(String account) {
		this.account = account;
	}
	public BizAliRequest(int id, int aliPayid, int level, int handicap, int status, BigDecimal amount,
			String createTime, String remark, String orderNo, int operator, String memberId, String memberName,
			String updateTime, String depositor, String handicapName, String account) {
		super();
		this.id = id;
		this.aliPayid = aliPayid;
		this.level = level;
		this.handicap = handicap;
		this.status = status;
		this.amount = amount;
		this.createTime = createTime;
		this.remark = remark;
		this.orderNo = orderNo;
		this.operator = operator;
		this.memberId = memberId;
		this.memberName = memberName;
		this.updateTime = updateTime;
		this.depositor = depositor;
		this.handicapName = handicapName;
		this.account = account;
	}
	public BizAliRequest() {
	}
	

}
