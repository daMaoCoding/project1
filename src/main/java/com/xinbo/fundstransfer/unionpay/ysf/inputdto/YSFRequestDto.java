/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import java.io.Serializable;
import java.util.List;

/**
 * 平台-用户获取银联云闪付付款二维码-请求参数
 * @author blake
 *
 */
public class YSFRequestDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6668428483268302514L;
	
	/**
	 * 业主编码-id
	 */
	private Integer handicap;
	
	/**
	 * 业主编码-盘口号
	 */
	private String handicapCode;
	
	/**
	 * 申请入款的用户名
	 */
	private String userName;
	
	/**
	 * 银联云闪付-银行卡号 <br>
	 * 一个通道可能存在多个银行卡号，所有这里使用 list
	 */
	private List<String> accountList;
	
	/**
	 * 申请入款金额（正整数）
	 */
	private Integer amount;

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
	 * @return the accountList
	 */
	public List<String> getAccountList() {
		return accountList;
	}

	/**
	 * @param accountList the accountList to set
	 */
	public void setAccountList(List<String> accountList) {
		this.accountList = accountList;
	}

	/**
	 * @return the amount
	 */
	public Integer getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(Integer amount) {
		this.amount = amount;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
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
