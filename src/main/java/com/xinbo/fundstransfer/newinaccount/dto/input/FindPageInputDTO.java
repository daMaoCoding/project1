package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.PageDTO;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class FindPageInputDTO extends PageDTO implements Serializable {
	String cardNo;// 否 银行卡号
	String bankName;// 否 银行名称
	Number bindStatus;// 否 是否绑定通道 0.否 1.是
	Number minInMoneyNoLimit;// 否 是否最小入款金额无限制 0.否 1.是
	String minInMoneyStart;// 否 最小入款金额开始值
	String minInMoneyEnd;// 否 最小入款金额结束值
}
