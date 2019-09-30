package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Anastasia on 2018/8/14.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyFixInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Long mobileId;// id
	@NotNull
	private String prefix;// 收款理由前缀
	@NotNull
	private String suffix;// 收款理由后缀
	@NotNull
	private Byte chkType;// 收款理由类型，0：前后缀形式，1：形容词-名词形式
}
