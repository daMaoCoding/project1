package com.xinbo.fundstransfer.domain.pojo;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by Administrator on 2018/7/2.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TroubleShootDTO implements Serializable {
	private String handicap;
	private String orderNo;
	private String level;
	private String member;
	private String drawer;
	private String outAccount;
	private String startTime;
	private String endTime;
	private String amountStart;
	private String amountEnd;
	/**
	 * 人工 "manual" 机器 "robot"
	 */
	private String type;
	/**
	 * 查询类型 正在排查3 正在接单1 已排查 2
	 */
	private String queryType;
	private int pageSize;
	private int pageNo;
	/**
	 * 操作人
	 */

	private String operator;
	/**
	 * 排查人
	 */
	private String shooter;
	/**
	 * 接单人页面 1 汇总页2
	 */
	private String pageType;
	/**
	 * 标识 平台请求的 1 其他为内部
	 */
	private Byte platFormQuery;

	public String getStartTime() {
		return StringUtils.trim(startTime);
	}

	public void setStartTime(String startTime) {
		this.startTime = StringUtils.trim(startTime);
	}

	public String getEndTime() {
		return StringUtils.trim(endTime);
	}

	public void setEndTime(String endTime) {
		this.endTime = StringUtils.trim(endTime);
	}
}
