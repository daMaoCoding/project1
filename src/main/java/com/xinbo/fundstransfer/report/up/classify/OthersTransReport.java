package com.xinbo.fundstransfer.report.up.classify;

import java.math.BigDecimal;
import java.util.Objects;

import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportUp;

/**
 * 上报处理： 其他卡：转账结果上报 --A->B
 * B转入流水：根据转账实体来找到转账任务，临时计算系统余额，并挂起；A转出流水：根据转账实体生成出款系统流水和银行余额，并计算系统余额。
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_OTHERS + SysBalPush.CLASSIFY_TRANSFER)
public class OthersTransReport extends Report {

	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		SysBalPush<TransferEntity> data = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<TransferEntity>>() {
				});
		if (Objects.isNull(data) || Objects.isNull(data.getData()))
			return;
		AccountBaseInfo base = accSer.getFromCacheById(data.getTarget());
		if (Objects.isNull(base))
			return;
		TransferEntity entity = data.getData();
		if (Objects.isNull(entity.getAmount()))
			return;
		long TASK_ID = SysBalUtils.taskId(entity);
		BigDecimal TR_AMT = SysBalUtils.transAmt(entity);
		int FR_ID = SysBalUtils.frId(entity), TO_ID = SysBalUtils.toId(entity);
		String FR_ACC_3 = SysBalUtils.last3letters(base.getAccount());
		String TO_ACC_3 = SysBalUtils.last3letters(entity.getAccount());
		String TO_OWN_2 = SysBalUtils.last2letters(entity.getOwner());
		BigDecimal BE_BAL = SysBalUtils.beforeBal(entity), AF_BAL = SysBalUtils.afterBal(entity);
		reRegist(template, entity, TASK_ID, TR_AMT, FR_ID, TO_ID, TO_ACC_3, TO_OWN_2, check);
		log.info("SB{} SB{} [ TRANSFER ENTITY REPORT ] >> before: {}  after: {}  amount: {} 2Acc3: {}", FR_ID, TO_ID,
				BE_BAL, AF_BAL, TR_AMT, TO_ACC_3);
		if (failHandler.invalid(base, entity, check)) {
			log.info("SB{} SB{} [ TRANSFER ENTITY INVALID ] >> before: {}  after: {}  amount: {} 2Acc3: {}", FR_ID,
					TO_ID, BE_BAL, AF_BAL, TR_AMT, TO_ACC_3);
			return;
		}
		// 转账前/转账后余额都为空
		if (BE_BAL.compareTo(BigDecimal.ZERO) == 0 && AF_BAL.compareTo(BigDecimal.ZERO) == 0) {
			log.info(
					"SB{} SB{} [ TRANSFER ENTITY BEFORE AND AFTER IS ZERO ] >> before: {}  after: {}  amount: {} 2Acc3: {}",
					FR_ID, TO_ID, BE_BAL, AF_BAL, TR_AMT, TO_ACC_3);
			return;
		}
		// 银行余额基准
		BigDecimal BENCHMARK = benchmark(template, base.getId());
		if (BENCHMARK.compareTo(BE_BAL) == 0) {
			// 转账之前无转入|转出
			out(template, base, BE_BAL, AF_BAL, TR_AMT, TASK_ID, FR_ID, TO_ID, FR_ACC_3, TO_ACC_3, BENCHMARK, check,
					entity);
		} else {
			// 转账之前有转入（可能）
			in(template, base, BE_BAL, AF_BAL, TR_AMT, TASK_ID, FR_ID, TO_ID, FR_ACC_3, TO_ACC_3, BENCHMARK, check,
					entity);
		}
		benchmark(template, base.getId(), AF_BAL);
	}

	/**
	 * 转账实体上报： 转账之前无转入|转出
	 */
	private boolean out(StringRedisTemplate template, AccountBaseInfo base, BigDecimal BE_BAL, BigDecimal AF_BAL,
			BigDecimal TR_AMT, long TASK_ID, int FR_ID, int TO_ID, String FR_ACC_3, String TO_ACC_3,
			BigDecimal BENCHMARK, ReportCheck check, TransferEntity entity) {
		// 转账过程中无转入金额，接转账成功
		// 1.转账后余额= 转账前余额- 转账金额
		// 2.转账后余额 ~ 转账前余额- 转账金额- 转账预计手续费
		if (successHandler.othersEqByEntity(template, base, BENCHMARK, entity, check))
			return true;
		// 转账过程有转入，且与转入金额，转出金额全都匹配上
		if (successHandler.othersEqAndInByEntity(template, base, BENCHMARK, entity, check))
			return true;
		// 转账实体上报，result ==1
		return successHandler.commonResultEq1(template, base, entity, check);
	}

	/**
	 * 转账之前有转入（可能）
	 */
	private boolean in(StringRedisTemplate template, AccountBaseInfo base, BigDecimal BE_BAL, BigDecimal AF_BAL,
			BigDecimal TR_AMT, long TASK_ID, int FR_ID, int TO_ID, String FR_ACC_3, String TO_ACC_3,
			BigDecimal BENCHMARK, ReportCheck check, TransferEntity entity) {
		// 转账之前有转入
		if (BENCHMARK.compareTo(BE_BAL) < 0) {
			// 转入记录确认
			successHandler.othersInByBal(template, base, BENCHMARK, BE_BAL, check);
			// 转账过程中无转入金额，接转账成功
			// 1.转账后余额= 转账前余额- 转账金额
			// 2.转账后余额 ~ 转账前余额- 转账金额- 转账预计手续费
			if (successHandler.othersEqByEntity(template, base, BENCHMARK, entity, check))
				return true;
			// 转账后入款情况
			if (successHandler.othersEqAndInByEntity(template, base, BENCHMARK, entity, check))
				return true;
			// 转账实体上报，result ==1
			return successHandler.commonResultEq1(template, base, entity, check);
		} else if (BENCHMARK.compareTo(BE_BAL) > 0) {
			// 转账之前可能存在盗刷
			// 转账过程中无转入金额，接转账成功
			// 1.转账后余额= 转账前余额- 转账金额
			// 2.转账后余额 ~ 转账前余额- 转账金额- 转账预计手续费
			if (successHandler.othersEqByEntity(template, base, BENCHMARK, entity, check))
				return true;
			// 转账后入款情况
			if (successHandler.othersEqAndInByEntity(template, base, BENCHMARK, entity, check))
				return true;
			// 转账实体上报，result ==1
			return successHandler.commonResultEq1(template, base, entity, check);
		}
		return false;
	}
}
