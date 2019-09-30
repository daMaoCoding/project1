package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by Administrator on 2018/7/23.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ApplyMoneyDrawInputDTO implements Serializable {
	@NotNull
	private String account;// 请求下发的账号
	@NotNull
	private String handicapCode;// 盘口编码
	@NotNull
	private Integer level;// 层级 内 外 中
	@NotNull
	private BigDecimal balance;// 余额
	@NotNull
	private Byte accountType;// 账号类型 1 微信 2 支付宝
}
