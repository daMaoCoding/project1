package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindEpDetailOutputDTO implements Serializable {
	private String id;
	private String oid; //
	private String name; // 商城名称
	private String code; // 商号
	private String epKey; // 企业支付宝私钥
	private String epUrl; // 企业支付宝回调地址
	private String epUrl1; // 代理地址
	private String minMoney; // 最小入款金额
	private String maxMoney; // 最大入款金额
	private String stopMoney; // 停用金额
	private String pubKey; // 公钥
	private String createTime; // 创建时间
	private String status; // 状态 0:停用 1:启用
	private String color; // 停用金额颜色
	private String proxyPort; // 代理地址端口
	private String apiGateway; // 企业支付宝网关
	private String onlyCode;// 唯一自定义编码
	private String totalMoney;// 累计入款金额
}
