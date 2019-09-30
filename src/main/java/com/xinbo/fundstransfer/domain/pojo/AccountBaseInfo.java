package com.xinbo.fundstransfer.domain.pojo;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.*;

@Data
public class AccountBaseInfo {
	private Integer id;

	private Integer type;
	private Integer subType;
	private Integer status;

	private Integer handicapId;

	private List<BizLevel> levelList;

	private String account;

	private String bankName;

	private String owner;

	private String bankType;

	private Integer currSysLevel;

	private Integer limitIn;

	private Integer limitOut;

	private Integer limitOutOne;

	private Integer limitOutOneLow;

	private Integer limitOutCount;

	private Integer limitBalance;

	private String alias;

	private Integer holder;

	private Integer lowestOut;

	private Integer peakBalance;
	private Float rate;
	private int rateType;
	private String rateValue;
	private Integer flag;
	private String mobile;

	public boolean checkMobile() {
		return Objects.equals(this.flag, 1) || Objects.equals(this.flag, 2);
	}

	private Long passageId;// 绑定的通道Id
	private BigDecimal minInAmount;// 入款单笔最小限额
	private Byte outEnable;// 是否可以用于出款
	private BigDecimal limitPercentage;// 信用额度百分比
	private BigDecimal minBalance;// 专注卡保留余额
	private String province;// 省份
	private String city;// 城市

	public AccountBaseInfo() {
	}

	public AccountBaseInfo(BizAccount account, List<BizLevel> levelList) {
		this.id = account.getId();
		this.type = account.getType();
		this.subType = account.getSubType();
		this.status = account.getStatus();
		this.handicapId = account.getHandicapId();
		this.holder = account.getHolder();
		this.levelList = levelList;
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
		this.passageId = account.getPassageId();
		this.minInAmount = account.getMinInAmount();
		this.outEnable = account.getOutEnable();
		this.limitPercentage = account.getLimitPercentage();
		this.minBalance = account.getMinBalance();
		this.province = account.getProvince();
		this.city = account.getCity();

	}

	@Override
	public String toString() {
		return "AccountBaseInfo{" + "id=" + id + ", type=" + type + ", subType=" + subType + ", status=" + status
				+ ", handicapId=" + handicapId + ", levelList=" + levelList + ", account='" + account + '\''
				+ ", bankName='" + bankName + '\'' + ", owner='" + owner + '\'' + ", bankType='" + bankType + '\''
				+ ", currSysLevel=" + currSysLevel + ", limitIn=" + limitIn + ", limitOut=" + limitOut
				+ ", limitOutOne=" + limitOutOne + ", limitOutOneLow=" + limitOutOneLow + ", limitOutCount="
				+ limitOutCount + ", limitBalance=" + limitBalance + ", alias='" + alias + '\'' + ", holder=" + holder
				+ ", lowestOut=" + lowestOut + ", peakBalance=" + peakBalance + ", rate=" + rate + ", rateType="
				+ rateType + ", rateValue='" + rateValue + '\'' + ", flag=" + flag + ", mobile='" + mobile + '\''
				+ ", passageId=" + passageId + ", minInAmount=" + minInAmount + ", outEnable=" + outEnable
				+ ", limitPercentage=" + limitPercentage + ", minBalance=" + minBalance + ", province='" + province
				+ '\'' + ", city='" + city + '\'' + '}';
	}
}
