package com.xinbo.fundstransfer.chatPay.commons.config;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 支付宝出款配置,sys_user_profile/key=OUT_ALI_CONFIG
 *
 * 注意：
 * 1、不同盘口的会员可以匹配到同一个聊天室（注意：返利网兼职代收、代付不需要区分盘口）
 * 2、如果出款没有完成，出款人就提前离开了聊天室，入款人可继续入款，并使用工具确认到账，或申请客服介入确认到账
 * 3、一笔会员出款单匹配多笔入款单时，尽最大可能将降低匹配的入款单笔数，按照匹配的入款单笔数越少越好的原则匹配
 *
 * @author wira
 * @author tony
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliOutConfig {

	/**
	 * 会员首次出款不能使用聊天室出款方式，推荐会员使用银行转账出款
	 * 1.启用，0未启用
	 */
	int firstNoUseForAliOut;

	/**
	 * 自动出款系统中的黑名单会员出款（姓名、银行卡号只要符合其中一项），不能使用聊天室出款方式（注意：指的是出款管理——账号管理——出款银行卡页面的黑名单会员）
	 * 1.启用，0未启用
	 */
	int noUseBlackForAliOut;


	/**
	 * 同一个支付宝账号每日累计最大出款金额不能超过 【50000】 元
	 */
	int maxOutAmountForAliOut;


	/**
	 * 会员提交一笔出款，优先匹配 【5】 秒内提交的会员入款单，如果没有匹配到会员入款，就自动匹配返利兼职代付
	 * 单位：秒
	 */
	int matchSecondForAliOut;



	/**
	 * 出款人进入聊天室后，在没有全部完成出款之前，每隔 【120】秒系统提示出款人继续出款或离开
	 * 单位：秒
	 */
	int alertConfimEachSecondForAliOut;



	/**
	 * 接上面的注释： 系统会倒计时【20】秒，如果出款人没有点击继续或离开，系统会强制取消本次出款（如果部分出款完成了，剩余的未完成金额会退回出款人的账户余额）
	 * 客服系统倒计时时间
	 * 单位：秒
	 */
	int confimCountDownSecondForAliOut;

	//--------------------------------------------------原型分割线--------------------------------------------------------------------------

	/**
	 * 返利网兼职的支付宝账号余额小于 【1000】 元（“0”表示不限制），不能接代付任务
	 * 单位：元
	 */
	int balanceLessThanForAliOut;



	/**
	 * 返利网兼职的支付宝账号余额大于或等于信用额度的 【90】 % （“0”表示不限制）只能接代付任务
	 */
	int moreThanPercentForAliOut;



	/**
	 * 返利网兼职支付宝最开始的 【1】 笔(“0”表示不限制）代付任务不能超过 【100】 元
	 */
	int firstOrderMaxForAliOut;


	/**
	 * 接上面注释：返利网兼职支付宝最开始的 【1】 笔(“0”表示不限制）代付任务不能超过 【100】 元
	 */
	int firstOrderMaxMoneyForAliOut;


	/**
	 * 返利网兼职代收 【3】 笔提款没有主动点击【到账确认】按钮（入款人员用工具确认或客服介入确认），禁止继续指派代收任务，兼职可以联系返利网客服申请恢复
	 * 兼职有工具，工具可以自动上报
	 */
	int noConfimIncomeForAliOut;


	/**
	 * 返利网兼职代付时，最多可同时接 【1】 个代付任务，确认到账后，才能接下一个代付任务
	 */
	int maxOutTastSameTimeForAliOut;


	//--------------------------------------------------原型分割线--------------------------------------------------------------------------


	/**
	 * 出款人连续 【3 】次出入款没有主动点击【确认到账】（入款人员用工具确认或客服介入确认），系统将禁止其下次再使用支付宝聊天室出款，并弹出提示让其选择银行卡出款或者会员向在线客服申请恢复使用（选择申请开通会进入在线客服聊天室
	 */
	int noComfirmOutOrImcomeForAliOut;



	/**
	 * 初始化Json配置
	 */
	public static void main(String[] args) {
		//出款配置
		AliOutConfig aliOutConfig = getDefaultConfig();
		System.out.println(JSON.toJSONString(aliOutConfig));
	}



	/**
	 * 获取默认配置(获取数据库数据异常时使用默认配置)
	 */
	public static AliOutConfig getDefaultConfig(){
		AliOutConfig aliOutConfig = new AliOutConfig();
		aliOutConfig.setFirstNoUseForAliOut(0); //首次出款不能使用支付宝
		aliOutConfig.setNoUseBlackForAliOut(1); //出入款黑名单启用
		aliOutConfig.setMaxOutAmountForAliOut(5000000); //会员支付宝出款当日出款最大金额
		aliOutConfig.setMatchSecondForAliOut(60); //60秒匹配入款单，超时拉兼职
		aliOutConfig.setAlertConfimEachSecondForAliOut(120); //未完成出款，客服系统每n秒弹框询问出款人是否继续等待
		aliOutConfig.setConfimCountDownSecondForAliOut(20); //未完成出款，客服系统每次弹框的倒计时开始时间。
		aliOutConfig.setBalanceLessThanForAliOut(0); //兼职支付宝余额<=n不能接收代付任务
		aliOutConfig.setMoreThanPercentForAliOut(0); //兼职支付宝余额>=n%额度，只能接收代付任务
		aliOutConfig.setFirstOrderMaxForAliOut(0); //兼职前n笔代付任务，接下行
		aliOutConfig.setFirstOrderMaxMoneyForAliOut(0); //....,单笔最大代付金额
		aliOutConfig.setNoConfimIncomeForAliOut(0); //兼职代收n笔每点击[确认到账]加入黑名单-----返利网与老板确认无需限制
		aliOutConfig.setMaxOutTastSameTimeForAliOut(1);//兼职同时处理n个代付任务
		aliOutConfig.setNoConfimIncomeForAliOut(5); //出款人不主动确认n次加入黑名单(兼职除外参见上面---)
		return aliOutConfig;
	}



}
