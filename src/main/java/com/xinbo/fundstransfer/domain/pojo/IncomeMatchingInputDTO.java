package com.xinbo.fundstransfer.domain.pojo;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class IncomeMatchingInputDTO implements Serializable {
	private Integer[] accountId;
	private String payMan;
	private Integer pageNo;
	private Integer level;
	private String orderNo;
	private String member;
	private String fromMoney;
	private String toMoney;
	private String startTime;
	private String endTime;

}
