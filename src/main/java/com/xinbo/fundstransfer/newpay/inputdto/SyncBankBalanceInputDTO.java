package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/9/24.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SyncBankBalanceInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 是 盘口编码
	@NotNull
	private String account;// 是 银行卡号
	@NotNull
	private Double balance;// 是 设备余额
	@NotNull
	private Double sysBalance;// 是 系统余额
}
