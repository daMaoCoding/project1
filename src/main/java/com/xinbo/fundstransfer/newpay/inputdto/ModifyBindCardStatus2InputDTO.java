package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyBindCardStatus2InputDTO implements Serializable {
	@NotNull
	private Integer oid;// 是 盘口编码

	@NotNull
	private Number status;// 是 0：停用 ，1：启用，2：被平台禁用，3：新卡，4：冻结，5：删除
	private String operationAdminName;// 是 操作人
	@NotNull
	private List<CardPayee> cardPayeeCol;

	@Data
	public static class CardPayee {
		@NotNull
		private String cardNo;// 是 银行卡号
		@NotNull
		private String payeeName;// 是 开户人
	}
}
