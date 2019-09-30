package com.xinbo.fundstransfer.unionpay.ysf.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class YSFQRrequestEntity implements Serializable {
	private Integer handicapId;// 盘口Id
	private String ysfAccount;// 云闪付账号
	private String bankAccount;// 绑定的银行卡
	private String expectAmounts;// 期望生成收款二维码的常用金额 多个金额以 “,”隔开
	private String loginPWD;// 云闪付登陆密码
}
