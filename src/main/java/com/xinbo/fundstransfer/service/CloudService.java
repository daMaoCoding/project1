package com.xinbo.fundstransfer.service;

import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 云端信息同步
 * 
 * @author 
 *
 */
public interface CloudService {

	Object rMobilePage(int pageNo, int pageSize, String[] statusArray, String search_LIKE_mobile,
			String search_LIKE_owner, String search_LIKE_wechat, String search_LIKE_alipay, String search_LIKE_bank,
			String search_EQ_handicap, String search_EQ_type, String search_EQ_level);

	Object rMobilePwd(String bank, String singBank, String pingBank, String bingBank, String uingBank, String alipay,
			String singAlipay, String pingAlipay, String wechat, String singWechat, String pingWechat);

	Object rMobileUpdAcc(String mobile, String account, Integer type, String owner, String accountName, String bankType,
			BigDecimal limitInDaily, BigDecimal limitBalance, Integer transOutType);

	/**
	 * 手机号同步到云端
	 * 
	 */
	Object rMobilePut(String handicap, String mobile, String owner, Integer type, Integer level, Integer creditLimit,
			String remark, String bankAcc, String bankOwner, String bankName, String bankType,
			BigDecimal bankLimitBalance, String wecAcc, String wecOwner, BigDecimal wecInLimitDaily,
			BigDecimal wechatLimitBalance, Integer wechatTransOutType, String aliAcc, String aliOwner,
			BigDecimal aliInLimitDaily, BigDecimal alipayLimitBalance, Integer alipayTransOutType, String bonusCard,
			String bonusCardOwner, String bonusCardName, Integer bonusCardStatus) throws Exception;

	Object rMobileUpdBase(Integer mobileId, String mobile, Integer status, BigDecimal creditLimit, Integer level,
			String owner, String bonusAccount, String bonusOwner, String bonusBankName);

	Object rMobileUpdAccStatus(String mobile, String account, Integer type, Integer status);

	Object rMobileBonusAccList(String mobile);

	/**
	 * 根据手机号码获取手机状态信息
	 * <p>
	 * 如果该手机信息在云端未存在,则直接返回null;</br>
	 * 如果在获取该信息过程中，程序抛出异常，则直接返回null;<br>
	 * </p>
	 * 
	 * @param mobile
	 *            手机号码
	 * @return data.mobile 手机号码</br>
	 *         data.type 分类 1：客户 2：自用</br>
	 *         data.status 状态 1:启用 3:冻结 4：停用</br>
	 *         data.creditLimit 信用额度</br>
	 *         data.deviceId 设备ID</br>
	 *         data.gps 位置</br>
	 *         data.battery 电量</br>
	 *         data.signal 信号</br>
	 *         data.device 设备</br>
	 *         data.handicap 盘口</br>
	 * @see com.xinbo.fundstransfer.domain.enums.MobileStatus
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType
	 * @since 1.8
	 */
	Object rMobileGet(String mobile);

	/**
	 * 根据手机号获取该手机号绑定的银行卡，支付宝，微信的当日收款，当日提现，当前金额
	 * 
	 * @param mobile
	 *            手机号码
	 * @return data.mobile 手机号码</br>
	 *         data.balAli 该手机绑定的支付宝账号余额</br>
	 *         data.balWec 该手机绑定的微信账号余额</br>
	 *         data.balBank 该手机绑定的银行卡余额</br>
	 *         data.balInAliDaily 该手机绑定的支付宝账号当日收款</br>
	 *         data.balInWecDaily 该手机绑定的微信账号当日收款</br>
	 *         data.balInBankDaily 该手机绑定的银行卡当日收款</br>
	 *         data.balOutAliDaily 该手机绑定的支付宝账号当日提现</br>
	 *         data.balOutWecDaily 该手机绑定的微信账号当日提现</br>
	 *         data.balOutBankDaily 该手机绑定的银行卡账号当日提现</br>
	 *         data.alipay 该手机号绑定的支付宝账号</br>
	 *         data.wechat 该手机号绑定的微信账号</br>
	 *         data.bank 该手机绑定的银行账号</br>
	 * @since 1.8
	 */
	Object rMobileGetBal(String mobile);

	Object rMobilePageForQr(String mobile, String account, Integer accountType, Integer amtBegin, Integer amtEnd,
			Integer pageSize, Integer pageNo);

	Object rMobileGenQR(String mobile, String account, Integer accountType, Integer amtBegin, Integer amtEnd,
			Integer step);

	Object rMobileGenExactQR(String mobile, String account, Integer accountType, String exactQR);

	Object rMobileGenZeroQR(String mobile, String account, Integer accountType);

	Object rMobileDelQR(String mobile, String account, Integer accountType, Integer amt);

	Object rMobileDelQRList(String account, Integer accountType, List<Integer> amtList);

	Object rMobileDel(String mobile);

	/**
	 * 
	 * 转账
	 * 
	 * @param account
	 *            账号
	 * @param amount
	 *            金额
	 * @param nickName
	 *            收款人
	 * @param toAccount
	 *            收款账号
	 * @param toAccountBank
	 *            收款银行开户行
	 */
	Object rBankTransfer(int frType, String account, BigDecimal amount, String nickName, String toAccount,
			String toAccountBank);

	Object rBonusTotalForEach(List<String> mobileList);

	Object rBonusFindIncomeLogByAliAcc(String mobile, BigDecimal startAmt, BigDecimal endAmt, Long startTime,
			Long endTime, Integer pageSize, Integer pageNo);

	Object rBonusFindIncomeLogByWecAcc(String mobile, BigDecimal startAmt, BigDecimal endAmt, Long startTime,
			Long endTime, Integer pageSize, Integer pageNo);

	Object rBonusBonus(String mobile, BigDecimal startAmt, BigDecimal endAmt, Long startTime, Long endTime,
			Integer pageSize, Integer pageNo);

	Object rSysFindPro();

	Object rSysSavePro(Map<String, Object> proMap);
}
