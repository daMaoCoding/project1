package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;

/**
 * Created by 000 on 2017/11/29.
 */
public class AcntPack {
	private Integer id;

	private Integer mapping;

	private Integer limitOut;

	private BigDecimal balance;

	private Integer currSysLevel;

	public AcntPack() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getMapping() {
		return mapping;
	}

	public void setMapping(BigDecimal mapping) {
		this.mapping = mapping == null ? 0 : mapping.intValue();
	}

	public Integer getLimitOut() {
		return limitOut;
	}

	public void setLimitOut(Integer limitOut) {
		this.limitOut = limitOut;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public Integer getCurrSysLevel() {
		return currSysLevel;
	}

	public void setCurrSysLevel(Integer currSysLevel) {
		this.currSysLevel = currSysLevel;
	}
}
