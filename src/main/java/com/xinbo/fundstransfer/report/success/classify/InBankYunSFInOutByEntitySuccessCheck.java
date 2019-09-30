package com.xinbo.fundstransfer.report.success.classify;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportYSF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.springframework.util.CollectionUtils;

/**
 * 入款卡实体转出确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_YunSFInOutByEntity)
public class InBankYunSFInOutByEntitySuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(InBankYunSFInOutByEntitySuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getEntity())
				|| Objects.isNull(check) || !SystemAccountUtils.ysf(base))
			return false;
		TransferEntity entity = param.getEntity();
		BigDecimal BE_BAL = SysBalUtils.beforeBal(entity), AF_BAL = SysBalUtils.afterBal(entity),
				AMT = SysBalUtils.transAmt(entity);
		if (BigDecimal.ZERO.compareTo(AMT) == 0 || BE_BAL.subtract(AMT).compareTo(AF_BAL) != 0)
			return false;
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(base.getId());
		Optional<BizSysLog> optional = sysList.stream().findFirst();
		if (!optional.isPresent())
			return false;
		BizSysLog first = optional.get();
		if (Objects.isNull(first) || !Objects.equals(first.getStatus(), SysLogStatus.Valid.getStatusId()))
			return false;
		BigDecimal firstSysBal = SysBalUtils.radix2(first.getBalance());
		BigDecimal firstBankBal = SysBalUtils.radix2(first.getBankBalance());
		if (firstSysBal.compareTo(firstBankBal) != 0)
			return false;
		BigDecimal diff = BE_BAL.subtract(firstSysBal);
		if (diff.compareTo(BigDecimal.ZERO) <= 0)
			return false;
		BizAccount acc = accSer.getById(base.getId());
		if (Objects.isNull(acc) || SysBalUtils.radix2(acc.getBalance()).compareTo(firstSysBal) != 0)
			return false;
		String TO_ACC_3 = SysBalUtils.last3letters(entity.getAccount());
		int FR_ID = SysBalUtils.frId(entity), TO_ID = SysBalUtils.toId(entity);
		BigDecimal TR_AMT = SysBalUtils.transAmt(entity);
		long TASK_ID = SysBalUtils.taskId(entity);
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_ENTITY) && !p.ackFr()
						&& p.getToId() == TO_ID && Objects.equals(p.getToAcc3Last(), TO_ACC_3)
						&& TR_AMT.compareTo(p.getAmt()) == 0 && TASK_ID == p.getTaskId())
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return false;
		SysBalTrans curr = dataList.get(0);
		List<Long> hisBankLogIdList = sysList.stream().filter(p -> Objects.nonNull(p.getBankLogId()))
				.map(BizSysLog::getBankLogId).collect(Collectors.toList());
		List<ReportYSF> ysfList = check.getYSF(template, check.getBase()).stream()
				.filter(p -> !hisBankLogIdList.contains(p.getFlogId()))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getFlogId() - o1.getFlogId()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(ysfList))
			return false;
		List<ReportYSF> combination = SystemAccountUtils.combinate(diff, ysfList);
		if (CollectionUtils.isEmpty(combination))
			return false;
		for (ReportYSF ysf : combination) {// 入款流水正常保存
			BizBankLog lg = bankLog(ysf);
			BigDecimal[] bs = storeHandler.setSysBal(template, base.getId(), lg.getAmount(), null, false);
			bs[0] = bs[1];
			long[] sg = storeHandler.transInBank(lg.getFromAccount(), lg, bs);
			check.init(bs, null);
			check.movYSF(template, lg);
			logger.info(
					"SB{} [ INBANK YUNSF INOUT INCOME  ] >>  bank: {} sys: {} amt: {} sysId: {}  oppAccount: {} oppOwner: {}",
					lg.getFromAccount(), bs[0], bs[1], lg.getAmount(), sg[1], lg.getToAccount(),
					lg.getToAccountOwner());
		}
		curr.setSys(SysBalTrans.SYS_SUB);
		curr.setAckByCurrTrans(SysBalTrans.ACK_ACK);
		BigDecimal[] bs = storeHandler.setSysBal(template, curr.getFrId(), curr.getAmt(), null, true);
		bs[0] = AF_BAL;
		check.init(bs, curr);
		long[] sg = storeHandler.transFrom(curr, null, null, bs);
		curr.setSysLgId(sg[0]);
		curr.setOppSysLgId(sg[1]);
		String k = handler.deStruct(template, curr, bs, ACK_FR);
		logger.info("SB{} SB{} [ INBANK YUNSF INOUT OUT  CONFIRMED ] >> before: {}  after: {}  amount: {} msg: {}",
				FR_ID, TO_ID, BE_BAL, AF_BAL, TR_AMT, k);
		return true;
	}
}
