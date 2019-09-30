package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class InAccountInfoDTO implements Serializable {
	private String accountNo;// 卡号
	private String bankType;// 银行类型
	private String bankName;// 开户行
	private String owner;// 开户人
	private Integer minInAmount;// 最小入款金额
}
