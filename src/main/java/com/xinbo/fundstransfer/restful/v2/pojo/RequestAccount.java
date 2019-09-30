package com.xinbo.fundstransfer.restful.v2.pojo;

import javax.validation.constraints.NotNull;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RequestAccount {
	@NotBlank
	String handicap;
	// @NotBlank 当是微信支付宝时候 此字段可以为空
	String account;
	@NotBlank
	String token;
	@NotNull
	Integer type;
	/**
	 * {@link com.xinbo.fundstransfer.domain.enums.InBankSubType}
	 */
	Integer subType;
	@NotNull
	Integer status;

	String bankType;

	String bankName;

	String owner;

	String levels;
}
