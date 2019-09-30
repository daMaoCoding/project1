package com.xinbo.fundstransfer.domain.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * 与平台同步信息存储表
 * @author Administrator
 *
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "biz_account_sync")
public class BizAccountSync implements java.io.Serializable{
	
	/** ID */
	private Integer id;
	/** 账号ID */
	private Integer accountId;
	/** 同步的Json字符串 */
	private String json;
	/** 更新时间 */
	private Date updateTime;
	/** 备注 */
	private String remark;
	/** 更新人 */
	private String operator;
	/** 二维码 */
	private Byte[] qrImage;

	private String updateTimeStr;
	
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
	@Column(name = "json")
	public String getJson() {
		return json;
	}
	public void setJson(String json) {
		this.json = json;
	}
	@Column(name = "update_time")
	public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
		if (null != updateTime) {
			SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			this.setUpdateTimeStr(SDF.format(updateTime));
		}
	}
	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	@Column(name = "operator")
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	@Column(name = "qr_image")
	public Byte[] getQrImage() {
		return qrImage;
	}
	public void setQrImage(Byte[] qrImage) {
		this.qrImage = qrImage;
	}
	
	@Transient
	public String getUpdateTimeStr() {
		return updateTimeStr;
	}
	public void setUpdateTimeStr(String updateTimeStr) {
		this.updateTimeStr = updateTimeStr;
	}
	
}
