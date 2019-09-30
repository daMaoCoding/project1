package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SavePayOwnerWordBindInputDTO extends CommonInputDTO {
	@NotNull
	private List<Long> typeIdList;// 必填 分类编号集合
}
