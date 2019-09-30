/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.http;

import java.io.Serializable;

/**
 * 第三方余额查询<pre>
 * 查询失败:
 * {
       "requestDaifuCode": "ERROR",                             //总处理结果
       "requestDaifuErrorMsg": "系统忙请稍后再试....",           //错误消息
       "requestDaifuOtherParam": "rest传递的其他参数,原样返回",  //其他参数
       "requestDaifuChannelId": "DUOFU",                       //第三方id
       "reqyestDaifuChannelMemberId": "MD63309719",            //商户号
       "requestDaifuBalance": "0",                             // 余额，单位分
       "requestDaifuDateTime": "1548753273991"                //当前时间戳
}
成功查询：
{
    "requestDaifuCode": "SUCCESS",
    "requestDaifuBalance": "0",
    "requestDaifuErrorMsg": "",
    "requestDaifuOtherParam": "rest传递的其他参数,原样返回",
    "requestDaifuChannelId": "DUOFU_BANK_WEB_DF_ZGJSYH",
    "reqyestDaifuChannelMemberId": "MD63309719",
    "requestDaifuDateTime": "1548754205688"
}
 * </pre>
 * @author blake
 *
 */
public class Crk2PayCoreBalance implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2866718381168000839L;
	
	private String requestDaifuCode;
	private String requestDaifuBalance;
	private String requestDaifuErrorMsg;
	private String requestDaifuOtherParam;
	private String requestDaifuChannelId;
	private String reqyestDaifuChannelMemberId;
	private String requestDaifuDateTime;
	/**
	 * @return the requestDaifuCode
	 */
	public String getRequestDaifuCode() {
		return requestDaifuCode;
	}
	/**
	 * @param requestDaifuCode the requestDaifuCode to set
	 */
	public void setRequestDaifuCode(String requestDaifuCode) {
		this.requestDaifuCode = requestDaifuCode;
	}
	/**
	 * @return the requestDaifuBalance
	 */
	public String getRequestDaifuBalance() {
		return requestDaifuBalance;
	}
	/**
	 * @param requestDaifuBalance the requestDaifuBalance to set
	 */
	public void setRequestDaifuBalance(String requestDaifuBalance) {
		this.requestDaifuBalance = requestDaifuBalance;
	}
	/**
	 * @return the requestDaifuErrorMsg
	 */
	public String getRequestDaifuErrorMsg() {
		return requestDaifuErrorMsg;
	}
	/**
	 * @param requestDaifuErrorMsg the requestDaifuErrorMsg to set
	 */
	public void setRequestDaifuErrorMsg(String requestDaifuErrorMsg) {
		this.requestDaifuErrorMsg = requestDaifuErrorMsg;
	}
	/**
	 * @return the requestDaifuOtherParam
	 */
	public String getRequestDaifuOtherParam() {
		return requestDaifuOtherParam;
	}
	/**
	 * @param requestDaifuOtherParam the requestDaifuOtherParam to set
	 */
	public void setRequestDaifuOtherParam(String requestDaifuOtherParam) {
		this.requestDaifuOtherParam = requestDaifuOtherParam;
	}
	/**
	 * @return the requestDaifuChannelId
	 */
	public String getRequestDaifuChannelId() {
		return requestDaifuChannelId;
	}
	/**
	 * @param requestDaifuChannelId the requestDaifuChannelId to set
	 */
	public void setRequestDaifuChannelId(String requestDaifuChannelId) {
		this.requestDaifuChannelId = requestDaifuChannelId;
	}
	/**
	 * @return the reqyestDaifuChannelMemberId
	 */
	public String getReqyestDaifuChannelMemberId() {
		return reqyestDaifuChannelMemberId;
	}
	/**
	 * @param reqyestDaifuChannelMemberId the reqyestDaifuChannelMemberId to set
	 */
	public void setReqyestDaifuChannelMemberId(String reqyestDaifuChannelMemberId) {
		this.reqyestDaifuChannelMemberId = reqyestDaifuChannelMemberId;
	}
	/**
	 * @return the requestDaifuDateTime
	 */
	public String getRequestDaifuDateTime() {
		return requestDaifuDateTime;
	}
	/**
	 * @param requestDaifuDateTime the requestDaifuDateTime to set
	 */
	public void setRequestDaifuDateTime(String requestDaifuDateTime) {
		this.requestDaifuDateTime = requestDaifuDateTime;
	}
	
}
