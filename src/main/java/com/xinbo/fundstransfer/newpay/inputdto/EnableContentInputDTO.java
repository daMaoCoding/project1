package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class EnableContentInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Long id;// /ownerNewpayWord/findByCondition返回的id 形容词名词ID
	@NotNull
	private Byte status;// 状态：1-在用 0-停用
	private Long operationAdminId;// 操作人id 
	private String operationAdminName;// 操作人账号

}
