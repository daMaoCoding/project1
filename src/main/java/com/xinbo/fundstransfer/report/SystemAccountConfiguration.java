package com.xinbo.fundstransfer.report;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 系统账目运行配置参数
 */
public class SystemAccountConfiguration {

	/**
	 * 系统账目总运行开关</br>
	 * 默认处于打开状态
	 *
	 * @return {@code true} 打开;{@code false} 关闭
	 */
	public static boolean mainOperatingSwitch() {
		return StringUtils.equals(
				MemCacheUtils.getInstance().getSystemProfile().getOrDefault("ENABLE_ACCOUNT_SYS_BAL", "1"), "1");
	}

	/**
	 * 当系统账目服务检测到转账任务失败时，是否自动重新分配
	 */
	public static boolean needAutoAssignIfTransactionFail() {
		return StringUtils.equals(
				MemCacheUtils.getInstance().getSystemProfile().getOrDefault("SYS_BAL_AUTO_INVESTIGATE", "0"), "1");
	}

	/**
	 * 当一个账号的系统余额与银行余额对不上时，</br>
	 * 人工排查该账号的系统账目服务是否打开
	 */
	public static boolean needOpenService4ManualSurveyIfAccountException() {
		return StringUtils.equals(
				MemCacheUtils.getInstance().getSystemProfile().getOrDefault("ENABLE_ACCOUNT_INVSTIAGET", "1"), "1");
	}

	/**
	 * 该开关打开时,该账号不在参与下发任务调度，出任任务分配
	 */
	public static boolean needOpenService4AccountingException() {
		return StringUtils.equals(
				MemCacheUtils.getInstance().getSystemProfile().getOrDefault("SYS_BAL_OPEN_ACCOUNTING_EXCEPTION", "0"),
				"1");
	}

	/**
	 * 为哪些盘口提供异常提供异常账号服务
	 *
	 * @return 盘口集合ID
	 */
	public static Set<Integer> hadicapSetByOpenService4AccountingException() {
		String ret = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("SYS_BAL_OPEN_HANDICAP_ACCOUNTING_EXCEPTION", StringUtils.EMPTY);
		if (StringUtils.isBlank(ret))
			return Collections.EMPTY_SET;
		return Stream.of(ret.trim().split(",")).filter(StringUtils::isNumeric).map(Integer::parseInt)
				.collect(Collectors.toSet());
	}

	/**
	 * 为哪些盘口提供自动排查服务</br>
	 * 工具上报未知的出款任务（会员出款/返利网用户提现），是否经过系统账目服务程序自动排查
	 */
	public static boolean checkHandicapByNeedOpenService4NeedAutoSurvey4UnknownTask(BizHandicap handicap) {
		if (Objects.isNull(handicap) || Objects.isNull(handicap.getId()))
			return false;
		String ret = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("SYS_BAL_OPEN_HANDICAP_NEED_AUTO_SURVEY_UNKNOWN_TASK", StringUtils.EMPTY);
		if (StringUtils.isBlank(ret))
			return false;
		if (ret.contains("all") || ret.contains("ALL"))
			return true;
		return Stream.of(ret.trim().split(",")).filter(StringUtils::isNumeric).map(Integer::parseInt)
				.filter(p -> Objects.equals(p, handicap.getId())).count() > 0;
	}
}
