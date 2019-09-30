package com.xinbo.fundstransfer.newinaccount.dto.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonTypeDTO;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class CardForPayOutputDTO extends CommonTypeDTO implements Serializable {
	private Number pocId;
	private Number amount;
	/** 带小数的最终金额 */
	private Number finalAmount;
	private List<BankInfo> accountList;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	@Data
	public static class BankInfo implements Serializable {
		private String cardNo;
		private String owner;
		private String bankType;
		private String bankName;
		private String bankMark;
		private String province;
		private String city;
		private String qrCode;
	}
}
