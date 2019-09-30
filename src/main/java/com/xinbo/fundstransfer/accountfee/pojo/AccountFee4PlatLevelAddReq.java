/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.pojo;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author Blake
 *
 */
public class AccountFee4PlatLevelAddReq extends AccountFee4PlatOperationReq implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3431951178170312087L;

	@NotNull
	@Min(value = 0)
	private Double moneyBegin;
	
	@NotNull
	private Double moneyEnd;

	@NotNull
	@Min(value = 0)
	@Max(value = 1)
	private Byte calFeeLevelType;
	
	@Min(value = 0)
	private BigDecimal feeMoney;
	
	@Min(value = 0)
	@Max(value = 1)
	private BigDecimal feePercent;

	public Double getMoneyBegin() {
		return moneyBegin;
	}

	public void setMoneyBegin(Double moneyBegin) {
		this.moneyBegin = moneyBegin;
	}

	public Double getMoneyEnd() {
		return moneyEnd;
	}

	public void setMoneyEnd(Double moneyEnd) {
		this.moneyEnd = moneyEnd;
	}

	public Byte getCalFeeLevelType() {
		return calFeeLevelType;
	}

	public void setCalFeeLevelType(Byte calFeeLevelType) {
		this.calFeeLevelType = calFeeLevelType;
	}

	public BigDecimal getFeeMoney() {
		return feeMoney;
	}

	public void setFeeMoney(BigDecimal feeMoney) {
		this.feeMoney = feeMoney;
	}

	public BigDecimal getFeePercent() {
		return feePercent;
	}

	public void setFeePercent(BigDecimal feePercent) {
		this.feePercent = feePercent;
	}
	
}
