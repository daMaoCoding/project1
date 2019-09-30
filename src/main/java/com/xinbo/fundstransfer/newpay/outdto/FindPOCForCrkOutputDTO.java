package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindPOCForCrkOutputDTO implements Serializable {
	private long id;// 支付通道id
	private String payCode;// 支付通道名称
	private Byte isEpAlipay;// 否 是否是企业支付宝 0:否 1:是
}
