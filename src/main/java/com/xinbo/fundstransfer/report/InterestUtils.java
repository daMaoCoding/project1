package com.xinbo.fundstransfer.report;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Objects;

public class InterestUtils {

	public static boolean interest(AccountBaseInfo base, BizBankLog lg) {
		if (Objects.isNull(base) || Objects.isNull(lg))
			return false;
		String summary = StringUtils.trimToEmpty(lg.getSummary());
		if (summary.contains("利息") || summary.contains("结息"))
			return true;
		String oppAccount = StringUtils.trimToEmpty(lg.getToAccount());
		if (oppAccount.contains("利息") || oppAccount.contains("结息"))
			return true;
		String accOwner = StringUtils.trimToEmpty(base.getOwner());
		String oppOwner = StringUtils.trimToEmpty(lg.getToAccountOwner());
		if (!accOwner.contains("利息") && !accOwner.contains("结息")
				&& (oppOwner.contains("利息") || oppOwner.contains("结息")))
			return true;
		BigDecimal amt = SysBalUtils.radix2(lg.getAmount());
		return amt.compareTo(BigDecimal.ZERO) > 0 && amt.compareTo(new BigDecimal("20.00")) < 0
				&& (Objects.isNull(lg.getTaskId()) || lg.getTaskId() == 0) && StringUtils.isBlank(summary)
				&& StringUtils.isBlank(oppOwner) && StringUtils.isBlank(oppAccount);
	}

	/**
	 * 季度末月的20日为结息日，次日付息。 结息日分别为: 3月20日 6月20日 9月20日 12月20日,入账日期为21日
	 */
	public static boolean checkQuarterInterestDate() {
		int currentMonth = SystemAccountUtils.currentMonth();
		if (Calendar.MARCH != currentMonth && Calendar.JUNE != currentMonth && Calendar.SEPTEMBER != currentMonth
				&& Calendar.DECEMBER != currentMonth)
			return false;
		int currentDayOfMonth = SystemAccountUtils.currentDayOfMonth();
		return 21 == currentDayOfMonth || 22 == currentDayOfMonth || 23 == currentDayOfMonth || 24 == currentDayOfMonth;
	}
}
