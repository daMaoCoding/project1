package com.xinbo.fundstransfer.unionpay.ysf.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class YSFQResponseEntity implements Serializable {
	private String handicapId;// 盘口Id
	private String ysfAccount;// 云闪付账号
	private String bankAccount;// 绑定的收款银行卡号
	private String amount;// 金额
	private String qrStreams;// 生成的单个金额二维码字符串,url 如果3秒之后没有返回直接返回空给平台
	private String date;// 二维码生成日期 YYYY-MM-DD HH:mm:SS
	private String token;
}
