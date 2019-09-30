package com.xinbo.fundstransfer.domain.entity;

import java.util.Date;

import javax.persistence.*;

@Entity
@Table(name = "biz_rebate_user")
public class BizRebateUser implements java.io.Serializable {

	private int id;
	private String uid;
	private String userName;
	private String password;
	private String contactor;
	private String contactText;
	private String salesName;
	private String salesld;
	private Integer status;
	private Date createTime;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public int getId() {
		return id;
	}

	//
	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "uid")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Column(name = "user_name")
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Column(name = "password")
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Column(name = "contactor")
	public String getContactor() {
		return contactor;
	}

	public void setContactor(String contactor) {
		this.contactor = contactor;
	}

	@Column(name = "contact_text")
	public String getContactText() {
		return contactText;
	}

	public void setContactText(String contactText) {
		this.contactText = contactText;
	}

	@Column(name = "salesld")
	public String getSalesld() {
		return salesld;
	}

	public void setSalesld(String salesld) {
		this.salesld = salesld;
	}

	@Column(name = "create_time")
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Column(name = "status")
	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Column(name = "salesName")
	public String getSalesName() {
		return salesName;
	}

	public void setSalesName(String salesName) {
		this.salesName = salesName;
	}

}
