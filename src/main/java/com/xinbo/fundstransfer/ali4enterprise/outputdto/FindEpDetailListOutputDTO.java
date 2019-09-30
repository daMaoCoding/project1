package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindEpDetailListOutputDTO implements Serializable {
	private String id;// ,
	private String oid;//
	private String name;// 商城名称
	private String code;// 商号
	private String minMoney;// 最小入款金额
	private String maxMoney;// 最大入款金额
	private String stopMoney;// 停用金额
	private String createTime;// 创建时间
	private String status;// 状态 0:停用 1:启用
	private String color;// 停用金额颜色
	private String aisleId;// 企业支付宝通道编号
	private String aisleName;// 企业支付宝通道名称
	private String onlyCode;// 唯一自定义编码
	private String totalMoney;// 累计入款金额
}
