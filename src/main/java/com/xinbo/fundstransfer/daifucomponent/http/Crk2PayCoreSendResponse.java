/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.http;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 出入款系统往payCore发送代付请求时的返回对象<pre>
 * { //代付请求成功，等待第三方转账
    "requestDaifuCode": "SUCCESS",
	"requestDaifuOrderState": "PAYING",
    "requestDaifuOid": "100",
    "requestDaifuErrorMsg": "",
    "requestDaifuAmount": "5000",
    "requestDaifuOrderId": "20190124131332703673",
    "requestDaifuOrderCreateTime": "1548054754000",
    "requestDaifuChannelBankName": "DUOFU_BANK_WEB_DF_ZGJSYH",
    "requestDaifuOtherParam": "BILL_INFO_这是其他参数将会原样返回",
    "requestDaifuTotalTime": "71135",
    "requestDaifuChannelTime": "67000",
    "requestDaifuGetReqDaifuInfoTime": "4135",
    "params": "",
    "details": ""
}
 * </pre>
 * @author blake
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Crk2PayCoreSendResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5952092263025166184L;
	
	private String requestDaifuCode;
	private String requestDaifuOrderState;
	private String requestDaifuOid;
	private String requestDaifuErrorMsg;
	private String requestDaifuAmount;
	private String requestDaifuOrderId;
	private String requestDaifuOrderCreateTime;
	private String requestDaifuChannelBankName;
	private String requestDaifuOtherParam;
	private String requestDaifuTotalTime;
	private String requestDaifuChannelTime;
	private String requestDaifuGetReqDaifuInfoTime;
	private String params;
	private String details;
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
	 * @return the requestDaifuOrderState
	 */
	public String getRequestDaifuOrderState() {
		return requestDaifuOrderState;
	}
	/**
	 * @param requestDaifuOrderState the requestDaifuOrderState to set
	 */
	public void setRequestDaifuOrderState(String requestDaifuOrderState) {
		this.requestDaifuOrderState = requestDaifuOrderState;
	}
	/**
	 * @return the requestDaifuOid
	 */
	public String getRequestDaifuOid() {
		return requestDaifuOid;
	}
	/**
	 * @param requestDaifuOid the requestDaifuOid to set
	 */
	public void setRequestDaifuOid(String requestDaifuOid) {
		this.requestDaifuOid = requestDaifuOid;
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
	 * @return the requestDaifuAmount
	 */
	public String getRequestDaifuAmount() {
		return requestDaifuAmount;
	}
	/**
	 * @param requestDaifuAmount the requestDaifuAmount to set
	 */
	public void setRequestDaifuAmount(String requestDaifuAmount) {
		this.requestDaifuAmount = requestDaifuAmount;
	}
	/**
	 * @return the requestDaifuOrderId
	 */
	public String getRequestDaifuOrderId() {
		return requestDaifuOrderId;
	}
	/**
	 * @param requestDaifuOrderId the requestDaifuOrderId to set
	 */
	public void setRequestDaifuOrderId(String requestDaifuOrderId) {
		this.requestDaifuOrderId = requestDaifuOrderId;
	}
	/**
	 * @return the requestDaifuOrderCreateTime
	 */
	public String getRequestDaifuOrderCreateTime() {
		return requestDaifuOrderCreateTime;
	}
	/**
	 * @param requestDaifuOrderCreateTime the requestDaifuOrderCreateTime to set
	 */
	public void setRequestDaifuOrderCreateTime(String requestDaifuOrderCreateTime) {
		this.requestDaifuOrderCreateTime = requestDaifuOrderCreateTime;
	}
	/**
	 * @return the requestDaifuChannelBankName
	 */
	public String getRequestDaifuChannelBankName() {
		return requestDaifuChannelBankName;
	}
	/**
	 * @param requestDaifuChannelBankName the requestDaifuChannelBankName to set
	 */
	public void setRequestDaifuChannelBankName(String requestDaifuChannelBankName) {
		this.requestDaifuChannelBankName = requestDaifuChannelBankName;
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
	 * @return the requestDaifuTotalTime
	 */
	public String getRequestDaifuTotalTime() {
		return requestDaifuTotalTime;
	}
	/**
	 * @param requestDaifuTotalTime the requestDaifuTotalTime to set
	 */
	public void setRequestDaifuTotalTime(String requestDaifuTotalTime) {
		this.requestDaifuTotalTime = requestDaifuTotalTime;
	}
	/**
	 * @return the requestDaifuChannelTime
	 */
	public String getRequestDaifuChannelTime() {
		return requestDaifuChannelTime;
	}
	/**
	 * @param requestDaifuChannelTime the requestDaifuChannelTime to set
	 */
	public void setRequestDaifuChannelTime(String requestDaifuChannelTime) {
		this.requestDaifuChannelTime = requestDaifuChannelTime;
	}
	/**
	 * @return the requestDaifuGetReqDaifuInfoTime
	 */
	public String getRequestDaifuGetReqDaifuInfoTime() {
		return requestDaifuGetReqDaifuInfoTime;
	}
	/**
	 * @param requestDaifuGetReqDaifuInfoTime the requestDaifuGetReqDaifuInfoTime to set
	 */
	public void setRequestDaifuGetReqDaifuInfoTime(String requestDaifuGetReqDaifuInfoTime) {
		this.requestDaifuGetReqDaifuInfoTime = requestDaifuGetReqDaifuInfoTime;
	}
	/**
	 * @return the params
	 */
	public String getParams() {
		return params;
	}
	/**
	 * @param params the params to set
	 */
	public void setParams(String params) {
		this.params = params;
	}
	/**
	 * @return the details
	 */
	public String getDetails() {
		return details;
	}
	/**
	 * @param details the details to set
	 */
	public void setDetails(String details) {
		this.details = details;
	}
}
