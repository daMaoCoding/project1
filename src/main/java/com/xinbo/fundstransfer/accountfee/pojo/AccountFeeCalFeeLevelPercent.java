/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.pojo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Blake
 *
 */
public class AccountFeeCalFeeLevelPercent extends AccountFeeCalFeeLevelBase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -506846893827540233L;
	
	/**
	 * 费用（%）
	 */
	private BigDecimal feePercent;

	public BigDecimal getFeePercent() {
		return feePercent;
	}

	public void setFeePercent(BigDecimal feePercent) {
		this.feePercent = feePercent;
	}

}
