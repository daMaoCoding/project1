package com.xinbo.fundstransfer.report.streamalarm.classify;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.streamalarm.ActionStreamAlarm;
import com.xinbo.fundstransfer.report.streamalarm.StreamAlarmAnnotation;
import com.xinbo.fundstransfer.report.streamalarm.StreamAlarmHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportParamStreamAlarm;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 流水告警处理：补单
 */
@StreamAlarmAnnotation(ActionStreamAlarm.PREFIX_ACTION_STREAM_ALARM
		+ ActionStreamAlarm.ACTION_STREAM_ALARM_TYPE_MakeUpOrder)
public class ActionStreamAlarmMakeUpOrder extends ActionStreamAlarm {
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, StreamAlarmHandler handler,
			ReportParamStreamAlarm param, ReportCheck check) {
		return false;
	}
}
