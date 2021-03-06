package com.xinbo.fundstransfer.domain.entity;

import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeConfig;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AccountStatInOut;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * BizAccount generated by hbm2java
 */
@Entity
@Table(name = "biz_account")
public class BizAccount implements java.io.Serializable {

	private Integer id;
	private Integer handicapId;
	private String account;
	private String bankType;
	private String bankName;
	private Integer status;
	private Integer type;
	private Integer subType;
	private Integer inCardType;// 公司入款卡类型（1:扫码卡，0:转账卡）
	private String owner;
	private BigDecimal balance;
	private BigDecimal bankBalance;
	private BigDecimal usableBalance;
	private Integer limitIn;
	private Integer limitOut;
	private Long passageId;// 绑定的通道Id
	private BigDecimal minInAmount;// 入款单笔最小限额
	private Byte outEnable;// 是否可以用于出款
	private BigDecimal limitPercentage;// 信用额度百分比
	private BigDecimal minBalance;// 专注卡保留余额

	private String province;// 省份
	private String city;// 城市
	/**
	 * 单笔出款限额（最高）
	 */
	private Integer limitOutOne;
	/**
	 * 单笔出款限额（最低）
	 */
	private Integer limitOutOneLow;
	/**
	 * 当日出款笔数
	 */
	private Integer limitOutCount;
	private Integer limitBalance;
	private Integer lowestOut;
	private String alias;
	private Date createTime;
	private Date updateTime;
	private Integer creator;
	private Integer modifier;
	private Integer holder;
	private String sign;
	private String hook;
	private String hub;
	private String bing;

	private String sign_;
	private String hook_;
	private String hub_;
	private String bing_;

	private Integer currSysLevel;
	private String remark;
	private String remark4Extra;// 储存操作记录用
	private Integer peakBalance;
	private String gps;

	private String handicapName;
	private String levelNameToGroup;
	private String typeStr;
	private String statusStr;
	private String creatorStr;
	private String modifierStr;
	private String createTimeStr;
	private String updateTimeStr;
	private String holderStr;
	private Boolean signAndHook;
	private AccountStatInOut outCount;
	private AccountStatInOut inCount;
	private String currSysLevelName;
	private Integer interval;// 抓取时间间隔

	private Integer bindId;

	private String bindInfo;
	private Float rate;
	private int rateType;
	private String rateValue;
	private BigDecimal totalAmount;
	private BigDecimal feeAmount;
	private BigDecimal amount;
	private BigDecimal mappingAmount;
	private BigDecimal mappedAmount;

	/**
	 * 二维码信息(兼职返利网同步微信/支付宝，收款码信息)
	 */
	private String qrContent;

	/**
	 * 边入边出 1 自购卡（大额专用）2 大额专用（返利网）3 先入后出（正在出）4 先入后出（正在入）5
	 */
	private Integer flagMoreStr;

	/**
	 * 当日收款
	 */
	private BigDecimal incomeAmountDaily;
	/**
	 * 当日出款
	 */
	private BigDecimal outwardAmountDaily;
	/**
	 * 0：未锁定 1：已锁定
	 */
	private int lockByOperator;

	private String lockerStr;

	private Integer locker;

	/**
	 * 入款审核人
	 */
	private String incomeAuditor;

	/**
	 * 下发金额
	 */
	private BigDecimal transInt;

	/**
	 * 下发金额小数值
	 */
	private BigDecimal transRadix;

	private Integer transBlackTo;

	private String mobile;

	/**
	 * 账户标识 1：手机出款 0: PC
	 */
	private Integer flag;

	private String userName;// 兼职姓名
	private String uid;// 兼职uid
	private BigDecimal margin;// 兼职信用额度

	private Integer isOnLine; // 是否在线
	private String bankBalTime; // 银行余额时间
	private String sysBalTime; // 系统余额时间
	private String isRetrieve; // 是否回收 1：回收 2：未回收

