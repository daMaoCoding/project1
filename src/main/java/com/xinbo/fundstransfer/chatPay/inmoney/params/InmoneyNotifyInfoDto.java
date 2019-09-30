package com.xinbo.fundstransfer.chatPay.inmoney.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 	向出款人员通知入款成功的返回值
 * @author ERIC
 *
 */
@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InmoneyNotifyInfoDto {
	private String inUserId;
	private String inCode;
	private String inRoomId;
	private String inToken;
}
