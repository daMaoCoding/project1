/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.outputdto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 云闪付二维码列表查询dto
 * @author blake
 *
 */
public class YSFQrCodeQueryDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9122041306650416376L;
	
	/**
	 * 二维码金额
	 */
	private BigDecimal amount;
	
	/**
	 * 二维码内容
	 */
	private String qrContent;

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
	
}
