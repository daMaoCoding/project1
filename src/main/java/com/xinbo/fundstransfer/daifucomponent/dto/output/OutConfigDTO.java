/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.output;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * 出款通道列表查询 dto
 * @author blake
 *
 */
public class OutConfigDTO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1667897272454016780L;
	
	private Integer id;
	
	private Integer handicapId;

	private String channelName;
	
	private String thirdId;
	
	private String memberId;
	
	private String aliasName;
	
	private Byte platformStatus;

	private Byte platformDeleteFlag;
	
	private Byte crkStatus;
	
	private BigDecimal platformOutMoney;
	
	private BigDecimal crkOutMoneyHistory;
	
	private Integer platformOutTimes;

	private BigDecimal crkOutMoney;
	
	private Integer crkOutTimes;
	
	private Integer crkOutTimesHistory;
	
	private List<String> bankNameList;
	private List<String> levelNameList;
	
	private Timestamp syncTime;
	
	private Timestamp createTime;
	
	/**
	 * 盘口名称
	 */
	private String handicapName;
	
	/**
	 * 支付成功数量
	 */
	private Integer countSuccess;
	
	/**
	 * 支付取消数量
	 */
	private Integer countError;
	
	/**
	 * 正在支付数量<br>
	 * 取状态为 未知 和 正在支付的订单数量
	 */
	private Integer countPaying;

	/**
	 * @return the handicapId
	 */
	public Integer getHandicapId() {
		return handicapId;
	}

	/**
	 * @param handicapId the handicapId to set
	 */
	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
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
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
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
	 * @return the countSuccess
	 */
	public Integer getCountSuccess() {
		return countSuccess;
	}

	/**
	 * @param countSuccess the countSuccess to set
	 */
	public void setCountSuccess(Integer countSuccess) {
		this.countSuccess = countSuccess;
	}

	/**
	 * @return the countError
	 */
	public Integer getCountError() {
		return countError;
	}

	/**
	 * @param countError the countError to set
	 */
	public void setCountError(Integer countError) {
		this.countError = countError;
	}

	/**
	 * @return the countPaying
	 */
	public Integer getCountPaying() {
		return countPaying;
	}

	/**
	 * @param countPaying the countPaying to set
	 */
	public void setCountPaying(Integer countPaying) {
		this.countPaying = countPaying;
	}

	/**
	 * @return the bankNameList
	 */
	public List<String> getBankNameList() {
		return bankNameList;
	}

	/**
	 * @param bankNameList the bankNameList to set
	 */
	public void setBankNameList(List<String> bankNameList) {
		this.bankNameList = bankNameList;
	}

	/**
	 * @return the levelNameList
	 */
	public List<String> getLevelNameList() {
		return levelNameList;
	}

	/**
	 * @param levelNameList the levelNameList to set
	 */
	public void setLevelNameList(List<String> levelNameList) {
		this.levelNameList = levelNameList;
	}
	
}
