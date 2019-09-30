package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UpdateYSFBasicInfoInputDTO extends AddYSFAccountInputDTO {
	@NotNull
	private Integer id;// 云闪付账号id
	private String accountNo;
	private String owner;
	private Integer handicapId;
	private Byte ownType;
	private Byte status;
	private String creater;
	private String remark;
}
