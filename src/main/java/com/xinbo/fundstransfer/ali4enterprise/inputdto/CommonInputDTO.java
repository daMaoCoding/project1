package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CommonInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 必填 盘口编码
	@NotNull
	private Long id;// Long 必填 主键编号
	private String orderField;
	private String orderSort;
}
