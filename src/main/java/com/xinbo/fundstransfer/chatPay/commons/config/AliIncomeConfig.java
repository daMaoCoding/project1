package com.xinbo.fundstransfer.chatPay.commons.config;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


/**
 * 支付宝入款配置,sys_user_profile/key=INCOME_ALI_CONFIG
 *
 * 注意：
 * 1、不同盘口的会员可以匹配到同一个聊天室（注意：返利网兼职代收、代付不需要区分盘口）
 * 2、如果出款没有完成，出款人就提前离开了聊天室，入款人可继续入款，并使用工具确认到账，或申请客服介入确认
 * 
 * @author wira
 * @author tony
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliIncomeConfig {

	/**
	 * 自动出款系统中的黑名单会员入款（姓名、银行卡号只要符合其中一项），
	 * 不能使用聊天室入款方式（注意：指的是出款管理——账号管理——出款银行卡页面的黑名单会员）
	 * 1.启用，0未启用
	 */
	int noUseBlackForAliIn;


	/**
	 * 同一个支付宝账号每日累计最大入款金额不能超过【n】元
	 */
	int maxAmountForAliIn;



	/**
	 * 会员提交一笔入款，等待【n】秒内提交的会员出款单匹配聊天室
	 * 判断：此值应该小于 totalMatchSecondForAliIn
	 */
	int matchSecondForAliIn;


	/**
	 * 如果没有匹配到会员出款，就自动匹配返利兼职代收，【n】秒内如果还没有匹配到出款会员或兼
	 * 职代收，系统提示入款会员系统繁忙稍后再试或更换其他入款通道
	 * 判断：此值应该大于 matchSecondForAliIn
	 */
	int totalMatchSecondForAliIn;


	/**
	 * 从系统匹配到入款任务进入聊天室开始【n】分钟内（倒计时显示在聊天室）入款人没有点击【已确认付款】按钮，并且收款人没有点击【确认到账】，
	 * 聊天室提示【继续入款】、【已确认付款】、【离开】（如果选择继续入款，重新倒数计时并弹提示）。
	 * 单位：分钟
	 * 判断：此值应该小于 incomeTimeOutTotalMinute
	 */
	int incomeTimeOutMinute;


	/**
	 * 从系统匹配到入款任务进入聊天室开始【n】分钟内（倒计时不需显示在聊天室）入款人没有完成入款，并且收款人没有点击【确认到账】，聊天室提示【已确认付款】、【离开】
	 * （选择【已确认付款】继续留在聊天室并提示收款人尽快确认是否到账，选择【离开】将离开聊天室，系统会取消该笔入款单。
	 * 如果该聊天室匹配了出款会员，那么该入款会员离开后，系统会匹配新的入款会员或兼职代付进入聊天室）。
	 * 单位：分钟
	 * 判断：此值应该大于 incomeTimeOutMinute
	 */
	int incomeTimeOutTotalMinute;



	//--------------------------------------------------原型分割线-----------------------------------------------------

	/**
	 * 返利网兼职的支付宝账号余额大于或等于 【n】元（“0”表示不限制），不能接代收任务
	 * 单位元。
	 */
	int balanceMaxForAliIn;



	/**
	 * 返利网兼职的支付宝账号余额小于或等于信用额度的【n】%（“0”表示不限制）只能接会员代收任务
	 */
	int lessThanPercentForAliIn;


	/**
	 * 返利网兼职支付宝最开始的【n】笔(“0”表示不限制）代收任务不能超过【n】元
	 */
	int firstOrderMaxForAliIn;


	/**
	 * 接上面注释：返利网兼职支付宝最开始的【n】笔(“0”表示不限制）代收任务不能超过【n】元
	 */
	int firstOrderMaxMoneyForAliIn;


	/**
	 * 返利网兼职代收时，最多可同时接 【n】个代收任务
	 */
	int rebateMaxOderForAliIn;


	/**
	 * 初始化Json配置
	 */
	public static void main(String[] args) {
		//入款配置
		AliIncomeConfig aliIncomeConfig = getDefaultConfig();
		System.out.println(JSON.toJSONString(aliIncomeConfig));
	}


	/**
	 * 获取默认配置(获取数据库数据异常时使用默认配置)
	 */
	public static AliIncomeConfig getDefaultConfig(){
		AliIncomeConfig aliIncomeConfig = new AliIncomeConfig();
		aliIncomeConfig.setNoUseBlackForAliIn(0);  //入款会员是否是出入款黑名单，未启用
		aliIncomeConfig.setMaxAmountForAliIn(5000000); //会员支付宝入款当日入款最大金额
		aliIncomeConfig.setMatchSecondForAliIn(5); //入款单匹配出款单等待时间，秒
		aliIncomeConfig.setTotalMatchSecondForAliIn(8); //入款单超时，秒
		aliIncomeConfig.setIncomeTimeOutMinute(2); //入款人不支付超时，第1次弹框提示(等待,已付，离开)，分钟
		aliIncomeConfig.setIncomeTimeOutTotalMinute(3); //入款人不支付超时，第2次弹框提示(已付，离开)，分钟
		aliIncomeConfig.setBalanceMaxForAliIn(0); //兼职支付宝余额>=n,不能接收代任务，0不限制  （此处用额度控制）
		aliIncomeConfig.setLessThanPercentForAliIn(0); //兼职支付宝余额<=n%额度,只能接会员代收任务 (此处用额度控制)
		aliIncomeConfig.setFirstOrderMaxForAliIn(0); //兼职前n笔代收任务，接下面一行
		aliIncomeConfig.setFirstOrderMaxMoneyForAliIn(0);//.....,单笔最大代收金额
		aliIncomeConfig.setRebateMaxOderForAliIn(1); //兼职同时最多接1个代收任务
		return aliIncomeConfig;
	}

}
