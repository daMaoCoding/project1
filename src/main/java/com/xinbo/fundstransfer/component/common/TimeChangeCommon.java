package com.xinbo.fundstransfer.component.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TimeChangeCommon {

	private static final Logger logger = LoggerFactory.getLogger(TimeChangeCommon.class);

	private static int getMondayPlus() {
		Calendar cd = Calendar.getInstance();
		// 获得今天是一周的第几天，星期日是第一天，星期二是第二天......
		int dayOfWeek = cd.get(Calendar.DAY_OF_WEEK) - 1; // 因为按中国礼拜一作为第一天所以这里减1
		if (dayOfWeek == 1) {
			return 0;
		} else {
			return 1 - dayOfWeek;
		}
	}

	// 获取过去7天开始时间
	public static String getPrevious7days() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -3);
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 7, 0, 0);
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
	}

	// 获取上周一时间
	public static String getPreviousWeekday() {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 7, 0, 0);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return abc.format(cal.getTime());
	}

	// 获得本周一6点59:59时间 ////////
	public static String getTimesWeekmorningAt6() {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 6, 59, 59);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return abc.format(cal.getTime());
	}

	// 获得本周一7点时间 ////////
	public static String getTimesWeekmorning() {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 7, 0, 0);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return abc.format(cal.getTime());
	}

	// 获得下周一 ///////////
	public static String getTimesWeeknight() throws ParseException {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 7);
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 7, 0, 0);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return abc.format(cal.getTime());
	}

	// 获取上月初0点时间///
	public static String getLastMonthStartMorning() throws ParseException {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.setTime(abc.parse(getTimesMonthmorning()));
		cal.add(Calendar.MONTH, -1);
		return abc.format(cal.getTime());
	}

	// 获取上月初0点时间////
	public static String getLastMonthEndMorning() throws ParseException {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.setTime(abc.parse(getTimesMonthnight()));
		cal.add(Calendar.MONTH, -1);
		return abc.format(cal.getTime());
	}

	public static String TimeStamp2Date(String timestampString, String formats) throws ParseException {
		if (!"".equals(timestampString) && null != timestampString) {
			SimpleDateFormat ab = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String date = ab.format(ab.parse(timestampString));
			return date;
		} else {
			return null;
		}
	}

	/**
	 * 本月开始时间 （1号 7:00:00）
	 * 
	 * @return
	 */
	public static String getTimesMonthmorning() {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 7, 0, 0);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		return abc.format(cal.getTime());
	}

	/**
	 * 下月结束时间 （开始第一天一天 6:59:59）
	 * 
	 * @return
	 */
	public static String getTimesMonthnight() {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		cal.add(Calendar.MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 24);
		cal.set(Calendar.HOUR, 06);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return abc.format(cal.getTime());
	}

	/**
	 * 昨日开始时间 （昨日 7:00:00）
	 * 
	 * @return
	 */
	public static String getYesterdayStartTime() {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar yesterdayStart = Calendar.getInstance();
		yesterdayStart.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
		yesterdayStart.add(Calendar.DATE, -1);
		yesterdayStart.set(Calendar.HOUR_OF_DAY, 07);
		yesterdayStart.set(Calendar.MINUTE, 0);
		yesterdayStart.set(Calendar.SECOND, 0);
		yesterdayStart.set(Calendar.MILLISECOND, 0);
		Date time = yesterdayStart.getTime();
		return abc.format(time);

	}

	/**
	 * 昨日结束时间 （当日 6:59:59）
	 * 
	 * @return
	 */
	public static String getYesterdayEndTime() {
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar yesterdayEnd = Calendar.getInstance();
		yesterdayEnd.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
		yesterdayEnd.add(Calendar.DATE, 0);
		yesterdayEnd.set(Calendar.HOUR_OF_DAY, 06);
		yesterdayEnd.set(Calendar.MINUTE, 59);
		yesterdayEnd.set(Calendar.SECOND, 59);
		yesterdayEnd.set(Calendar.MILLISECOND, 999);
		Date time = yesterdayEnd.getTime();
		return abc.format(time);

	}

	/**
	 * 今日开始时间 （当日 7:00:00）
	 * 
	 * @return
	 */
	public static String getTodayStartTime() {
		Date nowTime = new Date(System.currentTimeMillis());
		SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
		return sdFormatter.format(nowTime) + " 07:00:00";
	}

	/**
	 * 今日结束时间 （次日 6:59:59）
	 * 
	 * @return
	 */
	public static String getTodayEndTime() {
		Date nowTime = new Date(System.currentTimeMillis());
		SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
		return sdFormatter.format(nowTime.getTime() + (1000 * 60 * 60 * 24)) + " 06:59:59";
	}

	/**
	 * 生成时间范围结果集返回
	 * 
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @return
	 */
	public static Map<String, String> setTimeSizeByFieldval(String[] startAndEndTimeToArray, String fieldval) {
		Map<String, String> result = new HashMap<String, String>();
		try {
			String startTime = null, endTime = null;
			if (null != startAndEndTimeToArray && startAndEndTimeToArray.length > 0
					&& !startAndEndTimeToArray[0].equals("0")) {
				startTime = startAndEndTimeToArray[0];
				endTime = startAndEndTimeToArray[1];
			} else if (!fieldval.equals("0")) {
				if ("today".equals(fieldval)) {// 今日
					startTime = getTodayStartTime();
					endTime = getTodayEndTime();
				} else if ("yesterday".equals(fieldval)) {// 昨日
					startTime = getYesterdayStartTime();
					endTime = getYesterdayEndTime();
				} else if ("thisMonth".equals(fieldval)) {// 本月
					startTime = getTimesMonthmorning();
					endTime = getTimesMonthnight();
				}
			}
			result.put("startTime", startTime == null ? null : TimeStamp2Date(startTime, "yyyy-MM-dd HH:mm:ss"));
			result.put("endTime", endTime == null ? null : TimeStamp2Date(endTime, "yyyy-MM-dd HH:mm:ss"));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("日期搜索范围转换异常" + e);
			return result;
		}
	}

	/**
	 * 今日开始时间 （当日 00:00:00）
	 * 
	 * @return
	 */
	public static String getToday5StartTime() {
		Date nowTime = new Date(System.currentTimeMillis());
		SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
		String retStrFormatNowDate = sdFormatter.format(nowTime);
		return retStrFormatNowDate + " 00:00:00";
	}

	/**
	 * 今日结束时间 （当日日 23:59:59）
	 * 
	 * @return
	 */
	public static String getToday5EndTime() {
		Date nowTime = new Date(System.currentTimeMillis());
		SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
		String retStrFormatNowDate = sdFormatter.format(nowTime);
		return retStrFormatNowDate + " 23:59:59";
	}

}