package com.xinbo.fundstransfer.component.redis;

/**
 * 订阅事件主题定义
 *
 *
 */
public class RedisTopics {
	/**
	 * 刷新参加活动的account缓存
	 */
	public static final String REBAT_USER_ACTIVITY_TOPIC = "rebat_user_activity_topic";

	/**
	 * 刷新入款账号-云闪付账号和登陆密码缓存
	 */
	public static final String FRESH_INACCOUNT_YSFLOGIN_CACHE = "fresh_in_account_ysflogin_cache";
	/**
	 * 刷新入款账号缓存
	 */
	public static final String FRESH_INACCOUNT_CACHE = "fresh_in_account_cache";
	/**
	 * 第三方账号下发 channel topic
	 */
	public static final String FRESH_ACCOUNT_THIRDDRAW = "FRESH_ACCOUNT_THIRDDRAW";
	/**
	 * 公司入款请求
	 */
	public static final String INCOME_REQUEST = "INCOME_REQUEST";
	/**
	 * 第三方入款请求
	 */
	public static final String INCOME_THIRD_REQUEST = "INCOME_THIRD_REQUEST";
	/**
	 * 出款请求
	 */
	public static final String OUTWARD_REQUEST = "OUTWARD_REQUEST";
	/**
	 * 工具状态上报
	 */
	public static final String TOOLS_STATUS_REPORT = "TOOLS_STATUS_REPORT";
	/**
	 * 入款审核账号分配
	 */
	public static final String ACCOUNT_ALLOCATED = "ACCOUNT_ALLOCATED";
	/**
	 * 入款审核账号分配
	 */
	public static final String ACCOUNT_ALLOCATING = "ACCOUNT_ALLOCATING";
	/**
	 * 关闭WebSocket
	 */
	public static final String CLOSE_WEBSOCKET = "CLOSE_WEBSOCKET";
	/**
	 * 出款任务分配队列
	 */
	public static final String ALLOCATING_OUTWARD_TASK = "ALLOCATING_OUTWARD_TASK";

	/**
	 * 出款任务停止接单广播
	 */
	public static final String ALLOC_OUT_TASK_SUSPEND = "ALLOC_OUT_TASK_SUSPEND";

	// -------------------------------------- GeneralListenerAdapter Topic
	// Define
	/**
	 * 系统通知，系统业务消息
	 */
	public static final String BROADCAST = "BROADCAST";
	/**
	 * 向工具推送消息
	 */
	public static final String PUSH_MESSAGE_TOOLS = "PUSH_MESSAGE_TOOLS";
	/**
	 * 刷新系统菜单缓存
	 */
	public static final String REFRESH_MENUPERMISSION = "REFRESH_MENUPERMISSION";
	/**
	 * 刷新系统设置缓存
	 */
	public static final String REFRESH_SYSTEM_PROFILE = "REFRESH_SYSTEM_PROFILE";
	/**
	 * 刷新层级
	 */
	public static final String REFRESH_LEVEL = "REFRESH_LEVEL";
	/**
	 * 刷新帐号
	 */
	public static final String REFRESH_ACCOUNT = "REFRESH_ACCOUNT";

	public static final String REFRESH_ACCOUNT_MORE = "REFRESH_ACCOUNT_MORE";
	/**
	 * 刷新兼职用户信息
	 */

	public static final String REFRESH_REBATE_USER = "REFRESH_REBATE_USER";
	/**
	 * 刷新用户
	 */
	public static final String REFRESH_USER = "REFRESH_USER";
	/**
	 * 系统重新启动
	 */
	public static final String SYS_REBOOT = "SYS_REBOOT";
	/**
	 * 删除截图
	 */
	public static final String DELETED_SCREENSHOTS = "DELETED_SCREENSHOTS";
	/**
	 * 重新加载handicap 到 Local Cache
	 */
	public static final String REFRESH_ALL_HANDICAP = "REFRESH_ALL_HANDICAP";

	/**
	 * 重新加载所有系统设置 到 Local Cache
	 */
	public static final String REFRESH_ALL_SYS_SETTING = "REFRESH_ALL_SYS_SETTING";
	/**
	 * 转待排查的时候 分配待排查任务消息
	 */
	public static final String ASIGN_REVIEWTASK_TOPIC = "ASIGN_REVIEWTASK_TOPIC";

	/**
	 * 批量刷新帐号
	 */
	public static final String REFRESH_ACCOUNT_LIST = "REFRESH_ACCOUNT_LIST";

	/**
	 * 出款任务分配：层级合并
	 */
	public static final String REFRESH_OTASK_MERGE_LEVEL = "OTASK_MERGE_LEVEL";

	/**
	 * 删除问题反馈截图
	 */
	public static final String DELETED_FEEDBACK_SCREENSHOTS = "DELETED_FEEDBACK_SCREENSHOTS";
	/**
	 * 入款接单分配消息
	 */
	public static final String ASSIGN_INCOMEAWACCOUNT_TOPIC = "ASSIGN_INCOMEAWACCOUNT_TOPIC";
	/**
	 * 已分配入款接单 刷新页面消息
	 */
	public static final String ASSIGNED_INCOMEAWACCOUNT_TOPIC = "ASSIGNED_INCOMEAWACCOUNT_TOPIC";
	/**
	 * 刷新用户拥有数据权限的盘口信息
	 */
	public static final String REFRESH_USER_HANDICAP_PERMISSION_TOPIC = "REFRESH_USER_HANDICAP_PERMISSION_TOPIC";

	public static final String ACCOUNT_CHANGE_BROADCAST = "ACCOUNT_CHANGE_BROADCAST";

	public static final String REBATE_BANK_LOGS = "REBATE_BANK_LOGS";

	public static final String REBATE_AUDIT_ACCS = "REBATE_AUDIT_ACCS";

	public static final String REBATE_UP_LIMIT = "REBATE_UP_LIMIT";

	public static final String REBATE_DAY = "REBATE_DAY";

	public static final String ACCOUNT_MORE_CLEAN = "ACCOUNT_MORE_CLEAN";

	public static final String SYS_BAL_RPUSH = "SYS_BAL_RPUSH";

	public static final String REBATE_RECHARGE_LIMIT = "REBATE_RECHARGE_LIMIT";

	// blake start
	/**
	 * 银联云闪付-银行卡使用时间
	 */
	public static final String YSF_BANK_ACCOUNT_USE_TIME = "YSF_BANK_ACCOUNT_USE_TIME";
	/**
	 * 银联云闪付-收到二维码-消息广播
	 */
	public static final String YSF_QR_CODE_MSG = "YSF_QR_CODE_MSG";

	// blake end

	public static final String OTHER_ACCOUNT_CLEAN = "OTHER_ACCOUNT_CLEAN";

	/**
	 * 上报时，删除分配的账号或人员id
	 */
	public static final String DEL_ALLOCATEDID_AFTER_TRANSACK = "DEL_ALLOCATEDID_AFTER_TRANSACK";

	public static final String REBATE_USER_CLEAN = "REBATE_USER_CLEAN";

	/**
	 * 上报时银行流水处理
	 */
	public static final String BANK_STATEMENT = "BANK_STATEMENT";

	/**
	 * 返利网审核佣金数据
	 */
	public static final String REBATE_DATA = "Rebate_Data";
}
