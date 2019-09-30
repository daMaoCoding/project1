package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CreateYSFAccountInputDTO implements Serializable {
	@NotNull
	@Valid
	private AddYSFAccountInputDTO ysfAccountInputDTO;
	private List<InAccountBindedYSFInputDTO> inAccounts;
}
