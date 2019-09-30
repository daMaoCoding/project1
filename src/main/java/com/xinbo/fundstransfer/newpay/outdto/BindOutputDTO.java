package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

import org.aspectj.lang.annotation.AdviceName;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BindOutputDTO implements Serializable {
	private long mobileId;
	private Integer oid;// 业主oid
	private String ownerName;// 业主名
	private String tel;// 手机号
	private long aTypeId;// 形容词id
	private String adj;// 形容词名
	private long nTypeId; // 名词id
	private String noun;// 名词
	private long adminTime;// 最后更新时间，Long型时间戳 13位
	private Byte mcmStatus;// 支付宝是否有成功被点击过 0.否 1.是
	private Byte wechatMcmStatus;// 微信是否有成功被点击过 0.否 1.是
}
