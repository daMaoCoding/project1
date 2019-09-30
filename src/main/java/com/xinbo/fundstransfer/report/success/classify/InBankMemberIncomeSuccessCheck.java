package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.RefundUtil;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 入款卡：会员入款确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_MEMBER_INCOME)
public class InBankMemberIncomeSuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(InBankMemberIncomeSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getBankLog()) || Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankLogId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return income(template, base, lg, check);
	}

	private boolean income(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		if (Refunding == lg.getStatus() || Refunded == lg.getStatus() || Interest == lg.getStatus()
				|| Fee == lg.getStatus() || lg.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			return false;
		// 流水姓名中 带有冲正字眼，或 流水对方姓名与汇款账号的姓名一样 ：该流水为回冲流水(taskId == null||taskId ==0)
		String toOwner = lg.getToAccountOwner();
		if ((Objects.isNull(lg.getTaskId()) || lg.getTaskId() == 0) && Objects.nonNull(toOwner)
				&& (RefundUtil.refund(base.getBankType(), toOwner) || Objects
						.equals(SysBalUtils.last2letters(base.getOwner()), SysBalUtils.last2letters(toOwner))))
			return false;
		// 只有taskId为空或为0时,才进行以下检测
		if (Objects.isNull(lg.getTaskId()) || lg.getTaskId() == 0) {
			// 根据系统订单记录判定 回冲流水
			if (refundBySysHisList(lg)) {
				logger.error(
						"SB{} [ INBANK REFUND INCOME BY SYS LOG ] >> bankLogId: {}  amt: {}  oppAccount: {} oppOwner: {}",
						lg.getFromAccount(), lg.getId(), lg.getAmount(), lg.getToAccount(), lg.getToAccountOwner());
				return true;
			}
			// 根据流水历史记录判定 1.回冲流水 2.重复流水
			List<BizBankLog> bankHisList = storeHandler.findBankLogFromCache(base.getId());
			// 判定回冲流水
			if (refundByBankHisList(lg, bankHisList)) {
				logger.error(
						"SB{} [ INBANK REFUND INCOME BY BANK LOG ] >> bankLogId: {}  amt: {}  oppAccount: {} oppOwner: {}",
						lg.getFromAccount(), lg.getId(), lg.getAmount(), lg.getToAccount(), lg.getToAccountOwner());
				return true;
			}
			// 判定重复流水
			if (duplicate(lg, bankHisList)) {
				logger.error(
						"SB{} [ INBANK DUPLICATE INCOME BY BANK LOG ] >> bankLogId: {}  amt: {}  oppAccount: {} oppOwner: {}",
						lg.getFromAccount(), lg.getId(), lg.getAmount(), lg.getToAccount(), lg.getToAccountOwner());
				return true;
			}
		}
		// 入款流水正常保存
		BigDecimal[] bs = storeHandler.setSysBal(template, base.getId(), lg.getAmount(), null, false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0) {
			bs[0] = lg.getBalance();
		} else if (Objects.equals(base.getType(), InBank) && (Objects.equals(base.getSubType(), IN_BANK_YSF)
				|| Objects.equals(base.getSubType(), IN_BANK_YSF_MIX))) {
			List<BizSysLog> lastList = storeHandler.findSysLogFromCache(base.getId());
			BizSysLog last = CollectionUtils.isEmpty(lastList) ? null : lastList.get(0);
			if (Objects.nonNull(last) && Objects.nonNull(last.getBalance()) && Objects.nonNull(last.getBankBalance())
					&& last.getBalance().compareTo(last.getBankBalance()) == 0)
				bs[0] = last.getBankBalance().add(lg.getAmount());
		}
		long[] sg = storeHandler.transInBank(lg.getFromAccount(), lg, bs);
		check.init(bs, null);
		logger.info("SB{} [ INBANK INCOME  ] >>  bank: {} sys: {} amt: {} sysId: {}  oppAccount: {} oppOwner: {}",
				lg.getFromAccount(), bs[0], bs[1], lg.getAmount(), sg[1], lg.getToAccount(), lg.getToAccountOwner());
		return true;
	}

	/**
	 * 根据系统订单历史记录 判定 回冲流水
	 * 
	 */
	private boolean refundBySysHisList(BizBankLog lg) {
		if (Objects.isNull(lg) || Objects.isNull(lg.getAmount()))
			return false;
		String TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount());
		if (StringUtils.isEmpty(TO_OWN_2) && StringUtils.isEmpty(TO_ACC_3))
			return false;
		BigDecimal AMT = SysBalUtils.radix2(lg.getAmount());
		List<BizSysLog> sysHisList = storeHandler.findSysLogFromCache(lg.getFromAccount());
		return !CollectionUtils.isEmpty(sysHisList) && sysHisList.stream()
				.filter(p -> AMT.add(p.getAmount()).compareTo(BigDecimal.ZERO) == 0 && (StringUtils.isNotEmpty(TO_OWN_2)
						&& Objects.equals(TO_OWN_2, SysBalUtils.last2letters(p.getOppOwner()))
						|| StringUtils.isNotBlank(TO_ACC_3)
								&& Objects.equals(TO_ACC_3, SysBalUtils.last3letters(p.getOppAccount()))))
				.count() > 0;
	}

	/**
	 * 回冲流水判定:</br>
	 * 场景一:</br>
	 * 记录号 交易金额 对方姓名 交易后余额</br>
	 * 1 --- +500 --- 李四 --- 1000.00</br>
	 * 2 --- -100 --- 张三 --- 0900.00</br>
	 * 3 --- +100 --- 张三 --- 1000.00</br>
	 * 记录3 应该是 记录2 的回冲流水 </br>
	 * 场景二:</br>
	 * 记录号 交易金额 对方姓名 交易后金额</br>
	 * 1 --- +500 --- 李四 --- 1000.00</br>
	 * 2 --- -100 --- 张三 --- 0900.00</br>
	 * 3 --- -2.0 --- 费用 --- 0898.00</br>
	 * 4 --- +100 --- 张三 --- 0998.00</br>
	 * 记录4 应该是 记录2 的回冲流水
	 * 
	 */
	private boolean refundByBankHisList(BizBankLog lg, List<BizBankLog> bankHisList) {
		int index = bankHisList.indexOf(lg);
		if (index < 0)
			return false;
		int l = bankHisList.size();
		if (index + 1 <= l - 1 && SysBalUtils.refund(lg, bankHisList.get(index + 1), BigDecimal.ZERO))
			return true;
		if (index + 1 < l - 1) {
			BizBankLog feeLog = bankHisList.get(index + 1);
			BigDecimal feeAmt = feeLog.getAmount();
			boolean fee = SysBalUtils.fee(feeLog) || SysBalUtils.fee050(feeAmt);
			return fee && SysBalUtils.refund(lg, bankHisList.get(index + 2), feeAmt);
		}
		return false;
	}

	/**
	 * 会员重复流水判定：</br>
	 * 判定标准</br>
	 * 1.存在转账历史记录 </br>
	 * 2.转账金额相等</br>
	 * 3.ID不一样</br>
	 * 4.转账金额为正</br>
	 * 5.转账后的余额不为空，且相等</br>
	 * 6.交易时间在10秒之内</br>
	 */
	private boolean duplicate(BizBankLog lg, List<BizBankLog> bankHisList) {
		if (!CollectionUtils.isEmpty(bankHisList) || Objects.isNull(lg))
			return false;
		BigDecimal amt0 = SysBalUtils.radix2(lg.getAmount());
		BigDecimal bal0 = SysBalUtils.radix2(lg.getBalance());
		if (bal0.compareTo(BigDecimal.ZERO) == 0)
			return false;
		boolean result = false;
		for (BizBankLog item : bankHisList) {
			if (Objects.isNull(item) || Objects.equals(lg.getId(), item.getId())
					|| bal0.compareTo(SysBalUtils.radix2(item.getBalance())) != 0
					|| amt0.compareTo(SysBalUtils.radix2(item.getAmount())) != 0
					|| Math.abs(lg.getTradingTime().getTime() - item.getTradingTime().getTime()) > 10000)
				continue;
			result = true;
			break;
		}
		return result;
	}
}
