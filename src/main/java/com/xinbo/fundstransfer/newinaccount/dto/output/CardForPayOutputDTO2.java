package com.xinbo.fundstransfer.newinaccount.dto.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonTypeDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO.BankInfo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class CardForPayOutputDTO2 extends CommonTypeDTO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2899018579144337222L;
	private Number pocId;
	private Number amount;
	/** 带小数的最终金额 */
	private Number finalAmount;
	private BankInfo account;

}
