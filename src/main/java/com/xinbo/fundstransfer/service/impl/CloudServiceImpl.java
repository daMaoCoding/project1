package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.xinbo.fundstransfer.AppConstants;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.component.net.http.cloud.HttpClientCloud;
import com.xinbo.fundstransfer.component.net.http.cloud.ReqCoudBodyParser;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.service.CloudService;

@Service
public class CloudServiceImpl implements CloudService {
	private static final Logger log = LoggerFactory.getLogger(CloudServiceImpl.class);
	@Autowired
	private ReqCoudBodyParser reqCoudBodyParser;

	@Override
	public Object rMobilePage(int pageNo, int pageSize, String[] statusArray, String search_LIKE_mobile,
			String search_LIKE_owner, String search_LIKE_wechat, String search_LIKE_alipay, String search_LIKE_bank,
			String search_EQ_handicap, String search_EQ_type, String search_EQ_level) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rMobilePage(reqCoudBodyParser.rMobilePage(pageNo, pageSize, statusArray, search_LIKE_mobile,
						search_LIKE_owner, search_LIKE_wechat, search_LIKE_alipay, search_LIKE_bank, search_EQ_handicap,
						search_EQ_type, search_EQ_level))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobilePage >> ", e));
		return ret[0];
	}

	@Override
	public Object rMobilePwd(String bank, String singBank, String pingBank, String bingBank, String uingBank,
			String alipay, String singAlipay, String pingAlipay, String wechat, String singWechat, String pingWechat) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rMobilePwd(reqCoudBodyParser.rMobilePwd(bank, singBank, pingBank, bingBank, uingBank, alipay,
						singAlipay, pingAlipay, wechat, singWechat, pingWechat))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobilePwd >> ", e));
		return ret[0];
	}

	@Override
	public Object rMobileUpdAcc(String mobile, String account, Integer type, String owner, String accountName,
			String bankType, BigDecimal limitInDaily, BigDecimal limitBalance, Integer transOutType) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rMobileUpdAcc(reqCoudBodyParser.rMobileUpdAcc(mobile, account, type, owner, accountName, bankType,
						limitInDaily, limitBalance, transOutType))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileUpdAcc >> ", e));
		return ret[0];
	}

	@Override
	public Object rMobilePut(String handicap, String mobile, String owner, Integer type, Integer level,
			Integer creditLimit, String remark, String bankAcc, String bankOwner, String bankName, String bankType,
			BigDecimal bankLimitBalance, String wecAcc, String wecOwner, BigDecimal wecInLimitDaily,
			BigDecimal wechatLimitBalance, Integer wechatTransOutType, String aliAcc, String aliOwner,
			BigDecimal aliInLimitDaily, BigDecimal alipayLimitBalance, Integer alipayTransOutType, String bonusCard,
			String bonusCardOwner, String bonusCardName, Integer bonusCardStatus) throws Exception {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rMobilePut(reqCoudBodyParser.rMobilePut(handicap, mobile, owner, type, level, creditLimit, remark,
						bankAcc, bankOwner, bankName, bankType, bankLimitBalance, wecAcc, wecOwner, wecInLimitDaily,
						wechatLimitBalance, wechatTransOutType, aliAcc, aliOwner, aliInLimitDaily, alipayLimitBalance,
						alipayTransOutType, bonusCard, bonusCardOwner, bonusCardName, bonusCardStatus))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobilePut >> ", e));
		return ret[0];
	}

	@Override
	public Object rMobileUpdBase(Integer mobileId, String mobile, Integer status, BigDecimal creditLimit, Integer level,
			String owner, String bonusAccount, String bonusOwner, String bonusBankName) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rMobileUpdBase(reqCoudBodyParser.rMobileUpdbase(mobileId, mobile, status, creditLimit, level, owner,
						bonusAccount, bonusOwner, bonusBankName))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileUpdBase>> ", e));
		return ret[0];
	}

	@Override
	public Object rMobileUpdAccStatus(String mobile, String account, Integer type, Integer status) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rMobileUpdAccStatus(reqCoudBodyParser.rMobileUpdAccStatus(mobile, account, type, status))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileUpdBase>> ", e));
		return ret[0];
	}

	@Override
	public Object rMobileBonusAccList(String mobile) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rMobileBonusAccList(reqCoudBodyParser.rMobileBonusAccList(mobile))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileBonusAccList>> ", e));
		return ret[0];
	}

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
	@Override
	public Object rMobileGet(String mobile) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService().rMobileGet(reqCoudBodyParser.rMobileGet(mobile))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileGet Error.", e));
		return ret[0];
	}

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
	@Override
	public Object rMobileGetBal(String mobile) {
		Object[] ret = new Object[1];
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().rMobileGetBal(req.rMobileGetBal(mobile))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileGetBal Error.", e));
		return ret[0];
	}

	@Override
	public Object rMobilePageForQr(String mobile, String account, Integer accountType, Integer amtBegin, Integer amtEnd,
			Integer pageSize, Integer pageNo) {
		Object[] ret = new Object[1];
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.rMobilePageForQr(
						req.rMobilePageForQr(mobile, account, accountType, amtBegin, amtEnd, pageSize, pageNo))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobilePageForQr Error.", e));
		return ret[0];
	}

	@Override
	public Object rMobileGenQR(String mobile, String account, Integer accountType, Integer amtBegin, Integer amtEnd,
			Integer step) {
		Object[] ret = new Object[1];
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.rMobileGenQR(req.rMobileGenQR(mobile, account, accountType, amtBegin, amtEnd, step))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileGenQR Error.", e));
		return ret[0];
	}

	@Override
	public Object rMobileGenExactQR(String mobile, String account, Integer accountType, String exactQR) {
		Object[] ret = new Object[1];
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.rMobileGenExactQR(req.rMobileGenExactQR(mobile, account, accountType, exactQR))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileGenExactQR Error.", e));
		return ret[0];
	}

	@Override
	public Object rMobileGenZeroQR(String mobile, String account, Integer accountType) {
		Object[] ret = new Object[1];
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.rMobileGenZeroQR(req.rMobileGenZeroQR(mobile, account, accountType))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileGenZeroQR Error.", e));
		return ret[0];
	}

	@Override
	public Object rMobileDelQR(String mobile, String account, Integer accountType, Integer amt) {
		Object[] ret = new Object[1];
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.rMobileDelQR(req.rMobileGenQR(mobile, account, accountType, amt))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileDelQR Error.", e));
		return ret[0];
	}

	@Override
	public Object rMobileDelQRList(String account, Integer accountType, List<Integer> amtList) {
		Object[] ret = new Object[1];
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.rMobileDelQRList(req.rMobileDelQRList(account, accountType, amtList))
				.subscribe(d -> ret[0] = d, e -> log.error("rMobileDelQR Error.", e));
		return ret[0];
	}

	@Override
	public Object rMobileDel(String mobile) {
		Object[] ret = new Object[1];
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().rMobileDel(req.rMobileDel(mobile)).subscribe(d -> ret[0] = d,
				e -> log.error("rMobileDel Error.", e));
		return ret[0];
	}

	@Override
	public Object rBankTransfer(int frType, String account, BigDecimal amount, String nickName, String toAccount,
			String toAccountBank) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rBankTransfer(
						reqCoudBodyParser.rBankTransfer(frType, account, amount, nickName, toAccount, toAccountBank))
				.subscribe(d -> ret[0] = d, e -> log.error("rBankTransfer Error.", e));
		return ret[0];
	}

	@Override
	public Object rBonusTotalForEach(List<String> mobileList) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rBonusTotalForEach(reqCoudBodyParser.rBonusTotalForEach(mobileList))
				.subscribe(d -> ret[0] = d, e -> log.error("rBonusTotalForEach Error.", e));
		return ret[0];
	}

	@Override
	public Object rBonusFindIncomeLogByAliAcc(String mobile, BigDecimal startAmt, BigDecimal endAmt, Long startTime,
			Long endTime, Integer pageSize, Integer pageNo) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rBonusFindIncomeLogByAliAcc(reqCoudBodyParser.rBonusFindIncomeLogByAliAcc(mobile, startAmt, endAmt,
						startTime, endTime, pageSize, pageNo))
				.subscribe(d -> ret[0] = d, e -> log.error("rBonusFindIncomeLogByAliAcc Error.", e));
		return ret[0];
	}

	@Override
	public Object rBonusFindIncomeLogByWecAcc(String mobile, BigDecimal startAmt, BigDecimal endAmt, Long startTime,
			Long endTime, Integer pageSize, Integer pageNo) {
		pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rBonusFindIncomeLogByWecAcc(reqCoudBodyParser.rBonusFindIncomeLogByWecAcc(mobile, startAmt, endAmt,
						startTime, endTime, pageSize, pageNo))
				.subscribe(d -> ret[0] = d, e -> log.error("rBonusFindIncomeLogByWecAcc Error.", e));
		return ret[0];
	}

	@Override
	public Object rBonusBonus(String mobile, BigDecimal startAmt, BigDecimal endAmt, Long startTime, Long endTime,
			Integer pageSize, Integer pageNo) {
		pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rBonusBonus(
						reqCoudBodyParser.rBonusBouns(mobile, startAmt, endAmt, startTime, endTime, pageSize, pageNo))
				.subscribe(d -> ret[0] = d, e -> log.error("rBonusFindIncomeLogByWecAcc Error.", e));
		return ret[0];
	}

	@Override
	public Object rSysFindPro() {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService()
				.rSysFindPro(RequestBody.create(MediaType.parse("application/json"), "{}"))
				.subscribe(d -> ret[0] = d, e -> log.error("rSysFindPro Error.", e));
		return ret[0];
	}

	@Override
	public Object rSysSavePro(Map<String, Object> proMap) {
		Object[] ret = new Object[1];
		HttpClientCloud.getInstance().getCloudService().rSysSavePro(reqCoudBodyParser.rSysSavePro(proMap))
				.subscribe(d -> ret[0] = d, e -> log.error("rSysSavePro Error.", e));
		return ret[0];
	}
}
