package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/12.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyUoFlagInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Long mobileId;// /ownerNewpayConfig/findByCondition返回的id
	@NotNull
	private Byte uoFlag;// 未确认出款金额开关，1-开，0-关
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号
}
