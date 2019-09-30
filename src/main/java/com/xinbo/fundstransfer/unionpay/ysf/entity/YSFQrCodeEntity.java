/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 银联云闪付二维码
 * @author blake
 *
 */
public class YSFQrCodeEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3645107883406187358L;
	
	/**
	 * 二维码付款金额
	 */
	private BigDecimal money;
	
	/**
	 * 二维码内容
	 */
	private String qrContent;
	
	/**
	 * 云闪付账号
	 */
	private String ysfAccount;
	
	/**
	 * 银行账号
	 */
	private String bindedBankAccount;
	
	/**
	 * app生成二维码日期
	 */
	private Long genDate;

	/**
	 * @return the money
	 */
	public BigDecimal getMoney() {
		return money;
	}

	/**
	 * @param money the money to set
	 */
	public void setMoney(BigDecimal money) {
		this.money = money;
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
	public String getBindedBankAccount() {
		return bindedBankAccount;
	}

	/**
	 * @param bindedBankAccount the account to set
	 */
	public void setBindedBankAccount(String bindedBankAccount) {
		this.bindedBankAccount = bindedBankAccount;
	}

	/**
	 * @return the genDate
	 */
	public Long getGenDate() {
		return genDate;
	}

	/**
	 * @param genDate the genDate to set
	 */
	public void setGenDate(Long genDate) {
		this.genDate = genDate;
	}

	/**
	 * @return the ysfAccount
	 */
	public String getYsfAccount() {
		return ysfAccount;
	}

	/**
	 * @param ysfAccount the ysfAccount to set
	 */
	public void setYsfAccount(String ysfAccount) {
		this.ysfAccount = ysfAccount;
	}
}
