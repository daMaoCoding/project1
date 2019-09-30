package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author Administrator
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CheckCardStatusInputDTO implements Serializable {
	@NotNull
	private Integer oid;
	@NotNull
	private String cardNo;
	private Integer handicapId;
}
