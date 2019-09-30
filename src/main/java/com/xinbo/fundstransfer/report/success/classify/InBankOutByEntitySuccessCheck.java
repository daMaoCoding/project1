package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.FeeUtil;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
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
 * 入款卡实体转出确认
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_OUT_BY_ENTITY)
public class InBankOutByEntitySuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(InBankOutByEntitySuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getEntity())
				|| Objects.isNull(check))
			return false;
		return out(handler, template, base, param.getEntity(), check);
	}

	private boolean out(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base,
			TransferEntity entity, ReportCheck check) {
		BigDecimal BE_BAL = SysBalUtils.beforeBal(entity), AF_BAL = SysBalUtils.afterBal(entity);
		// 有转账前，转账后余额处理
		if (BE_BAL.compareTo(BigDecimal.ZERO) == 0 && AF_BAL.compareTo(BigDecimal.ZERO) == 0)
			return false;
		// String FR_ACC_3 = SysBalUtils.last3letters(base.getAccount());
		String TO_ACC_3 = SysBalUtils.last3letters(entity.getAccount());
		int FR_ID = SysBalUtils.frId(entity), TO_ID = SysBalUtils.toId(entity);
		BigDecimal TR_AMT = SysBalUtils.transAmt(entity);
		long TASK_ID = SysBalUtils.taskId(entity);
		// 转账前无操作:
		// 转账过程中无转入金额，接转账成功
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.filter(p -> SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_ENTITY) && !p.ackFr()
						&& p.getToId() == TO_ID && Objects.equals(p.getToAcc3Last(), TO_ACC_3)
						&& TR_AMT.compareTo(p.getAmt()) == 0 && TASK_ID == p.getTaskId())
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return false;
		SysBalTrans curr = dataList.get(0);
		// 入款卡 转出挂起标识符,转账任务中,ackTm!=0 且 ackFr()==false
		// 转账实体一上报则，挂起该转出任务，无论 ( 转账前余额 - 转账后余额 = 转账金额)
		// 是否成立,该转账任务的确认操作，只发生在流水上报中，
		// 如果：流水上报后, 需要检测该任务，是否已挂起，如果已挂起，且无转账流水匹配，则任务 该转账任务失败。
		curr.setAckTm(System.currentTimeMillis());
		curr.setBefore(BE_BAL);
		curr.setAfter(AF_BAL);
		curr.setReslt(entity.getResult());
		BizAccount acc = accSer.getById(base.getId());
		// 入款卡实体上报：余额确认 条件
		// 1.转账前余额等于系统余额
		// 2.转账前余额 - 转账金额 = 转账后余额
		// 3.转账前余额 - 转账金额 - 手续费 = 转账后余额
		if (Objects.nonNull(acc) && Objects.nonNull(acc.getBalance()) && acc.getBalance().compareTo(BE_BAL) == 0
				&& (BE_BAL.subtract(TR_AMT).compareTo(AF_BAL) == 0
						|| BE_BAL.subtract(TR_AMT).subtract(FeeUtil.fee(base.getBankType(), TR_AMT)).subtract(AF_BAL)
								.abs().intValue() < Report.FEE_TOLERANCE
								&& SysBalUtils.fee050(BE_BAL.subtract(TR_AMT).subtract(AF_BAL).negate()))) {
			BigDecimal fee = BE_BAL.subtract(TR_AMT).subtract(AF_BAL);
			curr.setSys(SysBalTrans.SYS_SUB);
			curr.setAckByCurrTrans(SysBalTrans.ACK_ACK);
			BigDecimal[] bs = storeHandler.setSysBal(template, curr.getFrId(), curr.getAmt(), fee, true);
			bs[0] = AF_BAL;
			check.init(bs, curr);
			long[] sg = storeHandler.transFrom(curr, fee, null, bs);
			curr.setSysLgId(sg[0]);
			curr.setOppSysLgId(sg[1]);
			String k = handler.deStruct(template, curr, bs, ACK_FR);
			logger.info(
					"SB{} SB{} [ TRANSFER ENTITY INBANK EQ CONFIRMED ] >> before: {}  after: {}  amount: {} msg: {}",
					FR_ID, TO_ID, BE_BAL, AF_BAL, TR_AMT, k);
			return true;
		}
		String msg = handler.reWriteMsg(template, curr, SysBalUtils.expireAck(curr.getGetTm()), TimeUnit.MINUTES);
		logger.info("SB{} SB{} [ TRANSFER ENTITY INBANK SUSPEND ] >> before: {} after: {}  amount: {} msg: {}",
				curr.getFrId(), curr.getToId(), curr.getBefore(), curr.getAfter(), curr.getAmt(), msg);
		return false;
	}
}
