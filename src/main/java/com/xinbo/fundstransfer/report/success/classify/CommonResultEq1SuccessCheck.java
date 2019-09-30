package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
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
 * 转账结果result ==1 ，确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_RESULTEQ1)
public class CommonResultEq1SuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(CommonResultEq1SuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(handler) || Objects.isNull(param)
				|| Objects.isNull(param.getEntity()) || Objects.isNull(check))
			return false;
		TransferEntity entity = param.getEntity();
		int FR_ID = SysBalUtils.frId(entity), TO_ID = SysBalUtils.toId(entity);
		String TO_ACC_3 = SysBalUtils.last3letters(entity.getAccount());
		BigDecimal TR_AMT = SysBalUtils.transAmt(entity);
		long TASK_ID = SysBalUtils.taskId(entity);
		return resultEq1(handler, template, base, param.getEntity(), FR_ID, TO_ID, TO_ACC_3, TR_AMT, TASK_ID, check);

	}

	protected boolean resultEq1(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base,
			TransferEntity entity, int FR_ID, int TO_ID, String TO_ACC_3, BigDecimal TR_AMT, long TASK_ID,
			ReportCheck check) {
		// 如果 result !=1|| 账号用途为PC||账号分类为入款卡 则：直接返回
		if (!Objects.equals(entity.getResult(), 1) || Objects.equals(base.getFlag(), AccountFlag.PC.getTypeId())
				|| Objects.equals(base.getType(), AccountType.InBank.getTypeId()))
			return false;
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.filter(p -> Objects.equals(TO_ACC_3, p.getToAcc3Last()) && TR_AMT.compareTo(p.getAmt()) == 0
						&& TASK_ID == p.getTaskId() && TO_ID == p.getToId()
						&& (!p.ackFr() && SysBalTrans.SYS_REFUND != p.getSys()))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return false;
		SysBalTrans tsout = dataList.get(0);
		BigDecimal BE_BAL = SysBalUtils.beforeBal(entity);
		BigDecimal AF_BAL = SysBalUtils.afterBal(entity);
		if (AF_BAL.compareTo(BE_BAL) == 0 || AF_BAL.compareTo(BigDecimal.ZERO) == 0) {
			AF_BAL = BE_BAL.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BE_BAL.subtract(TR_AMT);
		}
		tsout.setReslt(entity.getResult());
		tsout.setSys(SysBalTrans.SYS_SUB);
		tsout.setAckByCurrResult1(SysBalTrans.ACK_ACK);
		tsout.setAckTm(System.currentTimeMillis());
		BigDecimal[] bs = storeHandler.setSysBal(template, tsout.getFrId(), tsout.getAmt(), null, true);
		bs[0] = AF_BAL;
		check.init(bs, tsout);
		long[] sg = storeHandler.transFrom(tsout, null, null, bs);
		tsout.setSysLgId(sg[0]);
		tsout.setOppSysLgId(sg[1]);
		String k = handler.deStruct(template, tsout, bs, ACK_FR);
		logger.info("SB{} SB{} [ TRANSFER ENTITY RESULT EQ 1 ] >>  after: {}  amount: {} msg: {}", FR_ID, TO_ID, AF_BAL,
				TR_AMT, k);
		return true;
	}
}
