package com.xinbo.fundstransfer.restful.v3.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqV3Status {
	@NotNull
	private List<String> accList;
	private String token;

	public List<String> getAccList() {
		return accList;
	}

	public void setAccList(List<String> accList) {
		this.accList = accList;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
