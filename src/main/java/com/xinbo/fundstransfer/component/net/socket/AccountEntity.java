package com.xinbo.fundstransfer.component.net.socket;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 银行帐号实体类
 * 
 * 
 *
 */

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountEntity {

	public AccountEntity() {

	}

	public AccountEntity(Integer id) {
		this.id = id;
	}

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
	 * 帐号子类型，0银行入款卡 1支付宝入款卡
	 */
	int subType;

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
	 * 编号
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
	 * 当日出款限额
	 */
	private Integer limitOutOne;

	/**
	 * 余额限额，银行卡余额超过这个值要下发
	 */
	private Integer limitBalance;

	/**
	 * 出款卡最低余额
	 */
	private Integer lowestOut;

	/**
	 * 银行余额
	 */
	private BigDecimal bankBalance;
	
	/**
	 * 用途
	 */
	private Integer flag;

	/**
	 * 手机号
	 */
	private String mobile;


	/**
	 * 金额，用在出款卡要打款金额
	 */
	private BigDecimal amount;

	private String sign;

	private String hook;

	private String hub;

	private String bing;

	private String sign_;

	private String hook_;

	private String hub_;

	private String bing_;


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

	public int getSubType() {
		return subType;
	}

	public void setSubType(int subType) {
		this.subType = subType;
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
	
	

	public Integer getLimitOutOne() {
		return limitOutOne;
	}

	public void setLimitOutOne(Integer limitOutOne) {
		this.limitOutOne = limitOutOne;
	}

	public Integer getFlag() {
		return flag;
	}

	public void setFlag(Integer flag) {
		this.flag = flag;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getSign_() {
		return sign_;
	}

	public void setSign_(String sign_) {
		this.sign_ = sign_;
	}

	public String getHook_() {
		return hook_;
	}

	public void setHook_(String hook_) {
		this.hook_ = hook_;
	}

	public String getHub_() {
		return hub_;
	}

	public void setHub_(String hub_) {
		this.hub_ = hub_;
	}

	public String getBing_() {
		return bing_;
	}

	public void setBing_(String bing_) {
		this.bing_ = bing_;
	}

	@Override
	public String toString() {
		return "ID:" + this.id + ",Account:" + this.account + this.bank;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AccountEntity) {
			return ((AccountEntity) obj).getId().intValue() == this.id.intValue();
		}
		return false;
	}

}
