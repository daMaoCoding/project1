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
public class AccountFeeLevelDelInputDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 416601852832051507L;

	@NotNull
	@Positive
	private Integer handicapId;
	
	@NotNull
	@Positive
	private Integer accountId;

	@NotNull
	@Min(value = 0)
	@Max(value = 1)
	private Byte calFeeLevelType;
	
	@NotNull
	@Positive
	private Long index;

	public Integer getHandicapId() {
		return handicapId;
	}

	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}

	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

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
