package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BindOrUnBindInputDTO implements Serializable {
	/**
	 * 绑定的卡id
	 */
	@NotNull
	private List<Integer> issueAccountId;
	/**
	 * 第三方入款卡id
	 */
	@NotNull
	private Integer incomeAccountId;
	/**
	 * 绑定 1 解绑 0
	 */
	@NotNull
	private Byte binding0binded1;
}
