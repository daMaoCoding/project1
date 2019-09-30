package com.xinbo.fundstransfer.domain.entity;

import java.util.Date;

import javax.persistence.*;

@Entity
@Table(name = "biz_black_list")
public class BizBlackList implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Integer id;
	private Integer handicapId;
	private String name;
	private String memberCode;
	private String account;
	private String operator;
	private Date createTime;
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

	@Column(name = "name", length = 45)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "operator", length = 45)
	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	@Column(name = "account", length = 45)
	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	@Column(name = "create_time")
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "handicap_id")
	public Integer getHandicapId() {
		return handicapId;
	}

	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}

	@Column(name = "member_code")
	public String getMemberCode() {
		return memberCode;
	}

	public void setMemberCode(String memberCode) {
		this.memberCode = memberCode;
	}
	
	
}
