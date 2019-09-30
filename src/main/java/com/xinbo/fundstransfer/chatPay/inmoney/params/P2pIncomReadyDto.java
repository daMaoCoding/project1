package com.xinbo.fundstransfer.chatPay.inmoney.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * 	入款方开始转账的请求参数
 * @author ERIC
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class P2pIncomReadyDto {
	@NotBlank(message = "入款订单号不能为空")
	private String orderNumber;
	@NotNull(message = "请求时间戳不能为空")
	private Long timestamp;
	
	public P2pIncomReadyDto(@NotBlank(message = "入款订单号不能为空") String orderNumber, @NotNull(message = "请求时间戳不能为空") Long timestamp) {
		this.orderNumber = orderNumber;
		this.timestamp = timestamp;
	}
	
	public P2pIncomReadyDto() {
		super();	
	}
}
