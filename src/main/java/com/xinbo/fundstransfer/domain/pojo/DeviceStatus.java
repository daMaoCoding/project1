package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceStatus implements Serializable {

	private static final long serialVersionUID = 7078741399908209580L;

	/**
	 * 当前账号ID
	 */
	private Integer id;
	/**
	 * 工具版本
	 */
	private String appVersion;
	/**
	 * 银行APP版本 ex:中国银行5.1.2
	 */
	private String bankAppVersion;
	/**
	 * 银行APP安装渠道-应用市场-本地安装
	 */
	private String installer;
	/**
	 * 手机型号 HUAWEI LDN-LX2
	 */
	private String model;
	/**
	 * 分辨率 1080*2040
	 */
	private String resolution;
	/**
	 * 电量
	 */
	private String battery;
	/**
	 * 网络类型WiFi、4G
	 */
	private String netType;
	/**
	 * 网络质量
	 */
	private String netLevel;
	/**
	 * 工具异常信息集合(模拟点击屏幕无效/银行助手未开启等) 目前有的key：电量、密码、余额、屏幕、冻结、限额、短信、app状态、对账、设备、其他
	 * 没有错误信息，传输的是内容是{},后台可以做处理，如果有错误，就会有对应的key和错误信息作为value
	 */
	private Map<String, String> errMsgMap;
	private String errMsg;

	/**
	 * 发生时间
	 */
	private Long errTime;

	/**
	 * 加密后的设备唯一编号
	 */
	private String iMEICode;
	/**
	 * 锁定时间
	 */
	private Long lockTime;
	/**
	 * 解决时间
	 */
	private Long solveTime;
	/**
	 * 操作人
	 */
	private String operator;
	/**
	 * 返利网用户
	 */
	private String rebateUser;
	/**
	 * 手机号
	 */
	private String mobile;
	/**
	 * 状态 errMsgMap 中有值为异常 否则为正常 0 异常 1 正常
	 */
	private Integer status;
	/**
	 * 处理状态 0 待处理 1 处理中 2 处理完成
	 */
	private Integer dealStatus;
	/**
	 * 电量状态 0 异常 1 正常
	 */
	private Integer batteryStatus;
	/**
	 * 是否锁定 0 未锁定 1 已锁定
	 */
	private Integer lockStatus;
	/**
	 * 在线离线 0在线1离线
	 */
	private String offLineStatus;

	private String bankType;
	private String owner;
	private String account;
	private String alias;
	private String appPatchVersion;
	private String remark;
	/**
	 * 返利网用户名
	 */
	private String userName;
	// 处理耗时(异常情况被锁定时才会有处理耗时,其余情况为空)
	private Long dealTime;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getBankAppVersion() {
		return bankAppVersion;
	}

	public void setBankAppVersion(String bankAppVersion) {
		this.bankAppVersion = bankAppVersion;
	}

	public String getInstaller() {
		return installer;
	}

	public void setInstaller(String installer) {
		this.installer = installer;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getBattery() {
		return battery;
	}

	public void setBattery(String battery) {
		this.battery = battery;
	}

	public String getNetType() {
		return netType;
	}

	public void setNetType(String netType) {
		this.netType = netType;
	}

	public String getNetLevel() {
		return netLevel;
	}

	public void setNetLevel(String netLevel) {
		this.netLevel = netLevel;
	}

	public Map<String, String> getErrMsgMap() {
		return errMsgMap;
	}

	public void setErrMsgMap(Map<String, String> errMsgMap) {
		this.errMsgMap = errMsgMap;
	}

	public void setErrTime(Long errTime) {
		this.errTime = errTime;
	}

	public Long getErrTime() {
		return errTime;
	}

	public String getiMEICode() {
		return iMEICode;
	}

	public void setiMEICode(String iMEICode) {
		this.iMEICode = iMEICode;
	}

	public Long getLockTime() {
		return lockTime;
	}

	public Long getSolveTime() {
		return solveTime;
	}

	public void setLockTime(Long lockTime) {
		this.lockTime = lockTime;
	}

	public void setSolveTime(Long solveTime) {
		this.solveTime = solveTime;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getRebateUser() {
		return rebateUser;
	}

	public void setRebateUser(String rebateUser) {
		this.rebateUser = rebateUser;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public void setDealStatus(Integer dealStatus) {
		this.dealStatus = dealStatus;
	}

	public void setBatteryStatus(Integer batteryStatus) {
		this.batteryStatus = batteryStatus;
	}

	public Integer getStatus() {
		return status;
	}

	public Integer getDealStatus() {
		return dealStatus;
	}

	public Integer getBatteryStatus() {
		return batteryStatus;
	}

	public String getErrMsg() {
		return errMsg;
	}

	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}

	public Integer getLockStatus() {
		return lockStatus;
	}

	public void setLockStatus(Integer lockStatus) {
		this.lockStatus = lockStatus;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getAppPatchVersion() {
		return appPatchVersion;
	}

	public void setAppPatchVersion(String appPatchVersion) {
		this.appPatchVersion = appPatchVersion;
	}

	public void setRemark(String remark){
		this.remark = remark;
	}

	public String getRemark(){
		return remark;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getOffLineStatus() {
		return offLineStatus;
	}

	public void setOffLineStatus(String offLineStatus) {
		this.offLineStatus = offLineStatus;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Long getDealTime() {
		return dealTime;
	}

	public void setDealTime(Long dealTime) {
		this.dealTime = dealTime;
	}
}