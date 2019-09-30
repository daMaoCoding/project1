package com.xinbo.fundstransfer.report.streamalarm.classify;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.streamalarm.ActionStreamAlarm;
import com.xinbo.fundstransfer.report.streamalarm.StreamAlarmAnnotation;
import com.xinbo.fundstransfer.report.streamalarm.StreamAlarmHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportParamStreamAlarm;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

/**
 * 流水告警处理：其他
 */
@StreamAlarmAnnotation(ActionStreamAlarm.PREFIX_ACTION_STREAM_ALARM
		+ ActionStreamAlarm.ACTION_STREAM_ALARM_TYPE_Disposed)
public class ActionStreamAlarmDisposed extends ActionStreamAlarm {
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, StreamAlarmHandler handler,
			ReportParamStreamAlarm param, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(handler) || Objects.isNull(param)
				|| Objects.isNull(check))
			return false;
		return backCashPledge(template, base, handler, param, check)
				|| duplicateStatement(template, base, handler, param, check);
	}

	/**
	 * 兼职退押金处理
	 */
	private boolean backCashPledge(StringRedisTemplate template, AccountBaseInfo base, StreamAlarmHandler handler,
			ReportParamStreamAlarm param, ReportCheck check) {
		String remark = StringUtils.trimToEmpty(param.getRemark());
		if (StringUtils.isBlank(remark) || !remark.contains("退押金"))
			return false;
		return false;
	}

	/**
	 * 重复流水处理
	 */
	private boolean duplicateStatement(StringRedisTemplate template, AccountBaseInfo base, StreamAlarmHandler handler,
			ReportParamStreamAlarm param, ReportCheck check) {
		String remark = StringUtils.trimToEmpty(param.getRemark());
		if (StringUtils.isBlank(remark) || !remark.contains("重复流水"))
			return false;
		return false;
	}
}
