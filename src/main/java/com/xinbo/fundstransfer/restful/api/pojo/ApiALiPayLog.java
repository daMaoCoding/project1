package com.xinbo.fundstransfer.restful.api.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ApiALiPayLog {
	private String tradingTime;
	private String amount;
	private String balance;
	private String summary;
	private String depositor;
	private String account;
	private String token;

	/**
	 * @return the tradingTime
	 */
	public String getTradingTime() {
		return tradingTime;
	}

	/**
	 * @param tradingTime
	 *            the tradingTime to set
	 */
	public void setTradingTime(String tradingTime) {
		this.tradingTime = tradingTime;
	}

	/**
	 * @return the amount
	 */
	public String getAmount() {
		return amount;
	}

	/**
	 * @param amount
	 *            the amount to set
	 */
	public void setAmount(String amount) {
		this.amount = amount;
	}

	/**
	 * @return the balance
	 */
	public String getBalance() {
		return balance;
	}

	/**
	 * @param balance
	 *            the balance to set
	 */
	public void setBalance(String balance) {
		this.balance = balance;
	}

	/**
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * @param summary
	 *            the summary to set
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * @return the depositor
	 */
	public String getDepositor() {
		return depositor;
	}

	/**
	 * @param depositor
	 *            the depositor to set
	 */
	public void setDepositor(String depositor) {
		this.depositor = depositor;
	}

	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * @param account
	 *            the account to set
	 */
	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @param token
	 *            the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

}
