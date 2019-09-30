package com.xinbo.fundstransfer.domain.enums;

/**
 * System profile
 *
 *
 */
public enum UserProfileKey {
	/**
	 * 云闪付（边入边出）卡每日入款限额（元）
	 */
	Income_YSF_OneDay_Limit("Income_YSF_OneDay_Limit"),
	/**
	 * 机器/出款人员对应审核最大额度
	 */
	OUTDRAW_MONEYLIMIT("OUTDRAW_SYSMONEYLIMIT"),
	/**
	 * 入款审核人员对应审核最大额度
	 */
	INCOME_AUDITLIMIT("INCOME_SYSAUDITLIMIT"),
	/**
	 * 出款款审核人员对应审核最大额度
	 */
	OUTDRAW_AUDITLIMIT("OUTDRAW_SYSAUDITLIMIT"),
	/**
	 * 当日入款额度
	 */
	INCOME_LIMIT_CHECKIN_TODAY("INCOME_LIMIT_CHECKIN_TODAY"),
	/**
	 * 当日出款额度
	 */
	OUTDRAW_LIMIT_CHECKOUT_TODAY("OUTDRAW_LIMIT_CHECKOUT_TODAY"),
	/**
	 * 单笔出款额度
	 */
	OUTDRAW_LIMIT_OUT_ONE("OUTDRAW_LIMIT_OUT_ONE"),

	/**
	 * 出款审批额度,大于此设置即大额出款
	 */
	OUTDRAW_LIMIT_APPROVE("OUTDRAW_LIMIT_APPROVE"),

	/**
	 * 出款拆单金额
	 */
	OUTDRAW_MAX_OUT_SPLIT_BILL("OUTDRAW_MAX_SPLIT_BILL"),

	/**
	 * 出款审核几倍打码量限制
	 */
	OUTDRAW_CHECK_CODE("OUTDRAW_CHECK_CODE"),

	/**
	 * 银行流水匹配浮动比例（%）
	 */
	INCOME_OFFSET_PERCENT("INCOME_OFFSET_PERCENT"),
	/**
	 * 入款审核人员分配账号最大数目
	 */
	INCOME_ACCOUNTS_PERUSER("INCOME_ACCOUNTS_PERUSER"),

	/**
	 * 冻结卡有效流水期限（天）
	 */
	FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY("FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY"),
	/**
	 * 流水抓取超时时间（分钟）
	 */
	INCOME_LIMIT_MONITOR_TIMEOUT("INCOME_LIMIT_MONITOR_TIMEOUT"),
	/**
	 * 出入款账号余额告警值
	 */
	FINANCE_ACCOUNT_BALANCE_ALARM("FINANCE_ACCOUNT_BALANCE_ALARM"),

	/**
	 * 出款卡最低余额，低于此设置通知下发人员（元）
	 */
	OUTDRAW_SYSMONEY_LOWEST("OUTDRAW_SYSMONEY_LOWEST"),

	/**
	 * 入款取消时间，超过将取消入款请求（小时）
	 */
	INCOME_LIMIT_REQUEST_CANCEL("INCOME_LIMIT_REQUEST_CANCEL"),

	/**
	 * 停止派单时间
	 */
	OUTDRA_HALT_ALLOC_START_TIME("OUTDRA_HALT_ALLOC_START_TIME"),

	/**
	 * 正在维护的银行列表 英文逗号隔开
	 */
	OUTDRAW_SYS_MAINTAIN_BANKTYPE("OUTDRAW_SYS_MAINTAIN_BANKTYPE"),

	/**
	 * 同行转账列表 英文逗号隔开
	 */
	OUTDRAW_SYS_PEER_TRANSFER("OUTDRAW_SYS_PEER_TRANSFER"),

	/**
	 * 所有银行类型列表，英文分号;隔开
	 */
	OUTDRAW_SYS_ALL_BANKTYPE("OUTDRAW_SYS_ALL_BANKTYPE"),
	/**
	 * 外层拆单金额
	 */
	OUTDRAW_SPLIT_AMOUNT_OUTSIDE("OUTDRAW_SPLIT_AMOUNT_OUTSIDE"),
	/**
	 * 内层拆单金额
	 */
	OUTDRAW_SPLIT_AMOUNT_INSIDE("OUTDRAW_SPLIT_AMOUNT_INSIDE"),

