/**
 * 
 */
package com.xinbo.fundstransfer.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Blake
 *
 */
@Entity
@Table(name = "biz_account_fee")
public class AccountFee implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3682195554198768258L;
	
	@Id
	@Column(name = "id",updatable = false)
	private Integer id;
	@Column(name = "handicap_id",updatable = false)
	private Integer handicapId;
	@Column(name = "account_id",updatable = false)
	private Integer accountId;
	@Column(name = "fee_type")
	private Byte feeType;
	@Column(name = "cal_fee_type")
	private Byte calFeeType;
	@Column(name = "cal_fee_percent", precision = 4)
	private BigDecimal calFeePercent;
	@Column(name = "cal_fee_level_type")
	private Byte calFeeLevelType;
	@Column(name = "cal_fee_level_money")
	private String calFeeLevelMoney;
	@Column(name = "cal_fee_level_percent")
	private String calFeeLevelPercent;
	@Column(name = "create_name",updatable = false)
	private String createName;
	@Column(name = "create_time",updatable = false)
	private Timestamp createTime;
	@Column(name = "update_name")
	private String updateName;
	@Column(name = "update_time")
	private Timestamp updateTime;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getHandicapId() {
		return handicapId;
	}
	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}
	public Integer getAccountId() {
		return accountId;
	}
	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}
	public Byte getFeeType() {
		return feeType;
	}
	public void setFeeType(Byte feeType) {
		this.feeType = feeType;
	}
	public Byte getCalFeeType() {
		return calFeeType;
	}
	public void setCalFeeType(Byte calFeeType) {
		this.calFeeType = calFeeType;
	}
	public BigDecimal getCalFeePercent() {
		return calFeePercent;
	}
	public void setCalFeePercent(BigDecimal calFeePercent) {
		this.calFeePercent = calFeePercent;
	}
	public Byte getCalFeeLevelType() {
		return calFeeLevelType;
	}
	public void setCalFeeLevelType(Byte calFeeLevelType) {
		this.calFeeLevelType = calFeeLevelType;
	}
	public String getCalFeeLevelMoney() {
		return calFeeLevelMoney;
	}
	public void setCalFeeLevelMoney(String calFeeLevelMoney) {
		this.calFeeLevelMoney = calFeeLevelMoney;
	}
	public String getCalFeeLevelPercent() {
		return calFeeLevelPercent;
	}
	public void setCalFeeLevelPercent(String calFeeLevelPercent) {
		this.calFeeLevelPercent = calFeeLevelPercent;
	}
	
	public String getCreateName() {
		return createName;
	}
	public void setCreateName(String createName) {
		this.createName = createName;
	}
	public Timestamp getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}
	public String getUpdateName() {
		return updateName;
	}
	public void setUpdateName(String updateName) {
		this.updateName = updateName;
	}
	public Timestamp getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}
}
