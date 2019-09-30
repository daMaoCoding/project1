package com.xinbo.fundstransfer.domain.pojo;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/10/5.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AliInSummaryInputDTO implements Serializable {
	private String handicapId;
	private String level;
	private String orderNo;
	private String member;
	private String aliAccount;
	private String fromMoney;
	private String toMoney;
	@NotNull
	private String startTime;
	@NotNull
	private String endTime;
	@NotNull
	private Integer pageNo;
	@NotNull
	private Integer pageSize;
	/**
	 * {@link com.xinbo.fundstransfer.domain.enums.IncomeRequestType}
	 */
	@NotNull
	private Integer type;

	public void setPageNo(Integer pageNo) {
		if (pageNo == null) {
			pageNo = 0;
		}
		this.pageNo = pageNo;
	}

	public void setPageSize(Integer pageSize) {
		if (pageSize == null) {
			pageSize = 10;
		}
		this.pageSize = pageSize;
	}
}
