package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
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
import java.util.stream.Collectors;

/**
 * 依靠收款账号（下发卡；出款卡）流水匹配数据库下发订单</br>
 * 1.内部下发。2.第三方下发
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_MATCHED_DEPOSIT_IN_DB)
public class OthersMatchedDepositInDBSuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(OthersMatchedDepositInDBSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getBankLog()) || Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		if (Objects.isNull(lg.getAmount()) || lg.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			return false;
		if (!Objects.equals(lg.getTaskType(), SysBalTrans.TASK_TYPE_INNER) || lg.getTaskId() == null
				|| lg.getTaskId() == 0)
			return false;
		Integer st = lg.getStatus();
		if (Objects.nonNull(st) && (Refunding == st || Refunded == st || Interest == st || Fee == st))
			return false;
		int TP = base.getType();
		return depositInner(template, base, TP, lg, check) || deposit3Th(template, base, TP, lg, check);
	}

	/**
	 * 内部下发
	 */
	private boolean depositInner(StringRedisTemplate template, AccountBaseInfo base, int TP, BizBankLog lg,
			ReportCheck check) {
		if (BindWechat == TP || BindAli == TP || ThirdCommon == TP || BindCommon == TP)
			return false;
		if (!SysBalUtils.radix(lg.getAmount()))
			return false;
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		// 查看缓存有无订单数据
		List<SysBalTrans> dataList = check.getTransInAll().stream().filter(p -> TR_AMT.compareTo(p.getAmt()) == 0)
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(dataList))
			return false;
		// 查看数据库最近8个小时有无该数据
		if (storeHandler.findSysLogFromCache(base.getId()).stream()
				.filter(p -> Objects.nonNull(p.getAmount()) && p.getAmount().compareTo(TR_AMT) == 0).count() > 0)
			return false;
		BizIncomeRequest req = incomeDao.findById2(lg.getTaskId());
		if (Objects.isNull(req) || req.getFromId() == null || req.getFromId() == 0)
			return false;
		BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), TR_AMT, null, false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, null);
		SysBalTrans ts = new SysBalTrans();
		ts.setFrId(req.getFromId());
		ts.setToId(lg.getFromAccount());
		ts.setOppBankLgId(lg.getId());
		ts.setTaskType(lg.getTaskType());
		ts.setTaskId(lg.getTaskId());
		ts.setSys(SysBalTrans.SYS_SUB);
		ts.setAckByOppFlow(SysBalTrans.ACK_ACK);
		ts.setAmt(TR_AMT);
		ts.setAckTm(System.currentTimeMillis());
		long[] sg = storeHandler.transTo(ts, lg, bs);
		logger.info(
				"SB{} [ FLOW DEPOSIT_INNER CONFIRMED ] >> amount: {} orderId: {} orderNo: {} flowId: {}  sysLogId: {}",
				ts.getFrId(), ts.getAmt(), lg.getTaskId(), lg.getOrderNo(), lg.getId(), sg[0]);
		return true;
	}

	/**
	 * 第三方下发
	 */
	private boolean deposit3Th(StringRedisTemplate template, AccountBaseInfo base, int TP, BizBankLog lg,
			ReportCheck check) {
		if (BindWechat != TP && BindAli != TP && ThirdCommon != TP && BindCommon != TP && OutBank != TP)
			return false;
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		// 查看缓存有无订单数据
		List<SysBalTrans> dataList = check.getTransInAll().stream().filter(
				p -> Objects.equals(p.getTaskType(), lg.getTaskType()) && Objects.equals(p.getTaskId(), lg.getTaskId()))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(dataList))
			return false;
		// 查看数据库有无该数据
		List<BizSysLog> sysHistList = storeHandler.findSysLogFromCache(base.getId());
		if (sysHistList.stream().filter(p -> Objects.equals(p.getType(), SysLogType.Transfer.getTypeId())
				&& Objects.equals(p.getOrderId(), lg.getTaskId())).count() > 0)
			return false;
		BizIncomeRequest req = incomeDao.findById2(lg.getTaskId());
		if (Objects.isNull(req) || req.getFromId() == null || req.getFromId() == 0)
			return false;
		AccountBaseInfo frAcc = accSer.getFromCacheById(req.getFromId());
		// 检测：汇款账号是否是第三方
		if (Objects.isNull(frAcc)
				|| (!Objects.equals(frAcc.getType(), InThird) && !Objects.equals(frAcc.getType(), OutThird)))
			return false;
		// 检测：防止是内部下发任务
		if (sysHistList.stream().filter(p -> Objects.nonNull(p.getAmount()) && p.getAmount().compareTo(TR_AMT) == 0
				&& Objects.equals(req.getFromId(), p.getOppId())).count() > 0)
			return false;
		BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), TR_AMT, null, false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, null);
		SysBalTrans ts = new SysBalTrans();
		ts.setFrId(req.getFromId());
		ts.setToId(lg.getFromAccount());
		ts.setOppBankLgId(lg.getId());
		ts.setTaskType(lg.getTaskType());
		ts.setTaskId(lg.getTaskId());
		ts.setSys(SysBalTrans.SYS_SUB);
		ts.setAckByOppFlow(SysBalTrans.ACK_ACK);
		ts.setAmt(TR_AMT);
		ts.setAckTm(System.currentTimeMillis());
		long[] sg = storeHandler.transTo(ts, lg, bs);
		logger.info(
				"SB{} [ FLOW DEPOSIT_3TH CONFIRMED ] >> amount: {} orderId: {} orderNo: {} flowId: {}  sysLogId: {}",
				ts.getFrId(), ts.getAmt(), lg.getTaskId(), lg.getOrderNo(), lg.getId(), sg[0]);
		return true;
	}
}
