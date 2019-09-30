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
public class AccountListInputDTO implements Serializable {
	@NotNull
	private Integer pageNo;
	/** 盘口 */
	@NotNull
	private List<Integer> handicapId;
	@NotNull
	private List<Integer> typeToArray;
	@NotNull
	private List<Integer> statusToArray;
	private Integer pageSize;

	private String operator;
	private String holderType;

	/** 内外层 指定层 */
	private List<Integer> currSysLevel;
	/** 银行类型 */
	private String bankType;
	/** 注意要 跟实体类字段一致 */
	private String sortProperty;
	private Integer sortDirection;
	private String auditor;
	private String deviceStatus;
	private Integer transBlackTo;
	private String isRetrieve;
	/**
	 * 第三方下发卡查询 标识 1
	 */
	private String queryFlag;
}
