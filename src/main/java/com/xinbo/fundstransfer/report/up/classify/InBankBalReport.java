package com.xinbo.fundstransfer.report.up.classify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportUp;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 上报处理： 入款卡：余额上报
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_INBANK + SysBalPush.CLASSIFY_BANK_BAL)
public class InBankBalReport extends Report {
	private static final Cache<Integer, BigDecimal> lastBalance = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES).build();

	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		SysBalPush<BigDecimal> data = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<BigDecimal>>() {
				});
		if (Objects.isNull(data) || Objects.isNull(data.getData()) || data.getData().compareTo(BigDecimal.ZERO) <= 0)
			return;

		BigDecimal lastBal = lastBalance.getIfPresent(data.getTarget());
		if (lastBal != null && lastBal.equals(data.getData())) {
			return;
		}
		lastBalance.put(data.getTarget(), data.getData());

		AccountBaseInfo base = accSer.getFromCacheById(data.getTarget());
		if (Objects.isNull(base))
			return;
		BigDecimal entity = SysBalUtils.radix2(data.getData());
		BigDecimal benchmark = benchmark(template, base.getId());
		log.info("SB{} [ INBANK BANK BALANCE REPORT ] >> benchmark: {}  balance: {}", base.getId(), benchmark, entity);
		BizAccount acc = accSer.getById(base.getId());
		if (benchmark.compareTo(entity) != 0) {
			if (successHandler.inbankOutByBal(template, base, entity, acc, check))
				successHandler.yunSFIncomeByBalance(template, base, entity, check);
		}
		lasttime(template, base.getId());
		if (Objects.nonNull(acc)) {
			failHandler.record(base, entity, acc.getBalance(), data.getCurrTm(), check);
			sendCapture(template, base, acc.getBalance(), entity);
		}
	}
}