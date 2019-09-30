package com.xinbo.fundstransfer.report.patch.classify;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.patch.PatchAction;
import com.xinbo.fundstransfer.report.patch.PatchAnnotation;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 收款方 确认收款相同金额 （1.按照余额确认，2.按照流水确认），确认后，银行余额与系统余额之间存在差额，差额等与该确认金额。
 * 则：按照余额确认的订单，确认失败。
 */
@PatchAnnotation(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_DepositSameAmount)
public class DepositSameAmountPatchAction extends PatchAction {
	protected static final Logger logger = LoggerFactory.getLogger(DepositSameAmountPatchAction.class);

	public boolean deal(StringRedisTemplate template, BizAccount account, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(check) || Objects.isNull(check.getTarget())
				|| Objects.isNull(check.getBase()) || check.getCheckLast() || check.getCount() == 0)
			return false;
		if (Objects.equals(check.getBase().getType(), AccountType.InBank.getTypeId())) // 入款卡不考虑此场景
			return false;
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(check.getTarget());
		if (CollectionUtils.isEmpty(sysList) || sysList.size() == 1)
			return false;
		Integer[] indexArray = computeIndex(sysList);
		int rightIndex = indexArray[0], wrongIndex = indexArray[1];
		if (rightIndex < 0 || wrongIndex < 0)
			return false;
		List<BizSysLog> cloned = clone(rightIndex, wrongIndex, sysList);
		BizSysLog first = cloned.get(0);
		BigDecimal diff = SysBalUtils.radix2(first.getBankBalance().subtract(first.getBalance()));
		List<BizSysLog> sorted = sort(rightIndex, wrongIndex, diff, sysList);
		storeHandler.updateByBatch(check.getBase(), diff, sorted, cloned);
		logger.info("SB{} [ PATCH DEPOSIT SAME AMOUNT ] >> msg:{}", check.getTarget(),
				ObjectMapperUtils.serialize(sorted));
		return false;
	}

	private List<BizSysLog> sort(int rightIndex, int wrongIndex, BigDecimal diff, List<BizSysLog> targetList) {
		List<BizSysLog> result = new ArrayList<>();
		int minIndex = Math.min(rightIndex, wrongIndex);
		for (int index = 0; index < minIndex; index++) {
			BizSysLog item = targetList.get(index);
			item.setBalance(item.getBalance().add(diff));
			result.add(item);
		}
		BizSysLog wrong = targetList.get(wrongIndex);
		wrong.setStatus(SysLogStatus.Invalid.getStatusId());
		result.add(wrong);
		if (wrongIndex > rightIndex) {
			BizSysLog right = targetList.get(rightIndex);
			long tmp = right.getId();
			right.setId(wrong.getId());
			wrong.setId(tmp);
			right.setBalance(wrong.getBalance());
			result.add(right);
		}
		return result;
	}

	private List<BizSysLog> clone(int rightIndex, int wrongIndex, List<BizSysLog> targetList) {
		List<BizSysLog> result = new ArrayList<>();
		int maxIndex = Math.max(rightIndex, wrongIndex);
		for (int index = 0; index <= maxIndex; index++) {
			BizSysLog sys = targetList.get(index);
			if (Objects.nonNull(sys)) {
				BizSysLog cloned = sys.clone();
				if (Objects.nonNull(cloned)) {
					result.add(cloned);
				}
			}
		}
		return result;
	}

	/**
	 * @return [0] 有效数据，[1] 无效数据
	 */
	private Integer[] computeIndex(List<BizSysLog> lgList) {
		if (CollectionUtils.isEmpty(lgList) || lgList.size() < 2)
			return new Integer[] { -1, -1 };
		BizSysLog first = lgList.get(0);
		BigDecimal diff = SysBalUtils.radix2(first.getBankBalance()).subtract(first.getBalance());
		if (diff.compareTo(BigDecimal.ZERO) >= 0)
			return new Integer[] { -1, -1 };
		int rightIndex = -1, wrongIndex = -1, l = lgList.size();
		for (int index = 0; index < l; index++) {
			if (index > 12 || rightIndex >= 0 && wrongIndex >= 0)// 只考虑最近12条记录
				break;
			BizSysLog item = lgList.get(index);
			if (Objects.isNull(item))
				continue;
			if (Objects.equals(SysLogType.Init.getTypeId(), item.getType()))
				break;
			BigDecimal amt = SysBalUtils.radix2(item.getAmount());
			if (diff.add(amt).compareTo(BigDecimal.ZERO) == 0) {
				if (rightIndex == -1 && Objects.nonNull(item.getBankLogId()) && item.getBankLogId() > 0)
					rightIndex = index;
				if (wrongIndex == -1 && Objects.isNull(item.getBankLogId()) || item.getBankLogId() == 0)
					wrongIndex = index;
				continue;
			}
			if (SysBalUtils.radix2(item.getBankBalance()).compareTo(SysBalUtils.radix2(item.getBalance())) != 0)
				break;
		}
		if (rightIndex < 0 || wrongIndex < 0)
			return new Integer[] { -1, -1 };
		int maxIndex = Math.max(rightIndex, wrongIndex);
		BizSysLog max = lgList.get(maxIndex);
		if (SysBalUtils.radix2(max.getBankBalance()).compareTo(SysBalUtils.radix2(max.getBalance())) == 0)
			return new Integer[] { rightIndex, wrongIndex };
		return new Integer[] { -1, -1 };
	}
}