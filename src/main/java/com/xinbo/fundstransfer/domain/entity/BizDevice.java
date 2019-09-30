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
 * 终端类（抓取支付宝，微信）
 * @author Administrator
 *
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "biz_device")
public class BizDevice implements java.io.Serializable{
	
	/** ID */
	private Integer id;
	/** 设备持有人 */
	private String owner;
	/** 设备持有人电话 */
	private String ownerPhoneNo;
	/** 设备当前手机号 */
	private String mobileNo;
	/** 电量 */
	private String battery;
	/** 信号 */
	private String signal;
	/** 设备号（SN） */
	private String deviceId;
	/** 设备名 */
	private String deviceName;
	/** GPS */
	private String gps;
	/** 微信 */
	private String wechat;
	/** 支付宝 */
	private String alipay;
	/** 更新时间 */
	private Date updateTime;
	

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
	@Column(name = "owner")
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	@Column(name = "owner_phone_no")
	public String getOwnerPhoneNo() {
		return ownerPhoneNo;
	}
	public void setOwnerPhoneNo(String ownerPhoneNo) {
		this.ownerPhoneNo = ownerPhoneNo;
	}
	@Column(name = "mobile_no")
	public String getMobileNo() {
		return mobileNo;
	}
	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}
	@Column(name = "battery")
	public String getBattery() {
		return battery;
	}
	public void setBattery(String battery) {
		this.battery = battery;
	}
	@Column(name = "signal")
	public String getSignal() {
		return signal;
	}
	public void setSignal(String signal) {
		this.signal = signal;
	}
	@Column(name = "device_id")
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	@Column(name = "device_name")
	public String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	@Column(name = "gps")
	public String getGps() {
		return gps;
	}
	public void setGps(String gps) {
		this.gps = gps;
	}
	@Column(name = "wechat")
	public String getWechat() {
		return wechat;
	}
	public void setWechat(String wechat) {
		this.wechat = wechat;
	}
	@Column(name = "alipay")
	public String getAlipay() {
		return alipay;
	}
	public void setAlipay(String alipay) {
		this.alipay = alipay;
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
	
	@Transient
	public String getUpdateTimeStr() {
		return updateTimeStr;
	}
	public void setUpdateTimeStr(String updateTimeStr) {
		this.updateTimeStr = updateTimeStr;
	}
	
	
	
}
