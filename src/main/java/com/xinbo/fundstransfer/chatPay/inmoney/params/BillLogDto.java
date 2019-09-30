package com.xinbo.fundstransfer.chatPay.inmoney.params;

import java.math.BigDecimal;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * 	流水上报的请求参数
 * @author ERIC
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillLogDto {
	@NotBlank(message = "工具设备标识不能为空")
	private String devicesId;
	@NotNull(message = "调用者标识不能为空") //，1会员流水工具，2兼职流水工具
	private Integer toolsId;
	@NotBlank(message = "被抓取的 支付宝账号/微信账号不能为空")
	private String fromAccount;
	@NotBlank(message = "对方的 支付宝账号/微信账号不能为空")
	private String toAccount;
	@NotBlank(message = "对方的真实姓名不能为空")
	private String toRealName;
	@NotBlank(message = "流水金额不能为空") //(正数转入，负数转出)
	private BigDecimal amount;
	@NotBlank(message = "流水订单号不能为空") //(支付宝或微信内部订单号)
	private String orderNumber;
	@NotBlank(message = "备注码不能为空") //(转入，转出都有，必填)
	private String bzm;
	private String summary;
	@NotBlank(message = "流水中时间戳不能为空") //(支付宝/微信时间)
	private Long timestamp;
	@NotBlank(message = "工具抓取完成时间不能为空")
	private Long toolsTimestamp;
	@NotBlank(message = "截图的图片地址不能为空")
	private String picUrl;
	
	public BillLogDto(@NotBlank(message = "工具设备标识不能为空") String devicesId, @NotNull(message = "调用者标识不能为空") Integer toolsId,
			@NotBlank(message = "被抓取的 支付宝账号/微信账号不能为空") String fromAccount, @NotBlank(message = "对方的 支付宝账号/微信账号不能为空") String toAccount, 
			@NotBlank(message = "对方的真实姓名不能为空") String toRealName, @NotBlank(message = "流水金额不能为空") BigDecimal amount, @NotBlank(message = "流水订单号不能为空") String orderNumber, 
			@NotBlank(message = "备注码不能为空") String bzm, String summary, @NotBlank(message = "流水中时间戳不能为空") Long timestamp, 
			@NotBlank(message = "工具抓取完成时间不能为空") Long toolsTimestamp, @NotBlank(message = "截图的图片地址不能为空") String picUrl) {
		this.devicesId = devicesId;
		this.toolsId = toolsId;
		this.fromAccount = fromAccount;
		this.toAccount = toAccount;
		this.toRealName = toRealName;
		this.amount = amount;
		this.orderNumber = orderNumber;
		this.bzm = bzm;
		this.summary = summary;
		this.timestamp = timestamp;
		this.toolsTimestamp = toolsTimestamp;
		this.picUrl = picUrl;
	}
	
	public BillLogDto() {
		super();	
	}
}
