package com.xinbo.fundstransfer.report.up.classify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportInitParam;
import com.xinbo.fundstransfer.report.up.ReportUp;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 入款卡初始化
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_INBANK + SysBalPush.CLASSIFY_INIT)
public class InBankInitReport extends Report {

	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		SysBalPush<ReportInitParam> data = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<ReportInitParam>>() {
				});
		if (Objects.isNull(data) || Objects.isNull(data.getData()) || Objects.isNull(data.getData().getOperator()))
			return;
		SysUser operator = userSer.findFromCacheById(data.getData().getOperator());
		initHandler.initDynamicIndividual(check.getTarget(), operator, data.getData().getRemark());
	}
}
