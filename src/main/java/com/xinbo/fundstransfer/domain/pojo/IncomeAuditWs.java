package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.xinbo.fundstransfer.component.net.socket.RunningStatusEnum;
import com.xinbo.fundstransfer.domain.enums.IncomeAuditWsEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by 000 on 2017/7/19.
 */
@JsonInclude(Include.NON_NULL)
public class IncomeAuditWs {

	private Integer accountId;

	private String account;

	private String bankType;

	private String bankName;

	private String alias;

	private String owner;
	// 返回前端消息内容
	private String message;

	/**
	 * @see com.xinbo.fundstransfer.domain.enums.AccountStatus
	 */
	private Integer status;

	/**
	 * 不要设默认值
	 *
	 * @see com.xinbo.fundstransfer.domain.enums.IncomeAuditWsEnum
	 */
	private Integer incomeAuditWsFrom;

	/**
	 * @see com.xinbo.fundstransfer.component.net.socket.RunningStatusEnum
	 */
	private Integer monitor = RunningStatusEnum.OFFLINE.ordinal();

	public IncomeAuditWs() {
	}

	public IncomeAuditWs(Integer accountId, String account, Integer status, IncomeAuditWsEnum incomeAuditWsFrom,
			Integer monitor, String bankType, String bankName, String alias, String owner, String message) {
		this.accountId = accountId;
		this.account = account;
		this.status = status;
		this.incomeAuditWsFrom = incomeAuditWsFrom == null ? this.incomeAuditWsFrom : incomeAuditWsFrom.ordinal();
		this.monitor = monitor == null ? this.monitor : monitor;
		this.bankType = StringUtils.trimToEmpty(bankType);
		this.bankName = StringUtils.trimToEmpty(bankName);
		this.alias = StringUtils.trimToEmpty(alias);
		this.owner = StringUtils.trimToEmpty(owner);
		this.message = StringUtils.trimToEmpty(message);
	}

	public String msg(Integer accountId, String account, Integer status, IncomeAuditWsEnum incomeAuditWsFrom,
			Integer monitor, String bankType, String bankName, String alias, String owner, String message) {
		return null;
	}

	@Override
	public String toString() {
		return "IncomeAuditWs{" + "accountId=" + accountId + ", account='" + account + '\'' + ", message='" + message
				+ '\'' + '}';
	}

	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public Integer getIncomeAuditWsFrom() {
		return incomeAuditWsFrom;
	}

	public void setIncomeAuditWsFrom(Integer incomeAuditWsFrom) {
		this.incomeAuditWsFrom = incomeAuditWsFrom;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getMonitor() {
		return monitor;
	}

	public void setMonitor(Integer monitor) {
		this.monitor = monitor;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
