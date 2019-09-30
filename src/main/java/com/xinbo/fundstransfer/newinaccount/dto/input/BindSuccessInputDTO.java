package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonDTO;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class BindSuccessInputDTO extends CommonDTO {
	/**
	 * {@link com.xinbo.fundstransfer.domain.enums.InBankSubType}
	 */
	@NotNull
	Number type;
	@NotNull
	List<String> cardNoCol;// 是 绑定的银行卡号集合
	@NotNull
	String operator;// 是 操作人账号
	@NotNull
	String operateTime;// 是 时间(“yyyy-MM-DD HH:mm:ss”)
	@NotNull
	Number pocId; // 是 绑定的通道id
	String remark;// 否 备注
}
