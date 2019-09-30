package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class FindAlarmInputDTO implements Serializable {
	@NotNull
	private Integer pageSize;
	@NotNull
	private Integer pageNo;
	/**
	 * 盘口编码
	 */
	@NotNull
	private Integer oid;
	@NotNull
	private List<Long> pocIdList;
}
