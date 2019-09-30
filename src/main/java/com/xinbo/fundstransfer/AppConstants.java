package com.xinbo.fundstransfer;

public class AppConstants {
	/**
	 * 老系统盘口
	 */
	// public static final String OLD_PLATFORM_HANDICAP = "cp699,wcp,ysc";
	/**
	 * 分页每页显示条数
	 */
	public static final int PAGE_SIZE = 10;
	/**
	 * 导出时用，每页条数最大限制
	 */
	public static final int PAGE_SIZE_MAX = 99999999;
	/**
	 * token 有效期7*24小时，转换毫秒7*24*3600*1000,单位毫秒
	 */
	public static final int TOKEN_EXPIRES = 604800000;

	/**
	 * 动态查询，参数前缀
	 */
	public final static String SEARCH_PREFIX = "search_";
	/**
	 * 正则表达式--电话
	 */
	public static final String REGEXP_PHONE = "^(\\d{3,4}-)\\d{7,8}$";
	/**
	 * 正则表达式--手机号码
	 */
	public static final String REGEXP_MOBILE = "^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$";

	public static final int USER_ID_4_ADMIN = 1;

	public static final int USER_ID_1_ADMIN = -1;

	public static final String DEFAULT_PASSWORD = "a12345678";

	public static final int DAYS_OF_NO_OWNER = 1;

	public static final String HISTORY_WS = "History";

	public static final String LOGOUT_WS = "Logout";

	public static ThreadLocal<Integer> THREADLOCAL_USER_ID = new ThreadLocal<>();

	/**
	 * 往平台HTTP请求异常重连间隔，单位：秒
	 */
	public static final int RECONNECTION_INTERVAL = 60;
	/**
	 * 往平台HTTP请求异常后，重连次数
	 */
	public static final int RECONNECTION_TIMES = 2;

	/**
	 * Shiro Session Id
	 */
	public static final String JSESSIONID = "JSESSIONID";

	/**
	 * 测试环境版本号
	 */
	public static final String TEST_VERSION = "v1.0";

	/**
	 * 是否开启responseDataNewPay != null && responseDataNewPay.getCode() == 200出款
	 */
	public static final boolean OUTDREW_THIRD = true;

	/**
	 * 新下发开启开关
	 */
	public static final boolean NEW_TRANSFER = true;

	/**
	 * CAI99
	 */
	public static final String EXCLUSIVE_HANDICAP = ",199,";

	public static final String ZONE_MANILA = "MANILA";

	public static final String ZONE_TAIWAN = "TAIWAN";

	/**
	 * 合并中层和内层
	 */
	public static final boolean MERGE_MID_INNER = true;

	/**
	 * 任务为ROBOT_OUT_YES时，按金额优先分配
	 */
	public static final String OUTTASK_ALLOCATE_AMT_FIRST = "0";
	/**
	 * 任务为ROBOT_OUT_YES时，优先分配给PC
	 */
	public static final String OUTTASK_ALLOCATE_PC_FIRST = "1";
	/**
	 * 任务为ROBOT_OUT_YES时，优先分配给手机
	 */
	public static final String OUTTASK_ALLOCATE_MOBILE_FIRST = "2";
	/**
	 * 任务为ROBOT_OUT_YES时，仅分配给PC
	 */
	public static final String OUTTASK_ALLOCATE_ONLY_PC = "3";
	/**
	 * 任务为ROBOT_OUT_YES时，仅分配给手机
	 */
	public static final String OUTTASK_ALLOCATE_ONLY_MOBILE = "4";
	/**
	 * 任务为ROBOT_OUT_YES时，分配给人工
	 */
	public static final String OUTTASK_ALLOCATE_TO_MANULE = "5";
}
