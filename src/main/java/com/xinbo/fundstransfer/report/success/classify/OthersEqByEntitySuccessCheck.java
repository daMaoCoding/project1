package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.FeeUtil;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 实体上报 EQ 确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_EQBYENTITY)
public class OthersEqByEntitySuccessCheck extends SuccessCheck {
	protected static final Logger logger = LoggerFactory.getLogger(OthersEqByEntitySuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getEntity()) || Objects.isNull(param.getBenchmark()))
			return false;
		TransferEntity entity = param.getEntity();
		long TASK_ID = SysBalUtils.taskId(entity);
		BigDecimal TR_AMT = SysBalUtils.transAmt(entity);
		BigDecimal BE_BAL = SysBalUtils.beforeBal(entity), AF_BAL = SysBalUtils.afterBal(entity);
		int FR_ID = SysBalUtils.frId(entity), TO_ID = SysBalUtils.toId(entity);
		String FR_ACC_3 = SysBalUtils.last3letters(base.getAccount());
		String TO_ACC_3 = SysBalUtils.last3letters(entity.getAccount());
		return eq(handler, template, base, BE_BAL, AF_BAL, TR_AMT, TASK_ID, FR_ID, TO_ID, FR_ACC_3, TO_ACC_3,
				param.getBenchmark(), check, entity);
	}

	/**
	 * 1.转账后余额= 转账前余额- 转账金额 or 2.转账后余额 ~ 转账前余额- 转账金额- 转账预计手续费
	 */
	private boolean eq(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base, BigDecimal BE_BAL,
			BigDecimal AF_BAL, BigDecimal TR_AMT, long TASK_ID, int FR_ID, int TO_ID, String FR_ACC_3, String TO_ACC_3,
			BigDecimal BENCHMARK, ReportCheck check, TransferEntity entity) {
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_ENTITY) && !p.ackFr()
						&& p.getToId() == TO_ID && Objects.equals(p.getToAcc3Last(), TO_ACC_3)
						&& TR_AMT.compareTo(p.getAmt()) == 0 && TASK_ID == p.getTaskId())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		// 没有找到相对应的转账任务：直接返回
		if (CollectionUtils.isEmpty(dataList))
			return false;
		SysBalTrans outts = dataList.get(0);
		outts.setBefore(BE_BAL);
		outts.setAfter(AF_BAL);
		outts.setAckTm(System.currentTimeMillis());
		outts.setReslt(entity.getResult());
		if (BE_BAL.subtract(TR_AMT).compareTo(AF_BAL) == 0
				|| BE_BAL.subtract(TR_AMT).subtract(FeeUtil.fee(base.getBankType(), TR_AMT)).subtract(AF_BAL).abs()
						.intValue() < Report.FEE_TOLERANCE
						&& SysBalUtils.fee050(BE_BAL.subtract(TR_AMT).subtract(AF_BAL).negate())) {
			BigDecimal fee = BE_BAL.subtract(TR_AMT).subtract(AF_BAL);
			outts.setSys(SysBalTrans.SYS_SUB);
			outts.setAckByCurrTrans(SysBalTrans.ACK_ACK);
			BigDecimal[] bs = storeHandler.setSysBal(template, outts.getFrId(), outts.getAmt(), fee, true);
			bs[0] = AF_BAL;
			check.init(bs, outts);
			long[] sg = storeHandler.transFrom(outts, fee, null, bs);
			outts.setSysLgId(sg[0]);
			outts.setOppSysLgId(sg[1]);
			String k = handler.deStruct(template, outts, bs, ACK_FR);
			logger.info(
					"SB{} SB{} [ TRANSFER ENTITY EQ CONFIRMED ] >> before: {}  after: {}  amount: {} benchmark: {} msg: {}",
					FR_ID, TO_ID, BE_BAL, AF_BAL, TR_AMT, BENCHMARK, k);
			return true;
		}
		String k = handler.reWriteMsg(template, outts, SysBalUtils.expireAck(outts.getGetTm()), TimeUnit.MINUTES);
		logger.info(
				"SB{} SB{} [ TRANSFER ENTITY EQ NON-CONFIRM ] >> before: {}  after: {}  amount: {} benchmark: {} msg: {}",
				FR_ID, TO_ID, BE_BAL, AF_BAL, TR_AMT, BENCHMARK, k);
		return false;
	}
}
