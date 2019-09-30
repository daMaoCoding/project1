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
public class CardAvailAlarmInputDTO extends CommonDTO {
	@NotNull
	Byte cardWarnFlag;// 1 // （必传）通道银行卡是否告警 0.否 1.是
	@NotNull
	Number pocId;// 是 绑定的通道id
}
