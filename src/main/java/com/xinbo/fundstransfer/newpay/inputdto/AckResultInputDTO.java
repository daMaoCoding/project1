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
public class AckResultInputDTO implements Serializable {
	@NotNull
	private String account;// 请求下发的账号
	@NotNull
	private Integer toAccountId;// 下发到的账号id
	@NotNull
	private BigDecimal amount;// 金额
	@NotNull
	private Long ackTime;// 反馈时间
	@NotNull
	private Boolean flag;// 完成转账成功失败标识 true 成功,false 失败。
	@NotNull
	private String code;// 订单号
	@NotNull
	private Integer oid;// 盘口编码
}
