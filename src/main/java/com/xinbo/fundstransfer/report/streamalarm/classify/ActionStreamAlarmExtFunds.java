package com.xinbo.fundstransfer.report.streamalarm.classify;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysInvstType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.streamalarm.ActionStreamAlarm;
import com.xinbo.fundstransfer.report.streamalarm.StreamAlarmAnnotation;
import com.xinbo.fundstransfer.report.streamalarm.StreamAlarmHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportParamStreamAlarm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * 流水告警处理：外部资金
 */
@StreamAlarmAnnotation(ActionStreamAlarm.PREFIX_ACTION_STREAM_ALARM
		+ ActionStreamAlarm.ACTION_STREAM_ALARM_TYPE_ExtFunds)
public class ActionStreamAlarmExtFunds extends ActionStreamAlarm {
	protected static final Logger logger = LoggerFactory.getLogger(ActionStreamAlarmExtFunds.class);

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, StreamAlarmHandler handler,
			ReportParamStreamAlarm param, ReportCheck check) {
		// 检测：缓存中有无该流水记录
		Optional<BizBankLog> optionalBank = storeHandler.findBankLogFromCache(base.getId()).stream()
				.filter(p -> Objects.equals(param.getBankLogId(), p.getId())).findFirst();
		if (!optionalBank.isPresent())
			return false;
		BizBankLog bank = optionalBank.get();
		// 检测：该流水在系统账目中有无该记录
		BigDecimal amt = SysBalUtils.radix2(bank.getAmount());
		if (amt.compareTo(BigDecimal.ZERO) <= 0)
			return false;
		Optional<BizSysLog> optionalSys = storeHandler.findSysLogFromCache(base.getId()).stream()
				.filter(p -> Objects.equals(bank.getId(), p.getBankLogId()) || SysBalUtils.radix(amt)
						&& Objects.nonNull(p.getAmount()) && amt.abs().compareTo(p.getAmount().abs()) == 0)
				.findFirst();
		if (optionalSys.isPresent())
			return false;
		// 检测：排查记录中有无该流水
		if (!checkBankLog(base.getId(), param.getBankLogId()))
			return false;
		// 从系统余额中新增该金额
		BigDecimal[] bs = storeHandler.setSysBal(template, base.getId(), amt, null, false);
		// 保存操作记录
		Date d = new Date();
		Date occurTime = (Objects.isNull(bank.getCreateTime()) ? d : bank.getCreateTime());
		BizSysInvst actionExtFunds = sysInvstSer.saveAndFlush(bank.getFromAccount(), bank.getId(), bank.getAmount(),
				bs[1], bs[0], null, base.getHandicapId(), bank.getToAccount(), bank.getToAccountOwner(), null,
				SystemAccountUtils.generateId(), null, null, SysInvstType.UnknowIncome.getType(), bank.getSummary(),
				param.getRemark(), param.getOperator(), d, occurTime, d.getTime() - occurTime.getTime());
		logger.info(
				"SB{} [ ACTION STREAM ALARM EXT FUNDS ] >>  amount:{} operator: {} remark: {} bankLogId:{}  ActionId:{}",
				base.getId(), bank.getAmount(), param.getOperator(), param.getRemark(), bank.getId(),
				actionExtFunds.getId());
		return true;
	}
}
