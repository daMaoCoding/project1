package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AddEpAisleDetailInputDTO extends CommonFor1A8InputDTO {
	private Long id;// 非必填 主键编号
}
