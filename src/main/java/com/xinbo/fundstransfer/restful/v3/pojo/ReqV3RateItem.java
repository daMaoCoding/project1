package com.xinbo.fundstransfer.restful.v3.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ReqV3RateItem implements Serializable {

	public ReqV3RateItem() {
	}

	public ReqV3RateItem(@NotNull float amount, @NotNull float rate, Float uplimit, int type) {
		this.amount = amount;
		this.rate = rate;
		this.uplimit = uplimit;
		this.type = type;
	}

	@NotNull
	private float amount;
	@NotNull
	private float rate;
	private Float uplimit;
	private int type;

	public float getAmount() {
		return amount;
	}

	public void setAmount(float amount) {
		this.amount = amount;
	}

	public float getRate() {
		return rate;
	}

	public void setRate(float rate) {
		this.rate = rate;
	}

	public Float getUplimit() {
		return uplimit;
	}

	public void setUplimit(Float uplimit) {
		this.uplimit = uplimit;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
