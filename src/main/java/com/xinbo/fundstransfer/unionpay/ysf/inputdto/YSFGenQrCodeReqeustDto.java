/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import java.io.Serializable;

/**
 * @author blake
 *
 */
public class YSFGenQrCodeReqeustDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4257338711446680658L;
	
	/**
	 * 云闪付账号
	 */
	private String ysfAccount;
	
	/**
	 * 绑定的银行卡
	 */
	private String bindedBankAccount;

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

	/**
	 * @return the bindedBankAccount
	 */
	public String getBindedBankAccount() {
		return bindedBankAccount;
	}

	/**
	 * @param bindedBankAccount the bindedBankAccount to set
	 */
	public void setBindedBankAccount(String bindedBankAccount) {
		this.bindedBankAccount = bindedBankAccount;
	}
}
