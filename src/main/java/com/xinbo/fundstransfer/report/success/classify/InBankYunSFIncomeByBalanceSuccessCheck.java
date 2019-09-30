package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportYSF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 入款卡-云闪付：根据工具端上报的银行余额确认会员入款记录
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK
		+ SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_YunSFIncomeByBalanceSuccess)
public class InBankYunSFIncomeByBalanceSuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(InBankYunSFIncomeByBalanceSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBalance())
				|| Objects.isNull(check) || !SystemAccountUtils.ysf(base))
			return false;
		BigDecimal bal = SysBalUtils.radix2(param.getBalance());
		if (BigDecimal.ZERO.compareTo(bal) == 0)
			return false;
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(base.getId());
		Optional<BizSysLog> optional = sysList.stream().findFirst();
		if (!optional.isPresent())
			return false;
		BizSysLog first = optional.get();
		if (!Objects.equals(first.getStatus(), SysLogStatus.Valid.getStatusId()))
			return false;
		BigDecimal firstSysBal = SysBalUtils.radix2(first.getBalance());
		BigDecimal firstBankBal = SysBalUtils.radix2(first.getBankBalance());
		if (firstSysBal.compareTo(firstBankBal) != 0 || bal.compareTo(firstBankBal) <= 0)
			return false;
		BigDecimal diff = bal.subtract(firstBankBal);
		BizAccount acc = accSer.getById(base.getId());
		if (Objects.isNull(acc) || SysBalUtils.radix2(acc.getBalance()).compareTo(firstSysBal) != 0)
			return false;
		List<Long> hisBankLogIdList = sysList.stream().filter(p -> Objects.nonNull(p.getBankLogId()))
				.map(BizSysLog::getBankLogId).collect(Collectors.toList());
		List<ReportYSF> ysfList = check.getYSF(template, check.getBase()).stream()
				.filter(p -> !hisBankLogIdList.contains(p.getFlogId()))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getFlogId() - o1.getFlogId()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(ysfList))
			return false;
		List<ReportYSF> combList = SystemAccountUtils.combinate(diff, ysfList);
		if (CollectionUtils.isEmpty(combList))
			return false;
		for (ReportYSF ysf : combList) {// 入款流水正常保存
			BizBankLog lg = bankLog(ysf);
			BigDecimal[] bs = storeHandler.setSysBal(template, base.getId(), lg.getAmount(), null, false);
			bs[0] = bs[1];
			long[] sg = storeHandler.transInBank(lg.getFromAccount(), lg, bs);
			check.init(bs, null);
			check.movYSF(template, lg);
			logger.info(
					"SB{} [ INBANK YUNSF INCOME BY BALANCE ] >>  bank: {} sys: {} amt: {} sysId: {}  oppAccount: {} oppOwner: {}",
					lg.getFromAccount(), bs[0], bs[1], lg.getAmount(), sg[1], lg.getToAccount(),
					lg.getToAccountOwner());
		}
		return true;
	}
}
