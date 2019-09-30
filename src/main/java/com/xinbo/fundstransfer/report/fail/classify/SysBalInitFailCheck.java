package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.fail.*;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 系统初始化，在失败模块中的处理
 */
@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_INIT_BAL)
public class SysBalInitFailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(SysBalInitFailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(param)
				|| Objects.isNull(param.getBankBal()))
			return false;
		BigDecimal bankBal = param.getBankBal();
		push(base.getId(), EntityRecord.genMsg(Common.WATCHER_4_INIT_BAL, bankBal, bankBal, System.currentTimeMillis()),
				System.currentTimeMillis());
		return true;
	}
}