	/**
	 * 第三方账号手续费
	 */
	@Transient
	private AccountFeeConfig accountFeeConfig;
	/**
	 * 被设定 1
	 */
	@Transient
	private Byte isSetUpFlag;
	/**
	 * 可下发的金额
	 */
	@Transient
	private BigDecimal drawAbleAmount;
	/**
	 * 可下发的手续费
	 */
	@Transient
	private BigDecimal drawAbleFee;
	@Transient
	private String bankNameUrl;
	/**
	 * 1000一下真实押金
	 */
	private BigDecimal deposit;

	private String thirdLockOper;

	private int freezeTe;

	/**
	 * 二维码内容(解析后的内容) (兼职返利网同步微信/支付宝，收款码信息)
	 */
	@Column(name = "qr_content")
	public String getQrContent() {
		return qrContent;
	}

	public void setQrContent(String qrContent) {
		this.qrContent = qrContent;
	}

	@Transient
	public BigDecimal getDrawAbleAmount() {
		return drawAbleAmount;
	}

	public void setDrawAbleAmount(BigDecimal drawAbleAmount) {
		this.drawAbleAmount = drawAbleAmount;
	}

	@Transient
	public BigDecimal getDrawAbleFee() {
		return drawAbleFee;
	}

	public void setDrawAbleFee(BigDecimal drawAbleFee) {
		this.drawAbleFee = drawAbleFee;
	}

	@Transient
	public String getBankNameUrl() {
		return bankNameUrl;
	}

	public void setBankNameUrl(String bankNameUrl) {
		this.bankNameUrl = bankNameUrl;
	}

	@Transient
	public Byte getIsSetUpFlag() {
		return isSetUpFlag;
	}

	public void setIsSetUpFlag(Byte isSetUpFlag) {
		this.isSetUpFlag = isSetUpFlag;
	}

	public BizAccount() {
	}

	public BizAccount baseToBizAccount(AccountBaseInfo account) {
		this.account = account.getAccount();
		this.id = account.getId();
		this.type = account.getType();
		this.subType = account.getSubType();
		this.status = account.getStatus();
		this.handicapId = account.getHandicapId();
		this.holder = account.getHolder();
		this.account = account.getAccount();
		this.bankName = account.getBankName();
		this.owner = account.getOwner();
		this.bankType = account.getBankType();
		this.currSysLevel = account.getCurrSysLevel();
		this.limitIn = account.getLimitIn();
		this.limitOut = account.getLimitOut();
		this.alias = account.getAlias();
		this.limitBalance = account.getLimitBalance();
		this.lowestOut = account.getLowestOut();
		this.peakBalance = account.getPeakBalance();
		this.limitOutOne = account.getLimitOutOne();
		this.rate = account.getRate();
		this.rateType = account.getRateType();
		this.rateValue = account.getRateValue();
		this.flag = account.getFlag();
		this.mobile = account.getMobile();
		this.limitOutOneLow = account.getLimitOutOneLow();
		this.limitOutCount = account.getLimitOutCount();
		this.outEnable = account.getOutEnable();
		this.limitPercentage = account.getLimitPercentage();
		this.minBalance = account.getMinBalance();
		this.province = account.getProvince();
		this.city = account.getCity();
		return this;
	}

