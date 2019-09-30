package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "biz_other_account", schema = "fundsTransfer")
public class BizOtherAccountEntity {
	private Integer id;
	private String accountNo;
	private String owner;
	private String loginPWD;
	private String payPWD;
	private String creater;
	private String operator;
	private Byte type;
	private Byte ownType;
	private Byte status;
	private Integer handicapId;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String remark;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "account_no")
	public String getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	@Column(name = "owner")
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Column(name = "login_pwd")
	public String getLoginPWD() {
		return loginPWD;
	}

	public void setLoginPWD(String loginPWD) {
		this.loginPWD = loginPWD;
	}

	@Column(name = "pay_pwd")
	public String getPayPWD() {
		return payPWD;
	}

	public void setPayPWD(String payPWD) {
		this.payPWD = payPWD;
	}

	@Column(name = "creater")
	public String getCreater() {
		return creater;
	}

	public void setCreater(String creater) {
		this.creater = creater;
	}

	@Column(name = "operator")
	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	@Column(name = "type")
	public Byte getType() {
		return type;
	}

	public void setType(Byte type) {
		this.type = type;
	}

	@Column(name = "own_type")
	public Byte getOwnType() {
		return ownType;
	}

	public void setOwnType(Byte ownType) {
		this.ownType = ownType;
	}

	@Column(name = "status")
	public Byte getStatus() {
		return status;
	}

	public void setStatus(Byte status) {
		this.status = status;
	}

	@Column(name = "handicap_id")
	public Integer getHandicapId() {
		return handicapId;
	}

	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}

	@Column(name = "create_time")
	public Timestamp getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	@Column(name = "update_time")
	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BizOtherAccountEntity that = (BizOtherAccountEntity) o;
		return id == that.id && ownType == that.ownType && status == that.status
				&& Objects.equals(accountNo, that.accountNo) && Objects.equals(owner, that.owner)
				&& Objects.equals(creater, that.creater) && Objects.equals(operator, that.operator)
				&& Objects.equals(type, that.type) && Objects.equals(handicapId, that.handicapId)
				&& Objects.equals(createTime, that.createTime) && Objects.equals(updateTime, that.updateTime)
				&& Objects.equals(remark, that.remark);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, accountNo, owner, creater, operator, type, ownType, status, handicapId, createTime,
				updateTime, remark);
	}
}
