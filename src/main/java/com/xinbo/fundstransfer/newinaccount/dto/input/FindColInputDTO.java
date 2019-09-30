package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonTypeDTO;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class FindColInputDTO extends CommonTypeDTO implements Serializable {
	/** 待绑定的通道id */
	@NotNull
	Long pocId;
	/** 否 银行卡号 */
	String cardNo;
	/** 否 银行名称 */
	String bankName;
	/** 否 是否已绑定通道 0.否 1.是 */
	Number isBind;
}
