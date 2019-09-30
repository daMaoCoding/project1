/**
 * 
 */
package com.xinbo.fundstransfer.utils.randutil;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 适用随机金额、入款卡和扫描卡
 * @author blake
 *
 */
public class InAccountRandUtilResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8286869937377036018L;
	
	/**
	 * 扫码卡卡号
	 */
	private String scanCardNum;
	
	/**
	 * 转账卡卡号
	 */
	private String transferCardNum;
	
	/**
	 * 随机金额
	 */
	private BigDecimal amount;

	/**
	 * 获取扫描卡卡号
	 * @return the scanCardNum
	 */
	public String getScanCardNum() {
		return scanCardNum;
	}

	/**
	 * @param scanCardNum the scanCardNum to set
	 */
	public void setScanCardNum(String scanCardNum) {
		this.scanCardNum = scanCardNum;
	}

	/**
	 * 获取转账卡卡号
	 * @return the transferCardNum
	 */
	public String getTransferCardNum() {
		return transferCardNum;
	}

	/**
	 * @param transferCardNum the transferCardNum to set
	 */
	public void setTransferCardNum(String transferCardNum) {
		this.transferCardNum = transferCardNum;
	}

	/**
	 * @return the amount
	 */
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

}
