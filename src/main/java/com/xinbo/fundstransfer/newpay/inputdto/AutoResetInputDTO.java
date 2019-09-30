package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/8/29.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AutoResetInputDTO implements Serializable {
	@NotNull
	private String openMan; // 姓名
	@NotNull
	private String account; // 收款银行卡号
	@NotNull
	private Long inTime; // 入款时间，时间戳
	@NotNull
	private Number money; // 金额，小于兼职人员的信用额度
	@NotNull
	private Integer oid; // 盘口编码

}
