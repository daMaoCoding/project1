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
public class BindingWordTypeInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 类型 ：0：形容词，1：名词
	@NotNull
	private Long mobileId;// ownerNewpayConfig/findByCondition返回的id
	@NotNull
	private Long wordTypeId;// 类型Id
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号
}
