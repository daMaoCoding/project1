package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
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
 * 出款/返利 流水确认订单
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_MATCHEDOUTWARD)
public class CommonMatchedOutwardSuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(CommonMatchedOutwardSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBankLog())
				|| Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return matchedOutward(handler, template, lg, param.getFee(), check);
	}

	protected boolean matchedOutward(SuccessHandler handler, StringRedisTemplate template, BizBankLog lg,
			BigDecimal fee, ReportCheck check) {
		if (Objects.isNull(lg.getTaskType())
				|| (SysBalTrans.TASK_TYPE_OUTMEMEBER != lg.getTaskType()
						&& SysBalTrans.TASK_TYPE_OUTREBATE != lg.getTaskType())
				|| Objects.isNull(lg.getTaskId()) || Objects.equals(Refunded, lg.getStatus())
				|| Objects.equals(Refunding, lg.getStatus()) || Objects.equals(Fee, lg.getStatus())
				|| Objects.equals(Interest, lg.getStatus()))
			return false;
		List<SysBalTrans> filList = check.getTransOutAll().stream()
				.filter(p -> !p.ackFr() && Objects.equals(p.getTaskId(), lg.getTaskId())
						&& Objects.equals(p.getTaskType(), lg.getTaskType()) && p.getSys() != SysBalTrans.SYS_REFUND)
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(filList)) {
			filList = check.getTransOutAll().stream()
					.filter(p -> SysBalTrans.SYS_REFUND != p.getSys() && p.getBankLgId() == 0 && p.getSysLgId() != 0
							&& p.ackFr() && Objects.equals(p.getTaskId(), lg.getTaskId())
							&& Objects.equals(p.getTaskType(), lg.getTaskType()))
					.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
					.collect(Collectors.toList());
			if (CollectionUtils.isEmpty(filList)) {
				return false;
			}
			SysBalTrans ts = filList.get(0);
			BizSysLog sys = storeHandler.findSysOne(check.getTarget(), ts.getSysLgId());
			if (Objects.isNull(sys)) {
				return false;
			}
			// 反写：银行流水ID
			if (Objects.isNull(sys.getBankLogId()) || sys.getBankLogId() == 0) {
				// 设置系统账目银行流水ID
				sys.setBankLogId(lg.getId());
				storeHandler.saveAndFlush(sys);
				// 设置转账任务银行流水ID
				ts.setBankLgId(lg.getId());
				String k = handler.deStruct(template, ts, null, ACK_FR);
				logger.info("SB{} SB{} [ FLOW MATCHEDOUTWARD AFTER CONFIRMED ] >> amount: {} flowId: {}  msg: {}",
						ts.getFrId(), ts.getToId(), ts.getAmt(), lg.getId(), k);
			}
			return true;
		}
		SysBalTrans ts = filList.get(0);
		ts.setAckByCurrFlow(SysBalTrans.ACK_ACK);
		ts.setSys(SysBalTrans.SYS_SUB);
		ts.setAckTm(System.currentTimeMillis());
		BigDecimal[] bs = storeHandler.setSysBal(template, ts.getFrId(), ts.getAmt(), fee, true);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, ts);
		long[] sg = storeHandler.transFrom(ts, fee, lg, bs);
		ts.setSysLgId(sg[0]);
		ts.setOppSysLgId(sg[1]);
		ts.setBankLgId(lg.getId());
		String k = handler.deStruct(template, ts, bs, ACK_FR);
		logger.info("SB{} [ FLOW MATCHEDOUTWARD CONFIRMED ] >> amount: {} orderId: {} orderNo: {} flowId: {}  msg: {}",
				ts.getFrId(), ts.getAmt(), lg.getTaskId(), lg.getOrderNo(), lg.getId(), k);
		return true;
	}
}
