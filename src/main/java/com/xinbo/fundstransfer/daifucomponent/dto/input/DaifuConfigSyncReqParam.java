/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.input;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 平台同步出款通道配置信息参数对象
 * 
 * @author blake
 *
 */
public class DaifuConfigSyncReqParam implements Serializable {
	
	public enum Status{
		Active((byte)1,"启用"),Disable((byte)0,"停用"),Delete((byte)2,"删除");
		private Byte value;
		private String desc;
		private Status(Byte b,String desc) {
			this.value = b;
			this.desc = desc;
		}
		public Byte getValue() {
			return this.value;
		}
		public String getDesc() {
			return this.desc;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 9065921012661112624L;

	private String handicap;
	private String channelName;
	private String thirdId;
	private String memberId;
	private String privateKey;
	private String publicKey;
	private String notifyUrl;
	private BigDecimal storeMaxMoney;
	private BigDecimal storeMinMoney;
	private BigDecimal storeStopMoney;
	
	private Byte status;
	private String aisleName;
	private BigDecimal outMoney;
	private Integer outTimes;
	private Map<String,String> supportBankConfig;
	private Map<String,DaifuConfigSyncReqParamLevelConfig> supportLevelConfig;
	private DaifuConfigSyncReqParamFeeConfig feeConfig;
	private Map<String,DaifuConfigSyncReqParamOutConfig> outConfigSet;
	private DaifuConfigSyncReqParamWarnConfig warnConfig;

	public void setHandicap(String handicap) {
		this.handicap = handicap;
	}

	public String getHandicap() {
		return handicap;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setStatus(Byte status) {
		this.status = status;
	}

	public Byte getStatus() {
		return status;
	}

	public void setAisleName(String aisleName) {
		this.aisleName = aisleName;
	}

	public String getAisleName() {
		return aisleName;
	}

	public void setOutMoney(BigDecimal outMoney) {
		this.outMoney = outMoney;
	}

	public BigDecimal getOutMoney() {
		return outMoney;
	}

	public void setOutTimes(Integer outTimes) {
		this.outTimes = outTimes;
	}

	public Integer getOutTimes() {
		return outTimes;
	}

	public void setFeeConfig(DaifuConfigSyncReqParamFeeConfig feeConfig) {
		this.feeConfig = feeConfig;
	}

	public DaifuConfigSyncReqParamFeeConfig getFeeConfig() {
		return feeConfig;
	}

	public void setWarnConfig(DaifuConfigSyncReqParamWarnConfig warnConfig) {
		this.warnConfig = warnConfig;
	}

	public DaifuConfigSyncReqParamWarnConfig getWarnConfig() {
		return warnConfig;
	}

	/**
	 * @return the supportBankConfig
	 */
	public Map<String, String> getSupportBankConfig() {
		return supportBankConfig;
	}

	/**
	 * @param supportBankConfig the supportBankConfig to set
	 */
	public void setSupportBankConfig(Map<String, String> supportBankConfig) {
		this.supportBankConfig = supportBankConfig;
	}

	/**
	 * @return the supportLevelConfig
	 */
	public Map<String, DaifuConfigSyncReqParamLevelConfig> getSupportLevelConfig() {
		return supportLevelConfig;
	}

	/**
	 * @param supportLevelConfig the supportLevelConfig to set
	 */
	public void setSupportLevelConfig(Map<String, DaifuConfigSyncReqParamLevelConfig> supportLevelConfig) {
		this.supportLevelConfig = supportLevelConfig;
	}

	/**
	 * @return the outConfigSet
	 */
	public Map<String, DaifuConfigSyncReqParamOutConfig> getOutConfigSet() {
		return outConfigSet;
	}

	/**
	 * @param outConfigSet the outConfigSet to set
	 */
	public void setOutConfigSet(Map<String, DaifuConfigSyncReqParamOutConfig> outConfigSet) {
		this.outConfigSet = outConfigSet;
	}

	/**
	 * @return the notifyUrl
	 */
	public String getNotifyUrl() {
		return notifyUrl;
	}

	/**
	 * @param notifyUrl the notifyUrl to set
	 */
	public void setNotifyUrl(String notifyUrl) {
		this.notifyUrl = notifyUrl;
	}

	/**
	 * @return the storeMaxMoney
	 */
	public BigDecimal getStoreMaxMoney() {
		return storeMaxMoney;
	}

	/**
	 * @param storeMaxMoney the storeMaxMoney to set
	 */
	public void setStoreMaxMoney(BigDecimal storeMaxMoney) {
		this.storeMaxMoney = storeMaxMoney;
	}

	/**
	 * @return the storeMinMoney
	 */
	public BigDecimal getStoreMinMoney() {
		return storeMinMoney;
	}

	/**
	 * @param storeMinMoney the storeMinMoney to set
	 */
	public void setStoreMinMoney(BigDecimal storeMinMoney) {
		this.storeMinMoney = storeMinMoney;
	}

	/**
	 * @return the storeStopMoney
	 */
	public BigDecimal getStoreStopMoney() {
		return storeStopMoney;
	}

	/**
	 * @param storeStopMoney the storeStopMoney to set
	 */
	public void setStoreStopMoney(BigDecimal storeStopMoney) {
		this.storeStopMoney = storeStopMoney;
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
	
}
