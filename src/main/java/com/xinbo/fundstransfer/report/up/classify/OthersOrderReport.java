package com.xinbo.fundstransfer.report.up.classify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogMatchWay;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportUp;

/**
 * 上报处理： 其他卡：第三方提现 --- 转入流水：根据转账任务（挂起）该系统流水，临时计算系统余额。
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_OTHERS + SysBalPush.CLASSIFY_BANK_LOG_)
public class OthersOrderReport extends Report {

	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		if (Objects.isNull(template) || StringUtils.isBlank(rpushData))
			return;
		SysBalPush<BizBankLog> entity = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<BizBankLog>>() {
				});
		if (Objects.isNull(entity))
			return;
		BizBankLog flow = entity.getData();
		if (Objects.isNull(flow) || Objects.isNull(flow.getAmount()) || flow.getAmount().compareTo(BigDecimal.ZERO) <= 0
				|| flow.getMatchWay() != BankLogMatchWay.OrderFindFlow.getWay() || flow.getTaskId() == null
				|| flow.getTaskType() == null || flow.getOrderNo() == null)
			return;
		AccountBaseInfo base = accSer.getFromCacheById(entity.getTarget());
		if (Objects.isNull(base))
			return;
		log.info(
				"SB{} [ OTHERS ORDER REPORT ] >>  amount: {} toAcc: {} toOwner: {} orderId: {} orderNo: {} summary: {}",
				base.getId(), flow.getAmount(), flow.getToAccount(), flow.getToAccountOwner(), flow.getTaskId(),
				flow.getOrderNo(), flow.getSummary());
		int type = base.getType();
		if (Objects.equals(type, AccountType.BindAli.getTypeId())
				|| Objects.equals(type, AccountType.BindWechat.getTypeId())
				|| Objects.equals(type, AccountType.BindCommon.getTypeId())
				|| Objects.equals(type, AccountType.ThirdCommon.getTypeId())) {
			BigDecimal amt = SysBalUtils.radix2(flow.getAmount());
			List<SysBalTrans> oriList = check.getTransInAll().stream()
					.filter(p -> SysBalTrans.SYS_REFUND != p.getSys() && p.getTaskType() == SysBalTrans.TASK_TYPE_INNER
							&& p.getTaskId() == flow.getTaskId())
					.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
					.collect(Collectors.toList());
			List<SysBalTrans> dataList = oriList.stream().filter(p -> !p.ackTo()).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(dataList)) {
				dataList = oriList.stream().filter(p -> p.getOppBankLgId() == 0 && p.getOppSysLgId() != 0 && p.ackTo())
						.collect(Collectors.toList());
				if (CollectionUtils.isEmpty(dataList)) {
					return;
				}
				SysBalTrans ts = dataList.get(0);
				BizSysLog sys = storeHandler.findSysOne(check.getTarget(), ts.getOppSysLgId());
				if (Objects.isNull(sys)) {
					return;
				}
				// 反写：银行流水ID
				if (Objects.isNull(sys.getBankLogId()) || sys.getBankLogId() == 0) {
					// 设置系统账目银行流水ID
					sys.setBankLogId(flow.getId());
					storeHandler.saveAndFlush(sys);
					// 设置转账任务银行流水ID
					ts.setOppBankLgId(flow.getId());
					String k = successHandler.deStruct(template, ts, null, ACK_FR);
					log.info("SB{} SB{} [ FLOW MATCHEDTH3_ AFTER CONFIRMED ] >> amount: {} flowId: {}  msg: {}",
							ts.getFrId(), ts.getToId(), ts.getAmt(), flow.getId(), k);
				}
				return;
			}
			SysBalTrans tsin = dataList.get(0);
			tsin.setSys(SysBalTrans.SYS_SUB);
			tsin.setAckTm(System.currentTimeMillis());
			tsin.setAckByOppFlow(SysBalTrans.ACK_ACK);
			BigDecimal[] bs = storeHandler.setSysBal(template, flow.getFromAccount(), amt, null, false);
			if (Objects.nonNull(flow.getBalance()) && flow.getBalance().compareTo(BigDecimal.ZERO) != 0)
				bs[0] = flow.getBalance();
			long[] sg = storeHandler.transTh3(tsin, flow, bs);
			tsin.setOppSysLgId(sg[1]);
			tsin.setOppBankLgId(flow.getId());
			String k = successHandler.deStruct(template, tsin, bs, ACK_TO);
			log.info("SB{} [ OTHERS ORDER CONFIRMED ] >>  amount: {} flowId: {} orderId: {} orderNo: {} msg: {}",
					base.getId(), flow.getAmount(), flow.getId(), flow.getTaskId(), flow.getOrderNo(), k);
		}
	}
}
