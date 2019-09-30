package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CommonFor1A8InputDTO implements Serializable {
	@NotNull
	private Integer oid;// 必填 盘口编码
	@NotNull
	private String name1;// String 必填 商城名称
	@NotNull
	private String code;// String 必填 商号
	@NotNull
	private String epKey;// String 必填 企业支付宝私钥
	@NotNull
	private String epUrl;// String 必填 企业支付宝回调地址
	@NotNull
	private String epUrl1;// String 必填 代理地址
	@NotNull
	private String proxyPort;// String 必填 代理地址端口
	private Double minMoney;// Double 非必填 最小入款金额
	private Double maxMoney;// Double 非必填 最大入款金额
	private Double stopMoney;// Double 非必填 停用金额
	@NotNull
	private String pubKey;// String 必填 公钥
	@NotNull
	private String apiGateway;// 必填 企业支付宝网关
}