	/**
	 * 第三方出款最低金额
	 */
	OUTDRAW_THIRD_LOWEST_BAL("OUTDRAW_THIRD_LOWEST_BAL"),

	/**
	 * 第三方下发到出款卡锁定的过期时间
	 */
	OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME("OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME"),

	/**
	 * 第三方代付限额
	 */
	DAIFU_AMOUNT_UPLIMIT("DAIFU_AMOUNT_UPLIMIT"),

	/**
	 * 收款账号,用于转账测试的收款
	 */
	TRANSFER_TEST_ACCOUNT("TRANSFER_TEST_ACCOUNT"),
	/**
	 * 开户行,用于转账测试的收款
	 */
	TRANSFER_TEST_BANKNAME("TRANSFER_TEST_BANKNAME"),
	/**
	 * 收款人,用于转账测试的收款
	 */
	TRANSFER_TEST_OWNER("TRANSFER_TEST_OWNER"),
	/**
	 * 转账金额,用于转账测试的收款
	 */
	TRANSFER_TEST_AMOUNT("TRANSFER_TEST_AMOUNT"),

	/**
	 * ACK数据过期时间,保存用于对账的ACK数据的过期时间，单位分钟
	 */
	ACK_RESULT_EXPIRE_TIME("ACK_RESULT_EXPIRE_TIME"),

	/**
	 * 任务分配策略<br>
	 * <code>AppConstants.OUTTASK_ALLOCATE_AMT_FIRST</code><br>
	 * <code>AppConstants.OUTTASK_ALLOCATE_PC_FIRST</code><br>
	 * <code>AppConstants.OUTTASK_ALLOCATE_MOBILE_FIRST</code><br>
	 * <code>AppConstants.OUTTASK_ALLOCATE_ONLY_PC</code><br>
	 * <code>AppConstants.OUTTASK_ALLOCATE_ONLY_MOBILE</code><br>
	 * <code>AppConstants.OUTTASK_ALLOCATE_TO_MANULE</code><br>
	 */
	OUTWARD_TASK_STRATEGY("OUTWARD_TASK_ALLOCATE_STRATEGY"),

	/**
	 * 是否开启入款卡出款
	 */
	ALLOCATE_OUTWARD_TASK_ENABLE_INBANK("ALLOCATE_OUTWARD_TASK_ENABLE_INBANK"),

	/**
	 * 入款卡出款 的盘口
	 */
	ENABLE_INBANK_ALLOCATE_OUTWARD_HANDICAP("ENABLE_INBANK_ALLOCATE_OUTWARD_HANDICAP"),
	/**
	 * 马尼拉地区盘口，每个盘口ID后拼接一个英文分号
	 */
	HANDICAP_MANILA_ZONE("HANDICAP_MANILA_ZONE"),
	/**
	 * 台湾地区盘口，每个盘口ID后拼接一个英文分号
	 */
	HANDICAP_TAIWAN_ZONE("HANDICAP_TAIWAN_ZONE"),
	/**
	 * 已开启新公司入款的盘口，每个盘口ID后拼接一个英文分号
	 */
	NEW_INCOME_FLAG_HANDICAPID("NEW_INCOME_FLAG_HANDICAPID"),

	/**
	 * 已开启第三方代付的盘口，每个盘口ID后拼接一个英文分号
	 */
	OUTWARD_THIRD_INSTEAD_PAY("OUTWARD_THIRD_INSTEAD_PAY"),
	/**
	 * 下发是否考参流水抓取时间 开关 1：参考；2： 不参考
	 */
	PC_FLOW_TIME_FACTOR("PC_FLOW_TIME_FACTOR"),

	/**
	 * 返利网返利设置
	 */
	REBATE_SYS_RATE_SETTING("REBATE_SYS_RATE_SETTING"),

	/**
	 * 返利网代理返利设置啊
	 */
	REBATE_AGENT_SYS_RATE_SETTING("REBATE_AGENT_SYS_RATE_SETTING"),
	/**
	 * 返利网微信返利设置啊
	 */
	REBATE_WX_SYS_RATE_SETTING("REBATE_WX_SYS_RATE_SETTING"),
	/**
	 * 返利网支付宝返利设置啊
	 */
	REBATE_ZFB_SYS_RATE_SETTING("REBATE_ZFB_SYS_RATE_SETTING"),

