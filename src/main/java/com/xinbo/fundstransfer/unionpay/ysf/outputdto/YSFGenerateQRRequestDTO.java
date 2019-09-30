/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.outputdto;

import java.io.Serializable;

/**
 * 下发app生成二维码的dto
 * @author blake
 *
 */
public class YSFGenerateQRRequestDTO implements Serializable {

	private static final long serialVersionUID = -763693507568753343L;
	private String ysfAccount;// 云闪付账号
	private String bindedBankAccount;// 绑定的银行卡
	private Double[] expectAmounts;// 期望生成收款二维码的常用金额

	public String getYsfAccount() {
		return ysfAccount;
	}

	public void setYsfAccount(String ysfAccount) {
		this.ysfAccount = ysfAccount;
	}

	public String getBindedBankAccount() {
		return bindedBankAccount;
	}

	public void setBindedBankAccount(String bindedBankAccount) {
		this.bindedBankAccount = bindedBankAccount;
	}

	public Double[] getExpectAmounts() {
		return expectAmounts;
	}

	public void setExpectAmounts(Double[] expectAmounts) {
		this.expectAmounts = expectAmounts;
	}
	
}
