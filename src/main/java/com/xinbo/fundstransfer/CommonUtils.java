package com.xinbo.fundstransfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.impl.SysUserProfileServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
public class CommonUtils {
	private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);
	private static String INTERNAL_IP = null;
	private static ObjectMapper mapper = new ObjectMapper();//

	public static String getInternalIp(Set<String> allHost) {
		if (INTERNAL_IP != null) {
			return INTERNAL_IP;
		}
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			if (!inetAddress.isLoopbackAddress() && allHost.contains(inetAddress.getHostAddress())) {
				return INTERNAL_IP = inetAddress.getHostAddress();
			}
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				if (intf.getName().contains("lo") || intf.getName().contains("docker")) {
					continue;
				}
				for (Enumeration<InetAddress> ei = intf.getInetAddresses(); ei.hasMoreElements();) {
					InetAddress ne = ei.nextElement();
					if (!ne.isLoopbackAddress() && !ne.getHostAddress().contains("fe80")
							&& !ne.getHostAddress().contains(":") && allHost.contains(ne.getHostAddress())) {
						return INTERNAL_IP = ne.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			return "unknown";
		} catch (UnknownHostException e) {
			return "unknown";
		}
		return INTERNAL_IP;
	}

	public static String getInternalIp() {
		return INTERNAL_IP;
	}

	private static long EXPIRE_TIME_4_AMOUNT_DAILY = 0;

	public static long getExpireTime4AmountDaily() {
		long curr = System.currentTimeMillis();
		if (curr <= EXPIRE_TIME_4_AMOUNT_DAILY)
			return EXPIRE_TIME_4_AMOUNT_DAILY;
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		EXPIRE_TIME_4_AMOUNT_DAILY = calendar.getTimeInMillis();
		return EXPIRE_TIME_4_AMOUNT_DAILY;
	}

	private static long CURR_DAY_START = 0, CURR_DAY_END = 0;
	private static String CURR_DAY_START_STR = "";

	/**
	 * format:yyyy-MM-dd HH:mm:ss
	 */
	public static String getStartTimeOfCurrDay() {
		long curr = System.currentTimeMillis();
		if (curr >= CURR_DAY_START && curr <= CURR_DAY_END)
			return CURR_DAY_START_STR;
		synchronized (StringUtils.EMPTY) {
			if (curr >= CURR_DAY_START && curr <= CURR_DAY_END)
				return CURR_DAY_START_STR;
			Calendar calendar = Calendar.getInstance();
			int delimiter = 7, currHour = calendar.get(Calendar.HOUR_OF_DAY);
			calendar.set(Calendar.HOUR_OF_DAY, delimiter);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			if (currHour < delimiter) {
				calendar.add(Calendar.DAY_OF_YEAR, -1);
			}
			Date d = calendar.getTime();
			CURR_DAY_START_STR = String.format("%tF %tT", d, d);
			CURR_DAY_START = d.getTime();
			CURR_DAY_END = d.getTime() + 86400000;
			return CURR_DAY_START_STR;
		}
	}

	private static long AMOUNT_DAILY_START = 0, AMOUNT_DAILY_END = 0;

	public static long getStartMillisOfAmountDaily() {
		long curr = System.currentTimeMillis();
		if (curr >= AMOUNT_DAILY_START && curr <= AMOUNT_DAILY_END)
			return AMOUNT_DAILY_START;
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		AMOUNT_DAILY_START = calendar.getTime().getTime();
		AMOUNT_DAILY_END = AMOUNT_DAILY_START + 86400000;
		return AMOUNT_DAILY_START;
	}

	/**
	 * @param startAndEndTimeToArray
	 *            format:yyyy-MM-dd HH:mm:ss
	 * @return Date[0] startTime,Date[1] endTime
	 */
	public static Date[] parseStartAndEndTime(String[] startAndEndTimeToArray) throws ParseException {
		return parseStartAndEndTime(null, startAndEndTimeToArray);
	}

	/**
	 * @param statusToArray
	 *            已匹配，未匹配，未认领 状态组合
	 * @param startAndEndTimeToArray
	 *            format:yyyy-MM-dd HH:mm:ss
	 * @return Date[0] startTime,Date[1] endTime
	 */
	public static Date[] parseStartAndEndTime(Integer[] statusToArray, String[] startAndEndTimeToArray)
			throws ParseException {
		Date[] result = new Date[2];
		if (statusToArray != null && statusToArray.length != 3) {
			Date t = new Date(System.currentTimeMillis() - 86400000 * AppConstants.DAYS_OF_NO_OWNER);
			for (Integer status : statusToArray) {
				if (BankLogStatus.NoOwner.getStatus().equals(status)) {
					result[0] = new Date(System.currentTimeMillis() - 630720000000L);
					result[1] = result[1] == null ? t : result[1];
				} else if (BankLogStatus.Matching.getStatus().equals(status)) {
					result[0] = result[0] == null ? t : result[0];
					result[1] = new Date();
				}
			}
			for (Integer status : statusToArray) {
				if (BankLogStatus.Matched.getStatus().equals(status)) {
					result[0] = result[1] = null;
				}
			}
		}
		if (startAndEndTimeToArray == null || startAndEndTimeToArray.length == 0) {
			return result;
		}
		SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (startAndEndTimeToArray[0] != null) {
			result[0] = SDF.parse(startAndEndTimeToArray[0]);
		}
		if (startAndEndTimeToArray[1] != null) {
			Date end = SDF.parse(startAndEndTimeToArray[1]);
			if (result[1] != null) {
				result[1] = end != null && end.getTime() < result[1].getTime() ? end : result[1];
			} else {
				result[1] = end;
			}
		}
		return result;
	}

	public static String transToStarString(String o) {
		if (o == null) {
			return null;
		}
		int s, e, l = o.length();
		e = (l == 1 || l == 2) ? (s = l - 1) : (s = l / 3) * 2;
		return o.substring(0, s) + "*" + o.substring(e);
	}

	public static String genJSessionIdByFactor(int factor) {
		String[] arr = UUID.randomUUID().toString().split("-");
		int l = String.valueOf(factor).length();
		char[] ch = String.valueOf(Math.round(Math.pow(10, l) * l) + factor).toCharArray();
		for (int i = 0; i < ch.length; i++) {
			int in = i <= 4 ? i : 4, start = i == 0 ? 1 : i;
			arr[in] = new StringBuffer(arr[in]).replace(start, start + 1, String.valueOf(ch[i])).toString();
		}
		return StringUtils.join(arr, "-");
	}

	public static int genFactorByJSessionId(Serializable jSessionId) {
		String[] arr = jSessionId.toString().split("-");
		int l = Integer.parseInt(arr[0].substring(1, 2));
		String[] ret = new String[l];
		for (int i = 0; i < l; i++) {
			ret[i] = arr[i <= 3 ? (i + 1) : 4].substring(i + 1, i + 2);
		}
		return Integer.parseInt(StringUtils.join(ret, StringUtils.EMPTY));
	}

	/**
	 * 时间字符串 转换为时间 非线性安全
	 */
	public static Date string2Date(String dateStr) {
		SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		if (StringUtils.isNotBlank(dateStr)) {
			try {
				date = SDF.parse(dateStr);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return date;
	}

	/**
	 * Created by on 2017/10/18. 如果使用先查询记录<br/>
	 * 然后查询总记录 查询总金额的方式则调用该类的方法 <br/>
	 * 注意 pageNo pageSize totalElements 必须传值
	 */
	public static Paging getPage(int pageNo, int realPageSize, String totalElements) {
		Paging page = new Paging();
		page.setTotalElements(Long.parseLong(totalElements));
		page.setPageNo(pageNo);
		page.setPageSize(realPageSize);
		page.setTotalPages(
				Integer.parseInt(totalElements) % realPageSize == 0 ? Integer.parseInt(totalElements) / realPageSize
						: (Integer.parseInt(totalElements) / realPageSize) + 1);
		if (Integer.parseInt(totalElements) > 0) {
			// 有记录
			if (page.getPageNo() >= 1) {
				if (page.getPageNo() == 1) {
					page.setFirst(true);
					page.setHasPrevious(false);
					page.setPreviousPageNo(page.getPageNo());
				} else {
					page.setFirst(false);
					page.setHasPrevious(true);
					page.setPreviousPageNo(page.getPageNo() - 1);
				}
			}
			if (page.getTotalPages() >= page.getPageNo()) {
				if (page.getTotalPages() == page.getPageNo()) {
					page.setLast(true);
					page.setHasNext(false);
					page.setNextPageNo(page.getPageNo());
				} else {
					page.setLast(false);
					page.setHasNext(true);
					page.setNextPageNo(page.getPageNo() + 1);
				}
			}
		} else {
			// 无记录
			page.setFirst(true);
			page.setHasPrevious(true);
			page.setPreviousPageNo(0);
			page.setLast(true);
			page.setHasNext(true);
			page.setNextPageNo(0);
		}
		return page;
	}

	/**
	 * 时间毫秒转为：xx:xx:xx
	 */
	public static String convertTime2String(Long timestamp) {
		StringBuilder times = new StringBuilder();
		if (timestamp != null && timestamp >= 0) {
			long d = timestamp / (24 * 60 * 60 * 1000);
			long dh = 0;
			long hs = timestamp % (24 * 60 * 60 * 1000);
			long h = hs / (60 * 60 * 1000);
			long ms = hs % (60 * 60 * 1000);
			long m = ms / (60 * 1000);
			long ss = ms % (60 * 1000);
			int s = Math.round(ss / 1000);
			if (d > 0) {
				dh += d * 24;
			}
			h += dh;
			if (h > 0) {
				if (h < 10) {
					times.append("0").append(h);
				} else {
					times.append(h);
				}
			} else {
				times.append("00");
			}
			if (m > 0) {
				if (h > 0) {
					if (m < 10) {
						times.append(":").append("0").append(m);
					} else {
						times.append(":").append(m);
					}
				} else {
					if (m < 10) {
						times.append(":0").append(m);
					} else {
						times.append(":").append(m);
					}
				}

			} else {
				times.append(":00");
			}
			if (s > 0) {
				if (m > 0) {
					if (s < 10) {
						times.append(":0").append(s);
					} else {
						times.append(":").append(s);
					}
				} else {
					if (s < 10) {
						times.append(":0").append(s);
					} else {
						times.append(":").append(s);
					}
				}
			} else {
				times.append(":00");
			}
		}
		return times.toString();
	}

	/***
	 * 格式化时间 ： yyyy-MM-dd HH:mm:ss
	 */
	public static String getNowDate() {
		Date d = new Date();
		return String.format("%tF %tT", d, d);
	}

	/***
	 * 格式化时间 ： yyyy-MM-dd HH:mm:ss
	 */
	public static String getDateStr(Date d) {
		return Objects.isNull(d) ? StringUtils.EMPTY : String.format("%tF %tT", d, d);
	}

	public static String genCloseMsg4WS(int userId, String excIp, Class inclusiveWsEndpoint, String wsType,
			String jSessionId) {
		StringBuilder sb = new StringBuilder().append(userId).append(":");
		if (StringUtils.isNotBlank(excIp) && !excIp.contains(":")) {
			sb.append(excIp);
		}
		sb.append(":");
		if (inclusiveWsEndpoint != null) {
			sb.append(inclusiveWsEndpoint.getName());
		}
		sb.append(":").append(StringUtils.trimToEmpty(wsType)).append(":").append(StringUtils.trimToEmpty(jSessionId));
		return sb.toString();
	}

	public static String genSysMsg4WS(Integer userId, SystemWebSocketCategory category, String message) {
		Map<String, Object> params = new HashMap<>();
		params.put("message", message);
		if (userId != null) {
			params.put("userId", userId);
		}
		if (category != null) {
			params.put("category", category.getCode());
		}
		try {
			return mapper.writeValueAsString(params);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return null;
	}

	public static String genRemark(String oldRemark, String newRemark, Date markTime, String operatorName) {
		if (StringUtils.isBlank(newRemark)) {
			return oldRemark;
		}
		StringBuilder builder = new StringBuilder();
		if (StringUtils.isNotBlank(oldRemark)) {
			builder.append(oldRemark).append("\r\n");
		}
		// 防止宽度太长撑爆页面
		StringBuilder wrapRemarks = new StringBuilder();
		if (newRemark.length() > 30) {
			int remarksLength = newRemark.length();
			int quotient = remarksLength / 30;
			int remainder = remarksLength % 30;

			for (int i = 0; i < quotient; i++) {
				wrapRemarks.append("\r\n").append(newRemark.substring(i * 30, (i + 1) * 30));
			}
			if (remainder > 0) {
				wrapRemarks.append("\r\n").append(newRemark.substring(quotient * 30, remarksLength));
			}
		} else {
			wrapRemarks.append("\r\n").append(newRemark);
		}
		if (Objects.nonNull(markTime)) {
			builder.append(getDateStr(markTime)).append("  ");
		}
		if (Objects.nonNull(wrapRemarks)) {
			builder.append(operatorName).append(wrapRemarks);
		}
		if (builder.length() > 1000) {
			return builder.substring(builder.length() - 960);
		}
		return builder.toString();
	}

	/**
	 * 获取当天凌晨 00：00：00
	 */
	public static Date getZeroDate(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 根据当前时间获取 当日时间 07:00:00--06:59:59 24小时跨度
	 * 
	 * @return
	 */
	public static Date[] getStart7AndEnd6() {
		LocalDateTime ldt = LocalDateTime.now();
		ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA));
		LocalDateTime ldt2 = ldt.withHour(7).withMinute(0).withSecond(0);
		// 当天日期与当天的7点0分0秒 比较 如果大于等于 则时间是 当天：07:00:00 到第二天:06:59:59
		// 否则 时间为 前一天:07:00 到当天:06:59:59
		long gap = ChronoUnit.SECONDS.between(ldt2, ldt);
		ZoneId zone = ZoneId.systemDefault();
		Date[] date = new Date[2];
		if (gap >= 0L) {

			Instant instant = ldt2.atZone(zone).toInstant();
			date[0] = Date.from(instant);

			LocalDateTime ldt3 = ldt2.plusDays(1).withHour(6).withMinute(59).withSecond(59);

			Instant instant3 = ldt3.atZone(zone).toInstant();
			date[1] = Date.from(instant3);

		} else {
			Instant instant = ldt2.atZone(zone).toInstant();
			date[1] = Date.from(instant);

			LocalDateTime ldt3 = ldt2.plusDays(-1).withHour(7).withMinute(0).withSecond(0);
			Instant instant3 = ldt3.atZone(zone).toInstant();
			date[0] = Date.from(instant3);

		}
		return date;
	}

	/***
	 * 格式化时间 ： yyyy-MM-dd HH:mm:ss
	 *
	 */
	public static String getDateFormat2Str(Date date) {
		if (date == null) {
			throw new RuntimeException("date can not be null");
		}
		return String.format("%tF %tT", date, date);
	}

	public static String getDateFormatyyyyMMdd2Str(Date date) {
		if (date == null) {
			throw new RuntimeException("date can not be null");
		}
		return String.format("%tF", date);
	}

	/**
	 * 判断字符串是否为纯数字
	 */
	public static boolean isNumeric(String str) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(str).matches();
	}

	/**
	 * 检测是否是生产环境
	 */
	public static boolean checkProEnv(String version) {
		return !StringUtils.equals(AppConstants.TEST_VERSION, version);
	}

	/**
	 * 毫秒转为时间字符串
	 */
	public static String millionSeconds2DateStr(Long millionSeconds) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millionSeconds);
		Date date = c.getTime();
		return formatter.format(date);
	}

	/**
	 * 毫秒转为时间字符串
	 */
	public static String millionSecondHHMMSS(Long millionSeconds) {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millionSeconds);
		Date date = c.getTime();
		return formatter.format(date);
	}

	/**
	 * 通过url访问并获取结果
	 */
	public static String getInfoByInternetUrl(String urlParam) {
		StringBuilder buffer = new StringBuilder(2048);
		try {
			URL url = new URL(urlParam);
			InputStream in = url.openStream();
			InputStreamReader isr = new InputStreamReader(in);
			BufferedReader bufr = new BufferedReader(isr);
			String str;
			while ((str = bufr.readLine()) != null) {
				System.out.println(str);
				buffer.append(str);
			}
			bufr.close();
			isr.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buffer.toString();
	}

	public static String formatAccount(String acc) {
		if (StringUtils.isBlank(acc)) {
			return null;
		}
		return "_" + acc.replaceAll("\\s+", StringUtils.EMPTY).replaceAll(":", StringUtils.EMPTY).replaceAll("\\*",
				StringUtils.EMPTY);
	}

	/**
	 * 生成加密token
	 *
	 * @param salt
	 *            加密密钥
	 * @param arg0
	 *            加密内容
	 * @return String 加密token
	 */
	public static String md5digest(String salt, Map<String, Object> arg0) {
		Map<String, Object> params = new TreeMap<String, Object>(Comparator.naturalOrder()) {
			{
				putAll(arg0);
			}
		};
		StringBuilder sb = new StringBuilder();
		params.values().forEach(p -> {
			if (Objects.nonNull(p)) {
				sb.append(p.toString().trim());
			}
		});
		return md5digest(salt, sb.toString());
	}

	/**
	 * 生成加密token
	 *
	 * @param salt
	 *            加密密钥
	 * @param content
	 *            加密内容
	 * @return String 加密token
	 */
	public static String md5digest(String salt, String content) {
		return md5digest(StringUtils.trimToEmpty(salt) + StringUtils.trimToEmpty(content));
	}

	/**
	 * 算法:md5加密
	 *
	 * @param content
	 *            加密内容
	 * @return md5值
	 */
	public static String md5digest(String content) {
		try {
			// 将字符串返序列化成byte数组
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(content);
			oos.close();
			byte[] bToEnc = baos.toByteArray();
			// 将byte数组进行“消息摘要”计算，得到加密串(result)
			MessageDigest e = MessageDigest.getInstance("MD5");
			e.update(bToEnc);
			return new BigInteger(1, e.digest()).toString(16);
		} catch (Exception e) {
			return "" + e.getLocalizedMessage();
		}
	}

	private static ThreadLocal<NumberFormat> ThreadLocal_NumberFormat = new ThreadLocal<>();

	/**
	 * 获取：格式化对象
	 *
	 * @param numInteger
	 *            被格式化数据：整数部分长度
	 * @param numFraction
	 *            被格式化数据：小数部分长度
	 */
	public static NumberFormat getNumberFormat(int numInteger, int numFraction) {
		NumberFormat ret = ThreadLocal_NumberFormat.get();
		if (ret == null || ret.getMaximumIntegerDigits() != numInteger
				|| ret.getMaximumFractionDigits() != numFraction) {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumIntegerDigits(numInteger);
			nf.setMinimumIntegerDigits(numInteger);
			nf.setMaximumFractionDigits(numFraction);
			nf.setMinimumFractionDigits(numFraction);
			nf.setGroupingUsed(false);
			ThreadLocal_NumberFormat.set(nf);
			return nf;
		}
		return ret;
	}

	public static String[] handicapCodes(List<BizHandicap> bizHandicapList, String handicapCode) {
		String[] handicapCodes = null;
		if (StringUtils.isNotBlank(handicapCode)) {
			handicapCodes = new String[] { handicapCode };
		} else {
			if (!CollectionUtils.isEmpty(bizHandicapList)) {
				handicapCodes = new String[bizHandicapList.size()];
				for (int i = 0; i < bizHandicapList.size(); i++) {
					handicapCodes[i] = bizHandicapList.get(i).getCode();
				}
			}

		}
		return handicapCodes;
	}

	/**
	 * <p>
	 * <ul>
	 * <li>是否开启入款卡 备用卡出款</li>
	 * <li>{@code 1 }开启</li>
	 * <li>{@code 其他值 }关闭</li>
	 * <p>
	 * <li>开启后的逻辑：
	 * <li>1、入款卡优先给自己盘口的会员出款</li>
	 * <li>2、入款卡只能往自己盘口的备用卡下发</li>
	 * <li>3、备用卡只能给自己盘口的会员出款</li>
	 * <li>4、开启后入款卡、备用卡都不往出款卡下发</li>
	 * </ul>
	 *
	 * @return true开启 false未开启
	 */
	public static boolean isEnableInBankOutTask() {
		return "1".equals(MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("ALLOCATE_OUTWARD_TASK_ENABLE_INBANK", "0"));
	}

	/**
	 * <p>
	 * <ul>
	 * <li>开启入款卡、备用卡出款的盘口，只有在CommonUtils.isEnableInBankOutTask()为true 的时候才有效</li>
	 * <p>
	 * <li>1、当值为ALL时，所有盘口都开启</li>
	 * <li>2、当值不为ALL时，各个盘口使用 ;(分号)分割，并且字符以分号开始和结束</li>
	 * </ul>
	 *
	 * @return true盘口开启出款 false盘口不开启出款
	 */
	public static boolean checkEnableInBankHandicap(Integer handicap) {
		String enableHandicap = getEnableInBankHandicap();
		return "ALL".equals(enableHandicap) || enableHandicap.contains(";" + handicap.toString() + ";");
	}

	/**
	 * 获取开启入款卡、备用卡出款的盘口的字符串
	 *
	 * @return 开启入款卡出款的盘口，以分号分隔
	 */
	public static String getEnableInBankHandicap() {
		String enableHandicap = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("ENABLE_INBANK_ALLOCATE_OUTWARD_HANDICAP", ";;").trim();
		return isEnableInBankOutTask() ? enableHandicap : "";
	}

	/**
	 * 获取客户端真实IP
	 *
	 * @param request
	 *            http请求
	 * @return IP字符串
	 */
	public static String getRemoteIp(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		String forwarded = request.getHeader("X-Forwarded-For");
		String realIp = request.getHeader("X-Real-IP");
		String ip;
		if (realIp == null) {
			if (forwarded == null) {
				ip = remoteAddr;
			} else {
				ip = remoteAddr + "/" + forwarded.split(",")[0];
			}
		} else {
			if (realIp.equals(forwarded)) {
				ip = realIp;
			} else {
				if (forwarded != null) {
					forwarded = forwarded.split(",")[0];
				}
				ip = realIp + "/" + forwarded;
			}
		}
		return ip;
	}

	/**
	 * 入款卡余额超过出款任务倍数时，可以进行出款
	 *
	 * @return 任务金额倍数，推荐为10
	 */
	public static int getInReserveOutwardMultiple() {
		String multiple = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("INBANK_RESERVEBANK_ALLOCATE_MULTIPLE", "10").trim();
		return StringUtils.isNumeric(multiple) ? Integer.parseInt(multiple) : 10;
	}

	public static int appearNumber(String srcText, String findText) {
		if (Objects.isNull(srcText) || Objects.isNull(findText))
			return 0;
		String[] arr = srcText.split(findText);
		int len = arr.length;
		if (srcText.endsWith(findText))
			len++;
		len = len - 1;
		return len;
	}

	/**
	 * 获取备用卡互转金额的最小值
	 *
	 * @return 备用互转最小金额，元
	 */
	public static int getReserveTOReserveMinAmount() {
		String reToReMin = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("RESERVE_TO_RESERVE_MINAMOUNT", "5000").trim();
		return StringUtils.isNumeric(reToReMin) ? Integer.parseInt(reToReMin) : 5000;
	}

	/**
	 * <li>入款、备用、客户绑定卡等是否中层内存合并</li>
	 * <li>1、合并给会员出款或下发时中层内存都合并</li>
	 * <li>0、不合并时给会员出款或下发时中层内存都不合并</li>
	 *
	 * @return true合并 false不合并
	 */
	public static boolean isMergeMidInner() {
		return "1".equals(MemCacheUtils.getInstance().getSystemProfile().getOrDefault("IS_MERGE_MIDINNER", "0"));
	}

	/**
	 * 触发备用卡金额互转余额阈值
	 *
	 * @return 余额阈值，元
	 */
	public static int getReserveToReserveBalance() {
		String reToReMin = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("RESERVE_TO_RESERVE_BALANCE", "20000").trim();
		return StringUtils.isNumeric(reToReMin) ? Integer.parseInt(reToReMin) : 20000;
	}

	public static List<String> getDiffrentList(List<String> list1, List<String> list2) {
		if (CollectionUtils.isEmpty(list1) || CollectionUtils.isEmpty(list2)) {
			return null;
		}
		long st = System.nanoTime();
		List<String> diff = new ArrayList<>();
		List<String> maxList = list1;
		List<String> minList = list2;
		if (list2.size() > list1.size()) {
			maxList = list2;
			minList = list1;
		}
		Map<String, Integer> map = new HashMap<>(maxList.size());
		for (String string : maxList) {
			map.put(string, 1);
		}
		for (String string : minList) {
			if (map.get(string) != null) {
				map.put(string, 2);
				continue;
			}
			diff.add(string);
		}
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			if (entry.getValue() == 1) {
				diff.add(entry.getKey());
			}
		}
		System.out.println("getDiffrentList total times " + (System.nanoTime() - st));
		return diff;

	}

	public static boolean isEnableCheckBank() {
		String enableCheckBank = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("ENABLE_CHECKBANK_ALLOCATE", "0");
		return "1".equalsIgnoreCase(enableCheckBank);
	}

	public static String getActiveAccFilterBank() {
		String active_acc_filter_bank = MemCacheUtils.getInstance().getSystemProfile().getOrDefault(
				"ACTIVE_ACC_FILTER_BANK",
				"'临商银行','包商银行','成都银行','南京银行','农业银行','工商银行','民生银行','交通银行','中信银行','柳州银行','兴业银行','中原银行','天府银行','北京农商','汉口银行','广发银行','威海市商业银行','桂林银行'");
		return active_acc_filter_bank;
	}

	public static int getMobileBeginIssuedPercent() {
		String issued_less_percent = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("MOBILE_BEGIN_ISSUED_LESS_PERCENT", "10");
		return Integer.parseInt(issued_less_percent);
	}

	/**
	 * 备用卡余额超过2万时主动分配任务的最低金额
	 *
	 * @return
	 */
	public static int getMinAmount4ReserveOver20K() {
		String min_amount_over20k = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("MIN_AMOUNT_FOR_RESERVE_OVER20K", "10000");
		return Integer.parseInt(min_amount_over20k);
	}

	/**
	 * 入款卡是否超过余额告警值才分配任务
	 *
	 * @return
	 */
	public static boolean getInbankOverLimitBeginAllocate() {
		String inbankOverLimit = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("INBANK_OVER_LIMITBALANCE_BEGIN_ALLOCATE", "1");
		return "1".equals(inbankOverLimit);
	}

	/**
	 * 入款卡正常分配任务的最大余额
	 *
	 * @return
	 */
	public static int getMaxBalanceForInbankCommonAllocate() {
		String maxBalance = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("MAXBALANCE_FOR_INBANK_COMMON_ALLOCATE", "15000");
		return Integer.parseInt(maxBalance);
	}

	/**
	 * 入款卡转备用卡的的最小余额
	 *
	 * @return
	 */
	public static int getMinBalanceForInbankToReserve() {
		String maxBalance = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("MINBALANCE_FOR_INBANK_TO_RESERVE", "50000");
		return Integer.parseInt(maxBalance);
	}

	/**
	 * 入款卡出款任务大小金额分界值
	 *
	 * @return
	 */
	public static int getMinTaskAmountForInbank() {
		String minTaskAmount = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("MIN_TASKAMOUNT_FOR_INBANK",
				"2000");
		return Integer.parseInt(minTaskAmount);
	}

	/**
	 * 余额超时时间
	 *
	 * @return
	 */
	public static long getBalanceTimeOut() {
		String balTmOut = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("BALANCE_TIMEOUT", "180000");
		return Long.parseLong(balTmOut);
	}

	/**
	 * 流水超时时间
	 *
	 * @return
	 */
	public static long getBankLogTimeOut() {
		String balTmOut = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("BANKLOG_TIMEOUT", "900000");
		return Long.parseLong(balTmOut);
	}

	/**
	 * 任务超时时间
	 *
	 * @return
	 */
	public static long getOutwardTaskTimeOut() {
		String balTmOut = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTWARDTASK_TIMEOUT", "600000");
		return Long.parseLong(balTmOut);
	}

	/**
	 * ProblemHandler处理周期
	 *
	 * @return
	 */
	public static long getProblemHandlerTime() {
		String probleTm = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("PROBLEM_HANDLER_TIME_CYCLE",
				"15000");
		return Long.parseLong(probleTm);
	}

	/**
	 * 支付宝入款卡开始转出余额
	 *
	 * @return
	 */
	public static int getMobileInbankBeginTransBalance() {
		String alipay_inbank_begintrans_balance = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("MOBILE_INBANK_BEGINTRANS_BALANCE", "10000");
		return Integer.parseInt(alipay_inbank_begintrans_balance);
	}

	/**
	 * APP允许与最新版本的最大差异
	 *
	 * @return
	 */
	public static int getMaxVersionDiff4Mobile() {
		String maxVerDiff = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("MAX_VERSION_DIFF_MOBILE", "5");
		return Integer.parseInt(maxVerDiff);
	}

	/**
	 * 下发卡超过限额或笔数时触发余额清零时的最低余额
	 *
	 * @return
	 */
	public static int getTriggerClearBalance4BindCommon() {
		String clearBalance = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("TRIGGER_CLEAR_BALANCE_BINDCOMMON", "1000");
		return Integer.parseInt(clearBalance);
	}

	/**
	 * 非下发卡超过限额或笔数时触发余额清零时的最低余额
	 *
	 * @return
	 */
	public static int getTriggerClearBalance4OtherCard() {
		String clearBalance = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("TRIGGER_CLEAR_BALANCE_OTHERCARD", "1000");
		return Integer.parseInt(clearBalance);
	}

	/**
	 * 下发/出款 fr,to 锁定 默认 true
	 */
	public static boolean getTransLockFromTo() {
		String p = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("SYS_TRANS_FROM_TO_LOCK", "1");
		return Objects.equals(p, "1");
	}

	public static String hideAccountAll(String account, String accountType) {
		if (StringUtils.isNotBlank(account)) {
			if (accountType != null) {
				if (accountType.equals("phone")) {
					// 手机
					account = account.substring(0, 3) + "**" + account.substring(account.length() - 3);
				} else {
					// 姓名
					if (account.length() >= 3) {
						account = account.substring(0, 1) + '*' + account.substring(2, account.length());
					} else {
						account = account.substring(0, 0) + '*' + account.substring(1, account.length());
					}
				}
			} else {
				// 账号
				if (account.length() >= 8) {
					account = account.substring(0, 4) + "****" + account.substring(account.length() - 4);
				} else if (account.length() >= 4) {
					account = account.substring(0, 2) + "********" + account.substring(account.length() - 2);
				} else if (account.length() >= 2) {
					account = account.substring(0, 1) + "**********" + account.substring(account.length() - 1);
				}
			}
		} else {
			account = "";
		}

		return account;
	}

	/**
	 * 描述 :获取入款账号下发(转出)到出款卡是能标识 INACCOUNT_TRANSFER_TO_OUTACCOUNT_FLAG 1 表示允许
	 * 其他值表示不允许
	 *
	 * @return true 表示可以 false 表示不可以
	 */
	public static boolean inAccountTransferToOutAccountFlag() {
		String p = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("INACCOUNT_TRANSFER_TO_OUTACCOUNT_FLAG",
				"1");
		return Objects.equals(p, "1");
	}

	/**
	 * 描述 :出入款新版本逻辑开关，0表示开启走新逻辑，1表示关闭走旧逻辑
	 *
	 * @return 0表示开启 表示关闭
	 */
	public static boolean transferFundsNewVersionLogic() {
		String p = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("TRANSFER_FUNDS_NEW_VERSION_LOGIC", "1");
		return Objects.equals(p, "1");
	}

	/**
	 * 描述 :入款卡出款信用额度百分比默认值
	 *
	 * @return
	 */
	public static int getInBankOutCreditPercentage() {
		String percentage = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("INBANK_OUT_DEFAULT_CREDIT_PERCENTAGE", "80");
		return Integer.parseInt(percentage);
	}

	/**
	 * 描述 :入款卡一笔出完预留手续费金额
	 *
	 * @return
	 */
	public static int getInBankOutReservedFees() {
		String fees = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("INBANK_OUT_RESERVED_FEES", "50");
		return Integer.parseInt(fees);
	}

	/**
	 * 描述 :非专注入款卡开启出款后达到最低余额关闭出款
	 *
	 * @return
	 */
	public static int getInBankCloseOutLowestBalance() {
		String lowestBalance = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("INBANK_CLOSE_OUT_LOWEST_BALANCE", "1000");
		return Integer.parseInt(lowestBalance);
	}

	/**
	 * 描述 :入款卡一键转出到PC出款卡的金额大小设置，默认值100000
	 *
	 * @return
	 */
	public static int balanceOfTransAllOut() {
		String balance = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("BALANCE_OF_TRANSALLOUT",
				"100000");
		return Integer.parseInt(balance);
	}

	/**
	 * 描述 :给出款卡补满余额时，所需金额低于设置的值不进行补满操作
	 *
	 * @return
	 */
	public static int outNeedMoneyLessValue() {
		String balance = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUT_NEEDMONEY_LESS_VALUE",
				"3000");
		return Integer.parseInt(balance);
	}

	/**
	 * 获取区分盘口是否走新逻辑的盘口ID字符串
	 *
	 * @return 区分盘口走新逻辑的盘口，以分号分隔
	 */
	public static String getDistHandicapNewVersion() {
		String enableHandicap = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("CHECK_DIST_HANDICAP_NEW_VERSION", ";;").trim();
		return !transferFundsNewVersionLogic() ? enableHandicap : "";
	}

	/**
	 * <p>
	 * <ul>
	 * <li>区分盘口走新逻辑，只有在CommonUtils.transferFundsNewVersionLogic()为false 的时候才有效</li>
	 * <p>
	 * <li>1、当值为ALL时，所有盘口都开启</li>
	 * <li>2、当值不为ALL时，各个盘口使用 ;(分号)分割，并且字符以分号开始和结束</li>
	 * </ul>
	 *
	 * @return true区分盘口 false不区分盘口
	 */
	public static boolean checkDistHandicapNewVersion(Integer handicap) {
		String enableHandicap = getDistHandicapNewVersion();
		return "ALL".equals(enableHandicap) || enableHandicap.contains(";" + handicap.toString() + ";");
	}

	/**
	 * 描述:检查某个盘口是否开启新公司入款通道
	 *
	 * @param handicapId
	 * @return
	 */
	public static boolean checkNewInComeEnabled(Integer handicapId) {
		String handicaps = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("NEW_INCOME_FLAG_HANDICAPID",
				"");
		final String separator = ";";
		boolean flag = false;
		if (ObjectUtils.isEmpty(handicaps)) {
			handicaps = SpringContextUtils.getBean(SysUserProfileServiceImpl.class)
					.findByPropertyKey(UserProfileKey.NEW_INCOME_FLAG_HANDICAPID.getValue()).get(0).getPropertyValue();
			if (ObjectUtils.isEmpty(handicaps)) {
				return flag;
			}
		}
		if (Arrays.asList(handicaps.split(separator)).stream().filter(p -> !ObjectUtils.isEmpty(p))
				.collect(Collectors.toList()).contains(handicapId.toString())) {
			flag = true;
		}
		log.debug("检查盘口id{},是否开启新公司入款:{}", handicapId, flag);
		return flag;
	}

	/**
	 * 描述:检查某个盘口是否开启第三方代付
	 *
	 * @param handicapId
	 * @return
	 */
	public static boolean checkOutwardThirdInsteadPayEnabled(Integer handicapId) {
		String handicaps = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTWARD_THIRD_INSTEAD_PAY", "");
		final String separator = ";";
		boolean flag = false;
		if (ObjectUtils.isEmpty(handicaps)) {
			handicaps = SpringContextUtils.getBean(SysUserProfileServiceImpl.class)
					.findByPropertyKey(UserProfileKey.OUTWARD_THIRD_INSTEAD_PAY.getValue()).get(0).getPropertyValue();
			if (ObjectUtils.isEmpty(handicaps)) {
				return flag;
			}
		}
		if (Arrays.asList(handicaps.split(separator)).stream().filter(p -> !ObjectUtils.isEmpty(p))
				.collect(Collectors.toList()).contains(handicapId.toString())) {
			flag = true;
		}
		log.debug("检查盘口id{},是否开启第三方代付:{}", handicapId, flag);
		return flag;
	}

	/**
	 * 获取区分盘口分配出款入款的盘口ID字符串
	 *
	 * @return 区分盘口出款入款的盘口，以分号分隔
	 */
	public static boolean checkDistHandicapAllocateOutAndIn(Integer handicap) {
		if (!checkDistHandicapNewVersion(handicap)) {
			return false;
		}
		String enableHandicap = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("DIST_HANDICAP_ALLOCATE_OUTANDIN", ";;").trim();
		return "ALL".equals(enableHandicap) || enableHandicap.contains(";" + handicap.toString() + ";");
	}

	/**
	 * 描述 :非专注入款卡切换下发模式，判断入款卡通道入款卡的数量值
	 *
	 * @return
	 */
	public static int inBankChangeBindCommModelCount() {
		String balance = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("INBANK_CHANGE_BINDCOMM_MODEL_COUNT", "3");
		return Integer.parseInt(balance);
	}

	/**
	 * 描述 :入款单用于计算额度的过期时间，单位：分钟
	 *
	 * @return
	 */
	public static int getAccountInComeExpireTime() {
		String balance = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("ACCOUNT_INCOME_EXPIRE_TIME",
				"15");
		return Integer.parseInt(balance);
	}

	/**
	 * 描述 :边入边出、先入后出入款卡超过信用额度百分比
	 *
	 * @return
	 */
	public static int getAccountInComeOverCredit() {
		String balance = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("ACCOUNT_INCOME_OVER_CREDIT",
				"20");
		return Integer.parseInt(balance);
	}

	/**
	 * 描述 :是否开启从缓存中取session数据
	 *
	 * @return
	 */
	public static boolean getSessionFromCache() {
		String cache = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("GET_SESSIONINFO_FROM_CACHE", "1");
		return "1".equalsIgnoreCase(cache);
	}

	/**
	 * 描述 :获取连续转账失败不再分配下发或出款任务配置
	 *
	 * @return
	 */
	public static int getFailureTransCount() {
		String counts = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("COUNT_FAILURE_TRANS", "3");
		return Integer.parseInt(counts);
	}

	/**
	 * <p>
	 * <ul>
	 * <li>出款任务银行类型关键词过滤，例如：信用社、农村商业等关键词，包含其中关键词的直接给人工出款</li>
	 * <p>
	 * <li>1、各个关键词使用 ;(分号)分割，并且字符以分号开始和结束</li>
	 * </ul>
	 *
	 * @return true包含 false不包含
	 */
	public static boolean checkOutWardBankTypeKeywordsFilter(String bankType) {
		String keywords = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("OUTWARD_BANKTYPE_KEYWORD_FILTER", "信用社;农村商业").trim();
		for (String keyword : keywords.split(";")) {
			if (bankType.toString().contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * <p>
	 * <ul>
	 * <li>设定的给出款卡补钱少于一定金额时，不给补钱</li>
	 * <p>
	 * </ul>
	 *
	 * @return
	 */
	public static Integer getLessEnactmentOutAmount() {
		String needMoney = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("LESS_ENACTMENT_OUT_AMOUNT",
				"5000");
		return Integer.parseInt(needMoney);
	}

	/**
	 * <p>
	 * <ul>
	 * <li>超过指定任务金额时，返利网余额不足时仍能分配</li>
	 * <p>
	 * </ul>
	 *
	 * @return
	 */
	public static Integer getTaskAmountCanAllocatToMobile() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("TASK_AMOUNT_CAN_ALLOCATE_TO_MOBILE",
				"20000");
		return Integer.parseInt(count);
	}

	/**
	 * 出款卡余额设置（用于判断同区域同层级中有多少张出款卡达到要求）
	 * @return
	 */
	public static Integer getThirdToOutMoreBalance() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("THIRD_TO_OUT_MORE_BALANCE",
				"20000");
		return Integer.parseInt(count);
	}

	/**
	 * 出款卡缺少的金额设置（用于判断缺多少钱给第三方下发）
	 * @return
	 */
	public static Integer getThirdToOutLessBalance() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("THIRD_TO_OUT_LESS_BALANCE",
				"20000");
		return Integer.parseInt(count);
	}

	/**
	 * 出款卡余额设置（用于判断当前出款卡的余额低于设定值给第三方下发）
	 * @return
	 */
	public static Integer getThirdToOutBelowBalance() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("THIRD_TO_OUT_BELOW_BALANCE",
				"1000");
		return Integer.parseInt(count);
	}

	/**
	 * 外层的满足条件的出款卡数量超过设定值给第三方下发
	 * @return
	 */
	public static Integer getThirdToOutOutterAmount() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("THIRD_TO_OUT_OUTTER_AMOUNT",
				"10");
		return Integer.parseInt(count);
	}

	/**
	 * 内层的满足条件的出款卡数量超过设定值给第三方下发
	 * @return
	 */
	public static Integer getThirdToOutInterAmount() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("THIRD_TO_OUT_INTER_AMOUNT",
				"15");
		return Integer.parseInt(count);
	}

	/**
	 * 内层的满足条件的出款卡数量超过设定值给第三方下发
	 * @return
	 */
	public static double getIssuedToOutExceedCreditsPercentage() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("ISSUED_TO_OUT_EXCEED_CREDITS_PERCENTAGE",
				"2");
		return Double.valueOf(count);
	}

	/**
	 * <p>
	 * <ul>
	 * <li>返利网卡余额小于任务金额时，差额最小值</li>
	 * <p>
	 * </ul>
	 *
	 * @return
	 */
	public static Integer getTaskAmountMinSubAmtMobile() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("TASK_AMOUNT_MIN_SUBAMT_MOBILE",
				"1000");
		return Integer.parseInt(count);
	}

	/**
	 * <p>
	 * <ul>
	 * <li>返利网卡余额小于任务金额时，差额最大值</li>
	 * <p>
	 * </ul>
	 *
	 * @return
	 */
	public static Integer getTaskAmountMaxSubAmtMobile() {
		String count = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("TASK_AMOUNT_MAX_SUBAMT_MOBILE",
				"5000");
		return Integer.parseInt(count);
	}

	/**
	 * <p>
	 * <ul>
	 * <li>获取不可用于入款的银行卡类型</li>
	 * <p>
	 * </ul>
	 *
	 * @return
	 */
	public static String getForbiddenUseAsInBank() {
		String inBankType = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("FORBIDDEN_USE_AS_INBANK",
				",,");
		return inBankType;
	}

	/**
	 * 需低于日出款限额金额
	 *
	 * @return
	 */
	public static int getLessThenSumDailyOutward(){
		String lessdailyout = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("LESS_THEN_SUM_DAILY_OUTWARD",
				"10000");
		return Integer.parseInt(lessdailyout);
	}

	/**
	 * 需低于日入款限额金额
	 *
	 * @return
	 */
	public static int getLessThenSumDailyIncome(){
		String lessdailyin = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("LESS_THEN_SUM_DAILY_INCOME",
				"10000");
		return Integer.parseInt(lessdailyin);
	}

	/**
	 * 出款任务是否按流水均匀分配
	 * 0-关闭 1-开启(默认：0-关闭)
	 * @return
	 */
	public static boolean outwardIsBankLogUniformAllocation(){
		return "0".equals(MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTWARD_IS_BANKLOG_UNIFORM_ALLOCATION", "0"));
	}

	/**
	 * 出款任务轮次后固定额度
	 * 默认-20000
	 * @return
	 */
	public static int outwardFixedAmount(){
		String fixedAmount = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTWARD_FIXED_AMOUNT",
				"20000");
		return Integer.parseInt(fixedAmount);
	}

	/**
	 * 出款任务是否要预留给余额多的入款卡
	 * 默认-false
	 * @return
	 */
	public static boolean reserveTask(Integer handicap){
		String enableHandicap = MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault("INBANK_RESERVE_OUTWARD_HANDICAP", ";;").trim();
		return "ALL".equals(enableHandicap) || enableHandicap.contains(";" + handicap.toString() + ";");
	}

	/**
	 * 本次下发金额 按千位进位 需求 7453
	 *
	 * @param amount
	 * @return
	 */
	public static BigDecimal wrapDrawAmount(BigDecimal amount) {
		if (null == amount)
			return BigDecimal.ZERO;
		BigDecimal bigDecimal_500 = new BigDecimal("500.00");
		if(amount.compareTo(bigDecimal_500)<0)
			return BigDecimal.ZERO;
		BigDecimal bigDecimal_1000= new BigDecimal("1000.00");
		BigDecimal[] divide =  amount.divideAndRemainder(bigDecimal_1000);
		if(divide[1].compareTo(bigDecimal_500)>=0){
			divide[0] = divide[0].add(BigDecimal.ONE);
		}
		return divide[0].multiply(bigDecimal_1000);
	}
}
