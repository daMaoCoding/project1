package com.xinbo.fundstransfer.report.up.classify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Objects;

import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportYSF;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportUp;

/**
 * 上报处理： 入款卡：流水 -- 1. 入款流水（直接记为系统流水）；2. 出款流水（要找出系统流水，修改系统流水顺序，更新系统余额和银行，并确认匹配）
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_INBANK + SysBalPush.CLASSIFY_BANK_LOGS)
public class InBankStreamReport extends Report {

	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		SysBalPush<ArrayList<BizBankLog>> data = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<ArrayList<BizBankLog>>>() {
				});
		if (Objects.isNull(data) || Objects.isNull(data.getData()) || CollectionUtils.isEmpty(data.getData()))
			return;
		AccountBaseInfo base = accSer.getFromCacheById(data.getTarget());
		if (Objects.isNull(base))
			return;
		int l = data.getData().size();
		for (int i = 0; i < l; i++) {
			BizBankLog lastlg = i == 0 ? null : data.getData().get(i - 1);
			// 如果是云闪付入款流水进行转换
			BizBankLog lg = ysf(template, base, data.getData().get(i), check);
			if (Objects.isNull(lg) || Objects.isNull(lg.getAmount()))
				continue;
			// 去掉云闪付流水标识
			check.movYSF(template, lg);
			// 检查：此流水是否是重复流水，判断标准：与工具上报的最近一条数据 1.转账金额一样 2.转账后的余额不为0.00，且相等
			if (storeHandler.duplicate(lg))
				continue;
			log.info("SB{} [ DEAL DATA ] bankLogId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
					lg.getAmount(), lg.getTaskId());
			lg.setAmount(SysBalUtils.radix2(lg.getAmount()));
			if (Objects.nonNull(lg.getBalance()))
				lg.setBalance(SysBalUtils.radix2(lg.getBalance()));
			try {
				if (i == 0 && Objects.equals(Fee, lg.getStatus())) {
					feeFist(template, lg);
					continue;
				}
				int st = lg.getStatus();
				BigDecimal fee = BigDecimal.ZERO;
				if (st != Fee && st != Interest && st != Refunded && st != Refunding
						&& lg.getAmount().compareTo(BigDecimal.ZERO) < 0 && (i + 1) < l) {
					BizBankLog flg = data.getData().get(i + 1);
					if (Objects.nonNull(flg) && SysBalUtils.fee(flg)) {
						lg.setBalance(flg.getBalance());
						fee = flg.getAmount().abs();
					}
				}
				if (!out(template, base, lg, fee, check)) {
					if (!in(template, base, lg, lastlg, check))
						check.error(lg);
				}
			} catch (Exception e) {
				log.info("SB{} [ FLOW INBANK ERROR ] >> lg: {} , msg: {} ", base.getId(),
						ObjectMapperUtils.serialize(lg), e.getLocalizedMessage(), e);
			}
		}
		// 流水处理后继操作
		post4BankFlow(template, base, check.getTransOutAll());
	}

	private boolean out(StringRedisTemplate template, AccountBaseInfo base, BizBankLog bankLog, BigDecimal fee,
			ReportCheck check) {
		return successHandler.commonMatchedOutward(template, base, bankLog, fee, check)
				|| successHandler.commonWithdraw(template, base, bankLog, fee, check)
				|| successHandler.commonMatchedOutward_(template, base, bankLog, fee, check)
				|| successHandler.commonWithdraw_(template, base, bankLog, fee, check)
				|| successHandler.commonMatchedWithdrawInDBWithoutOrder(template, base, bankLog, fee, check);
	}

	private boolean in(StringRedisTemplate template, AccountBaseInfo base, BizBankLog bankLog, BizBankLog lastBankLog,
			ReportCheck check) {
		return successHandler.commonEnhanceCredit(template, base, bankLog, check)
				|| successHandler.commonInterest(template, base, bankLog, check)
				|| failHandler.refund(base, bankLog, lastBankLog, check) || failHandler.refund1(base, bankLog, check)
				|| failHandler.refund2(base, bankLog, lastBankLog, check)
				|| failHandler.inBankIncomeTest(base, bankLog, check)
				|| failHandler.refundMatcheInMemery(base, bankLog, check)
				|| successHandler.yunSFAbsentIncomeByBankLog(template, base, bankLog, check)
				|| failHandler.incomeDuplicateMatch(base, bankLog, check)
				|| successHandler.inBankMemberIncome(template, base, bankLog, check);

	}

	private BizBankLog ysf(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		if (!SystemAccountUtils.ysf(base))
			return lg;
		if (storeHandler.findSysLogFromCache(base.getId()).stream()
				.filter(p -> Objects.equals(p.getBankLogId(), lg.getId())).count() > 0)
			return null;
		// 检验：是否是入款流水，否则：直接返回
		if (Objects.isNull(lg.getAmount()) || lg.getAmount().compareTo(BigDecimal.ZERO) < 0)
			return lg;
		BigDecimal bankBal = SysBalUtils.radix2(lg.getBalance());
		// 检验：是否是云闪付APP流水，标准如下： 2.转账后余额为零
		if (bankBal.compareTo(BigDecimal.ZERO) == 0) {
			check.setYSF(template, lg);
			log.info(
					"SB{} YSFID{} [ SET YSF ] -> logId：{} amount: {} toaccount: {} toowner: {} taskId: {} taskType: {} orderNo: {}",
					base.getId(), lg.getId(), lg.getId(), lg.getAmount(), lg.getToAccount(), lg.getToAccountOwner(),
					lg.getTaskId(), lg.getTaskType(), lg.getOrderNo());
			return null;
		}
		ReportYSF ysf = check.getYSF(template, lg.getId());
		if (Objects.isNull(ysf) || !Objects.equals(lg.getId(), ysf.getFlogId()))
			return lg;
		if (Objects.nonNull(ysf.getTaskId()))
			lg.setTaskId(ysf.getTaskId());
		if (Objects.nonNull(ysf.getTaskType()))
			lg.setTaskType(ysf.getTaskType());
		if (StringUtils.isNotBlank(ysf.getOrderNo()))
			lg.setOrderNo(ysf.getOrderNo());
		if (Objects.nonNull(ysf.getTaskId()) || Objects.nonNull(ysf.getTaskType())
				|| StringUtils.isNotBlank(ysf.getOrderNo()))
			log.info(
					"SB{} YSFID{} [ GET YSF ] -> logId：{} amount: {} toaccount: {} toowner: {} taskId: {} taskType: {} orderNo: {}",
					base.getId(), lg.getId(), lg.getId(), lg.getAmount(), lg.getToAccount(), lg.getToAccountOwner(),
					lg.getTaskId(), lg.getTaskType(), lg.getOrderNo());
		return lg;
	}
}
