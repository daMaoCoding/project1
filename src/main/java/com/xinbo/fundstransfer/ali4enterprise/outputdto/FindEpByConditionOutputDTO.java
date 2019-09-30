package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindEpByConditionOutputDTO implements Serializable {
	private String id;
	private String oid;
	private String name;
	private String code;
	private String epKey;
	private String epUrl;
	private String epUrl1;
	private String minMoney;
	private String maxMoney;
	private String stopMoney;
	private String pubKey;
	private String createTime;
	private String status;
	private String color;
	private String proxyPort;
	private String apiGateway;
	private String aisleId;
	private String aisleName;
	private Byte bingFlag;// 绑定商品说明分类 0:未绑定 1:已绑定
	private String onlyCode;// 唯一自定义编码
	private String totalMoney;// 累计入款金额
}
