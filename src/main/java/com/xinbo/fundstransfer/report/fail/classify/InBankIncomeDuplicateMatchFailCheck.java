package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.fail.*;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_IN_BANK_INCOME_DUPLICATE_MATCHED)
public class InBankIncomeDuplicateMatchFailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(InBankIncomeDuplicateMatchFailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(handler) || Objects.isNull(base) || Objects.isNull(check)
				|| Objects.isNull(param) || Objects.isNull(param.getBankLog()))
			return false;
		if (!Objects.equals(Inbank, base.getType()))
			return false;
		BizBankLog lg = param.getBankLog();
		Long taskId = lg.getTaskId();
		BigDecimal AMT = SysBalUtils.radix2(lg.getAmount());
		if (AMT.compareTo(BigDecimal.ZERO) < 0 || Objects.isNull(taskId) || taskId == 0)
			return false;
		List<BizSysLog> sysLogList = storeHandler.findSysLogFromCache(base.getId());
		if (CollectionUtils.isEmpty(sysLogList))
			return false;
		List<BizSysLog> targetList = sysLogList.stream().filter(p -> Objects.equals(taskId, p.getOrderId()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(targetList))
			return false;
		BizSysLog sys = targetList.get(0);
		BizSysLog cloned = sys.clone();
		sys.setSummary("[重复确认]" + StringUtils.trimToEmpty(sys.getRemark()));
		List<BizSysLog> newList = new ArrayList<>();
		newList.add(sys);
		List<BizSysLog> hisList = new ArrayList<>();
		hisList.add(cloned);
		storeHandler.updateByBatch(base, BigDecimal.ZERO, newList, hisList);
		return true;
	}
}
