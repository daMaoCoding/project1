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
@Data
@ToString(callSuper = true)
public class CardForPayInputDTO extends CommonTypeDTO {
	/** 是 待选定的银行卡号集合 */
	@NotNull
	Map<String, String> pocIdMapCardNosCol;
	/** 是 待收款金额 */
	@NotNull
	Number amount;
	/** 用户名/会员名即提单的用户名 */
	String userName;
	
	/**
	 * 银行卡-云闪付混合时，传递该参数<br> 
	 * 结构：key=userPayType,value=pocid集合
	 */
	Map<String, List<Long>> pocIdTypeCol;
	
	/**
	 * 需求 6437 层级编码 biz_level.code
	 */
	private String levelCode;
	
	/**
	 * 入款通道id
	 * 需求 6437 上线后每一次请求都会传递 ocId. 并且返回结果时带上该值
	 */
	private Long ocId;
	
	/**
	 * 银行类型id，枚举值id。传空表示全部，并且该值对云闪付无效
	 */
	private Long bankTypeId;
}
