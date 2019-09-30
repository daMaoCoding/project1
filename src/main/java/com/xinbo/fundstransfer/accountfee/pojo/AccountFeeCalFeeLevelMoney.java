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
public class AccountFeeCalFeeLevelMoney extends AccountFeeCalFeeLevelBase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -506846893827540233L;
	
	/**
	 * 费用（元）
	 */
	private BigDecimal feeMoney;

	public BigDecimal getFeeMoney() {
		return feeMoney;
	}

	public void setFeeMoney(BigDecimal feeMoney) {
		this.feeMoney = feeMoney;
	}

}
