package com.xinbo.fundstransfer.daifucomponent.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newpay.inputdto.PageInputDTO;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DaifuSurpportBankTypeInputDTO extends PageInputDTO implements Serializable {
	private Integer id;
	private String provider;
	private String bankType;
	private String surpportBankType;
}
