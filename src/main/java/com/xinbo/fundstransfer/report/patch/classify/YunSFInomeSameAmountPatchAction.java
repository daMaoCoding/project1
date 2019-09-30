package com.xinbo.fundstransfer.report.patch.classify;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.patch.PatchAction;
import com.xinbo.fundstransfer.report.patch.PatchAnnotation;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@PatchAnnotation(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_YunSFInomeSameAmount)
public class YunSFInomeSameAmountPatchAction extends PatchAction {
	protected static final Logger logger = LoggerFactory.getLogger(YunSFInomeSameAmountPatchAction.class);

	public boolean deal(StringRedisTemplate template, BizAccount account, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(check) || Objects.isNull(check.getBase()))
			return false;
		AccountBaseInfo base = check.getBase();
		if (!SystemAccountUtils.ysf(base))
			return false;
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(check.getTarget());
		if (CollectionUtils.isEmpty(sysList))
			return false;
		Number[] analyse = computeDifferance(sysList);
		BigDecimal diff = (BigDecimal) analyse[0];
		if (diff.compareTo(BigDecimal.ZERO) <= 0)
			return false;
		int stIndex = (int) analyse[1], edIndex = (int) analyse[2], smIndex = (int) analyse[3];
		BizSysLog first = sysList.get(stIndex), end = sysList.get(edIndex), same = sysList.get(smIndex);
		BizBankLog lg = bankLog(end, same);
		BigDecimal[] bs = storeHandler.setSysBal(template, base.getId(), lg.getAmount(), null, false);
		bs[0] = bs[1];
		long[] sg = storeHandler.transInBank(lg.getFromAccount(), lg, bs);
		check.init(bs, null);
		logger.info(
				"SB{} [ INBANK YunSF INCOME SAME AMOUNT  ] >>  bank: {} sys: {} amt: {} sysId: {}  oppAccount: {} oppOwner: {}",
				lg.getFromAccount(), bs[0], bs[1], lg.getAmount(), sg[1], lg.getToAccount(), lg.getToAccountOwner());
		sysList = storeHandler.findSysLogFromCache(check.getTarget());
		stIndex = sysList.indexOf(first);
		edIndex = sysList.indexOf(end);
		List<BizSysLog> cloned = cloned(sysList, edIndex);
		List<BizSysLog> sorted = sort(sysList, stIndex, edIndex);
		storeHandler.updateByBatch(base, BigDecimal.ZERO, sorted, cloned);
		return true;
	}

	private List<BizSysLog> sort(List<BizSysLog> sysList, int stIndex, int edIndex) {
		List<BizSysLog> result = new ArrayList<>();
		BigDecimal end = sysList.get(edIndex).getBalance();
		List<Long> idIndex = new ArrayList<>();
		for (int index = 0; index < edIndex; index++) {
			idIndex.add(sysList.get(index).getId());
		}
		int mov0 = 1;
		int mov1 = edIndex - stIndex;
		for (int index = 0; index < edIndex; index++) {
			BizSysLog sys = sysList.get(index);
			if (index < stIndex)
				sys.setId(idIndex.get(index + mov1));
			else
				sys.setId(idIndex.get(index - mov0));
		}
		List<BizSysLog> tmp = sysList.subList(stIndex, edIndex);
		tmp.addAll(sysList.subList(0, 1));
		tmp.add(sysList.get(edIndex));
		for (int index = edIndex - 1; index >= 0; index--) {
			BizSysLog lg = tmp.get(index);
			end = end.add(lg.getAmount());
			lg.setBalance(end);
			if (index == edIndex - 1) {
				lg.setBankBalance(lg.getBalance());
			}
			result.add(lg);
		}
		return result;
	}

	private List<BizSysLog> cloned(List<BizSysLog> sysList, int edIndex) {
		List<BizSysLog> result = new ArrayList<>();
		for (int index = 0; index < edIndex; index++) {
			BizSysLog sysLog = sysList.get(index);
			if (Objects.nonNull(sysLog)) {
				BizSysLog clone = sysLog.clone();
				if (Objects.nonNull(clone))
					result.add(clone);
			}
		}
		return result;
	}

	private BizBankLog bankLog(BizSysLog end, BizSysLog same) {
		BizBankLog result = new BizBankLog();
		result.setId(null);
		result.setCreateTime(end.getCreateTime());
		result.setTradingTime(end.getCreateTime());
		result.setFromAccount(same.getAccountId());
		result.setTaskId(null);
		result.setTaskType(SysLogType.Income.getTypeId());
		result.setOrderNo(null);
		result.setAmount(same.getAmount());
		result.setSummary("APP漏抓流水");
		result.setToAccount(null);
		result.setToAccountOwner(null);
		return result;
	}

	private Number[] computeDifferance(List<BizSysLog> sysList) {
		int l = sysList.size(), stIndex = -1, edIndex = -1, smIndex = -1;
		BigDecimal ret = BigDecimal.ZERO;
		for (int index = 0; index < l; index++) {
			if (index > 60) {
				ret = BigDecimal.ZERO;
				break;
			}
			BizSysLog em = sysList.get(index);
			if (Objects.isNull(em) || !Objects.equals(em.getStatus(), SysLogStatus.Valid.getStatusId()))
				continue;
			if (Objects.equals(SysLogType.Init.getTypeId(), em.getStatus())) {
				ret = BigDecimal.ZERO;
				break;
			}
			BigDecimal diff = SysBalUtils.radix2(em.getBankBalance()).subtract(SysBalUtils.radix2(em.getBalance()));
			if (BigDecimal.ZERO.compareTo(diff) == 0) {
				edIndex = index;
				break;
			}
			if (BigDecimal.ZERO.compareTo(ret) == 0) {
				stIndex = index;
				ret = diff;
			} else if (ret.compareTo(diff) != 0) {
				ret = BigDecimal.ZERO;
				break;
			}
		}
		if (ret.compareTo(BigDecimal.ZERO) == 0) {
			return new Number[] { BigDecimal.ZERO, -1, -1, -1 };
		}
		for (int index = edIndex; index < l; index++) {
			if (index > 60)
				break;
			BizSysLog em = sysList.get(index);
			if (Objects.isNull(em) || !Objects.equals(em.getStatus(), SysLogStatus.Valid.getStatusId()))
				continue;
			if (Objects.equals(SysLogType.Init.getTypeId(), em.getStatus()))
				break;
			BigDecimal diff = SysBalUtils.radix2(em.getBankBalance()).subtract(SysBalUtils.radix2(em.getBalance()));
			if (BigDecimal.ZERO.compareTo(diff) != 0)
				break;
			if (SysBalUtils.radix2(em.getAmount()).compareTo(ret) == 0) {
				smIndex = index;
				break;
			}
		}
		if (smIndex == -1)
			return new Number[] { BigDecimal.ZERO, -1, -1, -1 };
		return new Number[] { ret, stIndex, edIndex, smIndex };
	}
}
