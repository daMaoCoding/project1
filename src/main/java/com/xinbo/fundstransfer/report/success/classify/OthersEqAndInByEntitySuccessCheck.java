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
import java.util.stream.Collectors;

/**
 * 实体上报 EQ AND IN 确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_EQANDINBYENTITY)
public class OthersEqAndInByEntitySuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(OthersEqAndInByEntitySuccessCheck.class);

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
		return eqAndIn(handler, template, base, BE_BAL, AF_BAL, TR_AMT, TASK_ID, FR_ID, TO_ID, FR_ACC_3, TO_ACC_3,
				param.getBenchmark(), check);
	}

	/**
	 * 转账过程有转入，且与转入金额，转出金额全都匹配上
	 */
	private boolean eqAndIn(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base,
			BigDecimal BE_BAL, BigDecimal AF_BAL, BigDecimal TR_AMT, long TASK_ID, int FR_ID, int TO_ID,
			String FR_ACC_3, String TO_ACC_3, BigDecimal BENCHMARK, ReportCheck check) {
		BigDecimal fee_ = FeeUtil.fee(base.getBankType(), TR_AMT);
		List<SysBalTrans> oppList = check.getTransInAll().stream()
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_OPPBAL) && !p.ackTo()
						&& BE_BAL.subtract(TR_AMT).subtract(fee_).add(p.getAmt()).subtract(AF_BAL).abs()
								.intValue() < Report.FEE_TOLERANCE
						&& SysBalUtils.fee050(BE_BAL.subtract(TR_AMT).add(p.getAmt()).subtract(AF_BAL).negate()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(oppList))
			return false;
		// 转入金额
		SysBalTrans ints = oppList.get(0);
		// 转出金额
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_ENTITY) && !p.ackFr()
						&& p.getToId() == TO_ID && Objects.equals(p.getToAcc3Last(), TO_ACC_3)
						&& TR_AMT.compareTo(p.getAmt()) == 0 && TASK_ID == p.getTaskId())
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return false;
		SysBalTrans outts = dataList.get(0);
		BigDecimal fee;
		// 转出确认
		{
			fee = BE_BAL.subtract(TR_AMT).subtract(AF_BAL.subtract(ints.getAmt()));
			outts.setAckTm(System.currentTimeMillis());
			outts.setAckByCurrTrans(SysBalTrans.ACK_ACK);
			outts.setSys(SysBalTrans.SYS_SUB);
			BigDecimal[] bs = storeHandler.setSysBal(template, outts.getFrId(), outts.getAmt(), fee, true);
			bs[0] = AF_BAL.subtract(ints.getAmt());
			check.init(bs, outts);
			long[] sg = storeHandler.transFrom(outts, fee, null, bs);
			outts.setSysLgId(sg[0]);
			outts.setOppSysLgId(sg[1]);
			String k = handler.deStruct(template, outts, bs, ACK_FR);
			logger.info(
					"SB{} SB{} [ TRANSFER ENTITY EQ&IN CONFIRMED ] >> before: {}  after: {}  amount: {} benchmark: {} msg: {}",
					FR_ID, TO_ID, BE_BAL, AF_BAL, TR_AMT, BENCHMARK, k);
		}
		// 转入确认
		{
			ints.setAckTm(System.currentTimeMillis());
			ints.setAckByOppBal(SysBalTrans.ACK_ACK);
			ints.setSys(SysBalTrans.SYS_SUB);
			BigDecimal[] bs = storeHandler.setSysBal(template, ints.getToId(), ints.getAmt(), null, false);
			bs[0] = AF_BAL;
			check.init(bs, ints);
			long[] sg = storeHandler.transTo(ints, null, bs);
			ints.setSysLgId(sg[0]);
			ints.setOppSysLgId(sg[1]);
			String k = handler.deStruct(template, ints, bs, ACK_TO);
			logger.info(
					"SB{} SB{} [ TRANSFER ENTITY EQ&IN INCOME CONFIRMED ] >> amtin: {} amtout: {} feeout: {} beout: {} afout: {} benchmark: {} msg: {}",
					ints.getFrId(), ints.getToId(), ints.getAmt(), outts.getAmt(), fee, BE_BAL, AF_BAL, BENCHMARK, k);
		}
		return true;
	}
}
