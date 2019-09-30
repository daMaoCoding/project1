package com.xinbo.fundstransfer.report.survey;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizSysErr;
import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountConfiguration;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.acc.ErrorAlarm;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.service.SysErrService;
import com.xinbo.fundstransfer.service.SysInvstService;

@Component
public class InvstHandler {
	private static final Logger logger = LoggerFactory.getLogger(InvstHandler.class);
	@Autowired
	private SysErrService sysErrSer;
	@Autowired
	private SysInvstService sysInvstSer;
	@Autowired
	private StoreHandler storeHandler;

	/**
	 * 人工正在排查的账号
	 */
	public Set<Integer> accInvstByMan(StringRedisTemplate template) {
		Set<Integer> ids = new HashSet<>();
		if (!SystemAccountConfiguration.needOpenService4ManualSurveyIfAccountException())
			return ids;
		Map<Object, Object> kv = template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).entries();
		if (CollectionUtils.isEmpty(kv))
			return ids;
		kv.forEach((k, v) -> {
			if (k != null && v != null) {
				ErrorAlarm alarm = new ErrorAlarm((String) v);
				if (alarm.getErrorId() != 0)
					ids.add(Integer.valueOf((String) k));
			}
		});
		return ids;
	}

	public void autoClearError(StringRedisTemplate template) {
		long point700 = SystemAccountUtils.currentDayStartMillis() + SystemAccountUtils.SEVEN_HOURS_MILLIS;
		long curr = System.currentTimeMillis(), point730 = point700 + SystemAccountUtils.THIRTY_MINUTES_MILLIS;
		long clearPoint = (point700 <= curr && curr <= point730) ? (point700 - SystemAccountUtils.THIRTY_MINUTES_MILLIS)
				: (curr > point730 ? point700 : (point700 - SystemAccountUtils.ONE_DAY_MILLIS));
		Map<Object, Object> kv = template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).entries();
		if (CollectionUtils.isEmpty(kv))
			return;
		kv.forEach((k, v) -> {
			if (k != null) {
				ErrorAlarm alarm = new ErrorAlarm((String) v);
				if (alarm.getOccureTm() == 0 || alarm.getOccureTm() <= clearPoint) {
					String id = (String) k;
					if (Objects.nonNull(id) && StringUtils.isNumeric(id))
						clearError(template, Integer.valueOf(id.trim()));
				}
			}
		});
	}

	/**
	 * 检测银行账号是否需要人工排查
	 */
	public void invst(StringRedisTemplate template, ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(check) || Objects.isNull(check.getBase()))
			return;
		AccountBaseInfo base = check.getBase();
		// 如果人工排查开关处理关闭状态,执行清理异常逻辑，并返回
		if (!SystemAccountConfiguration.needOpenService4ManualSurveyIfAccountException()) {
			clearError(template, base.getId());
			return;
		}
		// 如果该账号正处于初始化过程中，则：清理该账号的ERROR记录
		// String initTime = (String)
		// template.boundHashOps(RedisKeys.ACC_SYS_INIT).get(String.valueOf(base.getId()));
		// boolean initing = StringUtils.isNumeric(initTime);
		// if (initing) {
		// clearError(template, base);
		// return;
		// }
		List<BizSysLog> sysLogList = storeHandler.findSysLogFromCache(base.getId()).stream()
				.filter(p -> Objects.equals(p.getStatus(), SysLogStatus.Valid.getStatusId()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(sysLogList)) {
			clearError(template, base.getId());
			return;
		}
		BigDecimal sys0 = SysBalUtils.radix2(sysLogList.get(0).getBalance());
		BigDecimal bank0 = SysBalUtils.radix2(sysLogList.get(0).getBankBalance());
		if (sys0.compareTo(bank0) == 0 || sysLogList.size() == 1) {
			clearError(template, base.getId());
		} else {
			BigDecimal sys1 = SysBalUtils.radix2(sysLogList.get(1).getBalance());
			BigDecimal bank1 = SysBalUtils.radix2(sysLogList.get(1).getBankBalance());
			if (sys1.compareTo(bank1) != 0) {
				Date tradeTime = sysLogList.get(0).getCreateTime();
				if (Objects.nonNull(tradeTime)) {
					long curr = System.currentTimeMillis(), tradePoint = tradeTime.getTime();
					long checkPoint700 = SystemAccountUtils.currentDayStartMillis()
							+ SystemAccountUtils.SEVEN_HOURS_MILLIS;
					long checkPoint730 = checkPoint700 + 1800000, checkPoint630 = checkPoint700 - 1800000;
					if ((curr < checkPoint700 && tradePoint <= (checkPoint700 - SystemAccountUtils.ONE_DAY_MILLIS))
							|| (checkPoint700 <= curr && curr <= checkPoint730 && tradePoint <= checkPoint630)
							|| (checkPoint730 < curr && tradePoint <= checkPoint700)) {
						clearError(template, base.getId());
						return;
					}
				}
				recordError(template, base, sys0, bank0, check);
			} else {
				clearError(template, base.getId());
			}
		}
	}

	private void clearError(StringRedisTemplate template, Integer accId) {
		try {
			String target = String.valueOf(accId);
			ErrorAlarm alarm = new ErrorAlarm((String) template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).get(target));
			template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).delete(target);
			if (alarm.getErrorId() == 0)
				return;
			sysErrSer.delete(alarm.getErrorId());
			List<BizSysInvst> errList = sysInvstSer.findByErrorId(alarm.getErrorId());
			if (!CollectionUtils.isEmpty(errList)) {
				sysInvstSer.delete(errList);
			}
			logger.info("INVST{} [ TURN NONE-RUN TO RUNNING ] >> errorId: {}", accId, alarm.getErrorId());
		} catch (Exception e) {
		}
	}

	private void recordError(StringRedisTemplate template, AccountBaseInfo base, BigDecimal sysBal, BigDecimal bankBal,
			ReportCheck check) {
		String target = String.valueOf(base.getId());
		ErrorAlarm alarm = new ErrorAlarm((String) template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).get(target));
		if (alarm.getErrorId() != 0) {
			if (needUpdate(alarm)) {
				BizSysErr sysErr = sysErrSer.findById(alarm.getErrorId());
				if (Objects.nonNull(sysErr)) {
					sysErr.setOccurTime(new Date());
					sysErrSer.save(sysErr, sysBal, bankBal);
					alarm.setOccureTm(sysErr.getOccurTime().getTime());
					template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).put(target, ErrorAlarm.genMsg(alarm));
					logger.info("INVST{} [ NONE-RUN ] >> update error record .  errorId: {} , sysBal：{} bankBal: {}   ",
							target, sysErr.getId(), sysBal, bankBal);
				} else {
					sysErr = sysErrSer.save(check.getBase(), sysBal, bankBal);
					alarm.setErrorId(sysErr.getId());
					alarm.setOccureTm(sysErr.getOccurTime().getTime());
					template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).put(target, ErrorAlarm.genMsg(alarm));
					logger.info("INVST{} [ NONE-RUN ] >> save error record .  errorId: {} , sysBal：{} bankBal: {}   ",
							target, sysErr.getId(), sysBal, bankBal);
				}
			}
		} else {
			BizSysErr sysErr = sysErrSer.save(check.getBase(), sysBal, bankBal);
			alarm.setErrorId(sysErr.getId());
			alarm.setOccureTm(sysErr.getOccurTime().getTime());
			template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).put(target, ErrorAlarm.genMsg(alarm));
			logger.info("INVST{} [ NONE-RUN ] >> save error record .  errorId: {} , sysBal：{} bankBal: {}   ", target,
					sysErr.getId(), sysBal, bankBal);
		}
	}

	private boolean needUpdate(ErrorAlarm alarm) {
		if (Objects.isNull(alarm) || alarm.getErrorId() == 0)
			return false;
		if (alarm.getOccureTm() == 0)
			return true;
		long checkPoint = SystemAccountUtils.currentDayStartMillis() + SystemAccountUtils.SEVEN_HOURS_MILLIS + 2000;
		long currPoint = System.currentTimeMillis(), lastOccur = alarm.getOccureTm();
		if (currPoint < checkPoint) {
			return lastOccur <= checkPoint - SystemAccountUtils.ONE_DAY_MILLIS;
		} else {
			return lastOccur <= checkPoint;
		}
	}
}
