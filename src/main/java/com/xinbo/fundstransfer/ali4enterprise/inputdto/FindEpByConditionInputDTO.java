package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import java.sql.Timestamp;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindEpByConditionInputDTO extends PageInputDTO {
	@NotNull
	private Integer oid;// 非必填 盘口编码
	private Timestamp createTimeStart;// 非必填 创建时间开始
	private Timestamp createTimeEnd;// 非必填 创建时间结束
	private String name;// 非必填 商城名称
	private String code;// 非必填 商号
	private Double minMoneyStart;// 非必填 最小入款金额开始
	private Double minMoneyEnd;// 非必填 最小入款金额结束
	private Double maxMoneyStart;// 非必填 最大入款金额开始
	private Double maxMoneyEnd;// 非必填 最大入款金额结束
	private Double stopMoneyStart;// 非必填 停用金额开始
	private Double stopMoneyEnd;// 非必填 停用金额结束
	private Byte status;// 非必填 状态 0:停用 1:启用
	private Long aisleId;// 非必填 企业支付宝通道编号
	private String aisleName;// 非必填 企业支付宝通道名称
	private Byte bingFlag;// 非必填 是否绑定商品说明分类 0:未绑定 1:已绑定
	private String onlyCode;// 唯一自定义编码
}
