package com.xinbo.fundstransfer.report.up.classify;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportUp;

/**
 * 上报处理： 其他卡：余额上报
 * ---入款流水（与出款流水类似），出款流水：当转账实体没有转账后余额的情况，收到余额以后，如果（与上次余额比）少出的部分等于转账金额，则写系统流水，并计算系统余额，将该余额记为银行余额。
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_OTHERS + SysBalPush.CLASSIFY_BANK_BAL)
public class OthersBalReport extends Report {
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
		log.info("SB{} [ OTHERS BANK BALANCE REPORT ] >> benchmark: {}  balance: {}", base.getId(), benchmark, entity);
		if (!(Objects.equals(OutBank, base.getType()) && Objects.nonNull(base.getHolder()))) {
			if (benchmark.compareTo(entity) != 0) {
				if (!successHandler.othersOutByBal(template, base, benchmark, entity, check))
					if (!in(template, base, benchmark, entity, check))
						successHandler.othersInOutByBal(template, base, benchmark, entity, check);
			}
		} else {// 人工出款按照余额确认
			if (successHandler.othersInByBal_(template, base, benchmark, entity, check))
				successHandler.othersOutByBal_(template, base, benchmark, entity, check);
		}
		benchmark(template, base.getId(), entity);
		lasttime(template, base.getId());
		BizAccount acc = accSer.getById(base.getId());
		if (Objects.nonNull(acc)) {
			failHandler.record(base, entity, acc.getBalance(), data.getCurrTm(), check);
			sendCapture(template, base, acc.getBalance(), entity);
		}
	}

	private boolean in(StringRedisTemplate template, AccountBaseInfo base, BigDecimal benchmark, BigDecimal realBal,
			ReportCheck check) {
		return successHandler.othersInByBal(template, base, benchmark, realBal, check);
	}
}