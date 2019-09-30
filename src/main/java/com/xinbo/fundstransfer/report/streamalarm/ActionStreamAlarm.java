package com.xinbo.fundstransfer.report.streamalarm;

import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportParamStreamAlarm;
import com.xinbo.fundstransfer.service.BankLogService;
import com.xinbo.fundstransfer.service.SysInvstService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Objects;

public abstract class ActionStreamAlarm {

	/* 处理流水告警 注解前缀 */
	public static final String PREFIX_ACTION_STREAM_ALARM = "ACTION_STREAM_ALARM_";

	/**
	 * 处理流水告警 注解后缀： 人工亏损
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_DeficitManual = "DeficitManual";

	/**
	 * 处理流水告警 注解后缀： 其他亏损
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_DeficitOthers = "DeficitOthers";

	/**
	 * 处理流水告警 注解后缀： 系统亏损
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_DeficitSystem = "DeficitSystem";

	/**
	 * 处理流水告警 注解后缀： 手续费
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_DeficitFee = "DeficitFee";

	/**
	 * 处理流水告警 注解后缀： 结息
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_Interest = "Interest";

	/**
	 * 处理流水告警 注解后缀： 补单
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_MakeUpOrder = "MakeUpOrder";

	/**
	 * 处理流水告警 注解后缀： 匹配
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_Match = "Match";

	/**
	 * 处理流水告警 注解后缀： 其他
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_Disposed = "Disposed";

	/**
	 * 处理流水告警 注解后缀： 其他
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_ReduceCredit = "ReduceCredit";
	/**
	 * 处理流水告警 注解后缀： 回冲
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_Refund = "Refund";
	/**
	 * 处理流水告警 注解后缀： 外部资金
	 */
	public static final String ACTION_STREAM_ALARM_TYPE_ExtFunds = "ExtFunds";

	@Autowired
	protected BankLogService bankLogService;
	@Autowired
	protected StoreHandler storeHandler;
	@Autowired
	protected SysInvstService sysInvstSer;

	protected abstract boolean deal(StringRedisTemplate template, AccountBaseInfo base, StreamAlarmHandler handler,
			ReportParamStreamAlarm param, ReportCheck check);

	protected boolean checkBankLog(Integer accId, Long bankLogId) {
		return Objects.nonNull(accId) && Objects.nonNull(bankLogId)
				&& CollectionUtils.isEmpty(sysInvstSer.findByAccountIdAndBankLogId(accId, bankLogId));
	}
}
