package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonDTO;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class ModifyBindInputDTO extends CommonDTO {
	/** 银行卡号 */
	@NotNull
	String cardNo;
	/** 是否取消通道绑定 0.否 1.是 */
	@NotNull
	Number cancelStatus;
	/** 操作人账号 */
	@NotNull
	String operator;
	/** 时间(“yyyy-MM-DD HH:mm:ss”) */
	@NotNull
	String operateTime;
	/** cancelStatus=0时必传 */
	Number pocId;
	/** 备注 */
	String remark;
}