	/**
	 * 返利网每日佣金计算 日期啊
	 */
	RECORD_TIME_4_DAILY_COMMISSION("RECORD_TIME_4_DAILY_COMMISSION"),

	/**
	 * 手机银行余额低于信用额度百分比触发下发（%）
	 */
	MOBILE_BEGIN_ISSUED_LESS_PERCENT("MOBILE_BEGIN_ISSUED_LESS_PERCENT"),

	/**
	 * 返利网同步来入款卡的默认值：入款卡是否可用作出款（是否可用做出款 1可用/0不可用）
	 */
	FLW_DEFAULT_INCOME_CAN_USE_OUT_ENBLED("FLW_DEFAULT_INCOME_CAN_USE_OUT_ENBLED"),

	/**
	 * 返利网同步来入款卡的默认值： 当余额达到信用额度的此百分比时触发操作
	 * 不可出款：保留FLW_DEFAULT_INCOME_TRANSFER_BALANCE元做手续费，其它一笔转出到出款卡 可用出款：自动转去做出款卡
	 */
	FLW_DEFAULT_INCOME_PERCENTAGE_PEAKBALANCE("FLW_DEFAULT_INCOME_PERCENTAGE_PEAKBALANCE"),

	/**
	 * 返利网同步来入款卡的默认值：不可用做出款的卡，余额达标时保留此值的余额，剩余一笔转出到出款卡
	 */
	FLW_DEFAULT_INCOME_TRANSFER_BALANCE("FLW_DEFAULT_INCOME_PERCENTAGE_TRANSFER_BALANCE"),

	/**
	 * 出入款新版本逻辑开关，开启走新逻辑，关闭走旧逻辑
	 */
	TRANSFER_FUNDS_NEW_VERSION_LOGIC("TRANSFER_FUNDS_NEW_VERSION_LOGIC"),

	/**
	 * 入款卡出款信用额度百分比默认值
	 */
	INBANK_OUT_DEFAULT_CREDIT_PERCENTAGE("INBANK_OUT_DEFAULT_CREDIT_PERCENTAGE"),

	/**
	 * 入款卡一笔出完预留手续费金额
	 */
	INBANK_OUT_RESERVED_FEES("INBANK_OUT_RESERVED_FEES"),

	/**
	 * 非专注入款卡开启出款后达到最低余额关闭出款
	 */
	INBANK_CLOSE_OUT_LOWEST_BALANCE("INBANK_CLOSE_OUT_LOWEST_BALANCE"),

	/**
	 * 入款卡一键转出到PC出款卡的金额大小设置，默认值100000
	 */
	BALANCE_OF_TRANSALLOUT("BALANCE_OF_TRANSALLOUT"),

	/**
	 * 给出款卡补满余额时，所需金额低于设置的值不进行补满操作
	 */
	OUT_NEEDMONEY_LESS_VALUE("OUT_NEEDMONEY_LESS_VALUE"),

	/**
	 * 区分盘口是否走新逻辑
	 */
	CHECK_DIST_HANDICAP_NEW_VERSION("CHECK_DIST_HANDICAP_NEW_VERSION"),

	/**
	 * 是否区分盘口出款和入款
	 */
	DIST_HANDICAP_ALLOCATE_OUTANDIN("DIST_HANDICAP_ALLOCATE_OUTANDIN"),

	/**
	 * 非专注入款卡切换下发模式，判断入款卡通道入款卡的数量值
	 */
	INBANK_CHANGE_BINDCOMM_MODEL_COUNT("INBANK_CHANGE_BINDCOMM_MODEL_COUNT"),

	/**
	 * APP预升级的手机号
	 */
	APP_PRE_UPDATE_MOBILES("APP_PRE_UPDATE_MOBILES"),

	/**
	 * 出款任务银行类型关键词过滤
	 */
	OUTWARD_BANKTYPE_KEYWORD_FILTER("OUTWARD_BANKTYPE_KEYWORD_FILTER")

