package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/12.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindBLog9ByConditionOutputDTO implements Serializable {
	private String inAccount;// 收款账号
	private String payAccount; // 付款账号
	private Double money; // 金额
	private Byte status; // 0：未匹配，1：已匹配
	private String createtime;// 创建时间
	private String admintime; // 匹配时间（确认时间）
	private String summary; // 摘要
}
