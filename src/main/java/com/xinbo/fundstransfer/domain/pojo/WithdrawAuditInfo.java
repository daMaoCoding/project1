package com.xinbo.fundstransfer.domain.pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 出款审核信息，备注： 中奖率，几倍打码量，动态算
 * 
 * 
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WithdrawAuditInfo {
	/**
	 * {"iUserKey":"397419","dWithdrawTime":43017.826428588,"dWithdrawTimeTEXT":"2017-10-09
	 * 19:50:03","fWithdrawAmount":50,"sWithdrawOrderId":"22017100993445283","iWithdrawStatus":0,"sWithdrawCard":"6222333344445555","fWithdrawBalance":40874.45,"sUserID":"domdom","sLevelCode":"A0001","dRegTime":42948.6853466435,"dRegTimeTEXT":"2017-08-01
	 * 16:26:53","dLoginTime":43017.8262078704,"dLoginTimeTEXT":"2017-10-09
	 * 19:49:44","sUserRemark":"","iDepositCount":25,"fAllDepositAmount":2008957.05,"fDepositMax":2000000,"iWithdrawCount":0,"fAllWithdrawAmount":0,"fCostAmount":3941488,"sUpWithdrawOrderId":"22017093061021312","fUpWithdrawAmount":50,"fUpWithdrawBalance":40924.45,"sUpWithdrawCard":"6222333344445555","dUpWithdrawTime":43008.6765866088,"dUpWithdrawTimeTEXT":"2017-09-30
	 * 16:14:17","RecordCount":0,"DepositLists":[],"Func":"WithdrawAuditInfo","Result":1,"Desc":"OK"}
	 */
	/** 用户KEY */
	String iUserKey;
	/** 出款时间 */
	double dWithdrawTime;
	/** 出款时间 */
	String dWithdrawTimeTEXT;
	/** 出款金额 */
	String fWithdrawAmount;
	/** 出款前余额（出款后余额=出款前余额-出款金额） */
	String fWithdrawBalance;
	/** 出款订单号 */
	String sWithdrawOrderId;
	/** 1表示首次出款 */
	int iWithdrawStatus;
	/** 出款卡号（如果与上次出款卡号不同，则必须人工审核） */
	String sWithdrawCard;
	/** 会员帐号 */
	String sUserID;
	/** 会员层级 */
	String sLevelCode;
	/** 注册时间 */
	double dRegTime;
	/** 注册时间 */
	String dRegTimeTEXT;
	/** 最近登录时间 */
	double dLoginTime;
	/** 最近登录时间 */
	String dLoginTimeTEXT;
	/** 会员备注（很重要，必须显示在界面上） */
	String sUserRemark;
	/** 入款次数 */
	int iDepositCount;
	/** 总入款金额 */
	String fAllDepositAmount;
	/** 单次最大入款金额 */
	String fDepositMax;
	/** 出款次数 */
	int iWithdrawCount;
	/** 总出款金额 */
	String fAllWithdrawAmount;
	/** 总打码量 */
	String fCostAmount;
	/** 上次出款订单号 */
	String sUpWithdrawOrderId;
	/** 上次出款金额 */
	String fUpWithdrawAmount;
	/** 上次出款前余额 */
	String fUpWithdrawBalance;
	/** 上次出款卡号 */
	String sUpWithdrawCard;
	/** 上次出款时间 */
	double dUpWithdrawTime;
	/** 上次出款时间 */
	String dUpWithdrawTimeTEXT;
	/** 本次下注金额,已达投注量 */
	String fBetAmount;
	/** 本次要求的投注量 */
	String fBetNeed;
	/** 本次中奖金额 */
	String fWinAmount;
	/** 本次获利金额 */
	String fOmProfit;
	/** 下注次数 */
	int iBetCount;
	/** 中奖次数 */
	int iWinCount;

	/** 上次出款到本次出款的入款列表数组 */
	List<Deposit> DepositLists;

	int RecordCount;
	String Func;
	/** 返回结果，1成功，其它失败 */
	int Result;
	/** 成功/失败描术信息 */
	String Desc;
	String Error;

	/* 显示该IP的登录账号--->系统:ip,账号，最后登陆时间 */
	private List<IP2AccountsDTO> IPsLoginedByAccountsList;
	/* 显示该账号的登录IP--->系统:账号，IP，最后登陆时间 */
	private List<IP2AccountsDTO> AccountsLoginedInIpsList;
	/* 注册IP */
	private String registerIp;
	/* 最后登陆IP */
	private String lastLoginIp;
	/* 最近入款明细总额 */
	private String recentDepositTotalAmount;

	public String getfOmProfit() {
		return fOmProfit;
	}

	public void setfOmProfit(String fOmProfit) {
		this.fOmProfit = fOmProfit;
	}

	public String getRecentDepositTotalAmount() {
		return recentDepositTotalAmount;
	}

	public void setRecentDepositTotalAmount(String recentDepositTotalAmount) {
		this.recentDepositTotalAmount = recentDepositTotalAmount;
	}

	@JsonProperty(value = "IPsLoginedByAccountsList")
	public List<IP2AccountsDTO> getIPsLoginedByAccountsList() {
		return IPsLoginedByAccountsList;
	}

	public void setIPsLoginedByAccountsList(List<IP2AccountsDTO> IPsLoginedByAccountsList) {
		this.IPsLoginedByAccountsList = IPsLoginedByAccountsList;
	}

	@JsonProperty(value = "AccountsLoginedInIpsList")
	public List<IP2AccountsDTO> getAccountsLoginedInIpsList() {
		return AccountsLoginedInIpsList;
	}

	public void setAccountsLoginedInIpsList(List<IP2AccountsDTO> accountsLoginedInIpsList) {
		AccountsLoginedInIpsList = accountsLoginedInIpsList;
	}

	public String getRegisterIp() {
		return registerIp;
	}

	public void setRegisterIp(String registerIp) {
		this.registerIp = registerIp;
	}

	public String getLastLoginIp() {
		return lastLoginIp;
	}

	public void setLastLoginIp(String lastLoginIp) {
		this.lastLoginIp = lastLoginIp;
	}

	public String getiUserKey() {
		return iUserKey;
	}

	public void setiUserKey(String iUserKey) {
		this.iUserKey = iUserKey;
	}

	public double getdWithdrawTime() {
		return dWithdrawTime;
	}

	public void setdWithdrawTime(double dWithdrawTime) {
		this.dWithdrawTime = dWithdrawTime;
	}

	public String getsWithdrawOrderId() {
		return sWithdrawOrderId;
	}

	public void setsWithdrawOrderId(String sWithdrawOrderId) {
		this.sWithdrawOrderId = sWithdrawOrderId;
	}

	public int getiWithdrawStatus() {
		return iWithdrawStatus;
	}

	public void setiWithdrawStatus(int iWithdrawStatus) {
		this.iWithdrawStatus = iWithdrawStatus;
	}

	public String getsWithdrawCard() {
		return sWithdrawCard;
	}

	public void setsWithdrawCard(String sWithdrawCard) {
		this.sWithdrawCard = sWithdrawCard;
	}

	public String getsUserID() {
		return sUserID;
	}

	public void setsUserID(String sUserID) {
		this.sUserID = sUserID;
	}

	public String getsLevelCode() {
		return sLevelCode;
	}

	public void setsLevelCode(String sLevelCode) {
		this.sLevelCode = sLevelCode;
	}

	public double getdRegTime() {
		return dRegTime;
	}

	public void setdRegTime(double dRegTime) {
		this.dRegTime = dRegTime;
	}

	public double getdLoginTime() {
		return dLoginTime;
	}

	public void setdLoginTime(double dLoginTime) {
		this.dLoginTime = dLoginTime;
	}

	public String getsUserRemark() {
		return sUserRemark;
	}

	public void setsUserRemark(String sUserRemark) {
		this.sUserRemark = sUserRemark;
	}

	public int getiDepositCount() {
		return iDepositCount;
	}

	public void setiDepositCount(int iDepositCount) {
		this.iDepositCount = iDepositCount;
	}

	public String getsUpWithdrawCard() {
		return sUpWithdrawCard;
	}

	public void setsUpWithdrawCard(String sUpWithdrawCard) {
		this.sUpWithdrawCard = sUpWithdrawCard;
	}

	public double getdUpWithdrawTime() {
		return dUpWithdrawTime;
	}

	public void setdUpWithdrawTime(double dUpWithdrawTime) {
		this.dUpWithdrawTime = dUpWithdrawTime;
	}

	@JsonProperty(value = "Func")
	public String getFunc() {
		return Func;
	}

	public void setFunc(String func) {
		Func = func;
	}

	@JsonProperty(value = "Result")
	public int getResult() {
		return Result;
	}

	public void setResult(int result) {
		Result = result;
	}

	@JsonProperty(value = "Desc")
	public String getDesc() {
		return Desc;
	}

	public void setDesc(String desc) {
		Desc = desc;
	}

	public String getdWithdrawTimeTEXT() {
		return dWithdrawTimeTEXT;
	}

	public void setdWithdrawTimeTEXT(String dWithdrawTimeTEXT) {
		this.dWithdrawTimeTEXT = dWithdrawTimeTEXT;
	}

	public String getdRegTimeTEXT() {
		return dRegTimeTEXT;
	}

	@JsonProperty(value = "DepositLists")
	public List<Deposit> getDepositLists() {
		return DepositLists;
	}

	public void setDepositLists(List<Deposit> depositLists) {
		DepositLists = depositLists;
	}

	public void setdRegTimeTEXT(String dRegTimeTEXT) {
		this.dRegTimeTEXT = dRegTimeTEXT;
	}

	public String getdLoginTimeTEXT() {
		return dLoginTimeTEXT;
	}

	public void setdLoginTimeTEXT(String dLoginTimeTEXT) {
		this.dLoginTimeTEXT = dLoginTimeTEXT;
	}

	public String getdUpWithdrawTimeTEXT() {
		return dUpWithdrawTimeTEXT;
	}

	public void setdUpWithdrawTimeTEXT(String dUpWithdrawTimeTEXT) {
		this.dUpWithdrawTimeTEXT = dUpWithdrawTimeTEXT;
	}

	@JsonProperty(value = "RecordCount")
	public int getRecordCount() {
		return RecordCount;
	}

	public void setRecordCount(int recordCount) {
		RecordCount = recordCount;
	}

	@JsonProperty(value = "Error")
	public String getError() {
		return Error;
	}

	public void setError(String error) {
		Error = error;
	}

	public int getiBetCount() {
		return iBetCount;
	}

	public void setiBetCount(int iBetCount) {
		this.iBetCount = iBetCount;
	}

	public int getiWinCount() {
		return iWinCount;
	}

	public void setiWinCount(int iWinCount) {
		this.iWinCount = iWinCount;
	}

	public String getfWithdrawAmount() {
		return fWithdrawAmount;
	}

	public void setfWithdrawAmount(String fWithdrawAmount) {
		this.fWithdrawAmount = fWithdrawAmount;
	}

	public String getfWithdrawBalance() {
		return fWithdrawBalance;
	}

	public void setfWithdrawBalance(String fWithdrawBalance) {
		this.fWithdrawBalance = fWithdrawBalance;
	}

	public String getfAllDepositAmount() {
		return fAllDepositAmount;
	}

	public void setfAllDepositAmount(String fAllDepositAmount) {
		this.fAllDepositAmount = fAllDepositAmount;
	}

	public String getfDepositMax() {
		return fDepositMax;
	}

	public void setfDepositMax(String fDepositMax) {
		this.fDepositMax = fDepositMax;
	}

	public int getiWithdrawCount() {
		return iWithdrawCount;
	}

	public void setiWithdrawCount(int iWithdrawCount) {
		this.iWithdrawCount = iWithdrawCount;
	}

	public String getfAllWithdrawAmount() {
		return fAllWithdrawAmount;
	}

	public void setfAllWithdrawAmount(String fAllWithdrawAmount) {
		this.fAllWithdrawAmount = fAllWithdrawAmount;
	}

	public String getfCostAmount() {
		return fCostAmount;
	}

	public void setfCostAmount(String fCostAmount) {
		this.fCostAmount = fCostAmount;
	}

	public String getsUpWithdrawOrderId() {
		return sUpWithdrawOrderId;
	}

	public void setsUpWithdrawOrderId(String sUpWithdrawOrderId) {
		this.sUpWithdrawOrderId = sUpWithdrawOrderId;
	}

	public String getfUpWithdrawAmount() {
		return fUpWithdrawAmount;
	}

	public void setfUpWithdrawAmount(String fUpWithdrawAmount) {
		this.fUpWithdrawAmount = fUpWithdrawAmount;
	}

	public String getfUpWithdrawBalance() {
		return fUpWithdrawBalance;
	}

	public void setfUpWithdrawBalance(String fUpWithdrawBalance) {
		this.fUpWithdrawBalance = fUpWithdrawBalance;
	}

	public String getfBetAmount() {
		return fBetAmount;
	}

	public void setfBetAmount(String fBetAmount) {
		this.fBetAmount = fBetAmount;
	}

	public String getfBetNeed() {
		return fBetNeed;
	}

	public void setfBetNeed(String fBetNeed) {
		this.fBetNeed = fBetNeed;
	}

	public String getfWinAmount() {
		return fWinAmount;
	}

	public void setfWinAmount(String fWinAmount) {
		this.fWinAmount = fWinAmount;
	}
}
