/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.output;

import java.io.Serializable;

/**
 * 支付平台通过订单号查询支付支付参数
 * <pre>
 * {
 * 	"aPI_AMOUNT": "5000",        
    "aPI_CHANNEL_BANK_NAME": "DUOFU_BANK_WEB_DF_ZGJSYH",
    "aPI_CUSTOMER_ACCOUNT": "wangxiaojun",
    "aPI_CUSTOMER_BANK_BRANCH": "陕西省分行",//mark 2019-05-23 需求5897 字段意义修改为分行所在省份,如：广东省
    "aPI_CUSTOMER_BANK_NAME": "中国建设银行",
    "aPI_CUSTOMER_BANK_NUMBER": "6217004160022335741",
    "aPI_CUSTOMER_BANK_SUB_BRANCH": "永寿县支行", //mark 2019-05-23 需求5897 字段意义修改为支行所在城市,如：广州市
    "aPI_CUSTOMER_NAME": "王小军",
    "aPI_Client_IP": "123.123.123.123",
    "aPI_KEY": "12919E9D90647E63040F17E67578234......3",  //加密的
    "aPI_MEMBERID": "MD63309719",
    "aPI_NOTIFY_URL_PREFIX": "http://66p.nsqmz6812.com:30000",
    "aPI_OID": "100",
    "aPI_ORDER_ID": "20190124115526959909",
    "aPI_ORDER_STATE": "0",   //DB内部订单状态，pay-core记录，无其他用处
    "aPI_OTHER_PARAM": "这是其他参数将会原样返回",  //这是db要传给fore的参数
    "aPI_OrDER_TIME": "1548054754000",
    "aPI_PUBLIC_KEY": ""
 * }</pre>
 * @author blake
 *
 */
