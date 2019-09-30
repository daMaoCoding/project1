package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RemoveCRInputDTO implements Serializable {
	@NotNull
	private long id;
	@NotNull
	private Integer oid;// 业主oid
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号
}
