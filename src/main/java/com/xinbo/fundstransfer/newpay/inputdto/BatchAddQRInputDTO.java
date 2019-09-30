package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BatchAddQRInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 0.wx 1.zfb
	@NotNull
	private Long accountId; /// /ownerNewpayConfig/findByCondition返回的wechatAccountId或者alipayAccountId
	@NotNull
	private Set<Double> moneySet; // 需要生成二维码的金额
	private Integer qrCodeCount; // 生成二维码的个数
}
