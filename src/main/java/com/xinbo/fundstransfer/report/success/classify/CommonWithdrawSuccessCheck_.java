package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流水确认数据库订单
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_WITHDRAW_)
public class CommonWithdrawSuccessCheck_ extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(CommonWithdrawSuccessCheck_.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBankLog())
				|| Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return withdraw_(template, lg, param.getFee(), check);
	}

	protected boolean withdraw_(StringRedisTemplate template, BizBankLog lg, BigDecimal fee, ReportCheck check) {
		if (Refunding == lg.getStatus() || Refunded == lg.getStatus() || Interest == lg.getStatus()
				|| Fee == lg.getStatus() || lg.getAmount().compareTo(BigDecimal.ZERO) >= 0
				|| !Objects.equals(lg.getTaskType(), SysBalTrans.TASK_TYPE_INNER) || lg.getTaskId() == null
				|| lg.getTaskId() == 0 || !SysBalUtils.radix(lg.getAmount()))
			return false;
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		List<SysBalTrans> reList = check.getTransOutAll().stream().filter(p -> TR_AMT.compareTo(p.getAmt()) == 0)
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(reList)) {
			return false;
		}
		// 查看数据库最近8个小时有无该数据
		if (storeHandler
				.findSysLogFromCache(lg.getFromAccount()).stream().filter(p -> Objects.nonNull(p.getAmount())
						&& p.getAmount().compareTo(BigDecimal.ZERO) < 0 && p.getAmount().abs().compareTo(TR_AMT) == 0)
				.count() > 0)
			return false;
		BizIncomeRequest req = incomeDao.findById2(lg.getTaskId());
		if (Objects.isNull(req) || req.getToId() == null || req.getToId() == 0)
			return false;
		BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), TR_AMT, fee, true);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, null);
		SysBalTrans ts = new SysBalTrans();
		ts.setFrId(lg.getFromAccount());
		ts.setToId(req.getToId());
		ts.setBankLgId(lg.getId());
		ts.setTaskType(lg.getTaskType());
		ts.setTaskId(lg.getTaskId());
		ts.setSys(SysBalTrans.SYS_SUB);
		ts.setAckByCurrFlow(SysBalTrans.ACK_ACK);
		ts.setAmt(TR_AMT);
		ts.setAckTm(System.currentTimeMillis());
		long[] sg = storeHandler.transFrom(ts, fee, lg, bs);
		logger.info("SB{} [ FLOW WITHDRAW_ CONFIRMED ] >> amount: {} orderId: {} orderNo: {} flowId: {}  sysLogId: {}",
				ts.getFrId(), ts.getAmt(), lg.getTaskId(), lg.getOrderNo(), lg.getId(), sg[0]);
		return true;
	}
}
