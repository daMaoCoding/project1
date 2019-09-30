package com.xinbo.fundstransfer.restful.v3.pojo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 */
public class ResV3DailyLogItem {
	private long id;
	private BigDecimal amount;
	private BigDecimal cashbackAmount;

	public ResV3DailyLogItem() {
	}

	public ResV3DailyLogItem(long id, BigDecimal amount, ReqV3RateItem rate) {
		this.id = id;
		this.amount = amount;
		if (Objects.nonNull(rate.getUplimit()) && rate.getUplimit() > 0) {
			BigDecimal rateAmount = new BigDecimal(Math.abs(amount.floatValue()) * rate.getRate() / 100).setScale(2,
					RoundingMode.HALF_UP);
			if (rateAmount.floatValue() > rate.getUplimit()) {
				this.cashbackAmount = new BigDecimal(rate.getUplimit()).setScale(2, RoundingMode.HALF_UP).abs();
			} else {
				this.cashbackAmount = new BigDecimal(Math.abs(amount.floatValue()) * rate.getRate() / 100).setScale(2,
						RoundingMode.HALF_UP);
			}
		} else {
			this.cashbackAmount = new BigDecimal(Math.abs(amount.floatValue()) * rate.getRate() / 100).setScale(2,
					RoundingMode.HALF_UP);
		}

	}

	public ResV3DailyLogItem(long id, BigDecimal amount, BigDecimal cashbackAmount) {
		this.id = id;
		this.amount = amount;
		this.cashbackAmount = cashbackAmount;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getCashbackAmount() {
		return cashbackAmount;
	}

	public void setCashbackAmount(BigDecimal cashbackAmount) {
		this.cashbackAmount = cashbackAmount;
	}
}
