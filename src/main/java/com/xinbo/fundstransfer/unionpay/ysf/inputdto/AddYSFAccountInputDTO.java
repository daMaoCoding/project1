package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AddYSFAccountInputDTO implements Serializable {
	private Integer id;
	@NotNull
	private String accountNo;
	@NotNull
	private String owner;
	@NotNull
	private String loginPWD;
	@NotNull
	private String payPWD;
	@NotNull
	private Integer handicapId;
	@NotNull
	private Byte ownType;
	private Byte status;

	private String creater;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String remark;
}
