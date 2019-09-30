package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Deposit {
	/** 订单号 */
	String sOrderId;
	/** 入款时间 */
	double dCreateTime;
	String dCreateTimeTEXT;
	/** 入款金额 */
	float fAmount;
	/**
	 * 入款类型iDepositType显示 （1支付接口在线入款 2 公司入款 3 快速入款 4 人工入款 (补点+公司收入) 5 冲帐-取消出款
	 * (补点+公司收入) 6 冲帐-重覆出款 (补点+公司收入) 7 存款优惠 (补点) 8 返点优惠 (补点) 9 活动优惠 (补点) 10 负数额度归零
	 * (补点) 11其它 (补点) 12 优惠补点(补点)-锁定转点功能 13 人工存提-紅利 16人工转点(入款) 17汇款优惠 18首存优惠
	 * 19公司入款优惠 20派彩 21退水 22退码 23返点 24微信入款 25 支付宝入款 26退佣 ）
	 */
	int iDepositType;
	/** 入款类型，字符串形式 */
	String sDepositType;
	/** 要求打码量 */
	float fBetNeeds;
	float fFavourFirst;
	float fFavour;

	public String getsOrderId() {
		return sOrderId;
	}

	public void setsOrderId(String sOrderId) {
		this.sOrderId = sOrderId;
	}

	public double getdCreateTime() {
		return dCreateTime;
	}

	public void setdCreateTime(double dCreateTime) {
		this.dCreateTime = dCreateTime;
	}

	public String getdCreateTimeTEXT() {
		return dCreateTimeTEXT;
	}

	public void setdCreateTimeTEXT(String dCreateTimeTEXT) {
		this.dCreateTimeTEXT = dCreateTimeTEXT;
	}

	public float getfAmount() {
		return fAmount;
	}

	public void setfAmount(float fAmount) {
		this.fAmount = fAmount;
	}

	public int getiDepositType() {
		return iDepositType;
	}

	public void setiDepositType(int iDepositType) {
		this.iDepositType = iDepositType;
	}

	public float getfBetNeeds() {
		return fBetNeeds;
	}

	public void setfBetNeeds(float fBetNeeds) {
		this.fBetNeeds = fBetNeeds;
	}

	public float getfFavourFirst() {
		return fFavourFirst;
	}

	public void setfFavourFirst(float fFavourFirst) {
		this.fFavourFirst = fFavourFirst;
	}

	public float getfFavour() {
		return fFavour;
	}

	public void setfFavour(float fFavour) {
		this.fFavour = fFavour;
	}

	public String getsDepositType() {
		return sDepositType;
	}

	public void setsDepositType(String sDepositType) {
		this.sDepositType = sDepositType;
	}

}
