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
import com.xinbo.fundstransfer.report.up.ReportYSF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@PatchAnnotation(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_YunSFAbsentFlow)
public class YunSFAbsentFlowPatchAction extends PatchAction {
	protected static final Logger logger = LoggerFactory.getLogger(YunSFAbsentFlowPatchAction.class);

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
		int stIndex = (int) analyse[1], edIndex = (int) analyse[2];
		if (diff.compareTo(BigDecimal.ZERO) == 0 || stIndex < 0 || edIndex < 0 || stIndex == edIndex)
			return false;
		List<Long> hisBankLogIdList = sysList.stream().filter(p -> Objects.nonNull(p.getBankLogId()))
				.map(BizSysLog::getBankLogId).collect(Collectors.toList());
		List<ReportYSF> ysfList = check.getYSF(template, check.getBase()).stream()
				.filter(p -> !hisBankLogIdList.contains(p.getFlogId()))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getFlogId() - o1.getFlogId()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(ysfList))
			return false;
		BizSysLog start = sysList.get(stIndex);
		if (Objects.nonNull(start) && Objects.nonNull(start.getBankLogId())) {
			Long maxId = start.getBankLogId();
			ysfList = ysfList.stream().filter(p -> maxId >= p.getFlogId()).collect(Collectors.toList());
		}
		List<ReportYSF> combination = SystemAccountUtils.combinate(diff, ysfList);
		if (CollectionUtils.isEmpty(combination))
			return false;
		BizSysLog first = sysList.get(stIndex), end = sysList.get(edIndex);
		for (ReportYSF ysf : combination) {// 入款流水正常保存
			BizBankLog lg = bankLog(ysf);
			BigDecimal[] bs = storeHandler.setSysBal(template, base.getId(), lg.getAmount(), null, false);
			long[] sg = storeHandler.transInBank(lg.getFromAccount(), lg, bs);
			check.init(bs, null);
			check.movYSF(template, lg);
			logger.info(
					"SB{} [ INBANK YunSF INCOME  ] >>  bank: {} sys: {} amt: {} sysId: {}  oppAccount: {} oppOwner: {}",
					lg.getFromAccount(), bs[0], bs[1], lg.getAmount(), sg[1], lg.getToAccount(),
					lg.getToAccountOwner());
		}
		sysList = storeHandler.findSysLogFromCache(check.getTarget());
		int stIndex0 = 0, edIndex0 = combination.size() - 1;
		stIndex = sysList.indexOf(first);
		edIndex = sysList.indexOf(end);
		List<BizSysLog> cloned = cloned(sysList, edIndex);
		List<BizSysLog> sorted = sort(sysList, stIndex0, edIndex0, stIndex, edIndex);
		storeHandler.updateByBatch(base, BigDecimal.ZERO, sorted, cloned);
		return true;
	}

	private List<BizSysLog> sort(List<BizSysLog> sysList, int stIndex0, int edIndex0, int stIndex, int edIndex) {
		List<BizSysLog> result = new ArrayList<>();
		BigDecimal end = sysList.get(edIndex).getBalance();
		List<Long> idIndex = new ArrayList<>();
		for (int index = stIndex0; index < edIndex; index++) {
			idIndex.add(sysList.get(index).getId());
		}
		int mov0 = edIndex0 - stIndex0 + 1;
		int mov1 = edIndex - stIndex;
		for (int index = stIndex0; index < edIndex; index++) {
			BizSysLog sys = sysList.get(index);
			if (index <= edIndex0)
				sys.setId(idIndex.get(index + mov1));
			else
				sys.setId(idIndex.get(index - mov0));
		}
		List<BizSysLog> tmp = sysList.subList(stIndex, edIndex);
		tmp.addAll(sysList.subList(stIndex0, edIndex0 + 1));
		tmp.add(sysList.get(edIndex));
		for (int index = edIndex - 1; index >= 0; index--) {
			BizSysLog lg = tmp.get(index);
			end = end.add(lg.getAmount());
			lg.setBalance(end);
			if (index <= edIndex0) {
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
			if (Objects.isNull(sysLog))
				continue;
			BizSysLog clone = sysLog.clone();
			if (Objects.nonNull(clone))
				result.add(clone);
		}
		return result;
	}

	private BizBankLog bankLog(ReportYSF ysf) {
		BizBankLog result = new BizBankLog();
		result.setId(ysf.getFlogId());
		result.setCreateTime(new Date(ysf.getCrawlTm()));
		result.setTradingTime(new Date(ysf.getCrawlTm()));
		result.setFromAccount(ysf.getFromAccount());
		result.setTaskId(ysf.getTaskId());
		result.setTaskType(ysf.getTaskType());
		result.setOrderNo(ysf.getOrderNo());
		result.setAmount(ysf.getAmount());
		result.setSummary(ysf.getSummary());
		result.setToAccount(ysf.getOppAccount());
		result.setToAccountOwner(ysf.getOppOwner());
		return result;
	}

	private Number[] computeDifferance(List<BizSysLog> sysList) {
		int l = sysList.size(), stIndex = 0, edIndex = 0;
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
			if (diff.compareTo(BigDecimal.ZERO) == 0) {
				edIndex = index;
				break;
			}
			if (ret.compareTo(BigDecimal.ZERO) == 0) {
				stIndex = index;
				ret = diff;
			} else if (ret.compareTo(diff) != 0) {
				ret = BigDecimal.ZERO;
				break;
			}
		}
		return ret.compareTo(BigDecimal.ZERO) == 0 ? new Number[] { BigDecimal.ZERO, -1, -1 }
				: new Number[] { ret, stIndex, edIndex };
	}
}
