package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.newinaccount.dto.CommonDTO;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class CardsStatisticOutputDTO extends CommonDTO implements Serializable {
	/** 通道id **/
	private Long pocId;
	/** 可用卡数 (转账卡) */
	private Integer mnt1;
	/** 不可用卡数 (转账卡) */
	private Integer mnt2;
	/** 可用卡数 (扫码卡) */
	private Integer mnt3;
	/** 不可用卡数 (扫码卡) */
	private Integer mnt4;
}
