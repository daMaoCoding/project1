package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.RefundUtil;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
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
import java.util.Date;
import java.util.List;
import java.util.Objects;

@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK
		+ SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_MATCHED_DEPOSIT_INDB_WITHOUT_ORDER)
public class OthersMatchedDepositInDBWithoutOrderSuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory
			.getLogger(OthersMatchedDepositInDBWithoutOrderSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getBankLog()) || Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return deposit(template, base, lg, check);
	}

	private boolean deposit(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		if (Objects.equals(InBank, base.getType()) || Objects.isNull(lg.getTradingTime()))
			return false;
		if (Objects.equals(Refunded, lg.getStatus()) || Objects.equals(Refunding, lg.getStatus())
				|| Objects.equals(Fee, lg.getStatus()) || Objects.equals(Interest, lg.getStatus()))
			return false;
		if (Objects.nonNull(lg.getTaskId()) && lg.getTaskId() > 0)
			return false;
		String toOwner = lg.getToAccountOwner();
		if (Objects.nonNull(toOwner) && (RefundUtil.refund(base.getBankType(), toOwner)
				|| Objects.equals(SysBalUtils.last2letters(base.getOwner()), SysBalUtils.last2letters(toOwner))))
			return false;
		if (!SysBalUtils.radix(lg.getAmount()) || lg.getAmount().compareTo(BigDecimal.ZERO) < 0)
			return false;
		long tdMil = lg.getTradingTime().getTime(), cuMil = System.currentTimeMillis();
		if (Math.abs(SystemAccountUtils.currentDayStartMillis() - tdMil) < 10000 || cuMil - tdMil < 3600000 * 5)
			return false;
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		if (storeHandler.findSysLogFromCache(lg.getFromAccount()).stream()
				.filter(p -> TR_AMT.compareTo(SysBalUtils.radix2(p.getAmount())) == 0).count() > 0)
			return false;
		Date stTm = new Date(cuMil - 3600000 * 12), edTm = new Date(cuMil - 3600000 * 5);
		List<BizIncomeRequest> reqList = incomeDao.find4LatestByTo(lg.getFromAccount(), TR_AMT, stTm, edTm);
		BizIncomeRequest req = findOrder(lg, reqList);
		if (Objects.isNull(req))
			return false;
		BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), TR_AMT, null, false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, null);
		SysBalTrans ts = new SysBalTrans();
		ts.setFrId(req.getFromId());
		ts.setToId(req.getToId());
		ts.setOppBankLgId(0);
		ts.setTaskType(SysBalTrans.TASK_TYPE_INNER);
		ts.setTaskId(req.getId());
		ts.setSys(SysBalTrans.SYS_SUB);
		ts.setAckByOppFlow(SysBalTrans.ACK_ACK);
		ts.setAmt(TR_AMT);
		ts.setAckTm(System.currentTimeMillis());
		long[] sg = storeHandler.transTo(ts, lg, bs);
		logger.info(
				"SB{} [ FLOW DEPOSIT IN DB WITHOUT ORDER ] >> amount: {} orderId: {} orderNo: {} flowId: {}  sysLogId: {}",
				ts.getFrId(), ts.getAmt(), lg.getTaskId(), lg.getOrderNo(), lg.getId(), sg[0]);
		return true;
	}

	private BizIncomeRequest findOrder(BizBankLog lg, List<BizIncomeRequest> reqList) {
		if (CollectionUtils.isEmpty(reqList))
			return null;
		if (reqList.size() == 1)
			return reqList.get(0);
		String OP_ACC = SysBalUtils.last3letters(lg.getToAccount());
		String OP_OWN = SysBalUtils.last2letters(lg.getToAccountOwner());
		if (StringUtils.isBlank(OP_ACC) && StringUtils.isBlank(OP_OWN))
			return null;
		BizIncomeRequest ret = null;
		for (BizIncomeRequest req : reqList) {
			if (Objects.nonNull(ret) || Objects.isNull(req) || Objects.isNull(req.getFromId()) || req.getFromId() == 0)
				continue;
			AccountBaseInfo fr = accSer.getFromCacheById(req.getFromId());
			if (Objects.isNull(fr))
				continue;
			if (StringUtils.isNotBlank(OP_ACC) && Objects.equals(OP_ACC, SysBalUtils.last3letters(fr.getAccount())))
				ret = req;
			if (StringUtils.isNotBlank(OP_OWN) && Objects.equals(OP_OWN, SysBalUtils.last3letters(fr.getOwner())))
				ret = req;
		}
		return ret;
	}
}
