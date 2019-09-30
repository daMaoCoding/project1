package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
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

@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_MANUAL_OUTWARD)
public class OthersManualOutwardSuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(OthersManualOutwardSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getBankLog()) || Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return manualOutward(handler, template, base, lg, param.getFee(), check);
	}

	private boolean manualOutward(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base,
			BizBankLog lg, BigDecimal fee, ReportCheck check) {
		Integer st = lg.getStatus();
		if (Objects.nonNull(st) && (Refunding == st || Refunded == st || Interest == st || Fee == st))
			return false;
		if (lg.getTaskId() != null && lg.getTaskId() > 0 || lg.getAmount().compareTo(BigDecimal.ZERO) >= 0)
			return false;
		if (!Objects.equals(base.getType(), OutBank)
				|| !Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus()))
			return false;
		if (Objects.isNull(base.getHolder()) || Objects.equals(base.getHolder(), AppConstants.USER_ID_4_ADMIN))
			return false;
		List<BizBankLog> dataList = storeHandler.findBankLogFromCache(base.getId());
		fee = SysBalUtils.radix2(fee);
		if (fee.compareTo(BigDecimal.ZERO) == 0)
			return noneFee(handler, template, lg, dataList, check);
		return withFee(handler, template, base, lg, fee, dataList, check);
	}

	private boolean noneFee(SuccessHandler handler, StringRedisTemplate template, BizBankLog lg,
			List<BizBankLog> dataList, ReportCheck check) {
		int position = dataList.indexOf(lg);
		if (position < 0 || position + 2 > dataList.size() - 1)
			return false;
		BizBankLog l1 = dataList.get(position + 1), l2 = dataList.get(position + 2);
		if (lg.getTradingTime() == null || l2.getTradingTime() == null || !SysBalUtils.refund(lg, l1, null)
				|| !SysBalUtils.refund(l1, l2, null))
			return false;
		if (lg.getTradingTime().getTime() - l2.getTradingTime().getTime() <= 20000)
			return false;
		List<SysBalTrans> sysList = check.getTransOutAll();
		SysBalTrans trans = findSysTrans(lg, sysList);
		trans = Objects.isNull(trans) ? findSysTrans(l2, sysList) : trans;
		if (Objects.isNull(trans) || trans.getRegistWay() != SysBalTrans.REGIST_WAY_MAN)
			return false;
		if (SysBalTrans.SYS_REFUND == trans.getSys() || SysBalTrans.SYS_NONE == trans.getSys()) {
			trans.setAckByCurrFlow(SysBalTrans.ACK_ACK);
			trans.setSys(SysBalTrans.SYS_SUB);
			trans.setAckTm(System.currentTimeMillis());
			BigDecimal[] bs = storeHandler.setSysBal(template, trans.getFrId(), trans.getAmt(), null, true);
			if (lg.getBalance() != null && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
				bs[0] = lg.getBalance();
			check.init(bs, trans);
			long[] sg = storeHandler.transFrom(trans, null, lg, bs);
			trans.setSysLgId(sg[0]);
			trans.setOppSysLgId(sg[1]);
			trans.setBankLgId(lg.getId());
			String k = handler.deStruct(template, trans, bs, ACK_FR);
			logger.info("SB{} SB{} [ FLOW OTHERS MANUAL OUTWARD CONFIRMED ] >> amount: {} flowId: {}  msg: {}",
					trans.getFrId(), trans.getToId(), trans.getAmt(), lg.getId(), k);
		}
		return true;
	}

	private boolean withFee(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			BigDecimal fee, List<BizBankLog> dataList, ReportCheck check) {
		// 携带手续费 场景，暂时不考虑，确实很复杂
		return false;
	}

	private SysBalTrans findSysTrans(BizBankLog lg, List<SysBalTrans> hisList) {
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount()),
				TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		List<SysBalTrans> reList = hisList.stream()
				.filter(p -> TR_AMT.compareTo(p.getAmt()) == 0 && (SysBalUtils.radix(p)
						|| StringUtils.isNotBlank(TO_OWN_2) && Objects.equals(TO_OWN_2, p.getToOwn2Last())
						|| StringUtils.isNotBlank(TO_ACC_3) && Objects.equals(TO_ACC_3, p.getToAcc3Last())))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getAckTm() - o1.getAckTm()))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(reList))
			return reList.get(0);
		if (Objects.nonNull(lg.getTaskId()) && lg.getTaskId() > 0) {
			reList = hisList.stream()
					.filter(p -> Objects.equals(p.getTaskId(), lg.getTaskId())
							&& Objects.equals(p.getTaskType(), lg.getTaskType()))
					.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
					.collect(Collectors.toList());
			return CollectionUtils.isEmpty(reList) ? null : reList.get(0);
		}
		return null;
	}
}
