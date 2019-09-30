package com.xinbo.fundstransfer.report.patch.classify;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.report.InterestUtils;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.patch.PatchAction;
import com.xinbo.fundstransfer.report.patch.PatchAnnotation;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 银行有季度结息，但是无结息流水，处理
 */
@PatchAnnotation(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_QuarterInterest)
public class QuarterInterestPatchAction extends PatchAction {
	protected static final Logger logger = LoggerFactory.getLogger(QuarterInterestPatchAction.class);

	public boolean deal(StringRedisTemplate template, BizAccount account, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(check) || Objects.isNull(check.getTarget())
				|| Objects.isNull(check.getBase()) || check.getCheckLast() || check.getCount() == 0)
			return false;
		// 季度末月的20日为结息日，次日付息。 结息日分别为: 3月20日 6月20日 9月20日 12月20日,入账日期为21日
		if (!InterestUtils.checkQuarterInterestDate())
			return false;
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(check.getTarget());
		int currentDayOfMonth = SystemAccountUtils.currentDayOfMonth();
		long currentDayStartMillis = SystemAccountUtils.currentDayStartMillis();
		// 检测：已有系统账目中有无结息流水，有结息流水直接返回
		long refMillis = currentDayOfMonth == 21 ? currentDayStartMillis
				: currentDayStartMillis - SystemAccountUtils.ONE_DAY_MILLIS;
		if (sysList.stream().filter(p -> Objects.nonNull(p.getCreateTime()) && p.getCreateTime().getTime() > refMillis
				&& Objects.equals(p.getType(), SysLogType.Interest.getTypeId())).count() > 0)
			return false;
		int findIndex = computeIndex(sysList);
		if (findIndex < 0)
			return false;
		BizBankLog lg = interestBankLog(sysList.get(0));
		BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), lg.getAmount().abs(), null, false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, null);
		storeHandler.transInterest(lg.getFromAccount(), lg, bs);
		logger.info("SB{} [  PATCH QUARTER INTEREST ] >>  msg:{}", check.getTarget(), ObjectMapperUtils.serialize(lg));
		return true;
	}

	private BizBankLog interestBankLog(BizSysLog first) {
		BigDecimal interest = SysBalUtils.radix2(first.getBankBalance()).subtract(first.getBalance());
		BizBankLog ret = new BizBankLog();
		ret.setId(null);
		ret.setFromAccount(first.getAccountId());
		ret.setTradingTime(new Date());
		ret.setAmount(interest);
		ret.setStatus(BankLogStatus.Interest.getStatus());
		ret.setRemark(null);
		ret.setToAccount(null);
		ret.setToAccountOwner(null);
		ret.setBalance(first.getBalance());
		ret.setCreateTime(ret.getTradingTime());
		ret.setSummary("季度结息[系统自动生成]");
		ret.setUpdateTime(ret.getTradingTime());
		return ret;
	}

	private int computeIndex(List<BizSysLog> lgList) {
		if (CollectionUtils.isEmpty(lgList) || lgList.size() <= 2)
			return -1;
		BizSysLog first = lgList.get(0), second = lgList.get(1);
		BigDecimal diff0 = SysBalUtils.radix2(first.getBankBalance()).subtract(first.getBalance());
		BigDecimal diff1 = SysBalUtils.radix2(second.getBankBalance()).subtract(second.getBalance());
		if (diff0.compareTo(BigDecimal.ZERO) <= 0 || diff0.compareTo(new BigDecimal("2.00")) >= 0
				|| diff0.compareTo(diff1) != 0)
			return -1;
		int findIndex = -1, l = lgList.size();
		for (int index = 2; index < l; index++) {
			if (index > 12)// 只考虑最近12条记录
				break;
			BizSysLog item = lgList.get(index);
			if (Objects.equals(SysLogType.Init.getTypeId(), item.getType()))
				break;
			BigDecimal diff = SysBalUtils.radix2(item.getBankBalance()).subtract(item.getBalance());
			if (diff.compareTo(BigDecimal.ZERO) == 0) {
				findIndex = index - 1;
				break;
			}
			if (diff.compareTo(diff0) != 0)
				break;
		}
		return findIndex;
	}
}
