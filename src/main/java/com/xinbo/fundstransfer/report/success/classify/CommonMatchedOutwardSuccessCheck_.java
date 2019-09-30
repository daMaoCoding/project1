package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
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

/**
 * 出款/返利 流水确认数据库订单
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_MATCHEDOUTWARD_)
public class CommonMatchedOutwardSuccessCheck_ extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(CommonMatchedOutwardSuccessCheck_.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBankLog())
				|| Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return matchedOutward_(template, lg, param.getFee(), check);
	}

	protected boolean matchedOutward_(StringRedisTemplate template, BizBankLog lg, BigDecimal fee, ReportCheck check) {
		if (Objects.isNull(lg.getTaskType())
				|| (SysBalTrans.TASK_TYPE_OUTMEMEBER != lg.getTaskType()
						&& SysBalTrans.TASK_TYPE_OUTREBATE != lg.getTaskType())
				|| Objects.isNull(lg.getTaskId()) || Objects.equals(Refunded, lg.getStatus())
				|| Objects.equals(Refunding, lg.getStatus()) || Objects.equals(Fee, lg.getStatus())
				|| Objects.equals(Interest, lg.getStatus()))
			return false;
		if (check.getTransOutAll().stream().filter(
				p -> Objects.equals(p.getTaskId(), lg.getTaskId()) && Objects.equals(p.getTaskType(), lg.getTaskType()))
				.count() > 0)
			return true;
		List<BizSysLog> his = storeHandler.findSysAll(lg.getFromAccount(),
				SysBalTrans.TASK_TYPE_OUTMEMEBER == lg.getTaskType() ? SysLogType.Outward.getTypeId()
						: SysLogType.Rebate.getTypeId(),
				lg.getTaskId());
		if (!CollectionUtils.isEmpty(his))
			return true;
		BigDecimal amt = SysBalUtils.radix2(lg.getAmount()).abs();
		BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), amt, fee, true);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, null);
		SysBalTrans ts = new SysBalTrans();
		ts.setFrId(lg.getFromAccount());
		ts.setBankLgId(lg.getId());
		ts.setTaskType(lg.getTaskType());
		ts.setTaskId(lg.getTaskId());
		ts.setSys(SysBalTrans.SYS_SUB);
		ts.setAckByCurrFlow(SysBalTrans.ACK_ACK);
		ts.setAmt(amt);
		ts.setAckTm(System.currentTimeMillis());
		long[] sg = storeHandler.transFrom(ts, fee, lg, bs);
		logger.info(
				"SB{} [ FLOW MATCHEDOUTWARD_ CONFIRMED ] >> amount: {} orderId: {} orderNo: {} flowId: {}  sysLogId: {}",
				ts.getFrId(), ts.getAmt(), lg.getTaskId(), lg.getOrderNo(), lg.getId(), sg[0]);
		return true;
	}
}