public class DaifuConfigParamTo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7394855344379575197L;
	
	private String aPI_AMOUNT;
    private String aPI_CHANNEL_BANK_NAME;
    private String aPI_CUSTOMER_ACCOUNT;
    private String aPI_CUSTOMER_BANK_BRANCH;
    private String aPI_CUSTOMER_BANK_NAME;
    private String aPI_CUSTOMER_BANK_NUMBER;
    private String aPI_CUSTOMER_BANK_SUB_BRANCH;
    private String aPI_CUSTOMER_NAME;
    private String aPI_Client_IP;
    private String aPI_KEY;
    private String aPI_MEMBERID;
    private String aPI_NOTIFY_URL_PREFIX;
    private String aPI_OID;
    private String aPI_ORDER_ID;
    private String aPI_ORDER_STATE;
    private String aPI_OTHER_PARAM;
    private String aPI_OrDER_TIME;
    private String aPI_PUBLIC_KEY;
    
	/**
	 * @return the aPI_AMOUNT
	 */
	public String getaPI_AMOUNT() {
		return aPI_AMOUNT;
	}
	/**
	 * @param aPI_AMOUNT the aPI_AMOUNT to set
	 */
	public void setaPI_AMOUNT(String aPI_AMOUNT) {
		this.aPI_AMOUNT = aPI_AMOUNT;
	}
	/**
	 * @return the aPI_CHANNEL_BANK_NAME
	 */
	public String getaPI_CHANNEL_BANK_NAME() {
		return aPI_CHANNEL_BANK_NAME;
	}
	/**
	 * @param aPI_CHANNEL_BANK_NAME the aPI_CHANNEL_BANK_NAME to set
	 */
	public void setaPI_CHANNEL_BANK_NAME(String aPI_CHANNEL_BANK_NAME) {
		this.aPI_CHANNEL_BANK_NAME = aPI_CHANNEL_BANK_NAME;
	}
	/**
	 * @return the aPI_CUSTOMER_ACCOUNT
	 */
	public String getaPI_CUSTOMER_ACCOUNT() {
		return aPI_CUSTOMER_ACCOUNT;
	}
	/**
	 * @param aPI_CUSTOMER_ACCOUNT the aPI_CUSTOMER_ACCOUNT to set
	 */
	public void setaPI_CUSTOMER_ACCOUNT(String aPI_CUSTOMER_ACCOUNT) {
		this.aPI_CUSTOMER_ACCOUNT = aPI_CUSTOMER_ACCOUNT;
	}
	/**
	 * @return the aPI_CUSTOMER_BANK_BRANCH
	 */
	public String getaPI_CUSTOMER_BANK_BRANCH() {
		return aPI_CUSTOMER_BANK_BRANCH;
	}
	/**
	 * @param aPI_CUSTOMER_BANK_BRANCH the aPI_CUSTOMER_BANK_BRANCH to set
	 */
	public void setaPI_CUSTOMER_BANK_BRANCH(String aPI_CUSTOMER_BANK_BRANCH) {
		this.aPI_CUSTOMER_BANK_BRANCH = aPI_CUSTOMER_BANK_BRANCH;
	}
	/**
	 * @return the aPI_CUSTOMER_BANK_NAME
	 */
	public String getaPI_CUSTOMER_BANK_NAME() {
		return aPI_CUSTOMER_BANK_NAME;
	}
	/**
	 * @param aPI_CUSTOMER_BANK_NAME the aPI_CUSTOMER_BANK_NAME to set
	 */
	public void setaPI_CUSTOMER_BANK_NAME(String aPI_CUSTOMER_BANK_NAME) {
		this.aPI_CUSTOMER_BANK_NAME = aPI_CUSTOMER_BANK_NAME;
	}
	/**
	 * @return the aPI_CUSTOMER_BANK_NUMBER
	 */
	public String getaPI_CUSTOMER_BANK_NUMBER() {
		return aPI_CUSTOMER_BANK_NUMBER;
	}
	/**
	 * @param aPI_CUSTOMER_BANK_NUMBER the aPI_CUSTOMER_BANK_NUMBER to set
	 */
	public void setaPI_CUSTOMER_BANK_NUMBER(String aPI_CUSTOMER_BANK_NUMBER) {
		this.aPI_CUSTOMER_BANK_NUMBER = aPI_CUSTOMER_BANK_NUMBER;
	}
	/**
	 * @return the aPI_CUSTOMER_BANK_SUB_BRANCH
	 */
	public String getaPI_CUSTOMER_BANK_SUB_BRANCH() {
		return aPI_CUSTOMER_BANK_SUB_BRANCH;
	}
	/**
	 * @param aPI_CUSTOMER_BANK_SUB_BRANCH the aPI_CUSTOMER_BANK_SUB_BRANCH to set
	 */
	public void setaPI_CUSTOMER_BANK_SUB_BRANCH(String aPI_CUSTOMER_BANK_SUB_BRANCH) {
		this.aPI_CUSTOMER_BANK_SUB_BRANCH = aPI_CUSTOMER_BANK_SUB_BRANCH;
	}
	/**
	 * @return the aPI_CUSTOMER_NAME
	 */
	public String getaPI_CUSTOMER_NAME() {
		return aPI_CUSTOMER_NAME;
	}
	/**
	 * @param aPI_CUSTOMER_NAME the aPI_CUSTOMER_NAME to set
	 */
	public void setaPI_CUSTOMER_NAME(String aPI_CUSTOMER_NAME) {
		this.aPI_CUSTOMER_NAME = aPI_CUSTOMER_NAME;
	}
	/**
	 * @return the aPI_Client_IP
	 */
	public String getaPI_Client_IP() {
		return aPI_Client_IP;
	}
	/**
	 * @param aPI_Client_IP the aPI_Client_IP to set
	 */
	public void setaPI_Client_IP(String aPI_Client_IP) {
		this.aPI_Client_IP = aPI_Client_IP;
	}
	/**
	 * @return the aPI_KEY
	 */
	public String getaPI_KEY() {
		return aPI_KEY;
	}
	/**
	 * @param aPI_KEY the aPI_KEY to set
	 */
	public void setaPI_KEY(String aPI_KEY) {
		this.aPI_KEY = aPI_KEY;
	}
	/**
	 * @return the aPI_MEMBERID
	 */
	public String getaPI_MEMBERID() {
		return aPI_MEMBERID;
	}
	/**
	 * @param aPI_MEMBERID the aPI_MEMBERID to set
	 */
	public void setaPI_MEMBERID(String aPI_MEMBERID) {
		this.aPI_MEMBERID = aPI_MEMBERID;
	}
	/**
	 * @return the aPI_NOTIFY_URL_PREFIX
	 */
	public String getaPI_NOTIFY_URL_PREFIX() {
		return aPI_NOTIFY_URL_PREFIX;
	}
	/**
	 * @param aPI_NOTIFY_URL_PREFIX the aPI_NOTIFY_URL_PREFIX to set
	 */
	public void setaPI_NOTIFY_URL_PREFIX(String aPI_NOTIFY_URL_PREFIX) {
		this.aPI_NOTIFY_URL_PREFIX = aPI_NOTIFY_URL_PREFIX;
	}
	/**
	 * @return the aPI_OID
	 */
	public String getaPI_OID() {
		return aPI_OID;
	}
	/**
	 * @param aPI_OID the aPI_OID to set
	 */
	public void setaPI_OID(String aPI_OID) {
		this.aPI_OID = aPI_OID;
	}
	/**
	 * @return the aPI_ORDER_ID
	 */
	public String getaPI_ORDER_ID() {
		return aPI_ORDER_ID;
	}
	/**
	 * @param aPI_ORDER_ID the aPI_ORDER_ID to set
	 */
	public void setaPI_ORDER_ID(String aPI_ORDER_ID) {
		this.aPI_ORDER_ID = aPI_ORDER_ID;
	}
	/**
	 * @return the aPI_ORDER_STATE
	 */
	public String getaPI_ORDER_STATE() {
		return aPI_ORDER_STATE;
	}
	/**
	 * @param aPI_ORDER_STATE the aPI_ORDER_STATE to set
	 */
	public void setaPI_ORDER_STATE(String aPI_ORDER_STATE) {
		this.aPI_ORDER_STATE = aPI_ORDER_STATE;
	}
	/**
	 * @return the aPI_OTHER_PARAM
	 */
	public String getaPI_OTHER_PARAM() {
		return aPI_OTHER_PARAM;
	}
	/**
	 * @param aPI_OTHER_PARAM the aPI_OTHER_PARAM to set
	 */
	public void setaPI_OTHER_PARAM(String aPI_OTHER_PARAM) {
		this.aPI_OTHER_PARAM = aPI_OTHER_PARAM;
	}
	/**
	 * @return the aPI_OrDER_TIME
	 */
	public String getaPI_OrDER_TIME() {
		return aPI_OrDER_TIME;
	}
	/**
	 * @param aPI_OrDER_TIME the aPI_OrDER_TIME to set
	 */
	public void setaPI_OrDER_TIME(String aPI_OrDER_TIME) {
		this.aPI_OrDER_TIME = aPI_OrDER_TIME;
	}
	/**
	 * @return the aPI_PUBLIC_KEY
	 */
	public String getaPI_PUBLIC_KEY() {
		return aPI_PUBLIC_KEY;
	}
	/**
	 * @param aPI_PUBLIC_KEY the aPI_PUBLIC_KEY to set
	 */
	public void setaPI_PUBLIC_KEY(String aPI_PUBLIC_KEY) {
		this.aPI_PUBLIC_KEY = aPI_PUBLIC_KEY;
	}

}
