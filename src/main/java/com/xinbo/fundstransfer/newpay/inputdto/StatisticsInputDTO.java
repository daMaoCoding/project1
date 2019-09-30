package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class StatisticsInputDTO implements Serializable {
	private Integer oid;
	@NotNull
	private String[] deviceCol;// 是 device数组
	@NotNull
	private Long timeStart;// 是 日期检索开始值
	@NotNull
	private Long timeEnd;// 是 日期检索结束值

}
