package com.xinbo.fundstransfer.domain.pojo;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Commission {
	private BigDecimal baseCommission;
	private BigDecimal activityCommission;

	public Commission() {
	}

	public Commission(BigDecimal baseCommission, BigDecimal activityCommission) {
		this.baseCommission = baseCommission;
		this.activityCommission = activityCommission;
	}

	public BigDecimal getBaseCommission() {
		return baseCommission;
	}

	public void setBaseCommission(BigDecimal baseCommission) {
		this.baseCommission = baseCommission;
	}

	public BigDecimal getActivityCommission() {
		return activityCommission;
	}

	public void setActivityCommission(BigDecimal activityCommission) {
		this.activityCommission = activityCommission;
	}
}