	public BizAccount(String account, String bankType, String bankName, Integer status, Integer type, Integer subType,
			Integer inCardType, String owner, BigDecimal balance, BigDecimal bankBalance, Integer limitIn,
			Integer limitOut, Integer limitOutOne, Integer lowestOut, String alias, Date createTime, Date updateTime,
			Integer creator, Integer modifier, String remark, String remark4Extra) {
		this.account = account;
		this.bankType = bankType;
		this.bankName = bankName;
		this.status = status;
		this.type = type;
		this.subType = subType;
		this.inCardType = inCardType;
		this.owner = owner;
		this.balance = balance;
		this.bankBalance = bankBalance;
		this.limitIn = limitIn;
		this.limitOut = limitOut;
		this.limitOutOne = limitOutOne;
		this.lowestOut = lowestOut;
		this.alias = alias;
		this.createTime = createTime;
		this.updateTime = updateTime;
		this.creator = creator;
		this.modifier = modifier;
		this.remark = remark;
		this.remark4Extra = remark4Extra;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "account", length = 200)
	public String getAccount() {
		return this.account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	@Column(name = "bank_type")
	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	@Column(name = "bank_name", length = 60)
	public String getBankName() {
		return this.bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	@Column(name = "status")
	public Integer getStatus() {
		return this.status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Column(name = "type")
	public Integer getType() {
		return this.type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	@Column(name = "sub_type")
	public Integer getSubType() {
		return subType;
	}

	public void setSubType(Integer subType) {
		this.subType = subType;
	}

	@Column(name = "in_card_type")
	public Integer getInCardType() {
		return inCardType;
	}

	public void setInCardType(Integer inCardType) {
		this.inCardType = inCardType;
	}

	@Column(name = "owner", length = 45)
	public String getOwner() {
		return this.owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Column(name = "balance", precision = 10)
	public BigDecimal getBalance() {
		return this.balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Column(name = "bank_balance", precision = 10)
	public BigDecimal getBankBalance() {
		return this.bankBalance;
	}

	public void setBankBalance(BigDecimal bankBalance) {
		this.bankBalance = bankBalance;
	}

	@Column(name = "usable_balance", precision = 10)
	public BigDecimal getUsableBalance() {
		return usableBalance;
	}

	public void setUsableBalance(BigDecimal usableBalance) {
		this.usableBalance = usableBalance;
	}

	@Column(name = "handicap_id")
	public Integer getHandicapId() {
		return handicapId;
	}

	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}

	@Column(name = "bing")
	public String getBing() {
		return bing;
	}

	public void setBing(String bing) {
		this.bing = bing;
	}

	@Column(name = "limit_in")
	public Integer getLimitIn() {
		return this.limitIn;
	}

	public void setLimitIn(Integer limitIn) {
		this.limitIn = limitIn;
	}

	@Column(name = "limit_out")
	public Integer getLimitOut() {
		return this.limitOut;
	}

	public void setLimitOut(Integer limitOut) {
		this.limitOut = limitOut;
	}

	@Column(name = "limit_out_one")
	public Integer getLimitOutOne() {
		return limitOutOne;
	}

	public void setLimitOutOne(Integer limitOutOne) {
		this.limitOutOne = limitOutOne;
	}

	@Column(name = "lowest_out")
	public Integer getLowestOut() {
		return lowestOut;
	}

	public void setLowestOut(Integer lowestOut) {
		this.lowestOut = lowestOut;
	}

	@Column(name = "alias")
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Column(name = "create_time")
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
		if (null != createTime) {
			SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			this.setCreateTimeStr(SDF.format(createTime));
		}
	}

	@Column(name = "update_time")
	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
		if (null != updateTime) {
			SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			this.setUpdateTimeStr(SDF.format(updateTime));
		}
	}

	@Column(name = "creator")
	public Integer getCreator() {
		return creator;
	}

	public void setCreator(Integer creator) {
		this.creator = creator;
	}

	@Column(name = "modifier")
	public Integer getModifier() {
		return modifier;
	}

	public void setModifier(Integer modifier) {
		this.modifier = modifier;
	}

	@Transient
	public String getTypeName() {
		AccountType type = AccountType.findByTypeId(this.getType());
		return type == null ? StringUtils.EMPTY : type.getMsg();
	}

	@Transient
	public String getStatusName() {
		AccountStatus accountStatus = AccountStatus.findByStatus(this.getStatus());
		return accountStatus == null ? StringUtils.EMPTY : accountStatus.getMsg();
	}

	@Transient
	public String getHandicapName() {
		return handicapName;
	}

	public void setHandicapName(String handicapName) {
		this.handicapName = handicapName;
	}

	@Transient
	public String getLevelNameToGroup() {
		return levelNameToGroup;
	}

	public void setLevelNameToGroup(String levelNameToGroup) {
		this.levelNameToGroup = levelNameToGroup;
	}

	@Transient
	public BigDecimal getIncomeAmountDaily() {
		return incomeAmountDaily;
	}

	public void setIncomeAmountDaily(BigDecimal incomeAmountDaily) {
		this.incomeAmountDaily = incomeAmountDaily;
	}

	@Transient
	public BigDecimal getOutwardAmountDaily() {
		return outwardAmountDaily;
	}

	public void setOutwardAmountDaily(BigDecimal outwardAmountDaily) {
		this.outwardAmountDaily = outwardAmountDaily;
	}

	@Transient
	public int getLockByOperator() {
		return lockByOperator;
	}

	public void setLockByOperator(int lockByOperator) {
		this.lockByOperator = lockByOperator;
	}

	@Transient
	public String getTypeStr() {
		AccountType temp = AccountType.findByTypeId(getType());
		if (null == temp) {
			return null;
		}
		this.typeStr = temp.getMsg();
		return typeStr;
	}

	public void setTypeStr(String typeStr) {
		this.typeStr = typeStr;
	}

	@Transient
	public String getStatusStr() {
		AccountStatus temp = AccountStatus.findByStatus(getStatus());
		if (null == temp) {
			return null;
		}
		this.statusStr = temp.getMsg();
		return statusStr;
	}

	public void setStatusStr(String statusStr) {
		this.statusStr = statusStr;
	}

	@Transient
	public String getCreatorStr() {
		return creatorStr;
	}

	public void setCreatorStr(String creatorStr) {
		this.creatorStr = creatorStr;
	}

	@Transient
	public String getModifierStr() {
		return modifierStr;
	}

	public void setModifierStr(String modifierStr) {
		this.modifierStr = modifierStr;
	}

	@Transient
	public AccountStatInOut getOutCount() {
		return outCount;
	}

	public void setOutCount(AccountStatInOut map) {
		this.outCount = map;
	}

	@Transient
	public AccountStatInOut getInCount() {
		return inCount;
	}

	public void setInCount(AccountStatInOut inCount) {
		this.inCount = inCount;
	}

	@Transient
	public String getLockerStr() {
		return lockerStr;
	}

	public void setLockerStr(String lockerStr) {
		this.lockerStr = lockerStr;
	}

	@Column(name = "holder")
	public Integer getHolder() {
		return holder;
	}

	public void setHolder(Integer holder) {
		this.holder = holder;
	}

	@Transient
	public String getHolderStr() {
		return holderStr;
	}

	public void setHolderStr(String holderStr) {
		this.holderStr = holderStr;
	}

	@Column(name = "sign")
	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	@Column(name = "hook")
	public String getHook() {
		return hook;
	}

	public void setHook(String hook) {
		this.hook = hook;
	}

	@Column(name = "hub")
	public String getHub() {
		return hub;
	}

	public void setHub(String hub) {
		this.hub = hub;
	}

	@Column(name = "curr_sys_level")
	public Integer getCurrSysLevel() {
		return currSysLevel;
	}

	public void setCurrSysLevel(Integer currSysLevel) {
		this.currSysLevel = currSysLevel;
	}

	@Transient
	public Boolean getSignAndHook() {
		return signAndHook;
	}

	public void setSignAndHook(Boolean signAndHook) {
		this.signAndHook = signAndHook;
	}

	@Transient
	public String getCreateTimeStr() {
		return createTimeStr;
	}

	public void setCreateTimeStr(String createTimeStr) {
		this.createTimeStr = createTimeStr;
	}

	@Transient
	public String getUpdateTimeStr() {
		return updateTimeStr;
	}

	public void setUpdateTimeStr(String updateTimeStr) {
		this.updateTimeStr = updateTimeStr;
	}

	@Transient
	public String getIncomeAuditor() {
		return incomeAuditor;
	}

	public void setIncomeAuditor(String incomeAuditor) {
		this.incomeAuditor = incomeAuditor;
	}

	@Transient
	public String getCurrSysLevelName() {
		return currSysLevelName;
	}

	public void setCurrSysLevelName(String currSysLevelName) {
		this.currSysLevelName = currSysLevelName;
	}

	@Transient
	public Integer getInterval() {
		return interval;
	}

	public void setInterval(Integer interval) {
		this.interval = interval;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "limit_balance")
	public Integer getLimitBalance() {
		return limitBalance;
	}

	public void setLimitBalance(Integer limitBalance) {
		this.limitBalance = limitBalance;
	}

	@Transient
	public String getRemark4Extra() {
		return remark4Extra;
	}

	public void setRemark4Extra(String remark4Extra) {
		this.remark4Extra = remark4Extra;
	}

	@Transient
	public BigDecimal getTransInt() {
		return transInt;
	}

	public void setTransInt(BigDecimal transInt) {
		this.transInt = transInt;
	}

	@Transient
	public BigDecimal getTransRadix() {
		return transRadix;
	}

	public void setTransRadix(BigDecimal transRadix) {
		this.transRadix = transRadix;
	}

	@Transient
	public Integer getLocker() {
		return locker;
	}

	public void setLocker(Integer locker) {
		this.locker = locker;
	}

	@Column(name = "peak_balance")
	public Integer getPeakBalance() {
		return peakBalance;
	}

	public void setPeakBalance(Integer peakBalance) {
		this.peakBalance = peakBalance;
	}

	@Transient
	public Integer getBindId() {
		return bindId;
	}

	public void setBindId(Integer bindId) {
		this.bindId = bindId;
	}

	@Transient
	public String getBindInfo() {
		return bindInfo;
	}

	public void setBindInfo(String bindInfo) {
		this.bindInfo = bindInfo;
	}

	@Transient
	public Integer getTransBlackTo() {
		return transBlackTo == null ? 0 : transBlackTo;
	}

	public void setTransBlackTo(Integer transBlackTo) {
		this.transBlackTo = transBlackTo;
	}

	@Transient
	public Float getRate() {
		return rate;
	}

	public void setRate(Float rate) {
		this.rate = rate;
	}

	@Transient
	public int getRateType() {
		return rateType;
	}

	public void setRateType(int rateType) {
		this.rateType = rateType;
	}

	@Transient
	public String getRateValue() {
		return rateValue;
	}

	public void setRateValue(String rateValue) {
		this.rateValue = rateValue;
	}

	@Transient
	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	@Transient
	public BigDecimal getFeeAmount() {
		return feeAmount;
	}

	public void setFeeAmount(BigDecimal feeAmount) {
		this.feeAmount = feeAmount;
	}

	@Transient
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Transient
	public BigDecimal getMappingAmount() {
		return mappingAmount;
	}

	public void setMappingAmount(BigDecimal mappingAmount) {
		this.mappingAmount = mappingAmount;
	}

	@Transient
	public BigDecimal getMappedAmount() {
		return mappedAmount;
	}

	public void setMappedAmount(BigDecimal mappedAmount) {
		this.mappedAmount = mappedAmount;
	}

	@Column(name = "flag")
	public Integer getFlag() {
		return flag;
	}

	public void setFlag(Integer flag) {
		this.flag = flag;
	}

	@Column(name = "mobile")
	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	@Column(name = "gps")
	public String getGps() {
		return gps;
	}

	public void setGps(String gps) {
		this.gps = gps;
	}

	@Column(name = "limit_out_one_low")
	public Integer getLimitOutOneLow() {
		return limitOutOneLow;
	}

	public void setLimitOutOneLow(Integer limitOutOneLow) {
		this.limitOutOneLow = limitOutOneLow;
	}

	@Column(name = "limit_out_count")
	public Integer getLimitOutCount() {
		return limitOutCount;
	}

	@Column(name = "passage_id")
	public Long getPassageId() {
		return passageId;
	}

	public void setPassageId(Long passageId) {
		this.passageId = passageId;
	}

	@Column(name = "min_in_amount")
	public BigDecimal getMinInAmount() {
		return minInAmount;
	}

	@Column(name = "out_enable")
	public Byte getOutEnable() {
		return outEnable;
	}

	public void setOutEnable(Byte outEnable) {
		this.outEnable = outEnable;
	}

	public void setMinInAmount(BigDecimal minInAmount) {
		this.minInAmount = minInAmount;
	}

	public void setLimitOutCount(Integer limitOutCount) {
		this.limitOutCount = limitOutCount;
	}

	@Column(name = "sign_")
	public String getSign_() {
		return sign_;
	}

	public void setSign_(String sign_) {
		this.sign_ = sign_;
	}

	@Column(name = "hook_")
	public String getHook_() {
		return hook_;
	}

	public void setHook_(String hook_) {
		this.hook_ = hook_;
	}

	@Column(name = "hub_")
	public String getHub_() {
		return hub_;
	}

	public void setHub_(String hub_) {
		this.hub_ = hub_;
	}

	@Column(name = "bing_")
	public String getBing_() {
		return bing_;
	}

	public void setBing_(String bing_) {
		this.bing_ = bing_;
	}

	@Column(name = "limit_percentage")
	public BigDecimal getLimitPercentage() {
		return limitPercentage;
	}

	public void setLimitPercentage(BigDecimal limitPercentage) {
		this.limitPercentage = limitPercentage;
	}

	@Column(name = "min_balance")
	public BigDecimal getMinBalance() {
		return minBalance;
	}

	public void setMinBalance(BigDecimal minBalance) {
		this.minBalance = minBalance;
	}

	@Column(name = "province")
	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	@Column(name = "city")
	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Transient
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Transient
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Transient
	public BigDecimal getMargin() {
		return margin;
	}

	public void setMargin(BigDecimal margin) {
		this.margin = margin;
	}

	@Transient
	public Integer getFlagMoreStr() {
		return flagMoreStr;
	}

	public void setFlagMoreStr(Integer flagMoreStr) {
		this.flagMoreStr = flagMoreStr;
	}

	@Transient
	public Integer getIsOnLine() {
		return isOnLine;
	}

	public void setIsOnLine(Integer isOnLine) {
		this.isOnLine = isOnLine;
	}

	@Transient
	public String getBankBalTime() {
		return bankBalTime;
	}

	public void setBankBalTime(String bankBalTime) {
		this.bankBalTime = bankBalTime;
	}

	@Transient
	public String getSysBalTime() {
		return sysBalTime;
	}

	public void setSysBalTime(String sysBalTime) {
		this.sysBalTime = sysBalTime;
	}

	@Transient
	public String getIsRetrieve() {
		return this.isRetrieve;
	}

	public void setIsRetrieve(String isRetrieve) {
		this.isRetrieve = isRetrieve;
	}

	@Transient
	public AccountFeeConfig getAccountFeeConfig() {
		return accountFeeConfig;
	}
	
	@Transient
	public boolean isAccountFeeEffect() {
		boolean result = false;
		AccountFeeConfig afc;
		if((afc = this.getAccountFeeConfig())!=null) {
			result = afc.isEffect();
		}
		return result;
	}

	public void setAccountFeeConfig(AccountFeeConfig accountFeeConfig) {
		this.accountFeeConfig = accountFeeConfig;
	}

	@Transient
	public BigDecimal getDeposit() {
		return deposit;
	}

	public void setDeposit(BigDecimal deposit) {
		this.deposit = deposit;
	}

	@Transient
	public String getThirdLockOper() {
		return thirdLockOper;
	}

	public void setThirdLockOper(String thirdLockOper) {
		this.thirdLockOper = thirdLockOper;
	}

	@Transient
	public int getFreezeTe() {
		return freezeTe;
	}

	public void setFreezeTe(int freezeTe) {
		this.freezeTe = freezeTe;
	}

}
