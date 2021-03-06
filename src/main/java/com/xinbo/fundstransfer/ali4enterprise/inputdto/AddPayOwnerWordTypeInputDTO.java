package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AddPayOwnerWordTypeInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 必填 盘口编码
	@NotNull
	private String typeName;// 必填 类型名称
	private String operationAdminName;// 非必填 操作人
	private Long operationAdminId;// 非必填 操作人ID

}
