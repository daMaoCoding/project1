package com.xinbo.fundstransfer.newinaccount.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class PageDTO<T> extends CommonTypeDTO implements Serializable {
	@NotNull
	private Integer pageSize;
	@NotNull
	private Integer pageNo;

	private Integer totalRecordNumber;
	private Integer totalPageNumber;

	private List<T> resultList;
}
