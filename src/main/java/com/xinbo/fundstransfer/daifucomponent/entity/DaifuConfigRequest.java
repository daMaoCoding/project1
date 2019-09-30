package com.xinbo.fundstransfer.daifucomponent.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "biz_daifu_config")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DaifuConfigRequest implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6846223130791338606L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	
	@Column(name = "handicap")
	private Integer handicapId;

	@Column(name = "channel_name")
	private String channelName;
	
	@Column(name = "third_id")
	private String thirdId;
	
	@Column(name = "member_id")
	private String memberId;
	
	@Column(name = "alias_name")
	private String aliasName;
	
	@Column(name = "platform_status")
	private Byte platformStatus;

	@Column(name = "platform_delete_flag")
	private Byte platformDeleteFlag;
	
	@Column(name = "crk_status")
	private Byte crkStatus;
	
	@Column(name = "platform_out_money")
	private BigDecimal platformOutMoney;
	
	@Column(name = "crk_out_money_history")
	private BigDecimal crkOutMoneyHistory;
	
	@Column(name = "platform_out_times")
	private Integer platformOutTimes;

	@Column(name = "crk_out_money")
	private BigDecimal crkOutMoney;
	
	@Column(name = "crk_out_times")
	private Integer crkOutTimes;
	
	@Column(name = "crk_out_times_history")
	private Integer crkOutTimesHistory;
	
	@Column( name = "config")
	private String config;
	@Column( name = "bank_config")
	private String bankConfig;
	@Column( name = "warn_config")
	private String warnConfig;
	@Column( name = "fee_config")
	private String feeConfig;
	@Column( name = "out_config_set")
	private String outConfigSet;
	@Column( name = "level_config")
	private String levelConfig;
	
	@Column(name = "sync_time")
	private Timestamp syncTime;
	
	@Column(name = "create_time")
	private Timestamp createTime;

	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the handicap
	 */
	public Integer getHandicapId() {
		return handicapId;
	}

	/**
	 * @param handicap the handicap to set
	 */
	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}

	/**
	 * @return the channelName
	 */
	public String getChannelName() {
		return channelName;
	}

	/**
	 * @param channelName the channelName to set
	 */
	public void setChannelName(String channelName) {
		this.channelName = channelName;
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
	 * @return the platformStatus
	 */
	public Byte getPlatformStatus() {
		return platformStatus;
	}

	/**
	 * @param platformStatus the platformStatus to set
	 */
	public void setPlatformStatus(Byte platformStatus) {
		this.platformStatus = platformStatus;
	}

	/**
	 * @return the platformDeleteFlag
	 */
	public Byte getPlatformDeleteFlag() {
		return platformDeleteFlag;
	}

	/**
	 * @param platformDeleteFlag the platformDeleteFlag to set
	 */
	public void setPlatformDeleteFlag(Byte platformDeleteFlag) {
		this.platformDeleteFlag = platformDeleteFlag;
	}

	/**
	 * @return the crkStatus
	 */
	public Byte getCrkStatus() {
		return crkStatus;
	}

	/**
	 * @param crkStatus the crkStatus to set
	 */
	public void setCrkStatus(Byte crkStatus) {
		this.crkStatus = crkStatus;
	}

	/**
	 * @return the syncTime
	 */
	public Timestamp getSyncTime() {
		return syncTime;
	}

	/**
	 * @param syncTime the syncTime to set
	 */
	public void setSyncTime(Timestamp syncTime) {
		this.syncTime = syncTime;
	}

	/**
	 * @return the config
	 */
	public String getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(String config) {
		this.config = config;
	}

	/**
	 * @return the bankConfig
	 */
	public String getBankConfig() {
		return bankConfig;
	}

	/**
	 * @param bankConfig the bankConfig to set
	 */
	public void setBankConfig(String bankConfig) {
		this.bankConfig = bankConfig;
	}

	/**
	 * @return the warnConfig
	 */
	public String getWarnConfig() {
		return warnConfig;
	}

	/**
	 * @param warnConfig the warnConfig to set
	 */
	public void setWarnConfig(String warnConfig) {
		this.warnConfig = warnConfig;
	}

	/**
	 * @return the feeConfig
	 */
	public String getFeeConfig() {
		return feeConfig;
	}

	/**
	 * @param feeConfig the feeConfig to set
	 */
	public void setFeeConfig(String feeConfig) {
		this.feeConfig = feeConfig;
	}

	/**
	 * @return the outConfigSet
	 */
	public String getOutConfigSet() {
		return outConfigSet;
	}

	/**
	 * @param outConfigSet the outConfigSet to set
	 */
	public void setOutConfigSet(String outConfigSet) {
		this.outConfigSet = outConfigSet;
	}

	/**
	 * @return the levelConfig
	 */
	public String getLevelConfig() {
		return levelConfig;
	}

	/**
	 * @param levelConfig the levelConfig to set
	 */
	public void setLevelConfig(String levelConfig) {
		this.levelConfig = levelConfig;
	}

	/**
	 * @return the createTime
	 */
	public Timestamp getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime the createTime to set
	 */
	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the platformOutMoney
	 */
	public BigDecimal getPlatformOutMoney() {
		return platformOutMoney;
	}

	/**
	 * @param platformOutMoney the platformOutMoney to set
	 */
	public void setPlatformOutMoney(BigDecimal platformOutMoney) {
		this.platformOutMoney = platformOutMoney;
	}

	/**
	 * @return the crkOutMoneyHistory
	 */
	public BigDecimal getCrkOutMoneyHistory() {
		return crkOutMoneyHistory;
	}

	/**
	 * @param crkOutMoneyHistory the crkOutMoneyHistory to set
	 */
	public void setCrkOutMoneyHistory(BigDecimal crkOutMoneyHistory) {
		this.crkOutMoneyHistory = crkOutMoneyHistory;
	}

	/**
	 * @return the platformOutTimes
	 */
	public Integer getPlatformOutTimes() {
		return platformOutTimes;
	}

	/**
	 * @param platformOutTimes the platformOutTimes to set
	 */
	public void setPlatformOutTimes(Integer platformOutTimes) {
		this.platformOutTimes = platformOutTimes;
	}

	/**
	 * @return the crkOutMoney
	 */
	public BigDecimal getCrkOutMoney() {
		return crkOutMoney;
	}

	/**
	 * @param crkOutMoney the crkOutMoney to set
	 */
	public void setCrkOutMoney(BigDecimal crkOutMoney) {
		this.crkOutMoney = crkOutMoney;
	}

	/**
	 * @return the crkOutTimes
	 */
	public Integer getCrkOutTimes() {
		return crkOutTimes;
	}

	/**
	 * @param crkOutTimes the crkOutTimes to set
	 */
	public void setCrkOutTimes(Integer crkOutTimes) {
		this.crkOutTimes = crkOutTimes;
	}

	/**
	 * @return the crkOutTimesHistory
	 */
	public Integer getCrkOutTimesHistory() {
		return crkOutTimesHistory;
	}

	/**
	 * @param crkOutTimesHistory the crkOutTimesHistory to set
	 */
	public void setCrkOutTimesHistory(Integer crkOutTimesHistory) {
		this.crkOutTimesHistory = crkOutTimesHistory;
	}

	/**
	 * @return the thirdId
	 */
	public String getThirdId() {
		return thirdId;
	}

	/**
	 * @param thirdId the thirdId to set
	 */
	public void setThirdId(String thirdId) {
		this.thirdId = thirdId;
	}

	/**
	 * @return the aliasName
	 */
	public String getAliasName() {
		return aliasName;
	}

	/**
	 * @param aliasName the aliasName to set
	 */
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}
	
}
