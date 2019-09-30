package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.FeeUtil;
import com.xinbo.fundstransfer.RefundUtil;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.fail.*;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 回冲确认：在内存中寻找订单信息（BIZ_SYS_LOG）
 */
@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_REFUND_MATCHED_IN_MEMERY)
public class RefundMatchedInMemeryFailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(RefundMatchedInMemeryFailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(handler) || Objects.isNull(param)
				|| Objects.isNull(param.getBankLog()))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		if (Objects.isNull(lg.getAmount()) || lg.getAmount().compareTo(BigDecimal.ZERO) < 0)
			return false;
		if (Objects.nonNull(lg.getTaskId()) && lg.getTaskId() > 0)
			return false;
		List<BizSysLog> logHisList = storeHandler.findSysLogFromCache(base.getId());
		if (CollectionUtils.isEmpty(logHisList))
			return false;
		BizSysLog sys = accurateMatched(logHisList, lg);
		if (Objects.isNull(sys))
			sys = indistinctMatched(base, logHisList, lg);
		if (Objects.isNull(sys))
			return false;
		SysBalTrans trans = new SysBalTrans(genMsg4FromSysLog(sys));
		BigDecimal fee = Objects.isNull(sys) ? BigDecimal.ZERO : sys.getFee();
		// 回冲手续费转换
		fee = FeeUtil.fee4Refund(base, fee);
		BigDecimal[] bs = storeHandler.setSysBal(template, trans.getFrId(), trans.getAmt(), fee.abs(), false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, trans);
		long[] sg = storeHandler.refund(trans, lg, bs);
		trans.setSysLgId(sg[0]);
		trans.setOppSysLgId(sg[1]);
		lg.setTaskId(trans.getTaskId());
		lg.setTaskType(trans.getTaskType());
		String remark = SysBalUtils.autoRemark4Refund(4, lg.getId());
		handler.fail(template, trans, lg, remark);
		String k = SysBalTrans.genMsg(trans);
		logger.info("SB{} SB{} [ FLOW REFUND MATCHED IN MEMERY ] >> amount: {} flowId: {}  msg: {}", trans.getFrId(),
				trans.getToId(), trans.getAmt(), lg.getId(), k);
		return true;
	}

	/**
	 * 精确查找
	 */
	private BizSysLog accurateMatched(List<BizSysLog> sysList, BizBankLog lg) {
		String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount());
		String TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		if (StringUtils.isBlank(TO_ACC_3) && StringUtils.isBlank(TO_OWN_2))
			return null;
		BigDecimal AMT = SysBalUtils.radix2(lg.getAmount());
		if (AMT.compareTo(BigDecimal.ZERO) <= 0)
			return null;
		List<BizSysLog> reList = sysList.stream()
				.filter(p -> Objects.nonNull(p.getAmount()) && AMT.add(p.getAmount()).compareTo(BigDecimal.ZERO) == 0
						&& (StringUtils.isNotBlank(TO_ACC_3)
								&& Objects.equals(TO_ACC_3, SysBalUtils.last3letters(p.getOppAccount()))
								|| StringUtils.isNotBlank(TO_OWN_2)
										&& Objects.equals(TO_OWN_2, SysBalUtils.last2letters(p.getOppOwner()))))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getId() - o1.getId())).collect(Collectors.toList());
		return CollectionUtils.isEmpty(reList) ? null : reList.get(0);
	}

	/**
	 * 非精确匹配
	 */
	private BizSysLog indistinctMatched(AccountBaseInfo base, List<BizSysLog> sysList, BizBankLog lg) {
		Integer st = lg.getStatus();
		String owner = StringUtils.trimToEmpty(lg.getToAccountOwner());
		boolean refund0 = Objects.nonNull(st) && (Refunded == st || Refunding == st);
		boolean refund2 = StringUtils.isNotBlank(owner) && (RefundUtil.refund(base.getBankType(), owner)
				|| Objects.equals(SysBalUtils.last2letters(base.getOwner()), SysBalUtils.last2letters(owner)));
		if (!refund0 && !refund2)
			return null;
		BigDecimal AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		List<BizSysLog> reList = sysList.stream()
				.filter(p -> Objects.nonNull(p.getAmount()) && AMT.add(p.getAmount()).compareTo(BigDecimal.ZERO) == 0)
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getId() - o1.getId())).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(reList))
			return null;
		BizSysLog ret = reList.get(0);
		int position = sysList.indexOf(ret);
		if (position > 30)
			return null;
		int tmp = -1;
		for (int index = 0; index < position; index++) {
			BizSysLog item = sysList.get(index);
			if (Objects.nonNull(item) && Objects.nonNull(item.getAmount()) && item.getAmount().compareTo(AMT) == 0) {
				tmp = index;
				break;
			}
		}
		return tmp >= 0 ? null : ret;
	}

	private String genMsg4FromSysLog(BizSysLog lg) {
		int sys = SysBalTrans.SYS_SUB;
		int frId = Objects.isNull(lg.getAccountId()) ? 0 : lg.getAccountId();
		int toId = Objects.isNull(lg.getOppId()) ? 0 : lg.getOppId();
		BigDecimal amt = SysBalUtils.radix2(lg.getAmount()).abs();
		BigDecimal before = BigDecimal.ZERO;
		BigDecimal after = BigDecimal.ZERO;
		String frAcc3Last = StringUtils.EMPTY;
		String toAcc3Last = SysBalUtils.last3letters(lg.getOppAccount());
		long getTm = Objects.isNull(lg.getCreateTime()) ? System.currentTimeMillis() : lg.getCreateTime().getTime();
		long ackTm = Objects.isNull(lg.getSuccessTime()) ? System.currentTimeMillis() : lg.getSuccessTime().getTime();
		int ackByCurrTrans = SysBalTrans.ACK_NONE;
		int ackByNextTrans = SysBalTrans.ACK_NONE;
		int ackByCurrBal = SysBalTrans.ACK_NONE;
		int ackByCurrFlow = SysBalTrans.ACK_ACK;
		int ackByOppBal = SysBalTrans.ACK_NONE;
		int ackByOppFlow = SysBalTrans.ACK_NONE;
		long taskId = Objects.isNull(lg.getOrderId()) ? 0 : lg.getOrderId();
		int taskType = SysBalTrans.TASK_TYPE_INNER;
		if (Objects.equals(lg.getType(), SysLogType.Rebate.getTypeId()))
			taskType = SysBalTrans.TASK_TYPE_OUTREBATE;
		else if (Objects.equals(lg.getType(), SysLogType.Outward.getTypeId()))
			taskType = SysBalTrans.TASK_TYPE_OUTMEMEBER;
		BigDecimal beopp = BigDecimal.ZERO;
		String frOwn2Last = StringUtils.EMPTY;
		String toOwn2Last = SysBalUtils.last2letters(lg.getOppOwner());
		long sysLgId = lg.getId();
		long oppSysLgId = 0;
		long bankLgId = Objects.isNull(lg.getBankLogId()) ? 0 : lg.getBankLogId();
		long oppBankLgId = 0;
		int result = 4;// 待确认
		int ackByCurrResult1 = SysBalTrans.ACK_NONE;//
		int registWay = SysBalTrans.REGIST_WAY_AUTO;//
		return SysBalTrans.genMsg(sys, frId, toId, amt, before, after, frAcc3Last, toAcc3Last, getTm, ackTm,
				ackByCurrTrans, ackByNextTrans, ackByCurrBal, ackByCurrFlow, ackByOppBal, ackByOppFlow, taskId,
				taskType, beopp, frOwn2Last, toOwn2Last, sysLgId, oppSysLgId, bankLgId, oppBankLgId, result,
				ackByCurrResult1, registWay);
	}

}
