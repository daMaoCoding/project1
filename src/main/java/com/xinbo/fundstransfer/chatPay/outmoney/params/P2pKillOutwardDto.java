package com.xinbo.fundstransfer.chatPay.outmoney.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 *	 结束出款任务的请求参数
 * @author ERIC
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class P2pKillOutwardDto {
	@NotBlank(message = "出款订单号不能为空")
	private String orderNumber;
	@NotBlank(message = "客服Uid不能为空")
	private String optId;
	@NotBlank(message = "客服账号不能为空")
	private String optAccount;
	@NotNull(message = "请求时间戳不能为空")
	private Long timestamp;

	public P2pKillOutwardDto(@NotBlank(message = "出款订单号不能为空") String orderNumber, @NotBlank(message = "客服Uid不能为空") String optId, @NotBlank(message = "客服账号不能为空") String optAccount,
			@NotNull(message = "请求时间戳不能为空") Long timestamp) {
		this.orderNumber = orderNumber;
		this.optId = optId;
		this.optAccount = optAccount;
		this.timestamp = timestamp;
	}

	public P2pKillOutwardDto() {
		super();
	}
}
