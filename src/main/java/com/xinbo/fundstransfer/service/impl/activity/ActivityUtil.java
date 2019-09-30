package com.xinbo.fundstransfer.service.impl.activity;

import com.google.common.collect.Lists;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.utils.TokenCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Slf4j
public class ActivityUtil {

	public static final String dateTimeString = "yyyy-MM-dd HH:mm:ss";

	/**
	 * 判断日期是否再给定范围内
	 */
	public static boolean isBetween(Date startDate, Date endDate, Date myDate) {
		if (null == startDate || null == endDate || null == myDate)
			return false;
		return startDate.compareTo(endDate) * myDate.compareTo(endDate) > 0;
	}

	/**
	 * 今日0点时间戳
	 */
	public static Long getTodayZeroPointTimestamps() {
		Long currentTimestamps = System.currentTimeMillis();
		Long oneDayTimestamps = Long.valueOf(60 * 60 * 24 * 1000);
		return currentTimestamps - (currentTimestamps + 60 * 60 * 8 * 1000) % oneDayTimestamps;
	}

	/**
	 * 解析日期字符串
	 */
	public static Date parseDateTime(String datetimeStr) {
		if (StringUtils.isEmpty(datetimeStr)) {
			return null;
		}
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(dateTimeString);
			return sdf.parse(datetimeStr);
		} catch (ParseException e) {
			return null;
		}
	}

	/**
	 * 解析biz_account_more.accounts字段
	 */
	public static ArrayList<Integer> findAccountIds(String accountsStr) {
		ArrayList<Integer> accounts = Lists.newArrayList();
		if (null != accountsStr) {
			Matcher matcher = Pattern.compile("(\\w+\\s)?(\"[^\"]+\"|\\w+)(\\(\\w\\d(,\\w\\d)*\\))?")
					.matcher(accountsStr);
			while (matcher.find()) {
				accounts.add(Integer.valueOf(matcher.group()));
			}
		}
		return accounts;
	}

	/**
	 * 验证请求token
	 */
	public static boolean reqTokenCheck(String formater, String token, Object... args) {
		return TokenCheckUtil.reqTokenCheck(formater,token,args);
	}

	public static BigDecimal radix2(BigDecimal arg0) {
		return Objects.isNull(arg0) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: arg0.setScale(2, RoundingMode.HALF_UP);
	}

	public static Date[] startAndEndTimeOfCommission(String time) {
		if (org.apache.commons.lang3.StringUtils.isBlank(time))
			return null;
		String[] yyyyMMdd = time.split("-");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, Integer.valueOf(yyyyMMdd[0]));
		calendar.set(Calendar.MONTH, Integer.valueOf(yyyyMMdd[1]) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(yyyyMMdd[2]));
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return new Date[] { calendar.getTime(), new Date(calendar.getTime().getTime() + 86400000 - 500) };
	}

}
