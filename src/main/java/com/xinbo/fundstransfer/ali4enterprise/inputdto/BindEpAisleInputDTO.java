package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BindEpAisleInputDTO extends CommonInputDTO {
	@NotNull
	private List<Long> idList;// 必填 企业支付宝通道资料编号集合
	@NotNull
	private Byte flag;// 必填 操作标记 0 : 绑定 1 : 解绑
	private Long operationAdminId;// 必填 操作人ID
	private String operationAdminName;// 必填 操作人
}
