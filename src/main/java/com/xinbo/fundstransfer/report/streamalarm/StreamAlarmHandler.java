package com.xinbo.fundstransfer.report.streamalarm;

import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportParamStreamAlarm;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class StreamAlarmHandler extends ApplicationObjectSupport {
	private static final Map<String, ActionStreamAlarm> dealMap = new LinkedHashMap<>();

	@PostConstruct
	public void init() {
		Map<String, Object> map = super.getApplicationContext().getBeansWithAnnotation(StreamAlarmAnnotation.class);
		map.forEach((k, v) -> dealMap.put(k, (ActionStreamAlarm) v));
	}

	public void deal(StringRedisTemplate template, AccountBaseInfo base, ReportParamStreamAlarm reportParam,
			ReportCheck check) throws Exception {
		if (true)
			return;
		int bankLogStatus = reportParam.getBankLogStatus();
		if (Objects.equals(BankLogStatus.Interest.getStatus(), bankLogStatus)) {// 结息
			dealMap.get(
					ActionStreamAlarm.PREFIX_ACTION_STREAM_ALARM + ActionStreamAlarm.ACTION_STREAM_ALARM_TYPE_Interest)
					.deal(template, base, this, reportParam, check);
		} else if (Objects.equals(BankLogStatus.ExtFunds.getStatus(), bankLogStatus)) {// 外部资金
			dealMap.get(
					ActionStreamAlarm.PREFIX_ACTION_STREAM_ALARM + ActionStreamAlarm.ACTION_STREAM_ALARM_TYPE_ExtFunds)
					.deal(template, base, this, reportParam, check);
		} else if (Objects.equals(BankLogStatus.Disposed.getStatus(), bankLogStatus)) {// 其他
			dealMap.get(
					ActionStreamAlarm.PREFIX_ACTION_STREAM_ALARM + ActionStreamAlarm.ACTION_STREAM_ALARM_TYPE_Disposed)
					.deal(template, base, this, reportParam, check);
		}
	}
}
