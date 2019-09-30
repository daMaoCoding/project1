/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.pojo;

import java.io.Serializable;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * @author Blake
 *
 */
public class AccountFee4PlatLevelDelReq extends AccountFee4PlatOperationReq implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4593455603077408276L;

	@NotNull
	@Min(value = 0)
	@Max(value = 1)
	private Byte calFeeLevelType;
	
	@NotNull
	@Positive
	private Long index;

	public Byte getCalFeeLevelType() {
		return calFeeLevelType;
	}

	public void setCalFeeLevelType(Byte calFeeLevelType) {
		this.calFeeLevelType = calFeeLevelType;
	}

	public Long getIndex() {
		return index;
	}

	public void setIndex(Long index) {
		this.index = index;
	}

}
