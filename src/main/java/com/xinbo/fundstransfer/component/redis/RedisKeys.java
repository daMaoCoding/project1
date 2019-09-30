package com.xinbo.fundstransfer.component.redis;

import com.xinbo.fundstransfer.CommonUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class RedisKeys {
	/**
	 * 手机端上报账号暂停状态 缓存账号id集合 key
	 */
	public static final String APP_ACCOUNT_PAUSED_SET_KEY = "mobile:account:paused";
	/**
	 * 手机端 88 22 命令缓存 上一个指令的值 只有上一次是22或者405 本次指令是405或者22的时候才能在线 其他情况暂停
	 */
	public static final String APP_ACCOUNT_LAST_STATUS_KEY = "mobile:tool:last:status:";
	/**
	 * 手机端 400 405 命令缓存 上一个指令的值 只有上一次是22或者405 本次指令是405或者22的时候才能在线 其他情况暂停
	 */
	public static final String YSF_ACCOUNT_LAST_STATUS_KEY = "mobile:ysf:last:status:";
	/**
	 * 通道id-使用过的账号id 缓存key
	 */
	public static final String NEWINACCOUNT_PASSAGEID_ACCOUNTIDS_USED_KEY = "pocId:accountIds:hash:";
	public static final String INCOME_ACCOUNT_NORMAL_STATUS_KEY = "1NormalAccounts";
	public static final String INCOME_ACCOUNT_FREEZE_STATUS_KEY = "3FreezeAccounts";
	public static final String INCOME_ACCOUNT_STOP_STATUS_KEY = "4StopAccounts";
	public static final String INCOME_ZONE_ALIPAY_ACCOUNT = "IncomeApprove:Zone:IncomeAlipay";
	public static final String INCOME_ZONE_WECHAT_ACCOUNT = "IncomeApprove:Zone:IncomeWechat";
	public static final String INCOME_APPROVE_ASSIGN_LOCK = "IncomeApprove:AssignLock";
	/**
	 * 入款审核开始接单 结束接单 平台同步支付宝账号 微信账号 通知分配账号 消息队列 key
	 */
	public static final String INCOME_APPROVE_MESSAGEQUEUE_KEY = "IncomeApprove:MessageKey";
	/**
	 * INCOME_APPROVE_MESSAGEQUEUE_KEY 消息的备份 key
	 */
	public static final String INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY = "IncomeApprove:MessageBackUpKey";

	/**
	 * 入款人员分配支付宝账号key
	 */
	public static final String INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY = "IncomeApprove:user:status:wechatInAccount:";
	/**
	 * 入款人员分配微信账号key
	 */
	public static final String INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY = "IncomeApprove:user:status:alipayInAccount:";
	/**
	 * 已分配接单账号的在线接单入款人员key
	 */
	public static final String INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY = "IncomeApprove:user:assigned:zone:";
	/**
	 * 在线接单入款人员key ,分区域保存
	 */
	public static final String INCOME_APPROVE_USER_ZONE_KEY = "IncomeApprove:user:zone:";
	/**
	 * 任务排查开始接单 结束接单 转排查 转主管 分配任务排查 消息队列 key
	 */
	public static final String TASK_REVIEW_MESSAGEQUEUE_KEY = "ReviewTask:MessageKey";
	public static final String TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY = "ReviewTask:MessageBackUpKey";
	public static final String TASK_REVIEW_ASSIGN_LOCK = "ReviewTask:AssignLock";
	/**
	 * 在正处理待排查单子的用户和出款账号ip
	 */
	public static final String TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY = "ReviewTask:User:Ip:Account:TaskId:";
	/**
	 * 用户锁定排查的任务
	 */
	public static final String TASK_REVIEW_USERLOCK_TASK_KEY = "ReviewTask:LockUserId:Ip:TaskId:";
	/**
	 * 任务分配单在线接单人所属区域
	 */
	public static final String TASK_REVIEW_ZONE_ONLINEUSER_KEY = "ReviewTask:Zone:OnlineUsers:";

	/**
	 * 用于第三方提现锁定key
	 */
	public static final String LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX = "thirdInAccount:withdraw:lockTargetAccId:byUser";
	/**
	 * 当日收款
	 */
	public final static String AMOUNT_SUM_BY_DAILY_INCOME = "AmountSumByDailyIncome";
	/**
	 * 当日出款
	 */
	public final static String AMOUNT_SUM_BY_DAILY_OUTWARD = "AmountSumByDailyOutward";

	/**
	 * 当日出款笔数
	 */
	public final static String COUNT_SUM_BY_DAILY_OUTWARD = "CountSumByDailyOutward";
	// 锁定返利佣金审核的key
	public static final String REBATE_AMOUTS_KEYS = "RebateAmounts";

	/**
	 * 上次出款时间
	 */
	public final static String LAST_TIME_OUTWARD = "LastTimeOutward";

	public final static String FROM_ACCOUNT_TRANS_RADIX_AMOUNT = "FromAccountTransRadixAmount";
	/**
	 * 入款审核账号分配：更新
	 */
	public static final String INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD = "IncomeAuditAccountAllocateUpd";

	public static String INCOME_AUDIT_ACCOUNT_ALLOCATE = "IncomeAuditAccountAllocate:";

	public final static String INCOME_AUDIT_ACCOUNT_NORMAL = "IncomeAuditAccountNormal";

	public final static String INCOME_AUDIT_ACCOUNT_STOP = "IncomeAuditAccountStop";

	public final static String INCOME_AUDIT_ACCOUNT_FREEZE = "IncomeAuditAccountFreeze";

	public final static String INCOME_AUDIT_ACCOUNT_HOST = "IncomeAuditAccountHost";

	public final static String ONLINE_CLUSTER_HOST = "OnlineClusterHost";
	// 不能进行定时任务的IP名单，以逗号隔开
	public final static String ONLINE_CLUSTER_HOST_BLACKLIST = "OnlineClusterHostBlackList";

	public final static String SHIRO_SESSION = "ShiroSession:";

	public final static String ACC_MODEL = "Model";

	public final static String ACC_NEW_MODEL = "AccNewModel";

	/**
	 * TransAck 记录出款上报和下发上报的数据，用于计算系统余额
	 */
	public final static String TRANS_ACK = "TransAck:";

	/**
	 * Redis Key:待分配出款任务队列
	 */
	public static final String ALLOCATING_OUTWARD_TASK = "AllocatingOutwardTask";

	/**
	 * Redis Key:待分配出款任务队列(新)
	 */
	public static final String ALLOCATING_NEW_OUTWARD_TASK = "AllocatingNewOutwardTask";

	/**
	 * Redis Key:待计算队列
	 */
	public static final String ALLOCATING_OUTWARD_TASK_CAL = "AllocatingOutwardTaskCal";

	/**
	 * Redis Key:待计算队列
	 */
	public static final String ALLOCATING_NEW_OUTWARD_TASK_CAL = "AllocatingNewOutwardTaskCal";

	/**
	 * Redis Key:待分配出款任务临时队列
	 */
	public static final String ALLOCATING_OUTWARD_TASK_TMP = "AllocatingOutwardTaskTmp" + CommonUtils.getInternalIp();

	/**
	 * Redis Key:待分配出款任务临时队列
	 */
	public static final String ALLOCATING_NEW_OUTWARD_TASK_TMP = "AllocatingNewOutwardTaskTmp"
			+ CommonUtils.getInternalIp();

	/**
	 * Redis Key:被分配对象集合 zset
	 */
	public static final String ALLOCATING_OUTWARD_TARGET = "AllocatingOutwardTaskTarget_";

	/**
	 * Redis Key:被分配对象集合 zset(新)
	 */
	public static final String ALLOCATING_NEW_OUTWARD_TARGET = "AllocatingNewOutwardTaskTarget_";

	/**
	 * Redis Key:Task 指定出款银行
	 */
	public static final String ALLOCATING_OUTWARD_TASK_BANK = "AllocatingOutwardTaskBank:";

	/**
	 * Redis Key:Task 上次出款信息
	 */
	public static final String ALLOC_OTASK_LAST = "AllocOTaskLast";

	/**
	 * Redis Key:需要人工出款 卡的列表
	 */
	public static final String ARTIFICIAL_CARD = "ArtificialCard";

	/**
	 * Redis Key:交易账号信息（金额）
	 */
	public static final String TRANS_ACC = "TransAcc";

	public static final String TRANS_AMT = "TransAmt";

	/**
	 * Redis Key:下发黑名单
	 */
	public static final String TRANS_BLACK = "TransBlack:";

	/**
	 * Redis Key:出款高优先级
	 */
	public static final String OUTWARD_HIGH_PRIORITY = "OUTWARD_HIGH_PRIORITY";

	/**
	 * Redis Key:下发高优先级
	 */
	public static final String ISSUED_HIGH_PRIORITY = "ISSUED_HIGH_PRIORITY";

	public static final String ALLOC_OUT_NEED_ORI = "AllocOutNeedOri";

	public static final String ALLOC_NEW_OUT_NEED_ORI = "AllocNewOutNeedOri";
	/**
	 * 出款卡所需金额
	 * 达到配置项金额(sys_user_profile表property_key=OUTCARD_NEED_THIRD_DRAW_MINAMOUNT)的就保存到
	 * 该key里 便于第三方绑定锁定下发 zset
	 */
	public static final String OUTCARD_NEED_THIRD_DRAW = "outcard:need:third:draw";
	/**
	 * 出款卡 添加下发队列 时间 hashmap
	 */
	public static final String DRAW_TASK_USER_ADD_CARD_TIME = "draw:task:user:addTime";
	/**
	 * 第三方下发到出款卡 锁定 key zset
	 */
	public static final String OUTCARD_NEED_THIRD_DRAW_LOCK = "outcard:need:third:draw:user:locked:";
	/**
	 * 第三方下发到出款卡 出款之后 待流水匹配 hash
	 */
	public static final String OUTCARD_NEED_THIRD_DRAW_UNFINISHED = "outcard:need:third:draw:unfinished";
	/**
	 * 第三方下发到出款卡 开关 1开启 2 关闭
	 */
	public static final String ENABLE_THIRD_DRAW_TO_OUTCARD = "outcard:need:third:draw:enable";
	/**
	 * 第三方下发到出款卡 第三方账号下发到多个出款卡hash
	 */
	public static final String OUTCARD_NEED_THIRD_DRAW_LOCKED_HASH = "outcard:need:third:draw:locked:hash";
	/**
	 * 第三方提现到出款卡 页面输入提现金额的时候 保存输入的金额 第三方账号的系统余额要扣除这个金额 当解锁的时候要加回
	 */
	public static final String OUTCARD_NEED_THIRD_DRAW_LOCKED_AMOUNTTODRAW = "outcard:need:third:draw:locked:amountToDraw";

	/**
	 * 下发任务 用户锁定的 key zset 出款卡ID 下发卡ID 分值 锁定时间
	 */
	public static final String DRAW_TASK_USER_LOCK_CARD = "draw:task:user:lock:card";
	/**
	 * 用户提现的时候 记录最新使用的第三方账号id map hkey:userid hval:thirdId
	 */
	public static final String DRAW_TASK_USER_USED_LASTIME = "draw:task:user:last:used";
	/**
	 * 下发卡 提现完成尚未匹配的时候 记录 hashmap
	 */
	public static final String THIRD_INACCOUNT_DRAW_UNFINISHED = "third:inAccount:draw:unfinished";

	/**
	 * 下发任务 用户锁定的 锁定的出款卡 hashmap
	 */
	public static final String DRAW_TASK_USER_LOCK_OUTCARD = "draw:task:user:lock:outcard";

	/**
	 * 记录所有用户 锁定的 账号id 出款卡或者下发卡 hashmap
	 */
	public static final String LOCKED_USER_ACCOUNT = "draw:task:user:locktime";
	/**
	 * 保存 提现的时候 出款卡 下发卡提现 的时候保存 以记录下发耗时 匹配(自动或者人工) 或者打回的时候 删除掉
	 */
	public static final String SAVE_TRANS_TIME = "draw:task:user:drawtime";
	/**
	 * 我的设定 set
	 */
	public static final String SETUP_USER_THIRDACCOUNT = "draw:task:user:setup";

	/**
	 * 我的设定 hashmap 选中某个去下发
	 */
	public static final String SETUP_USER_THIRDACCOUNT_SELECTED = "draw:task:user:selected";

	/**
	 * 记录 提现的时候 出款账号下发的金额和手续费 hashmap
	 */
	public static final String THIRD_DRAW_AMOUNT_FEE = "draw:task:account:amount:fee";
	/**
	 * 缓存用户拆单 如 split:user:order:5:单号 hashmap
	 */
	public static final String SPLIT_USER_ORDER = "split:user:order:";
	/**
	 * 缓存用户拆单 如 split:order:单号 hash <br>
	 * subkey 子单号 val 已完成的金额
	 */
	// public static final String SPLIT_ORDER_AMOUNT_FINISH = "split:order:finish:";
	/**
	 * 拆单使用的 第三方账号 使用的金额 <br>
	 * hashmap key split:third:amount:5:订单号<br>
	 * hkey 子订单号 <br>
	 * hval 三方账号id:金额
	 * 
	 */
	public static final String SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE = "split:finish:third:amount:fee:";
	public static final String SPLIT_SUBORDER_HISTORY_THIRD_AMOUNT_FEE = "split:finish:history:amount:fee:";
	private static final String ALLOC_TRANS_UP_LIM = "AllocTransUpLim";

	public static final String ALLOC_MERGE_LEVEL = "AllocMergeLevel";

	/**
	 * 转账账号锁定
	 */
	public static final String TRANSFER_ACCOUNT_LOCK = "TransferAccountLock:";

	public static final String TRANS_LOCK_DEL = "LockDel:";

	/**
	 * Account 账号实时余额
	 */
	public static final String ACC_REAL_BAL = "AccRealBal";

	public static final String ACC_SYS_BAL = "AccSysBal";

	public static final String TASK_REPORT_LAST_STEP = "TaskReportLastStep";

	public static final String ACC_LAST_REAL_BAL = "AccLastRealBal";

	/**
	 * Account 账号实时余额上报时间
	 */
	public static final String ACC_REAL_BAL_RPT_TM = "AccRealBalRptTm";

	/**
	 * api调用上报时间
	 */
	public static final String ACC_API_REVOKE_TM = "AccApiRevokeTm";

	/**
	 * 账号实时余额改变时间
	 */
	public static final String ACC_BAL_CHG_TM = "AccBalChgTm";
	public static final String ACC_LOG_TM = "AccLogTm";
	public static final String ACC_REAL_BAL_TM = "AccRealBalTm";
	public static final String ACC_NEW_REAL_BAL_TM = "AccNewRealBalTm";

	public final static String REMARK_TO_TRANSLOCK = "LockRem:";

	public final static String ACC_BAL_ALARM = "AccBalAlarm";

	public final static String ACC_MACH_ALARM = "AccMachAlarm";

	public final static String ACC_SYS_INIT = "ACC_SYS_INIT";

	public final static String ACC_LAST_RISK_LOG = "AccLastRiskLog";

	/**
	 * 第三方当日收款总金额
	 */
	public final static String AMOUNT_SUM_BY_DAILY_THIRD_TOTAL_INCOME = "AmountSumByDailyThirdTotalIncome";
	/**
	 * 第三方当日收款手续费
	 */
	public final static String AMOUNT_SUM_BY_DAILY_THIRD_FEE_INCOME = "AmountSumByDailyThirdFeeIncome";
	/**
	 * 第三方当日实际收款金额
	 */
	public final static String AMOUNT_SUM_BY_DAILY_THIRD_AMOUNT_INCOME = "AmountSumByDailyThirdAmountIncome";

	/**
	 * Redis Key:待分配下发的配对象集合 zset
	 */
	public static final String ALLOC_APPLY_BY_FROM = "AllocApplyByFrom";

	/**
	 * Redis Key:待分配下发的配对象集合 zset(新)
	 */
	public static final String ALLOC_NEW_APPLY_BY_FROM = "AllocNewApplyByFrom";

	/**
	 * Redis Key:可以出款的入款卡、备用卡等对象集合 zset
	 */
	public static final String ALLOC_APPLY_BY_FROM_OUT = "AllocApplyByFromOut";

	/**
	 * 账号恢复额度
	 */
	public final static String ACCOUNT_CREDIT_RESTORE = "AccountCreditRestore";

	/**
	 * 入款卡客户绑定卡余额超过余额告警值的时间
	 */
	public static final String INBANK_BAL_MORE_LIMITBAL_TM = "InbankBalMoreLimitBalTM";

	/**
	 * 激活账号测试转账任务
	 */
	public static final String ACTIVE_ACCOUNT_TEST_TRANS = "ActiveAccountTestTrans:";
	// 测试转账的key
	public static final String ACTIVE_ACCOUNT_TEST_KEYS = "ActiveAccountTestTransKey";
	// 激活的key
	public static final String ACTIVE_ACCOUNT_KEYS = "ActiveAccountTransKey";
	public static final String ACTIVE_ACCOUNT_TEST_FAIL = "ActiveAccountTestTransFail:";
	public static final String ONE_TIME_TRANS_OUT_KEYS = "OneTimeTransOutKeys";

	public static final String ACC_CHG = "ACC_CHG_APP";

	public static final String SYS_BAL = "SYS_BAL:";

	public static final String SYS_BAL_IN = "SYS_BAL_IN";

	public static final String SYS_BAL_OUT = "SYS_BAL_OUT";

	public static final String REAL_BAL_BENCHMARK = "REAL_BAL_BENCHMARK";

	public static final String REAL_BAL_LASTTIME = "REAL_BAL_LASTTIME";

	public static final String SYS_BAL_LASTTIME = "SYS_BAL_LASTTIME";

	public static final String SYS_BAL_LOGS = "SYS_BAL_LOGS";

	public static final String SYS_BAL_YSF = "SYS_BAL_YSF";

	public static final String SYS_BAL_BALANCING = "SYS_BAL_BALANCING";

	public static final String SYS_LAST_LOGS_TIME = "SYS_LAST_LOGS_TIME";

	public static final String SYS_ACC_RUNNING = "SYS_ACC_RUNNING";

	public static final String SYS_ACC_RUUNING_AUTO = "SYS_ACC_RUUNING_AUTO";

	/**
	 * 系统账目：对账异常账号，即系统余额不等于银行余额
	 */
	public static final String SYS_ACC_ACCOUNTING_EXCEPTION = "SYS_ACC_ACCOUNTING_EXCEPTION";

	public static final String PROBLEM_ACC_ALARM = "ProblemAccAlarmKeys";

	public static final String APP_NEED_UPGRADE = "APP_NEED_UPGRADE";

	public static final String ACC_DEVICE_STATUS_KEYS = "DEVICE_STATUS_KEYS";
	/**
	 * 一键转出、激活转账等排除的账号，防止一直转给同一张卡
	 */
	public static final String TRANS_EXCLUDE_ACCOUNT = "TRANS_EXCLUDE_ACCOUNT:";
	/**
	 * 账号激活
	 */
	public static final String ACCOUNT_ACTIVE = "ACCOUNT_ACTIVE:";
	/**
	 * 未确认的入款单金额
	 */
	public static final String ACCOUNT_INCOME_AMOUNT = "ACCOUNT_INCOME_AMOUNT";
	/**
	 * 最后一次入款时间
	 */
	public static final String ACCOUNT_INCOME_LASTTIME = "ACCOUNT_INCOME_LASTTIME";

	/**
	 * 入款卡在线集合
	 */
	public static final String INBANK_ONLINE = "INBANK_ONLINE";

	/**
	 * 禁用云闪付账号集合
	 */
	public static final String ACCOUNT_DISABLE_QUICKPAY = "ACCOUNT_DISABLE_QUICKPAY";

	/**
	 * 云闪付初始化时间
	 */
	public static final String YSF_INIT_TIME = "YSF_INIT_TIME:";

	/**
	 * 连续转账失败次数
	 */
	public final static String COUNT_FAILURE_TRANS = "CountFailureTrans";

	/**
	 * 回收的下发卡结果集
	 */
	public final static String RECYCLE_BINDCOMM_SET = "RecycleBindCommSet";

	/**
	 * 入款卡超过阈值次数
	 */
	public final static String INACCOUNT_EXCEED_CREDIT_COUNT = "InAccountExceedCreditCountNew";

	/**
	 * 入款卡超过阈值次数，按层级
	 */
	public final static String INACCOUNT_EXCEED_CREDIT_LEVEL = "InAccountExceedCreditCountLevel";

	/**
	 * 下发有效时间锁
	 */
	public static final String TRANSFER_ACCOUNT_LOCK_TIME = "TRANSFER_ACCOUNT_LOCK_TIME";
	/**
	 * 测试转账激活有效时间锁
	 */
	public static final String TEST_TRANSFER_ACCOUNT_LOCK_TIME = "TEST_TRANSFER_ACCOUNT_LOCK_TIME";
	/**
	 * 公司入款请求
	 */
	public static final String INCOME_REQUEST = "INCOME_REQUEST";

	/**
	 * 第三方入款请求
	 */
	public static final String INCOME_THIRD_REQUEST = "INCOME_THIRD_REQUEST";
	/**
	 * 下发卡分配任务最新时间
	 */
	public static final String BINDCOMMON_TASK_ALLOC_TIME = "BindcommonTaskAllocTime";

	public static final String genKey4AllocTransUpLim(int zone) {
		return ALLOC_TRANS_UP_LIM + zone;
	}

	public final static String genKey4AccLastRiskLog(int accId) {
		return ACC_LAST_RISK_LOG + ":" + accId;
	}

	public final static String genPattern4LockRem() {
		return REMARK_TO_TRANSLOCK + "*";
	}

	public final static String genKey4LockRem(String orderNo) {
		return REMARK_TO_TRANSLOCK + orderNo;
	}

	public static String genPattern4LockDel() {
		return TRANS_LOCK_DEL + "*";
	}

	public static String genPattern4LockDel_to(Integer toId) {
		return TRANS_LOCK_DEL + "*:" + toId + ":*:*:*";
	}

	public static String gen4TransferAccountLock(Integer fromId, Integer toId, Integer operatorId, String radix,
			String transInt) {
		return TRANSFER_ACCOUNT_LOCK + fromId + ":" + toId + ":" + operatorId + ":" + radix + ":" + transInt;
	}

	public static String genPattern4TransferAccountLock(Object fromId, Integer toId, Integer operatorId) {
		return TRANSFER_ACCOUNT_LOCK + fromId + ":" + toId + ":" + operatorId + ":*:*";
	}

	public static String genPattern4TransferAccountLock_from(Object fromId) {
		return TRANSFER_ACCOUNT_LOCK + fromId + ":*:*:*:*";
	}

	public static String genPattern4TransferAccountLock_to(Integer toId) {
		return TRANSFER_ACCOUNT_LOCK + "*:" + toId + ":*:*:*";
	}

	public static String genPattern4TransferAccountLock_All() {
		return TRANSFER_ACCOUNT_LOCK + "*:*:*:*:*";
	}

	public static String genPattern4TransferAccountLock_operator(Integer operator) {
		return TRANSFER_ACCOUNT_LOCK + "*:*:" + operator + ":*:*";
	}

	public static String genPattern4TransferAccountLock() {
		return TRANSFER_ACCOUNT_LOCK + "*:*:*:*";
	}

	public static String gen4HostMonitor(String clientIp) {
		return "Host:" + clientIp;
	}

	public static String genPattern4HostMonitor() {
		return "Host:*";
	}

	public static String genOrigin4IncomeAuditAccountAllocate() {
		return INCOME_AUDIT_ACCOUNT_ALLOCATE + "*";
	}

	public static String genOrigin4IncomeAuditAccountAllocate(int userId) {
		return INCOME_AUDIT_ACCOUNT_ALLOCATE + userId + ":";
	}

	/***
	 * redis里存储的入款审核人和审核账号格式: IncomeAuditAccountAllocate:userId:accountId:*:*:*
	 */
	public static String gen4IncomeAuditAccountAllocate(int userId, List<Integer> accountArray) {
		StringBuffer sb = new StringBuffer(INCOME_AUDIT_ACCOUNT_ALLOCATE + userId + ":");
		if (!CollectionUtils.isEmpty(accountArray)) {
			accountArray.forEach((p) -> sb.append(p + ":"));
		}
		return sb.toString();
	}

	public static String genPattern4IncomeAuditAccountAllocateByUserId(int userId) {
		return INCOME_AUDIT_ACCOUNT_ALLOCATE + userId + ":*";
	}

	public static String genPattern4IncomeAuditAccountAllocateByUserId(String userId) {
		return INCOME_AUDIT_ACCOUNT_ALLOCATE + userId + ":*";
	}

	public static String genPattern4IncomeAuditAccountAllocateByAccountId(int accountId) {
		return INCOME_AUDIT_ACCOUNT_ALLOCATE + "*:" + accountId + ":*";
	}

	public static String genPattern4IncomeAuditAccountAllocateByAccountId(long accountId) {
		return INCOME_AUDIT_ACCOUNT_ALLOCATE + "*:" + accountId + ":*";
	}

	public static String gen4TransBlack(Object fromId, Object toId, int amt) {
		return TRANS_BLACK + fromId + ":" + toId + ":" + amt;
	}

	public static String gen4TransBlackCanDel(Object fromId, Object toId, int amt) {
		return TRANS_BLACK + fromId + ":" + toId + ":" + amt + ":1";
	}

	public static String genPattern4TransBlack_toId(int toId) {
		return TRANS_BLACK + "*:" + toId + ":*";
	}

	public static String genPattern4TransBlack() {
		return TRANS_BLACK + "*";
	}

	public static String genPattern4ShiroSession() {
		return SHIRO_SESSION + "*";
	}

	public static String gen4ShiroSession(int userId) {
		return SHIRO_SESSION + userId;
	}

	public static String gen4ShiroSession(String userId) {
		return SHIRO_SESSION + userId;
	}

	public static String gen4OutwardTaskBank(long taskId) {
		return ALLOCATING_OUTWARD_TASK_BANK + taskId;
	}

	public static String genPattern4ActiveAccTrans_from(Object fromId) {
		return ACTIVE_ACCOUNT_TEST_TRANS + fromId + ":*:*:*:*:*";
	}

	public static String genPattern4ActiveAccTrans_to(Integer toId) {
		return ACTIVE_ACCOUNT_TEST_TRANS + "*:" + toId + ":*:*:*:*";
	}

	public static String genPattern4ActiveAccTransFail_from(Object fromId) {
		return ACTIVE_ACCOUNT_TEST_FAIL + fromId + ":*";
	}

	public static String genTransExcludeAccount(Integer accId) {
		if (accId == null) {
			return TRANS_EXCLUDE_ACCOUNT + "*";
		}
		return TRANS_EXCLUDE_ACCOUNT + accId;
	}

	public static String genKey4SysBalLogs(int accId) {
		return String.format("%s:%d", SYS_BAL_LOGS, accId);
	}
}
