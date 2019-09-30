package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FindDrawTaskResult implements Serializable {

	private List<FindDrawTaskOutputDTO> list;
	// 全部 新任务数量 下发中数量
	public static final String queryNewInAllCount = "queryStatus1InAllCount";
	public static final String queryLockedOrDrawingInAllCount = "queryStatus2InAllCount";
	// 我的锁定 下发中数量 等待到账数
	public static final String queryLockedByOneCount = "queryStatus1ByOneCount";
	public static final String queryDrawingByOneCount = "queryStatus2ByOneCount";

	// 数量统计:key:queryStatus1InAllCount,
	// queryStatus2InAllCount,queryStatus1ByOneCount,queryStatus2ByOneCount
	// value:queryNewInAllCount,queryLockedOrDrawingInAllCount,queryLockedByOneCount,queryDrawingByOneCount
	private Map<String, Object> countsMap;
	// 银行余额 key: sumBankBalance value:getBankBalance之合 ； 本次下发金额 key:singleDrawSum
	// value:singleTimeAvailableAmount之合
	// value:getLimitOut
	private Map<String, Object> sumAmountMap;

	public static final String sumBankBalance = "sumBankBalance";
	public static final String singleDrawSum = "singleDrawSum";

	public FindDrawTaskResult() {
		this.list = Lists.newLinkedList();
		this.countsMap = new LinkedHashMap(4) {
			{
				put(queryNewInAllCount, 0);
				put(queryLockedOrDrawingInAllCount, 0);
				put(queryLockedByOneCount, 0);
				put(queryDrawingByOneCount, 0);
			}
		};
		this.sumAmountMap = new LinkedHashMap(2) {
			{
				put(sumBankBalance, 0);
				put(singleDrawSum, 0);
			}
		};
	}
}
