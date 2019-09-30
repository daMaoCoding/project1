package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.AppConstants;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ThirdDrawFailStatisticInputDTO implements Serializable {
	private Integer pageNo;
	private Integer pageSize;
	/**
	 * 起始时间
	 */
	private String startTime;
	/**
	 * 截止时间
	 */
	private String endTime;

	/**
	 * 单个时间值
	 */
	private String createTime;

	private String startAmount;
	private String endAmount;
	private String operatorUid;
	private String drawToAccount;

	private String thirdName;
	private String thirdAccount;

	private String[] handicaps;
	/**
	 * 第三方账号id
	 */
	private Integer thirdAccountId;
	/**
	 * 统计 全部 1 我的 2
	 */
	private Byte queryFlag;
	private Integer operator;

	/**
	 * 查询 第三方1 银行 2
	 */
	private Byte queryAccount;
	/**
	 * 测试 1
	 */
	private Byte testFlag;

	public Integer getPageSize() {
		if (this.pageSize == null) {
			pageSize = AppConstants.PAGE_SIZE;
		}
		return pageSize;
	}
}
