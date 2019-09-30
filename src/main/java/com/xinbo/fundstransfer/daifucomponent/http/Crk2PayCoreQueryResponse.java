/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.http;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 查询第三方代付-payCore返回结果对象<pre>
 * {
    "responseOrderID": "20190122113634826105"   //代付订单号
    "responseDaifuCode": "SUCCESS",  //默认只会发送成功的消息，如果有收到ERROR的无需任何处理
    "responseOrderState": "ERROR",     //这个重要：代付订单状态，PAYING-支付中，SUCCESS-代付成功，ERROR-代付失败或者是取消的，UNKNOW-未知状态(会很少出现长见于第三方回调的数据状态有改变无法正确获取，db无需处理就好)
    "responseDaifuAmount": "5000",  //代付金额分，会与发送代付请求时一致。 
    "responseDaifuChannel": "DUOFU_BANK_WEB_DF_ZGJSYH",  //通道名称
    "responseDaifuErrorMsg": "第三方回调确定转账取消或失败。{\"order_no\":\"20181008165128\",\"notify_type\":\"back_notify\",\"merchant_code\":\"3018687\",\"return_params\":\"\",\"trade_time\":\"1538989431\",\"order_amount\":\"50.00\",\"trade_status\":\"success\",\"paid_amount\":\"50.00\",\"sign\":\"1a980312c256b10274f04e5020df3ff3\",\"trade_no\":\"118100816512990655\",\"order_time\":\"1538988689\"}",
    "responseDaifuMemberId": "3018687",  //商户号
    "responseDaifuMsg": "success",  //消息，没什么用
    "responseDaifuOid": "100",   //业主OID
    "responseDaifuOrderCreateTime": "1548055923000",  //代付订单创建时间
    "responseDaifuOtherParam": "这是其他参数将会原样返回", //传递的其他参数
    "responseDaifuSign": "0B64049055A550A5770206F1C3204321",  //pay-core 与业务系统确认身份的签名，目前没用
    "responseDaifuTotalTime": 0     //超时时间 
 }
 * </pre>
 * @author blake
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Crk2PayCoreQueryResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8352954194661315485L;
	
	private String responseOrderID;
	private String responseDaifuCode;
	private String responseOrderState;
	private String responseDaifuAmount;
	private String responseDaifuChannel;
	private String responseDaifuErrorMsg;
	private String responseDaifuMemberId;
	private String responseDaifuMsg;
	private String responseDaifuOid;
	private String responseDaifuOrderCreateTime;
	private String responseDaifuOtherParam;
	private String responseDaifuSign;
	private String responseDaifuTotalTime;
	/**
	 * @return the responseOrderID
	 */
	public String getResponseOrderID() {
		return responseOrderID;
	}
	/**
	 * @param responseOrderID the responseOrderID to set
	 */
	public void setResponseOrderID(String responseOrderID) {
		this.responseOrderID = responseOrderID;
	}
	/**
	 * @return the responseDaifuCode
	 */
	public String getResponseDaifuCode() {
		return responseDaifuCode;
	}
	/**
	 * @param responseDaifuCode the responseDaifuCode to set
	 */
	public void setResponseDaifuCode(String responseDaifuCode) {
		this.responseDaifuCode = responseDaifuCode;
	}
	/**
	 * @return the responseOrderState
	 */
	public String getResponseOrderState() {
		return responseOrderState;
	}
	/**
	 * @param responseOrderState the responseOrderState to set
	 */
	public void setResponseOrderState(String responseOrderState) {
		this.responseOrderState = responseOrderState;
	}
	/**
	 * @return the responseDaifuAmount
	 */
	public String getResponseDaifuAmount() {
		return responseDaifuAmount;
	}
	/**
	 * @param responseDaifuAmount the responseDaifuAmount to set
	 */
	public void setResponseDaifuAmount(String responseDaifuAmount) {
		this.responseDaifuAmount = responseDaifuAmount;
	}
	/**
	 * @return the responseDaifuChannel
	 */
	public String getResponseDaifuChannel() {
		return responseDaifuChannel;
	}
	/**
	 * @param responseDaifuChannel the responseDaifuChannel to set
	 */
	public void setResponseDaifuChannel(String responseDaifuChannel) {
		this.responseDaifuChannel = responseDaifuChannel;
	}
	/**
	 * @return the responseDaifuErrorMsg
	 */
	public String getResponseDaifuErrorMsg() {
		return responseDaifuErrorMsg;
	}
	/**
	 * @param responseDaifuErrorMsg the responseDaifuErrorMsg to set
	 */
	public void setResponseDaifuErrorMsg(String responseDaifuErrorMsg) {
		this.responseDaifuErrorMsg = responseDaifuErrorMsg;
	}
	/**
	 * @return the responseDaifuMemberId
	 */
	public String getResponseDaifuMemberId() {
		return responseDaifuMemberId;
	}
	/**
	 * @param responseDaifuMemberId the responseDaifuMemberId to set
	 */
	public void setResponseDaifuMemberId(String responseDaifuMemberId) {
		this.responseDaifuMemberId = responseDaifuMemberId;
	}
	/**
	 * @return the responseDaifuMsg
	 */
	public String getResponseDaifuMsg() {
		return responseDaifuMsg;
	}
	/**
	 * @param responseDaifuMsg the responseDaifuMsg to set
	 */
	public void setResponseDaifuMsg(String responseDaifuMsg) {
		this.responseDaifuMsg = responseDaifuMsg;
	}
	/**
	 * @return the responseDaifuOid
	 */
	public String getResponseDaifuOid() {
		return responseDaifuOid;
	}
	/**
	 * @param responseDaifuOid the responseDaifuOid to set
	 */
	public void setResponseDaifuOid(String responseDaifuOid) {
		this.responseDaifuOid = responseDaifuOid;
	}
	/**
	 * @return the responseDaifuOrderCreateTime
	 */
	public String getResponseDaifuOrderCreateTime() {
		return responseDaifuOrderCreateTime;
	}
	/**
	 * @param responseDaifuOrderCreateTime the responseDaifuOrderCreateTime to set
	 */
	public void setResponseDaifuOrderCreateTime(String responseDaifuOrderCreateTime) {
		this.responseDaifuOrderCreateTime = responseDaifuOrderCreateTime;
	}
	/**
	 * @return the responseDaifuOtherParam
	 */
	public String getResponseDaifuOtherParam() {
		return responseDaifuOtherParam;
	}
	/**
	 * @param responseDaifuOtherParam the responseDaifuOtherParam to set
	 */
	public void setResponseDaifuOtherParam(String responseDaifuOtherParam) {
		this.responseDaifuOtherParam = responseDaifuOtherParam;
	}
	/**
	 * @return the responseDaifuSign
	 */
	public String getResponseDaifuSign() {
		return responseDaifuSign;
	}
	/**
	 * @param responseDaifuSign the responseDaifuSign to set
	 */
	public void setResponseDaifuSign(String responseDaifuSign) {
		this.responseDaifuSign = responseDaifuSign;
	}
	/**
	 * @return the responseDaifuTotalTime
	 */
	public String getResponseDaifuTotalTime() {
		return responseDaifuTotalTime;
	}
	/**
	 * @param responseDaifuTotalTime the responseDaifuTotalTime to set
	 */
	public void setResponseDaifuTotalTime(String responseDaifuTotalTime) {
		this.responseDaifuTotalTime = responseDaifuTotalTime;
	}
}
