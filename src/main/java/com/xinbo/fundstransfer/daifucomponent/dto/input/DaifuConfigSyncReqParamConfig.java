/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.input;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 平台同步出款通道配置信息参数对象
 * 
 * @author blake
 *
 */
public class DaifuConfigSyncReqParamConfig implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4033930234709302348L;

	private String privateKey;
	
	private String publicKey;
	
	private String notifyUrl;
	
	private BigDecimal storeMaxMoney;
	private BigDecimal storeMinMoney;
	private BigDecimal storeStopMoney;

	/**
	 * @return the privateKey
	 */
	public String getPrivateKey() {
		return privateKey;
	}

	/**
	 * @param privateKey the privateKey to set
	 */
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	/**
	 * @return the publicKey
	 */
	public String getPublicKey() {
		return publicKey;
	}

	/**
	 * @param publicKey the publicKey to set
	 */
	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
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

	@Override
	public String toString() {
		StringBuffer str= new StringBuffer("{") ;
		if(this.privateKey!=null) {
			str.append("\"privateKey\":").append(this.privateKey).append(",");
		}
		if(this.publicKey!=null) {
			str.append("\"publicKey\":").append(this.publicKey).append(",");
		}
		if(this.notifyUrl!=null) {
			str.append("\"notifyUrl\":").append(this.notifyUrl).append(",");
		}
		if(this.storeMaxMoney!=null) {
			str.append("\"storeMaxMoney\":").append(this.storeMaxMoney).append(",");
		}
		if(this.storeMinMoney!=null) {
			str.append("\"storeMinMoney\":").append(this.storeMinMoney).append(",");
		}
		if(this.storeStopMoney!=null) {
			str.append("\"storeStopMoney\":").append(this.storeStopMoney).append(",");
		}
		if(str.toString().endsWith(",")) {
			str = new StringBuffer(str.substring(0, str.length()-1));
		}
		str.append("}");
		return str.toString();
	}
}
