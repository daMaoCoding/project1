package com.xinbo.fundstransfer.report.up.classify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.up.ReportCheck;
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
 * 上报处理： 其他卡：流水 --- 入款流水： 找出转账任务，该银行流水记为系统流水和银行余额，重新计算系统余额；
 * 出款流水：找出已有的系统流水，并调整顺序和核对系统余额和银行余额。
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_OTHERS + SysBalPush.CLASSIFY_BANK_LOGS)
public class OthersStreamReport extends Report {

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
		List<BizBankLog> bankLogList = data.getData();
		int l = bankLogList.size();
		for (int i = 0; i < l; i++) {
			BizBankLog lastlg = i == 0 ? null : data.getData().get(i - 1);
			BizBankLog lg = bankLogList.get(i);
			if (Objects.isNull(lg) || Objects.isNull(lg.getAmount()))
				continue;
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
						fee = flg.getAmount().abs();
						lg.setBalance(flg.getBalance());
					}
				}
				if (!out(template, base, lg, fee, check)) {
					if (!in(template, base, lg, lastlg, check))
						check.error(lg);
				}
			} catch (Exception e) {
				log.info("SB{} [ FLOW OTHERBANK ERROR ] >> lg: {} , msg: {} ", base.getId(),
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
				|| successHandler.commonMatchedWithdrawInDBWithoutOrder(template, base, bankLog, fee, check)
				|| successHandler.othersManualOutward(template, base, bankLog, fee, check);
	}

	private boolean in(StringRedisTemplate template, AccountBaseInfo base, BizBankLog bankLog, BizBankLog lastBankLog,
			ReportCheck check) {
		return successHandler.commonEnhanceCredit(template, base, bankLog, check)
				|| failHandler.refund(base, bankLog, lastBankLog, check)
				|| successHandler.commonInterest(template, base, bankLog, check)
				|| successHandler.othersMatched3Th(template, base, bankLog, check)
				|| successHandler.othersDeposit(template, base, bankLog, check)
				|| failHandler.refund1(base, bankLog, check) || failHandler.refund2(base, bankLog, lastBankLog, check)
				|| failHandler.refundMatcheInMemery(base, bankLog, check)
				|| successHandler.othersMatchedDepositInDB(template, base, bankLog, check)
				|| successHandler.othersMatchedDepositInDBWithoutOrder(template, base, bankLog, check);
	}
}
