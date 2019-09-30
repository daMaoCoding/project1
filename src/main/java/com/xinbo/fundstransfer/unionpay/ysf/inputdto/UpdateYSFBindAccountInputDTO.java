package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UpdateYSFBindAccountInputDTO extends InAccountBindedYSFInputDTO {
	@NotNull
	private Integer id;// 云闪付账号id
}
