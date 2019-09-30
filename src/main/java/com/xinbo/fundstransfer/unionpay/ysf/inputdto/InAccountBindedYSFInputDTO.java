package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class InAccountBindedYSFInputDTO extends BizAccount implements Serializable {
	Integer id;
	Integer handicapId;
	/** 如果绑定银行卡,则必传盘口编码 */
	String handicap;
	/** 如果绑定银行卡,则必传账号 */
	String account;
	/***
	 * {@link com.xinbo.fundstransfer.domain.enums.AccountType}
	 */
	Integer type;
	/**
	 * 如果绑定银行卡,则必传账号子类型 {@link com.xinbo.fundstransfer.domain.enums.InBankSubType}
	 */
	Integer subType;
	/**
	 * 如果绑定银行卡,则必传账号状态 {@link com.xinbo.fundstransfer.domain.enums.AccountStatus}
	 */
	Integer status;
	/** 如果绑定银行卡,则必传银行类型 */
	String bankType;
	String bankName;
	/** 如果绑定银行卡,则必传开户入 */
	String owner;
	/** 如果绑定银行卡,则必传层级编码 多个层级编码以“,”隔开 */
	Integer[] levelIds;
}
