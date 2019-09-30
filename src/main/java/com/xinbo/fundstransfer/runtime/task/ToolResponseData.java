package com.xinbo.fundstransfer.runtime.task;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizThirdLog;

@JsonInclude(Include.NON_NULL)
public class ToolResponseData {
	/**
	 * 余额
	 */
	private Float balance;

	/**
	 * 可用余额
	 */
	private Float usableBalance;
	/**
	 * 当前时间点
	 */
	private String thisTimeStamp;

	/**
	 * 银行流水记录
	 */
	private List<BizBankLog> banklogs;

	/**
	 * 第三方流水记录
	 */
	private List<BizThirdLog> thirdlogs;

	public Float getBalance() {
		return balance;
	}

	public void setBalance(Float balance) {
		this.balance = balance;
	}

	public Float getUsableBalance() {
		return usableBalance;
	}

	public void setUsableBalance(Float usableBalance) {
		this.usableBalance = usableBalance;
	}

	public List<BizBankLog> getBanklogs() {
		return banklogs;
	}

	public void setBanklogs(List<BizBankLog> banklogs) {
		this.banklogs = banklogs;
	}

	public List<BizThirdLog> getThirdlogs() {
		return thirdlogs;
	}

	public void setThirdlogs(List<BizThirdLog> thirdlogs) {
		this.thirdlogs = thirdlogs;
	}

	public String getThisTimeStamp() {
		return thisTimeStamp;
	}

	public void setThisTimeStamp(String thisTimeStamp) {
		this.thisTimeStamp = thisTimeStamp;
	}

}
