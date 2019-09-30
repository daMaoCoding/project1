package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
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

@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_IN_BANK_INCOME_TEST)
public class InBankIncomeTestFailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(InBankIncomeTestFailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(handler) || Objects.isNull(base) || Objects.isNull(check)
				|| Objects.isNull(param) || Objects.isNull(param.getBankLog()))
			return false;
		if (!Objects.equals(Inbank, base.getType()))
			return false;
		BizBankLog lg = param.getBankLog();
		BigDecimal AMT = SysBalUtils.radix2(lg.getAmount());
		if (AMT.compareTo(BigDecimal.ZERO) >= 0 || AMT.abs().compareTo(new BigDecimal("20.00")) > 0)
			return false;
		List<BizSysLog> sysLogList = storeHandler.findSysLogFromCache(base.getId());
		if (CollectionUtils.isEmpty(sysLogList))
			return false;
		BizSysLog first = sysLogList.get(0);
		if (Objects.isNull(first) || Objects.isNull(first.getAmount())
				|| AMT.add(first.getAmount()).compareTo(BigDecimal.ZERO) != 0
				|| StringUtils.isNotBlank(first.getOrderNo())
				|| (Objects.nonNull(first.getOrderId()) && first.getOrderId() != 0))
			return false;
		// String TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		// String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount());
		// boolean radix = SysBalUtils.radix(AMT);
		// if (radix || Objects.equals(TO_OWN_2,
		// SysBalUtils.last2letters(first.getOppOwner()))
		// || Objects.equals(TO_ACC_3,
		// SysBalUtils.last3letters(first.getOppAccount()))) {
		BizSysLog cloned = first.clone();
		first.setStatus(SysLogStatus.Invalid.getStatusId());
		List<BizSysLog> newList = new ArrayList<>();
		newList.add(first);
		first.setSummary("[测试]" + StringUtils.trimToEmpty(first.getSummary()));
		List<BizSysLog> hisList = new ArrayList<>();
		hisList.add(cloned);
		storeHandler.updateByBatch(base, AMT, newList, hisList);
		return true;
		// }
		// return false;
	}
}
