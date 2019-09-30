/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.outputdto;

import java.io.Serializable;

/**
 * 平台-用户获取银联云闪付付款二维码-返回参数
 * @author blake
 *
 */
public class YSFResponseDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4961871157617958463L;

	/**
	 * 业主编码-id
	 */
	private Integer handicap;
	
	/**
	 * 业主编码-盘口号
	 */
	private String handicapCode;
	
	/**
	 * 二维码内容
	 */
	private String qrContent;
	
	/**
	 * 二维码所属的银行卡号 
	 * 
	 */
	private String account;
	
	/**
	 * 二维码金额（随机数）
	 */
	private String amount;

	/**
	 * @return the handicap
	 */
	public Integer getHandicap() {
		return handicap;
	}

	/**
	 * @param handicap the handicap to set
	 */
	public void setHandicap(Integer handicap) {
		this.handicap = handicap;
	}

	/**
	 * @return the qrContent
	 */
	public String getQrContent() {
		return qrContent;
	}

	/**
	 * @param qrContent the qrContent to set
	 */
	public void setQrContent(String qrContent) {
		this.qrContent = qrContent;
	}

	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * @param account the account to set
	 */
	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * @return the amount
	 */
	public String getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(String amount) {
		this.amount = amount;
	}

	/**
	 * @return the handicapCode
	 */
	public String getHandicapCode() {
		return handicapCode;
	}

	/**
	 * @param handicapCode the handicapCode to set
	 */
	public void setHandicapCode(String handicapCode) {
		this.handicapCode = handicapCode;
	}
	
}
