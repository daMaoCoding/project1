package com.xinbo.fundstransfer.report.up.classify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportParamStreamAlarm;
import com.xinbo.fundstransfer.report.up.ReportUp;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

/**
 * 入款卡： 流水小铃铛处理
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_INBANK + SysBalPush.CLASSIFY_STREAM_ALARM)
public class InBankStreamAlarmReport extends Report {
	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		SysBalPush<ReportParamStreamAlarm> data = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<ReportParamStreamAlarm>>() {
				});
		if (Objects.nonNull(data) && Objects.nonNull(data.getData()) && Objects.nonNull(check))
			streamAlarmHandler.deal(template, check.getBase(), data.getData(), check);
	}
}
