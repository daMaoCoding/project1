package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UpdateYSFPWDInputDTO {
	@NotNull
	private Integer id;// 云闪付账号id
	private String logInPWD;// 登陆密码
	private String payPWD;// 支付密码

	public UpdateYSFPWDInputDTO(Integer id, String logInPWD, String payPWD) {
		this.id = id;
		this.logInPWD = logInPWD;
		this.payPWD = payPWD;
	}
}
