package com.xinbo.fundstransfer.domain.entity;


import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.*;

/**
 * 账号操作记录存储表
 * @author  2018.3.27
 *
 */
@Entity
@Table(name = "biz_account_extra")
public class BizAccountExtra implements java.io.Serializable {
	
	private Integer id;
	private Integer accountId;
	private String remark;
	private Date time;
	private String operator;
	
	
	private String timeStr;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "account_id")
	public Integer getAccountId() {
		return accountId;
	}
	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	

	@Column(name = "time")
	public Date getTime() {
		return time;
	}
	
	public void setTime(Date time) {
		this.time = time;
		if (null != time) {
			SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			this.setTimeStr(SDF.format(time));
		}
	}
	
	@Transient
	public String getTimeStr() {
		return timeStr;
	}
	public void setTimeStr(String timeStr) {
		this.timeStr = timeStr;
	}
	@Column(name = "operator")
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	
	
}
