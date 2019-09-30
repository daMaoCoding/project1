package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonDTO;
import lombok.Data;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class BindAccountInputDTO extends CommonDTO {
	/**
	 * 描述:请求绑定的银行卡类型 {@link com.xinbo.fundstransfer.domain.enums.InBankSubType}
	 */
	private Byte type;
	private Integer handicapId;// 盘口id
}
