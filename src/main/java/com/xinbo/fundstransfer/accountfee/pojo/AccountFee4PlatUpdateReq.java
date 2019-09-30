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
public class AccountFee4PlatUpdateReq extends AccountFee4PlatOperationReq implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6002557808655988742L;

	@NotNull
	private Byte feeType;
	
	@NotNull
	private Byte calFeeType;
	
	@Min(value = 0)
	@Max(value = 1)
	private BigDecimal calFeePercent;
	
	@Min(value = 0)
	@Max(value = 1)
	private Byte calFeeLevelType;

	public Byte getFeeType() {
		return feeType;
	}

	public void setFeeType(Byte feeType) {
		this.feeType = feeType;
	}

	public Byte getCalFeeType() {
		return calFeeType;
	}

	public void setCalFeeType(Byte calFeeType) {
		this.calFeeType = calFeeType;
	}

	public BigDecimal getCalFeePercent() {
		return calFeePercent;
	}

	public void setCalFeePercent(BigDecimal calFeePercent) {
		this.calFeePercent = calFeePercent;
	}

	public Byte getCalFeeLevelType() {
		return calFeeLevelType;
	}

	public void setCalFeeLevelType(Byte calFeeLevelType) {
		this.calFeeLevelType = calFeeLevelType;
	}
}
