package com.xinbo.fundstransfer.restful.v3.pojo;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqV3Rate {
	@NotNull
	private List<ReqV3RateItem> items;
	@NotBlank
	private String token;

	public List<ReqV3RateItem> getItems() {
		return items;
	}

	public void setItems(List<ReqV3RateItem> items) {
		this.items = items;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
