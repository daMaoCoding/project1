package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindBindIssueInputDTO implements Serializable {
	private Integer pageNo;
	private Integer pageSize;
	private Integer incomeAccountId;
	private String[] bankBrandList;
	private String bankType;
	private Integer[] statusOfIssueToArray;
	private Integer[] typeOfIssueToArray;
	private Integer[] currSysLevelToArray;
	private String flag;
	private Byte queryType;
	private Integer binding0binded1;
}
