package com.xinbo.fundstransfer.chatPay.outmoney.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * 	出款方主动确认收款的请求参数
 * @author ERIC
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class P2pOutwardConfimDto {
	@NotBlank(message = "入款订单号不能为空")
	private String orderNumber;
	@NotNull(message = "请求时间戳不能为空")
	private Long timestamp;
	
	public P2pOutwardConfimDto(@NotBlank(message = "入款订单号不能为空") String orderNumber, @NotNull(message = "请求时间戳不能为空") Long timestamp) {
		this.orderNumber = orderNumber;
		this.timestamp = timestamp;
	}
	
	public P2pOutwardConfimDto() {
		super();	
	}
}