	/**
	 * 内层的满足条件的出款卡数量超过设定值给第三方下发（张）
	 */
	, THIRD_TO_OUT_INTER_AMOUNT("THIRD_TO_OUT_INTER_AMOUNT")
	/**
	 * 出款卡余额设置,判断同区域同层级中达到要求的出款卡张数（元）
	 */
	, THIRD_TO_OUT_MORE_BALANCE("THIRD_TO_OUT_MORE_BALANCE")
	/**
	 * 出款卡缺少的金额设置,用于判断缺多少钱给第三方下发（元）
	 */
	, THIRD_TO_OUT_LESS_BALANCE("THIRD_TO_OUT_LESS_BALANCE")
	/**
	 * 外层的满足条件的出款卡数量超过设定值给第三方下发（张）
	 */
	, THIRD_TO_OUT_OUTTER_AMOUNT("THIRD_TO_OUT_OUTTER_AMOUNT")
	/**
	 * 出款卡余额设置,判断出款卡的余额低于设定值给第三方下发（元）
	 */
	, THIRD_TO_OUT_BELOW_BALANCE("THIRD_TO_OUT_BELOW_BALANCE")
	/**
	 * 下发给出款卡的钱允许超过该卡的信用额度百分比（%）
	 */
	, ISSUED_TO_OUT_EXCEED_CREDITS_PERCENTAGE("ISSUED_TO_OUT_EXCEED_CREDITS_PERCENTAGE")

	/**
	 * 返利网设置同时升级工具客户端个数
	 */
	, APP_TOOLS_MAX_UPGRADE_QUANTITY("APP_TOOLS_MAX_UPGRADE_QUANTITY")

	/**
	 * 支付宝入款配置(AliIncomeConfig)
	 */
	, INCOME_ALI_CONFIG("INCOME_ALI_CONFIG")

	/**
	 * 支付宝出款配置(AliOutConfig)
	 */
	,OUT_ALI_CONFIG("OUT_ALI_CONFIG")


	/**
	 * 聊天室支付-是否入款分配给-平台会员
	 */
	,CHAT_PAY_INMONEY_ASSIGN_PLANTUSER("CHAT_PAY_INMONEY_ASSIGN_PLANTUSER")

	/**
	 * 聊天室支付-是否入款分配给-返利网兼职
	 */
	,CHAT_PAY_INMONEY_ASSIGN_REBATEUSER("CHAT_PAY_INMONEY_ASSIGN_REBATEUSER")

	/**
	 * 聊天室支付-是否出款分配给-平台会员
	 */
	,CHAT_PAY_OUTMONEY_ASSIGN_PLANTUSER("CHAT_PAY_OUTMONEY_ASSIGN_PLANTUSER")

	/**
	 * 聊天室支付-是否出款分配给-返利网兼职
	 */
	,CHAT_PAY_OUTMONEY_ASSIGN_REBATEUSER("CHAT_PAY_OUTMONEY_ASSIGN_REBATEUSER")


	/**
	 * 聊天室支付-入款分配-匹配区域(兼职和会员)
	 */
	,CHAT_PAY_INMONEY_MATCHING_WITH_ZONE("CHAT_PAY_INMONEY_MATCHING_WITH_ZONE")

	/**
	 * 聊天室支付-入款分配-匹配层级(兼职和会员)
	 */
	,CHAT_PAY_INMONEY_MATCHING_WITH_LEVEL("CHAT_PAY_INMONEY_MATCHING_WITH_LEVEL")


	/**
	 * 聊天室支付-出款分配-匹配区域(兼职和会员)
	 */
	,CHAT_PAY_OUTMONEY_MATCHING_WITH_ZONE("CHAT_PAY_OUTMONEY_MATCHING_WITH_ZONE")


	/**
	 * 聊天室支付-出款分配-匹配区域(兼职和会员)
	 */
	,CHAT_PAY_OUTMONEY_MATCHING_WITH_LEVEL("CHAT_PAY_OUTMONEY_MATCHING_WITH_LEVEL")



	/**
	 * 聊天室支付-没进入聊天室超时-全单取消
	 */
	,CHAT_PAY_NOT_LOGIN_ROOM_TIME_OUT("CHAT_PAY_NOT_LOGIN_ROOM_TIME_OUT")











	;

	private String value;

	UserProfileKey(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

}
