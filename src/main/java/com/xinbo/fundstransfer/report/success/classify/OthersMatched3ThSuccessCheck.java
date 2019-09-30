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
 * 第三方下发确认（依靠收款账号流水确认Redis中的转账记录 ）</br>
 * 收款账号：下发卡；出款卡
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_MATCHED_3TH)
public class OthersMatched3ThSuccessCheck extends SuccessCheck {

	protected static final Logger logger = LoggerFactory.getLogger(OthersMatched3ThSuccessCheck.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(param) || Objects.isNull(param.getBankLog())
				|| Objects.isNull(check))
			return false;
		BizBankLog lg = param.getBankLog();
		logger.info("SB{} [ DEAL DATA ] bankId : {} amount: {} taskId: {}", lg.getFromAccount(), lg.getId(),
				lg.getAmount(), lg.getTaskId());
		return matched3Th(handler, template, base, lg, check);
	}

	private boolean matched3Th(SuccessHandler handler, StringRedisTemplate template, AccountBaseInfo base,
			BizBankLog lg, ReportCheck check) {
		if (Objects.isNull(lg.getAmount()) || lg.getAmount().compareTo(BigDecimal.ZERO) <= 0)
			return false;
		int type = base.getType();
		// 第三方可以下发给：下发卡,出款卡
		if (type != ThirdCommon && type != BindCommon && type != BindAli && type != BindWechat && type != OutBank)
			return false;
		if (Objects.isNull(lg.getTaskType()) || SysBalTrans.TASK_TYPE_INNER != lg.getTaskType()
				|| Objects.isNull(lg.getTaskId()) || Objects.equals(Refunded, lg.getStatus())
				|| Objects.equals(Refunding, lg.getStatus()) || Objects.equals(Fee, lg.getStatus())
				|| Objects.equals(Interest, lg.getStatus()))
			return false;
		BigDecimal amt = SysBalUtils.radix2(lg.getAmount());
		List<SysBalTrans> oriList = check.getTransInAll().stream()
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.filter(p -> SysBalTrans.SYS_REFUND != p.getSys() && p.getTaskType() == SysBalTrans.TASK_TYPE_INNER
						&& p.getTaskId() == lg.getTaskId())
				.collect(Collectors.toList());
		List<SysBalTrans> dataList = oriList.stream().filter(p -> !p.ackTo()).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList)) {
			dataList = oriList.stream().filter(p -> p.getOppBankLgId() == 0 && p.getOppSysLgId() != 0 && p.ackTo())
					.collect(Collectors.toList());
			if (CollectionUtils.isEmpty(dataList)) {
				return false;
			}
			SysBalTrans ts = dataList.get(0);
			BizSysLog sys = storeHandler.findSysOne(check.getTarget(), ts.getOppSysLgId());
			if (Objects.isNull(sys)) {
				return false;
			}
			// 反写：银行流水ID
			if (Objects.isNull(sys.getBankLogId()) || sys.getBankLogId() == 0) {
				// 设置系统账目银行流水ID
				sys.setBankLogId(lg.getId());
				storeHandler.saveAndFlush(sys);
				// 设置转账任务银行流水ID
				ts.setOppBankLgId(lg.getId());
				String k = handler.deStruct(template, ts, null, ACK_FR);
				logger.info("SB{} SB{} [ FLOW MATCHEDTH3 AFTER CONFIRMED ] >> amount: {} flowId: {}  msg: {}",
						ts.getFrId(), ts.getToId(), ts.getAmt(), lg.getId(), k);
			}
			return true;
		}
		SysBalTrans tsin = dataList.get(0);
		tsin.setSys(SysBalTrans.SYS_SUB);
		tsin.setAckTm(System.currentTimeMillis());
		tsin.setAckByOppFlow(SysBalTrans.ACK_ACK);
		BigDecimal[] bs = storeHandler.setSysBal(template, lg.getFromAccount(), amt, null, false);
		if (Objects.nonNull(lg.getBalance()) && lg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			bs[0] = lg.getBalance();
		check.init(bs, tsin);
		long[] sg = storeHandler.transTh3(tsin, lg, bs);
		tsin.setOppSysLgId(sg[1]);
		tsin.setOppBankLgId(lg.getId());
		String k = handler.deStruct(template, tsin, bs, ACK_TO);
		logger.info("SB{} SB{} [ FLOW MATCHED3TH CONFIRMED ] >> amount: {} flowId: {}  msg: {}", tsin.getFrId(),
				tsin.getToId(), tsin.getAmt(), lg.getId(), k);
		return true;
	}
}
