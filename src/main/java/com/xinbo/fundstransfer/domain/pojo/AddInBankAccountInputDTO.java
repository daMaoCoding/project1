package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class AddInBankAccountInputDTO extends BizAccount implements Serializable {
	private List<Integer> levelIds;
	@NotNull
	private Integer currSysLevel;
	@NotNull
	private Integer handicapId;
	@NotNull
	private Integer peakBalance;

	@NotNull
	private String bankType;
	@NotNull
	private String province;
	@NotNull
	private String city;
	private Integer subType;
	@NotNull
	private String account;
	@NotNull
	private String bankName;
	@NotNull
	private String owner;
	private BigDecimal balance;
	@NotNull
	private Integer limitIn;
	private Integer limitOut;
	@NotNull
	private Integer limitBalance;
	private Integer lowestOut;
	private Integer limitOutOne;
	private Integer limitOutOneLow;
	private Integer limitOutCount;
	/** 入款单笔最小限额 */
	private BigDecimal minInAmount;
	private String remark;

}
