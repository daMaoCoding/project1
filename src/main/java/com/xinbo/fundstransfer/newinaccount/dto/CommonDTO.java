package com.xinbo.fundstransfer.newinaccount.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CommonDTO implements Serializable {
	private String token;// 请求token
	@NotNull
	private Integer oid;// 盘口编码
}
