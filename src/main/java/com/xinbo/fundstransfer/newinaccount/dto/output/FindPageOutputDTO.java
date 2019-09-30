package com.xinbo.fundstransfer.newinaccount.dto.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Administrator
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class FindPageOutputDTO extends FindColOutputDTO implements Serializable {
	/** 转账卡 可用银行卡数量 大于或者等于0 */
	private Integer count;
	/** 扫码卡 可用银行卡数量 大于或者等于0 */
	private Integer count1;
}
