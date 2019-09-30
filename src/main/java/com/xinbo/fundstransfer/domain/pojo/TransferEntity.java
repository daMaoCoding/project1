package com.xinbo.fundstransfer.domain.pojo;

import java.util.Date;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TransferEntity {
	/** 来自哪个帐号 */
	Integer fromAccountId;
	/** 去往哪个帐号，冗余字段 */
	Integer toAccountId;
	/** 对方帐号 */
	String account;
	/** 开户人 */
	String owner;
	/** 银行类别 */
	String bankType;
	/** 开户地址 */
	String bankAddr;
	/** 转帐金额 */
	Float amount;
	/** 转帐后余额 */
	Float balance;
	/** 出款任务ID */
	Long taskId;
	/** 获取任务时间，缓存时根据这时间判断是否重新获取 */
	Long acquireTime;
	/** 备注信息，机器出款填写出款机器IP */
	String remark;
	/** 截图URL */
	String screenshot;
	/** 处理结果：1成功；其它值失败 */
	Integer result;
	/** 完成时间 */
	Date time;
	/** token */
	String token;

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getBankAddr() {
		return bankAddr;
	}

	public void setBankAddr(String bankAddr) {
		this.bankAddr = bankAddr;
	}

	public Float getAmount() {
		return amount;
	}

	public void setAmount(Float amount) {
		this.amount = amount;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public Integer getResult() {
		return result;
	}

	public void setResult(Integer result) {
		this.result = result;
	}

	public Integer getFromAccountId() {
		return fromAccountId;
	}

	public void setFromAccountId(Integer fromAccountId) {
		this.fromAccountId = fromAccountId;
	}

	public Integer getToAccountId() {
		return toAccountId;
	}

	public void setToAccountId(Integer toAccountId) {
		this.toAccountId = toAccountId;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Float getBalance() {
		return balance;
	}

	public void setBalance(Float balance) {
		this.balance = balance;
	}

	public Long getAcquireTime() {
		return acquireTime;
	}

	public void setAcquireTime(Long acquireTime) {
		this.acquireTime = acquireTime;
	}

	@Override
	public String toString() {
		return new ReflectionToStringBuilder(this).toString();
	}
}
