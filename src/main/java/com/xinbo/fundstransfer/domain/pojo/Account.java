package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Account {
	String mobile;
	/**
	 * 帐号ID
	 */
	Integer id;

	/**
	 * 帐号
	 */
	String account;
	/**
	 * 帐号类型，值定义参考枚举类AccountType
	 */
	int type;

	/**
	 * 开户行
	 */
	String bank;

	/**
	 * 开户人
	 */
	String owner;
	/**
	 * 持卡人ID
	 */
	Integer holder;

	/**
	 * 帐号状态，值定义参考枚举类AccountStatus
	 */
	int status;

	/**
	 * 别名
	 */
	String alias;

	/**
	 * 当日入款限额
	 */
	private Integer limitIn;

	/**
	 * 当日出款限额
	 */
	private Integer limitOut;

	/**
	 * 余额限额，银行卡余额超过这个值要下发
	 */
	private Integer limitBalance;

	/**
	 * 出款卡最低余额
	 */
	private Integer lowestOut;

	/**
	 * 登录密码
	 */
	private String sign;

	private String hook;

	/**
	 * 交易密码
	 */
	private String hub;

	/**
	 * U盾密码
	 */
	private String bing;

	String token;
	/**
	 * 运行状态，值定义参考枚举类RunningStatusEnum
	 */
	int runningStatus;

	/**
	 * 最后一次正常流水抓取时间
	 */
	long lastTime;

	/**
	 * 抓取休眠间隔，为null请取全局的interval设置
	 */
	Integer interval;

	/**
	 * 银行余额
	 */
	private BigDecimal bankBalance;
	/**
	 * 金额，用在出款卡要打款金额
	 */
	private BigDecimal amount;

	public Account() {
	}

	public Account(BizAccount acc) {
		mobile = acc.getMobile();
		id = acc.getId();
		account = acc.getAccount();
		type = acc.getType();
		bank = acc.getBankType();
		owner = acc.getOwner();
		holder = acc.getHolder();
		alias = acc.getAlias();
		status = acc.getStatus();
		limitIn = acc.getLimitIn();
		limitOut = acc.getLimitOut();
		limitBalance = acc.getLimitBalance();
		lowestOut = acc.getLowestOut();
		try {
			if (StringUtils.isNotBlank(acc.getSign_())) {
				sign = FundTransferEncrypter.encryptCabana(FundTransferEncrypter.decryptDb(acc.getSign_().trim()));
			}
			if (StringUtils.isNotBlank(acc.getBing_())) {
				bing = FundTransferEncrypter.encryptCabana(FundTransferEncrypter.decryptDb(acc.getBing_().trim()));
			}
			if (StringUtils.isNotBlank(acc.getHook_())) {
				hook = FundTransferEncrypter.encryptCabana(FundTransferEncrypter.decryptDb(acc.getHook_().trim()));
			}
			if (StringUtils.isNotBlank(acc.getHub_())) {
				hub = FundTransferEncrypter.encryptCabana(FundTransferEncrypter.decryptDb(acc.getHub_().trim()));
			}
		} catch (Exception e) {
		}
		interval = acc.getInterval();
		bankBalance = acc.getBankBalance();
		amount = acc.getAmount();
		if (acc.getType() == 1 && Objects.nonNull(acc.getSubType()))
			runningStatus = acc.getSubType();
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getBank() {
		return bank;
	}

	public void setBank(String bank) {
		this.bank = bank;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getHook() {
		return hook;
	}

	public void setHook(String hook) {
		this.hook = hook;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public long getLastTime() {
		return lastTime;
	}

	public void setLastTime(long lastTime) {
		this.lastTime = lastTime;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Integer getHolder() {
		return holder;
	}

	public void setHolder(Integer holder) {
		this.holder = holder;
	}

	public String getHub() {
		return hub;
	}

	public void setHub(String hub) {
		this.hub = hub;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Integer getLimitIn() {
		return limitIn;
	}

	public void setLimitIn(Integer limitIn) {
		this.limitIn = limitIn;
	}

	public Integer getLimitOut() {
		return limitOut;
	}

	public void setLimitOut(Integer limitOut) {
		this.limitOut = limitOut;
	}

	public Integer getLowestOut() {
		return lowestOut;
	}

	public void setLowestOut(Integer lowestOut) {
		this.lowestOut = lowestOut;
	}

	public int getRunningStatus() {
		return runningStatus;
	}

	public void setRunningStatus(int runningStatus) {
		this.runningStatus = runningStatus;
	}

	public Integer getInterval() {
		return interval;
	}

	public void setInterval(Integer interval) {
		this.interval = interval;
	}

	public String getBing() {
		return bing;
	}

	public void setBing(String bing) {
		this.bing = bing;
	}

	public Integer getLimitBalance() {
		return limitBalance;
	}

	public void setLimitBalance(Integer limitBalance) {
		this.limitBalance = limitBalance;
	}

	public BigDecimal getBankBalance() {
		return bankBalance;
	}

	public void setBankBalance(BigDecimal bankBalance) {
		this.bankBalance = bankBalance;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
