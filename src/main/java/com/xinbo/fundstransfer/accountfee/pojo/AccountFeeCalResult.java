/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.pojo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 第三方下发手续费计算结果
 * @author Blake
 *
 */
public class AccountFeeCalResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5143306206848450895L;

	private String bankType;
	
	private String account;
	
	/**
	 * 收费方式：0-从商户余额扣取手续费 1-从到账金额扣取手续费
	 */
	private Byte feeType;
	
	/**<pre>
	 * 传入金额，单次可下余额
	 * 
	 */
	private BigDecimal calMoney;
	
	/**<pre>
	 * 计算后所得金额
	 * 当feeType = 0-从商户余额扣取手续费 时 calMoney = money;
	 * 当feeType = 1-从到账金额扣取手续费 时 calMoney = money + fee;
	 * </pre>
	 */
	private BigDecimal money;
	
	/**
	 * 计算所得手续费
	 */
	private BigDecimal fee;
	
	/**
	 * 描述如何 fee 如何计算所得
	 */
	private String calDesc;

	public Byte getFeeType() {
		return feeType;
	}

	public void setFeeType(Byte feeType) {
		this.feeType = feeType;
	}

	public BigDecimal getCalMoney() {
		return calMoney;
	}

	public void setCalMoney(BigDecimal calMoney) {
		this.calMoney = calMoney;
	}

	public BigDecimal getMoney() {
		return money;
	}

	public void setMoney(BigDecimal money) {
		this.money = money;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	public String getCalDesc() {
		return calDesc;
	}

	public void setCalDesc(String calDesc) {
		this.calDesc = calDesc;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}
}
