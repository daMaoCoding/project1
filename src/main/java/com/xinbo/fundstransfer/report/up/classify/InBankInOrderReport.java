package com.xinbo.fundstransfer.report.up.classify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogMatchWay;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportUp;

/**
 * 上报处理： 入款卡：入款订单号 ---仅更新入款订单号，不做它用
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_INBANK + SysBalPush.CLASSIFY_BANK_LOG_)
public class InBankInOrderReport extends Report {

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
		if (Objects.isNull(base) || !Objects.equals(base.getStatus(), AccountType.InBank.getTypeId()))
			return;
		BigDecimal amt = SysBalUtils.radix2(flow.getAmount());
		List<BizSysLog> sysList = storeHandler.findSysAll(entity.getTarget(), amt, flow.getId());
		if (CollectionUtils.isEmpty(sysList))
			return;
		BizSysLog lg = sysList.get(0);
		lg.setOrderNo(flow.getOrderNo());
		lg.setOrderId(flow.getTaskId());
		storeHandler.saveAndFlush(lg);
		log.info("SB{} ( bb:{} sb:{} ) [ INCOME ORDERNO  ] {}", lg.getAccountId(), lg.getBankBalance(), lg.getBalance(),
				lg.getAmount());
	}
}
