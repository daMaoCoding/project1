/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.input;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author blake
 *
 */
public class DaifuConfigSyncReqParamFeeConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4176599948899936210L;

	private BigDecimal freeMoney;
	private BigDecimal feePercent;
	private BigDecimal limitMoney;

	public void setFreeMoney(BigDecimal freeMoney) {
		this.freeMoney = freeMoney;
	}

	public BigDecimal getFreeMoney() {
		return freeMoney;
	}

	public void setFeePercent(BigDecimal feePercent) {
		this.feePercent = feePercent;
	}

	public BigDecimal getFeePercent() {
		return feePercent;
	}

	public void setLimitMoney(BigDecimal limitMoney) {
		this.limitMoney = limitMoney;
	}

	public BigDecimal getLimitMoney() {
		return limitMoney;
	}

	@Override
	public String toString() {
		StringBuffer str= new StringBuffer("{") ;
		if(this.feePercent!=null) {
			str.append("\"feePercent\":").append(this.feePercent).append(",");
		}
		if(this.freeMoney!=null) {
			str.append("\"freeMoney\":").append(this.freeMoney).append(",");
		}
		if(this.limitMoney!=null) {
			str.append("\"limitMoney\":").append(this.limitMoney).append(",");
		}
		if(str.toString().endsWith(",")) {
			str = new StringBuffer(str.substring(0, str.length()-1));
		}
		str.append("}");
		return str.toString();
	}
}
