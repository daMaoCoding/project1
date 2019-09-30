package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonTypeDTO;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@Data
public class UpdateBindInputDTO extends CommonTypeDTO {
	/** 银行卡号集合 */
	List<Map<String, Object>> cardNoCol;
	/** operateType=3 时候必传！ */
	List<String> levelCol;
	/** 绑定的通道id */
	@NotNull
	Number pocId;
	/** 操作人名称 */
	String operator;
	/** 备注 */
	String remark;
}
