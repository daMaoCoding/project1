package com.xinbo.fundstransfer.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuConfigRequest;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.pojo.UserCategory;
import com.xinbo.fundstransfer.domain.repository.*;
import com.xinbo.fundstransfer.newinaccount.service.InAccountService;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;
import okhttp3.RequestBody;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AllocateOutwardTaskServiceImpl implements AllocateOutwardTaskService {
	private static final Logger log = LoggerFactory.getLogger(AllocateOutwardTaskServiceImpl.class);
	@Autowired
	private OutwardRequestService outwardRequestService;
	@Autowired
	private OutwardTaskService outwardTaskService;
	@Autowired
	private OutwardRequestRepository outwardRequestRepository;
	@Autowired
	private OutwardTaskRepository outwardTaskRepository;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private LevelService levelService;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	RequestBodyParser requestBodyParser;
	@Autowired
	AllocateIncomeAccountService incomeAccountAllocateService;
	@Autowired
	SysUserService userService;
	@Autowired
	TransactionLogService transactionLogService;
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	AllocateTransferService allocTransSer;
	@Autowired
	private BankLogRepository bankLogRepository;
	@Value("${funds.transfer.version}")
	private String CURR_VERSION;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	@Lazy
	private IncomeRequestService incomeRequestService;
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private TransMonitorService transMonitorService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private AccountRebateRepository accountRebateRepository;
	@Autowired
	private AsignFailedTaskService asignFailedTaskService;
	@Autowired
	private AccountChangeService accountChangeService;
	@Autowired
	private AccountRebateService accountRebateService;
	@Autowired
	private InAccountService inAccountService;
	@Autowired
	@Lazy
	private SystemAccountManager systemAccountManager;
	@Autowired
	private AccountRebateRepository accRebateDao;
	@Autowired
	private DaifuConfigRequestRepository daifuConfigRequestRepository;
	@Autowired
	private CabanaService cabanaService;
	private static final String ALLOC_MERGE_LEVEL_LAST_TIME = "LastTime", ALLOC_MERGE_LEVEL_DEADLINE = "DeadLine";

	/**
	 * 已分配目标对象
	 */
	private static final Cache<Integer, Integer> atedTarget = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES).build();

	/**
	 * 其他出款分配涉及到的缓存信息 出款接单人员持卡信息 USERACCMAP 最后出款信息 LAST 黑名单信息 BLACK
	 */
	private static final Cache<String, Object> allocNeedCache = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES).build();

	private static final Cache<Integer, Boolean> THIRD_RIGHT_OUTWARD_USER = CacheBuilder.newBuilder()
			.expireAfterWrite(2, TimeUnit.MINUTES).build();

	/**
	 * 定时刷新分配目标对象
	 *
	 */
	private LoadingCache<String, Set<ZSetOperations.TypedTuple<String>>> cardCache = CacheBuilder.newBuilder()
			.concurrencyLevel(1).refreshAfterWrite(30, TimeUnit.SECONDS) // 每30秒自动刷新
			.build(new CacheLoader<String, Set<ZSetOperations.TypedTuple<String>>>() {
				public Set<ZSetOperations.TypedTuple<String>> load(String key) {
					log.info("开始定时刷新分配目标缓存对象");
					// 未匹配流水告警数据
					Set<Integer> unMatched = buildAllUnMatched();
					log.trace("cache unMatched>> {}", unMatched);
					// 银行余额上报有效时间
					Set<Integer> accIdSetInValTm = allocateTransService.buildValidAcc();
					log.trace("cache accIdSetInValTm>> {}", accIdSetInValTm);
					// 出款频率限制
					Set<Integer> idsByNonExprie = buildIdByNonExprie();
					log.trace("cache idsByNonExprie>> {}", idsByNonExprie);
					// 转出限制
					// Set<Integer> invSet = sysBalService.alarm(true);
					// log.trace("cache invSet>> {}", invSet);
					// 当日出款限额
					Map<String, Float> outCtnDaily = accountService.findOutCountDaily();
					log.trace("cache outCtnDaily>> {}", outCtnDaily);
					// 账号设备告警
					Map<Object, Object> deviceAlarm = redisService.getStringRedisTemplate()
							.boundHashOps(RedisKeys.PROBLEM_ACC_ALARM).entries();
					if ("OUTBANK".equals(key)) {
						// 计算可用卡：1.状态正常，2.不在黑名单中, 3...
						Set<ZSetOperations.TypedTuple<String>> getAll = redisService.getStringRedisTemplate()
								.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).rangeWithScores(0, -1);
						log.trace("cache outbank >> {}", getAll);
						getAll = getAll.stream()
								.filter(p -> checkUnmatch(Integer.parseInt(p.getValue().split(":")[1]), unMatched)
										&& checkInvokeTm(accountService.getFromCacheById(
												Integer.parseInt(p.getValue().split(":")[1])), accIdSetInValTm)
										&& checkNonExpire(idsByNonExprie, Integer.parseInt(p.getValue().split(":")[1]))
										&& checkOutCtnDaily(accountService
												.getFromCacheById(Integer.parseInt(p.getValue().split(":")[1])),
												outCtnDaily)
										&& checkDailyOut(accountService
												.getFromCacheById(Integer.parseInt(p.getValue().split(":")[1])))
										&& !deviceAlarm.containsKey(p.getValue().split(":")[1])
										&& (accountService
												.getFromCacheById(Integer.parseInt(p.getValue().split(":")[1])) == null
												|| !checkMaintain(accountService
														.getFromCacheById(Integer.parseInt(p.getValue().split(":")[1]))
														.getBankType())))
								.collect(Collectors.toSet());
						log.trace("cache outbank after filter >> {}", getAll);
						return getAll;
					} else {
						Set<ZSetOperations.TypedTuple<String>> getAllIn = redisService.getStringRedisTemplate()
								.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM_OUT).rangeWithScores(0, -1);
						log.trace("cache other bank >> {}", getAllIn);
						removeExitLock(getAllIn);
						getAllIn = getAllIn.stream().filter(p -> checkUnmatch(Integer.parseInt(p.getValue()), unMatched)
								&& checkInvokeTm(accountService.getFromCacheById(Integer.parseInt(p.getValue())),
										accIdSetInValTm)
								&& checkNonExpire(idsByNonExprie, Integer.parseInt(p.getValue()))
								&& checkOutCtnDaily(accountService.getFromCacheById(Integer.parseInt(p.getValue())),
										outCtnDaily)
								&& checkDailyOut(accountService.getFromCacheById(Integer.parseInt(p.getValue())))
								&& !deviceAlarm.containsKey(p.getValue())
								&& ((accountService.getFromCacheById(Integer.parseInt(p.getValue()))) == null
										|| !checkMaintain(accountService
												.getFromCacheById(Integer.parseInt(p.getValue())).getBankType())))
								.collect(Collectors.toSet());
						;
						log.trace("cache other bank after filter >> {}", getAllIn);
						return getAllIn;
					}
				}
			});

	/**
	 * 十秒内停止接单的用户
	 */
	private static final Cache<Integer, Integer> SUSPEND_OPERATOR = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(20, TimeUnit.MINUTES).build();

	private static final Cache<Integer, Integer> SUSPEND_ACCOUNT = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(20, TimeUnit.MINUTES).build();

	/**
	 * 缓存：某一账号未匹配的出款任务数（ 有效时间为:60秒）</br>
	 * Key:账号ID value:未匹配出款任务数
	 */
	private static final Cache<Integer, Integer> UNMATCHED_COUNT = CacheBuilder.newBuilder().maximumSize(10000)
			.expireAfterWrite(60, TimeUnit.SECONDS).build();

	/**
	 * 层级合并设置
	 */
	public static Cache<String, Long>[] MERGE_LEVEL_SET = new Cache[99];

	/*
	 * 8小时所对应的毫秒数
	 */
	private static final long EIGHT_HOURS_MILIS = 8 * 60 * 60 * 1000;

	/**
	 * 任务需要等待的最大金额</br>
	 * （任务金额大于等于此值，直接分配，不需要等待）
	 */
	private static final int WAITING_AMT_MAX = 5000;

	/**
	 * 任务最大可以等待时间（2.5分钟）
	 */
	private static final int WAITING_MILIS_MAX = 150000;

	/**
	 * 分类：被分配对象集合 分类;首次出款标识（FIRT_OUT_YES），人工出款标识（MANUAL_OUT_YES）<br/>
	 * ###用户：TARGET_TYPE_USER<br/>
	 * ###机器：TARGET_TYPE_ROBOT<br/>
	 * ###第三方：TARGET_TYPE_THIRD</br>
	 * ###手机：TARGET_TYPE_MOBILE</br>
	 */
	private static final int TARGET_TYPE_USER = 0, TARGET_TYPE_ROBOT = 1, TARGET_TYPE_START = 2, TARGET_TYPE_THIRD = 3,
			TARGET_TYPE_MOBILE = 4;

	/**
	 * 常量：<br/>
	 * ###外层:OUTTER<br/>
	 * ###内层:INNER<br/>
	 * ###中层:Middle<br/>
	 */
	private static final int OUTTER = CurrentSystemLevel.Outter.getValue(), INNER = CurrentSystemLevel.Inner.getValue(),
			MIDDLE = CurrentSystemLevel.Middle.getValue(), DESIGNATED = CurrentSystemLevel.Designated.getValue();

	/***
	 * 格式：被分配对象数据格式{分类}:{ID} <br/>
	 * ###用户：TARGET_TYPE_USER:UserId <br/>
	 * ###机器：TARGET_TYPE_ROBOT:AccountId<br/>
	 * ###手机：TARGET_TYPE_MOBILE:AccountId<br/>
	 * ###重新分配：TARGET_TYPE_START:0<br/>
	 */
	private static final String QUEUE_FORMAT_USER_OR_ROBOT = "%d:%d";

	/**
	 * 常量：重新分配
	 */
	private static final String DUPLICATE_ALLOCATE = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_START, 0);

	/**
	 * 上次分配分数
	 */
	private volatile static double LAST_ALLOCATE_SCORE = 0D;

	/**
	 * 常量：<br/>
	 * ###锁定成功：LOCK_REQ_SUCCESS;<br/>
	 * ###锁定失败：LOCK_REQ_FAIL;<br/>
	 * ###重新分配：RE_ALLOCATE（请不要改动）<br/>
	 * ###财务确认:FIN_ACKED</br>
	 */
	private static final String LOCK_REQ_SUCCESS = "锁定成功", LOCK_REQ_FAIL = "锁定失败", RE_ALLOCATE = "重新分配";

	private static final String EXCP_NONE_TARGET = "EXCP_NONE_TARGET";

	/**
	 * 批量取出待出款任务数量
	 */
	private static final String NUM_LPOP = String.valueOf(1);

	/**
	 * 机器标识 ROBOT</br>
	 * 系统标识 SYS</br>
	 */
	private static final String ROBOT = "机器", SYS = "系统", THIRD = "第三方", MOBILE = "手机";

	/**
	 * 标识：</br>
	 * 银行维护 BANK_MAINTAIN，</br>
	 * 银行维护恢复 BANK_MAINTAIN_RESORE
	 */
	private static final String BANK_MAINTAIN = "银行维护", BANK_MAINTAIN_RESORE = "银行维护恢复";

	/**
	 * 常量：出款任务分配服务停止时间段</br>
	 * HALT_SERVICE_START_TIME 开始时间毫秒数；</br>
	 * HALT_SERVICE_END_TIME 结束时间毫秒数</br>
	 */
	private static volatile Long HALT_SERVICE_START_TIME = null, HALT_SERVICE_END_TIME = null;

	private static volatile Map<String, Long[]> BANK_TYPE_HALT_SERVICE = new ConcurrentHashMap<>();

	/**
	 * 常量：</br>
	 * ###入款卡:INBANK</br>
	 * ###备用卡:RESERVEBANK</br>
	 */
	private static final int OUTBANK = AccountType.OutBank.getTypeId();
	private static final int INBANK = AccountType.InBank.getTypeId();
	private static final int RESERVEBANK = AccountType.ReserveBank.getTypeId();
	private static final int OUTTHIRD = AccountType.OutThird.getTypeId();
	private static final int BINDCUSTOMER = AccountType.BindCustomer.getTypeId();
	private static final int BINDCOMMON = AccountType.BindCommon.getTypeId();

	/**
	 * lua script:批量取出待出款任务</br>
	 * 1.从 分配队列 移动一定数量的任务 到临时队列</br>
	 * 2.返回 从分配队列 移动到 临时队列 中的 出款任务
	 */
	private static final String LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_POP = "local num = ARGV[1];\n"
			+ "local k1 = ARGV[2];\n" + "local k2 = ARGV[3];\n" + "local ret ='';\n"
			+ "local vl = redis.call('zrange',k1,0,tonumber(num)-1);\n" + "for i,v in pairs(vl) do\n"
			+ " if not v then\n" + "  break;\n" + " end\n" + " redis.call('hset',k2,v,'1');\n"
			+ " redis.call('zrem',k1,v);\n" + " if ret == '' and v ~= nil and v ~= ''  then" + "  ret = v;\n"
			+ " elseif ret ~= '' and v ~= '' and v ~= nil then\n" + "  ret = ret..';'..v;\n" + " end\n" + "end\n"
			+ "return ret;";

	/**
	 * lua script:返回未分配的出款任务</br>
	 * 1.已分配任务 从 临时队列中 删除</br>
	 * 2.未分配任务 从 临时队列中 移动到 待分配队列</br>
	 */
	private static final String LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_BACK = "local key1 = ARGV[1];\n"
			+ "local key2 = ARGV[2];\n" + "local key3 = ARGV[3];\n" + "local ated = ARGV[4];\n"
			+ "local aing = ARGV[5];\n" + "local tar = ARGV[6];\n" + "if tar ~= nil and tar ~= '' then\n"
			+ " redis.call('zrem',key3,tar);\n" + "end\n" + "if ated ~= nil and ated ~= '' then\n"
			+ " local rt0 = {};\n" + " string.gsub(ated, '[^'..';'..']+', function(w) table.insert(rt0, w) end );\n"
			+ " for i0,v0 in pairs(rt0) do\n" + "  if v0 ~= nil and v0 ~= '' then\n"
			+ "   redis.call('hdel',key2,v0);\n" + "  end\n" + " end\n" + "end\n"
			+ "if aing == nil or aing == '' then\n" + " return 'ok';" + "end\n" + "local rt1 = {};\n"
			+ "string.gsub(aing, '[^'..';'..']+', function(w) table.insert(rt1, w) end );\n"
			+ "for i1,v1 in pairs(rt1) do\n" + " if v1 ~= nil and v1 ~= '' then\n" + "  redis.call('hdel',key2,v1);\n"
			+ "  redis.call('rpush',key1,v1);\n" + " end\n" + "end\n" + "return 'ok';";

	/**
	 * lua script:系统初始化</br>
	 * 把临时队列 中的待分配任务 移动到 待分配队列
	 */
	private static final String LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_INIT = "local key1 = ARGV[1];\n"
			+ "local key2 = ARGV[2];\n" + "local hks = redis.call('hkeys',key2);\n"
			+ "if hks ~= nil and next(hks) == nil then\n" + " return 'ok';" + "end\n" + "for i,v in pairs(hks) do\n"
			+ " redis.call('rpush',key1,v);\n" + " redis.call('hdel',key2,v);\n" + "end\n" + "return 'ok';";

	/**
	 * lua script:出款卡上次出款任务维护
	 */
	private static final String LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_LAST = "local curr = tonumber(ARGV[1]);\n"
			+ "local frId = ARGV[2];\n" + "local infs = ARGV[3];\n" + "local key = ARGV[4];\n"
			+ "redis.call('hset',key,frId,infs);\n" + "local kvs = redis.call('hgetall',key);\n"
			+ "for i0,v0 in pairs(kvs) do\n" + " if i0 % 2 == 0 then\n" + "  local acc = {};\n"
			+ "  string.gsub(v0, '[^'..':'..']+', function(w) table.insert(acc, w) end );"
			+ "  if tonumber(acc[1]) <= curr then\n" + "   redis.call('hdel',key,acc[2]);\n" + "  end\n" + " end\n"
			+ "end\n" + "return 'ok';\n";

	/**
	 * lua script:出款卡上次出款任务维护 TODO 注意往AllocatingOutwardTask后面增加信息时，这个脚本要改
	 */
	private static final String LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_ORDER = "local keyTask = ARGV[1];\n"
			+ "local keyCal = ARGV[2];\n" + "local num = redis.call('llen',keyTask);\n" + "local map = {};\n"
			+ "for i=1,num do\n" + " local v = redis.call('lpop',keyTask);\n" + " if not v then\n" + "  break;\n"
			+ " end\n" + " local rt0 = {};\n"
			+ " string.gsub(v,'[^'..':'..']+', function(w) table.insert(rt0, w) end );\n"
			+ " local len = table.maxn(rt0);\n" + " local tid = rt0[len-6];\n" + " map[tid] = v;\n" + "end\n"
			+ "for i,v in pairs(map) do\n" + " if not v then\n" + "  break;\n" + " end\n" + " local rt0 = {};\n"
			+ " string.gsub(v,'[^'..':'..']+', function(w) table.insert(rt0, w) end );\n"
			+ " local len = table.maxn(rt0);\n" + " local pTId = len-5;\n"
			+ " redis.call('zadd',keyCal,rt0[len-1],v);\n" + "end\n" + "return ''..redis.call('zcard',keyCal);\n";

	/**
	 * 描述:出款请求审批之后生成出款任务
	 *
	 * @param req
	 *            出款请求订单信息
	 * @param task
	 *            生成的出款任务信息
	 * @param third
	 *            该出款任务是否需要第三方出款
	 */
	@Override
	public void rpush(BizOutwardRequest req, BizOutwardTask task, boolean third) {
		rpush(req, task, checkFirst(req.getReview()), checkManual(req, task.getAmount().floatValue()), third);
	}

	/**
	 * @param req
	 *            返利请求
	 * @param isManual
	 *            是否人工出款
	 */
	@Override
	public void rpush(BizAccountRebate req, boolean isManual) {
		BizHandicap handi = handicapService.findFromCacheById(req.getHandicap());
		int zone = Objects.isNull(handi.getZone()) ? handi.getId() : handi.getZone();
		String msg = AllocatingOutwardTask.genMsg(zone, req, isManual);
		if (CommonUtils.checkDistHandicapNewVersion(handi.getId())) {
			rpushnew(msg);
		} else {
			rpush(msg);
		}
	}

	/**
	 * 停止接单
	 * <p>
	 * 广播接受函数接口
	 * </p>
	 *
	 * @param from
	 *            : 0 人工出款/第三方出款 1：cabana出款
	 */
	@Override
	public void suspend(int tarId, boolean real, int from) {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		if (from == 0) {
			String valUser = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_USER, tarId);
			String valThird = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_THIRD, tarId);
			if (real) {
				if (incomeAccountAllocateService.checkHostRunRight()) {
					template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).remove(valUser, valThird);
				}
				SUSPEND_OPERATOR.put(tarId, tarId);
			} else {
				SUSPEND_OPERATOR.invalidate(tarId);
			}
		} else if (from == 1) {
			String valRobot = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_ROBOT, tarId);
			String valMobile = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_MOBILE, tarId);

			if (real) {
				if (incomeAccountAllocateService.checkHostRunRight()) {
					template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).remove(valRobot, valMobile);
				}
				SUSPEND_ACCOUNT.put(tarId, tarId);
			} else {
				SUSPEND_ACCOUNT.invalidate(tarId);
			}
		}

	}

	private static volatile long LAST_TIME_REMOVE_DUPLICATE = 0;

	/**
	 * 分配 出款任务
	 */
	@Override
	public void schedule() throws InterruptedException {
		// 1.检测 是否具有 出款任务分配权限
		if (!incomeAccountAllocateService.checkHostRunRight()) {
			log.trace("AllocOTask >> the host has no right to allocate. {}", CommonUtils.getInternalIp());
			Thread.sleep(5000L);
			return;
		}
		// 检测：是否停止出款任务分配；
		checkHaltService();
		// 2.检测 是否执行 出款任务分配
		// 检测 DUPLICATE_ALLOCATE 在Redis中zset中score 与上次执行的score
		// 是否相同<br/>相同：不执行；不相同 执行
		Double allocateScore = redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET)
				.score(DUPLICATE_ALLOCATE);
		if (allocateScore != null && allocateScore == LAST_ALLOCATE_SCORE) {
			log.debug("AllocOTask >> no need to allocate , due to the same score with last time. {}", allocateScore);
			Thread.sleep(2000L);
			return;
		}
		LAST_ALLOCATE_SCORE = allocateScore == null ? 0D : allocateScore;
		if (allocateScore == null) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET)
					.add(DUPLICATE_ALLOCATE, LAST_ALLOCATE_SCORE);
		}
		int len = lorder();
		if (len == 0) {
			log.debug("AllocOTask >> no task to allocate .");
			Thread.sleep(2000L);
			return;
		}

		boolean enableInBank = CommonUtils.isEnableInBankOutTask();

		boolean isEnableInbankHandicap = false;

		log.debug("AllocOTask >> origin data . enIn: {} ,oTCnt: {}", enableInBank, len);
		boolean remove = System.currentTimeMillis() > LAST_TIME_REMOVE_DUPLICATE;
		LAST_TIME_REMOVE_DUPLICATE = System.currentTimeMillis() + 690000;
		for (int index = 0; index < len; index++) {
			long curr = System.currentTimeMillis();
			AllocatingOutwardTask tasking = null, ated = null;
			String allocating = null;
			Boolean peerTrans = null;// 同行转账
			boolean maintain = false;// 银行维护
			try {
				allocating = lpop();// 从redis中获取一条待分配任务
				if (allocating == null) {
					log.debug("AllocOTask >> already pop all task .");
					return;
				}

				tasking = new AllocatingOutwardTask(allocating);
				if (remove) {
					log.debug("AllocOTask >> clear invalid data.");
					continue;
				}
				// int taskAmount = tasking.getTaskAmount().intValue();
				// 分配条件
				// 1.同行转账模式关闭中，且：收款银行不处理维护中
				// 2.同行转账模式处于打开中
				peerTrans = checkPeerTrans(tasking.getToAccountBank());
				isEnableInbankHandicap = CommonUtils.checkEnableInBankHandicap(tasking.getHandicap());
				log.debug("AllocOTask >> allocating: {} , handicap: {} , enIn: {}", allocating, tasking.getHandicap(),
						isEnableInbankHandicap);
				if (peerTrans == null) {
					maintain = checkMaintain(tasking.getToAccountBank());
					if (!maintain) {
						// 盘口开启入款卡、备用卡出款，优先从入款卡找匹配的卡、其次找备用卡匹配的卡、最后找出款卡
						if (isEnableInbankHandicap) {
							ated = allocate4In(tasking, null);
						}
						if (ated == null) {
							log.debug("AllocOTask >> Ot oId: {} , oAmt: {} consume: {}", tasking.getTaskId(),
									tasking.getTaskAmount(), (curr - System.currentTimeMillis()));
							curr = System.currentTimeMillis();
							ated = allocate(tasking, null);
						}
					}
				} else if (peerTrans) {
					if (isEnableInbankHandicap) {
						ated = allocate4In(tasking, true);
					}
					if (ated == null) {
						log.debug("AllocOTask >> PeerOt oId: {} , oAmt: {}  consume: {}", tasking.getTaskId(),
								tasking.getTaskAmount(), (curr - System.currentTimeMillis()));
						curr = System.currentTimeMillis();
						ated = allocate(tasking, true);
					}
				}
			} catch (Exception e) {
				log.debug("AllocOTask >> ex0 oId: {} , oAmt: {} , enIn: {} , error: {} consume: {}",
						tasking.getTaskId(), tasking.getTaskAmount(), isEnableInbankHandicap, e.getLocalizedMessage(),
						(curr - System.currentTimeMillis()));
				curr = System.currentTimeMillis();
				if (EXCP_NONE_TARGET.equals(e.getMessage())) {
					break;
				}
			} finally {
				if (allocating != null) {
					if ((peerTrans != null && !peerTrans) || maintain) {
						saveMaintain(tasking);
						String[] msg = { allocating };
						lback(msg, null, null);
					} else {
						try {
							log.debug(
									"AllocOTask >> fi0 oId: {} , oAmt: {} , enIn: {} , accId: {} , userId: {}  consume: {}",
									tasking.getTaskId(), tasking.getTaskAmount(), isEnableInbankHandicap,
									(ated != null ? ated.getFromId() : null),
									(ated != null ? ated.getOperator() : null), (curr - System.currentTimeMillis()));
							curr = System.currentTimeMillis();
							if (ated != null) {
								// 入款卡、备用卡分配任务后，需要写redis数据，数据有效时间为1天，防止入款卡往备用卡或备用卡往备用卡转账
								Integer fromId = ated.getFromId();
								if (fromId != null) {
									AccountBaseInfo base = accountService.getFromCacheById(fromId);
									if (Objects.equals(INBANK, base.getType())
											|| Objects.equals(RESERVEBANK, base.getType())
											|| Objects.equals(BINDCUSTOMER, base.getType())) {
										allocateTransService.lockTrans(base.getId(), ated.getTaskId().intValue(),
												AppConstants.USER_ID_4_ADMIN, ated.getTaskAmount().intValue(), 86400);
									} else if (Objects.equals(OUTBANK, base.getType())
											&& CommonUtils.getTransLockFromTo()) {// 出款卡锁定：120秒
										allocateTransService.lockTrans(base.getId(), ated.getTaskId().intValue(),
												AppConstants.USER_ID_4_ADMIN, ated.getTaskAmount().intValue(), 120);
									}
								}
								log.debug(
										"AllocOTask >> sv oId: {} , oAmt: {} , enIn: {} , accId: {} , userId: {}   consume1: {}",
										tasking.getTaskId(), tasking.getTaskAmount(), isEnableInbankHandicap,
										ated.getFromId(), ated.getOperator(), (curr - System.currentTimeMillis()));
								curr = System.currentTimeMillis();
								saveAllocated(ated);
								log.debug(
										"AllocOTask >> sv oId: {} , oAmt: {} , enIn: {} , accId: {} , userId: {}   consume2: {}",
										tasking.getTaskId(), tasking.getTaskAmount(), isEnableInbankHandicap,
										ated.getFromId(), ated.getOperator(), (curr - System.currentTimeMillis()));
							}
						} catch (Exception e) {
							log.debug(
									"AllocOTask >> ex1 oId: {} , oAmt: {} , enIn: {} , accId: {} , userId: {} ,error: {}",
									tasking.getTaskId(), tasking.getTaskAmount(), isEnableInbankHandicap,
									(ated != null ? ated.getFromId() : null),
									(ated != null ? ated.getOperator() : null), e.getLocalizedMessage());
							if (ated != null) {
								addOrRemoveAllocated(ated.getOperator() == null ? ated.getFromId() : ated.getOperator(),
										true);
							}
							ated = null;
						} finally {
							if (remove) {
								boolean al = false;
								if (Objects.equals(tasking.getTaskType(), 0)) {
									BizOutwardTask task = outwardTaskRepository.findById2(tasking.getTaskId());
									if (Objects.isNull(task)
											|| !Objects.equals(task.getStatus(),
													OutwardTaskStatus.Undeposit.getStatus())
											|| Objects.nonNull(task.getAccountId())
											|| Objects.nonNull(task.getOperator())) {
										al = true;
									}
								} else if (Objects.equals(tasking.getTaskType(), 1)) {
									BizAccountRebate task = accountRebateRepository.findById2(tasking.getTaskId());
									if (Objects.isNull(task)
											|| !Objects.equals(task.getStatus(),
													OutwardTaskStatus.Undeposit.getStatus())
											|| Objects.nonNull(task.getAccountId())
											|| Objects.nonNull(task.getOperator())) {
										al = true;
									}
								}
								String[] msg = { allocating };
								if (al) {
									lback(msg, null, null);
									log.info("RemoveDuplicate >> {}", allocating);
								} else {
									lback(null, msg, null);
								}
							} else {
								String[] msg = { allocating };
								lback(ated == null ? null : msg, ated == null ? msg : null,
										ated == null ? null : ated.getTarget());
								if (ated != null) {
									addOrRemoveAllocated(
											ated.getOperator() == null ? ated.getFromId() : ated.getOperator(), true);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 取消排队 (机器人)
	 */
	@Override
	public void cancelQueue4Robot(List<Integer> accountIdList) {
		if (CollectionUtils.isEmpty(accountIdList)) {
			return;
		}
		log.debug("出款任务分配 取消排队 (机器人) accountId.size:{}", accountIdList.size());
		List<String> vals = accountIdList.stream()
				.map(p -> String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_ROBOT, p)).collect(Collectors.toList());
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).remove(vals.toArray());
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
				.remove(vals.toArray());
	}

	/**
	 * 取消排队 (机器人)
	 */
	@Override
	public void cancelQueue4Robot(Integer accountId) {
		log.debug("出款任务分配 取消排队 (机器人) accountId:{}", accountId);
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET)
				.remove(String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_ROBOT, accountId));
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
				.remove(String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_ROBOT, accountId));
	}

	/**
	 * 申请 出款任务 (机器)
	 */
	@Override
	public TransferEntity applyTask4Robot(int accountId, BigDecimal bankBalance) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		if (Objects.isNull(base) || Objects.nonNull(base.getHolder())) {
			return null;// 人工出款：立即返回
		}
		// 检测该账号是否处于正常状态, 否：直接返回，是：参与分配
		if (!checkRobot(accountId)) {
			return null;
		}
		TransferEntity ret = buildTransEntity(accountId, false, bankBalance);
		// 入款卡\备用卡\客户绑定卡只通过这个方法获取转账任务，不加到ALLOCATING_OUTWARD_TARGET中
		if (ret != null || base.getType() == INBANK || base.getType() == RESERVEBANK
				|| base.getType() == BINDCUSTOMER) {
			return ret;
		}
		int l = base.getCurrSysLevel() == null ? OUTTER : base.getCurrSysLevel();
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		String val = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_ROBOT, accountId);
		Double scoreHis = template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).score(val);
		int handicapId = handicapService.findHandiByHandiId(base.getHandicapId());
		int zone = handicapService.findZoneByHandiId(handicapId);
		handicapId = handicapService.checkDistHandi(zone) ? handicapId : zone;
		Double scoreNow = score4zset(l, TARGET_TYPE_ROBOT, zone, handicapId, bankBalance);
		if (scoreHis == null || !scoreHis.equals(scoreNow)) {
			template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).add(val, scoreNow);
			template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE, score4zset());
		}
		return ret;
	}

	/**
	 * 申请 出款任务 (机器) 新版本
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public TransferEntity applyTask4RobotNew(int accountId, BigDecimal bankBalance) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		if (Objects.isNull(base) || Objects.nonNull(base.getHolder())) {
			return null;// 人工出款：立即返回
		}
		// 检测该账号是否处于正常状态, 否：直接返回，是：参与分配
		if (!checkRobot(accountId)) {
			return null;
		}
		TransferEntity ret = buildTransEntity(accountId, true, bankBalance);
		// 备用卡，下发看只通过这边取任务，不写ALLOCATING_NEW_OUTWARD_TARGET
		if (ret != null || base.getType() == RESERVEBANK || base.getType() == BINDCOMMON
				|| (base.getType() == INBANK && base.getFlag().equals(AccountFlag.PC.getTypeId()))) {
			return ret;
		}
		if (Objects.isNull(bankBalance)) {
			return null;
		}
		// 判断是否有任务正在执行
		if (allocateTransService.hasTrans(base.getId())) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
					.remove(String.valueOf(base.getId()));
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.remove(String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_MOBILE, base.getId()));
			log.info("请求任务的银行卡,有任务正在执行:{}", base.getId());
			return null;
		}

		// 判断是否有任务完成未对账成功
		Set<Integer> accountingException = systemAccountManager.accountingException();
		Set<Integer> accountingSuspend = systemAccountManager.accountingSuspend();
		if (accountingException.contains(base.getId()) || accountingSuspend.contains(base.getId())) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
					.remove(String.valueOf(base.getId()));
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.remove(String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_ROBOT, base.getId()));
			log.info("请求任务的银行卡,有任务完成未对账成功:{}", base.getId());
			return null;
		}
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		int l = base.getCurrSysLevel() == null ? OUTTER : base.getCurrSysLevel();
		// 出款队列的key值格式
		String valOut = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_ROBOT, accountId);
		Double scoreHis = template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).score(valOut);
		// 下发队列的key值格式
		String valIn = String.valueOf(base.getId());
		int handicapId = handicapService.findHandiByHandiId(base.getHandicapId());
		int zone = handicapService.findZoneByHandiId(handicapId);
		handicapId = handicapService.checkDistHandi(zone) ? handicapId : zone;
		Object model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
				.get(base.getId().toString());
		int modelVal = model == null ? Constants.YSF_MODEL_IN : Integer.valueOf(model.toString());
		// 专注入款卡与非专注写入下发队列（非专注判断余额是否超过设定的信用额度）
		if (base.getType() == INBANK && !Objects.equals(base.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())
				&& modelVal == Constants.YSF_MODEL_IN) {
			if (!base.getFlag().equals(AccountFlag.PC.getTypeId())) {
				// 余额达到信用额度百分比 先看入款卡数量较少就转入款卡（判断逻辑待定），否则转出款卡出款
				double limitPercentage = Objects.isNull(base.getLimitPercentage())
						? CommonUtils.getInBankOutCreditPercentage()
						: base.getLimitPercentage().doubleValue();
				// 计算专注入款卡余额是否超过设定的信用额度百分比
				BigDecimal percentage = BigDecimal.valueOf(limitPercentage / 100);
				int toler = BigDecimal.valueOf(accountChangeService.margin(base)).multiply(percentage).intValue();
				// 判断余额是否超过设定的信用额度百分比
				if (bankBalance != null && bankBalance.intValue() > toler) {
					// 余额超过设定的信用额度百分比，切换成出款模式，写入target
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
							.put(base.getId().toString(), String.valueOf(Constants.YSF_MODEL_OUT));
					redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(valIn);
					cabanaService.inOutModel(base.getId(), Constants.YSF_MODEL_OUT + "");
					inAccountService.increaseInAccountExceed(base.getId());
					inAccountService.increaseInAccountExceedByLevel(base.getId());
					Double scoreNow = score4zset(l, TARGET_TYPE_ROBOT, zone, handicapId, bankBalance);
					if (scoreHis == null || !scoreHis.equals(scoreNow)) {
						template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(valOut, scoreNow);
						template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE,
								score4zset());
						log.info("ApplyByIn (id:{},bal:{}) >> update the real balance in redis.", base.getId(),
								bankBalance);
					}
					return ret;
				} else {
					redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(valIn);
					// 余额没超过信用额度百分比，不做操作
					return null;
				}
			}
			int limitBalance = buildLimitBalance(base);
			// 专注入款卡大于余额告警值才写入from队列
			if (bankBalance.intValue() < limitBalance) {
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(valIn);
				return null;
			}
			Double scr = allocateTransService.enScore4Fr(base.getType(), zone, l, handicapId, bankBalance.intValue());
			template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).add(valIn, scr);
			log.info("ApplyByIn (id:{},bal:{}) >> update the real balance in redis.", base.getId(), bankBalance);
			return null;
		} else {
			// 非专注入款卡为出款卡模式
			if (modelVal == Constants.YSF_MODEL_OUT) {
				// minBalance为空取配置表默认值
				int minBalance = Objects.nonNull(base.getMinBalance()) ? base.getMinBalance().intValue()
						: CommonUtils.getInBankCloseOutLowestBalance();
				if (bankBalance.intValue() < minBalance) {
					// 非专注入款卡模式为出款模式，余额低于设置的最低余额转入款卡入款
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
							.put(base.getId().toString(), String.valueOf(Constants.YSF_MODEL_IN));
					redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
							.remove(valOut);
					cabanaService.inOutModel(base.getId(), Constants.YSF_MODEL_IN + "");
					redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(valIn);
					return null;
				}
			}
			Double scoreNow = score4zset(l, TARGET_TYPE_ROBOT, zone, handicapId, bankBalance);
			if (bankBalance.intValue() > 100 && (scoreHis == null || !scoreHis.equals(scoreNow))) {
				template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(valOut, scoreNow);
				template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE, score4zset());
				log.info("ApplyByIn (id:{},bal:{}) >> update the real balance in redis.", base.getId(), bankBalance);
			}
		}
		return ret;
	}

	/**
	 * 申请 出款任务 (人工) TODO 返利任务暂时只分配给机器，后续如果分配人工，相应的取任务、上报再进行修改
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public BizOutwardTask applyTask4User(int userId) {
		BizOutwardTask ret = outwardTaskRepository.applyTask4User(userId, OutwardTaskStatus.Undeposit.getStatus());
		if (Objects.nonNull(ret)) {
			log.info("申请出款任务人工 => userId: {} taskId: {} orderNo: {} amt: {}", userId, ret.getId(), ret.getOrderNo(),
					ret.getAmount());
			systemAccountManager.registMan(ret);
			return ret;
		}
		SysUser oper = userService.findFromCacheById(userId);
		if (SUSPEND_OPERATOR.getIfPresent(userId) == null && oper != null
				&& (UserCategory.Outward.getCode() == oper.getCategory()
						|| UserCategory.Finance.getCode() == oper.getCategory())
				&& userService.online(userId) && Objects.nonNull(oper.getHandicap())) {
			boolean third = AppConstants.OUTDREW_THIRD && UserCategory.Finance.getCode() == oper.getCategory();
			if (third) {
				Boolean thirdRight = THIRD_RIGHT_OUTWARD_USER.getIfPresent(userId);
				if (Objects.isNull(thirdRight)) {
					String sql = "select user_id from sys_user_role r,sys_role_menu_permission rm,sys_menu_permission m where r.role_id=rm.role_id and m.id=rm.menu_permission_id and m.permission_key='OutwardTask:ThirdMgrReseiveOrder:*' and user_id='"
							+ userId + "'";
					thirdRight = entityManager.createNativeQuery(sql).getResultList().size() > 0;
					THIRD_RIGHT_OUTWARD_USER.put(userId, thirdRight);
				}
				if (!thirdRight)
					return null;
			}
			int TARGET_TYPE = third ? TARGET_TYPE_THIRD : TARGET_TYPE_USER;
			BigDecimal bal = third ? new BigDecimal("9999999") : new BigDecimal("49999");
			BizHandicap handi = handicapService.findFromCacheById(oper.getHandicap());
			if (Objects.isNull(handi)) {
				log.error("人工出款用户校验信息未通过：用户ID {} ，用户所属盘口不存在", userId);
				return null;
			}
			int handicapId = handicapService.findHandiByHandiId(oper.getHandicap());
			int zone = handicapService.findZoneByHandiId(handicapId);
			handicapId = handicapService.checkDistHandi(zone) ? handicapId : zone;
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			String val = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE, userId);
			Double scoreHis = template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).score(val);
			Double scoreNewHis = template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).score(val);
			Double scoreNow = score4zset(OUTTER, TARGET_TYPE, zone, handicapId, bal);// 条件假设
			if (scoreHis == null || scoreNewHis == null || !scoreHis.equals(scoreNow)
					|| !scoreNewHis.equals(scoreNow)) {
				log.info("AllocOTask >> applyTask4User (id: {} uid: {} )", oper.getId(), oper.getUid());
				template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).add(val, scoreNow);
				template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE, score4zset());
				template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(val, scoreNow);
				template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE, score4zset());
			}
		} else {
			log.error("人工出款用户校验信息未通过：用户ID {} ，是否10秒停止接单 {} ，用户信息是否存在 {} ，用户角色 {} ，用户是否在线 {} ，用户所属盘口 {}", userId,
					SUSPEND_OPERATOR.getIfPresent(userId) == null, oper != null, oper.getCategory(),
					userService.online(userId), oper.getHandicap());
		}
		return null;
	}

	/**
	 * 申请 出款任务 (手机)
	 */
	@Override
	public TransferEntity applyTask4Mobile(int accountId, BigDecimal bankBalance) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		if (Objects.isNull(base) || Objects.nonNull(base.getHolder())) {
			return null;// 人工出款：立即返回
		}
		// 检测该账号是否处于正常状态, 否：直接返回，是：参与分配
		if (!checkMobileNew(accountId)) {
			return null;
		}
		// 开启新版本，调用新的请求接口
		if (CommonUtils.checkDistHandicapNewVersion(base.getHandicapId())) {
			return applyTask4MobileNew(accountId, bankBalance);
		}
		TransferEntity ret = buildTransEntity(accountId, true, bankBalance);
		if (ret != null) {
			return ret;
		}
		int l = base.getCurrSysLevel() == null ? OUTTER : base.getCurrSysLevel();
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		String val = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_MOBILE, accountId);
		Double scoreHis = template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).score(val);
		int handicapId = handicapService.findHandiByHandiId(base.getHandicapId());
		int zone = handicapService.findZoneByHandiId(handicapId);
		handicapId = handicapService.checkDistHandi(zone) ? handicapId : zone;
		Double scoreNow = score4zset(l, TARGET_TYPE_MOBILE, zone, handicapId, bankBalance);
		if (scoreHis == null || !scoreHis.equals(scoreNow)) {
			template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).add(val, scoreNow);
			template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE, score4zset());
		}
		return null;
	}

	/**
	 * 申请 出款任务 (手机) 新版本
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public TransferEntity applyTask4MobileNew(int accountId, BigDecimal bankBalance) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		if (Objects.isNull(base) || !base.checkMobile()) {
			return null;// 人工出款：立即返回
		}
		// 检测该账号是否处于正常状态, 否：直接返回，是：参与分配
		if (!checkRobot(accountId)) {
			return null;
		}
		TransferEntity ret = buildTransEntity(accountId, true, bankBalance);
		// 备用卡、下发卡只通过这里取任务，不写ALLOCATING_NEW_OUTWARD_TARGET
		if (ret != null || base.getType() == RESERVEBANK || base.getType() == BINDCOMMON
				|| (base.getType() == INBANK && base.getFlag().equals(AccountFlag.PC.getTypeId()))) {
			return ret;
		}
		if (Objects.isNull(bankBalance)) {
			return null;
		}
		// 判断是否有任务正在执行
		if (allocateTransService.hasTrans(base.getId())) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
					.remove(String.valueOf(base.getId()));
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.remove(String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_MOBILE, base.getId()));
			log.info("请求任务的银行卡,有任务正在执行:{}", base.getId());
			return null;
		}
		// 判断是否有任务完成未对账成功
		Set<Integer> accountingException = systemAccountManager.accountingException();
		Set<Integer> accountingSuspend = systemAccountManager.accountingSuspend();
		if (accountingException.contains(base.getId()) || accountingSuspend.contains(base.getId())) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
					.remove(String.valueOf(base.getId()));
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.remove(String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_MOBILE, base.getId()));
			log.info("请求任务的银行卡,有任务完成未对账成功:{}", base.getId());
			return null;
		}
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		int l = base.getCurrSysLevel() == null ? OUTTER : base.getCurrSysLevel();
		// 出款队列的key值格式
		String valOut = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_MOBILE, accountId);
		Double scoreHis = template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).score(valOut);
		// 下发队列的key值格式
		String valIn = String.valueOf(base.getId());
		int handicapId = handicapService.findHandiByHandiId(base.getHandicapId());
		int zone = handicapService.findZoneByHandiId(handicapId);
		handicapId = CommonUtils.checkDistHandicapAllocateOutAndIn(handicapId) ? handicapId : zone;
		Object model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
				.get(base.getId().toString());
		int modelVal = model == null ? Constants.YSF_MODEL_IN : Integer.valueOf(model.toString());
		// 专注入款卡与非专注写入下发队列（非专注判断余额是否超过设定的信用额度）
		if (base.getType() == INBANK && !Objects.equals(base.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())
				&& modelVal == Constants.YSF_MODEL_IN) {
			// 判断是否非专注入款卡
			if (!Objects.equals(base.getFlag(), AccountFlag.PC.getTypeId())) {
				// 获取非专注入款卡信用额度
				double limitPercentage = Objects.isNull(base.getLimitPercentage())
						? CommonUtils.getInBankOutCreditPercentage()
						: base.getLimitPercentage().doubleValue();
				// 获取非专注入款卡余额设定百分比值
				BigDecimal percentage = BigDecimal.valueOf(limitPercentage / 100);
				// 计算信用额度乘以百分比
				int toler = BigDecimal.valueOf(accountChangeService.margin(base)).multiply(percentage).intValue();
				// 判断余额是否超过设定的信用额度百分比值
				if (bankBalance.intValue() > toler || !allocateTransService.checkDailyIn(base)) {
					// 切换出款模式
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
							.put(base.getId().toString(), String.valueOf(Constants.YSF_MODEL_OUT));
					cabanaService.inOutModel(base.getId(), Constants.YSF_MODEL_OUT + "");
					redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(valIn);
					inAccountService.increaseInAccountExceed(base.getId());
					inAccountService.increaseInAccountExceedByLevel(base.getId());
					Double scoreNow = score4zset(l, TARGET_TYPE_MOBILE, zone, handicapId, bankBalance);
					if (scoreHis == null || !scoreHis.equals(scoreNow)) {
						template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(valOut, scoreNow);
						template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE,
								score4zset());
						log.info("ApplyByIn (id:{},bal:{}) >> update the real balance in redis.", base.getId(),
								bankBalance);
					}
					return ret;
				} else {
					redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(valIn);
					// 余额没超过信用额度百分比，不做操作
					return null;
				}
			}
			int limitBalance = buildLimitBalance(base);
			// 专注入款卡大于余额告警值才写入from队列
			if (bankBalance.intValue() < limitBalance) {
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(valIn);
				return null;
			}
			Double scr = allocateTransService.enScore4Fr(base.getType(), zone, l, handicapId, bankBalance.intValue());
			template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).add(valIn, scr);
			log.info("ApplyByIn (id:{},bal:{}) >> update the real balance in redis.", base.getId(), bankBalance);
			return null;
		} else {
			// 非专注入款卡为出款卡模式
			if (modelVal == Constants.YSF_MODEL_OUT) {
				// minBalance为空取配置表默认值
				int minBalance = Objects.nonNull(base.getMinBalance()) ? base.getMinBalance().intValue()
						: CommonUtils.getInBankCloseOutLowestBalance();
				if (bankBalance.intValue() < minBalance) {
					// 非专注入款卡模式为出款模式，余额低于设置的最低余额转入款卡入款
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
							.put(base.getId().toString(), String.valueOf(Constants.YSF_MODEL_IN));
					redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
							.remove(valOut);
					redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(valIn);
					cabanaService.inOutModel(base.getId(), Constants.YSF_MODEL_IN + "");
					return null;
				}
			}
			Double scoreNow = score4zset(l, TARGET_TYPE_MOBILE, zone, handicapId, bankBalance);
			if (bankBalance.intValue() > 100 && (scoreHis == null || !scoreHis.equals(scoreNow))) {
				template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(valOut, scoreNow);
				template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE, score4zset());
				log.info("ApplyByIn (id:{},bal:{}) >> update the real balance in redis.", base.getId(), bankBalance);
			}
		}
		return ret;
	}

	/**
	 * 确认 转账结果(机器出款)
	 */
	@Override
	@Transactional
	public void ack4Robot(TransferEntity entity) {
		redisService.convertAndSend(RedisTopics.DEL_ALLOCATEDID_AFTER_TRANSACK, entity.getFromAccountId().toString());
		if (Objects.isNull(entity.getTaskId())) {
			if (Objects.nonNull(entity.getToAccountId()) && Objects.nonNull(entity.getAmount())) {
				int type = accountService.getFromCacheById(entity.getToAccountId()).getType();
				if (type == RESERVEBANK && entity.getResult() != null && entity.getResult() == 1) {
					incomeRequestService.incrementAmount(entity.getToAccountId(),
							BigDecimal.valueOf(entity.getAmount()));
				}
				allocateTransService.ackTrans(entity);
			}
			return;
		}
		boolean success = entity.getResult() != null && entity.getResult() == 1;
		log.debug("机器出款 确认操作. taskId:{},fromAccountId:{} success:{},banlance:{}", entity.getTaskId(),
				entity.getFromAccountId(), success, entity.getBalance());

		Date time = entity.getTime() == null ? new Date() : entity.getTime();
		ack(success, entity.getTaskId(), null, entity.getRemark(), entity.getFromAccountId(), time,
				entity.getScreenshot(), false, null);
		// 提交本次交易信息到REDIS
		lack(entity);
		// 银行流水监控：
		if (AppConstants.NEW_TRANSFER) {
			transMonitorService.reportTransResult(entity);
			allocateTransService.applyRelBal(entity.getFromAccountId(),
					entity.getBalance() != null ? new BigDecimal(entity.getBalance()) : null);
		} else if (entity.getBalance() != null) {
			log.info("OutwardTaskMonitor AccountId={},account:{} balance:{}", entity.getFromAccountId(),
					entity.getAccount(), entity.getBalance());
			allocTransSer.applyRelBal(entity.getFromAccountId(), new BigDecimal(entity.getBalance()), false);
		}
	}

	/**
	 * 完成出款 (人工出款)
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void ack4User(Long taskId, Integer operator, String remark, Integer accountId, boolean thirdOut,
			String platPayCode) {
		accountId = thirdOut && !remark.contains("调用代付成功") ? null : accountId;
		log.info("完成出款人工=>userId:{} taskId:{} accountId:{}  remark:{} ", operator, taskId, accountId, remark);
		// 防止上报后，对应的translock中的数据没有删除，影响后续出款，这里进行解锁
		allocateTransService.llockUpdStatus(accountId == null ? 0 : accountId, taskId == null ? 0 : taskId.intValue(),
				TransLock.STATUS_DEL);
		ack(true, taskId, operator, remark, accountId, new Date(), StringUtils.EMPTY, thirdOut, platPayCode);
		redisService.convertAndSend(RedisTopics.DEL_ALLOCATEDID_AFTER_TRANSACK, operator.toString());
	}

	/**
	 * 保存客服备注
	 */
	@Override
	@Transactional
	public void remark4Custom(long taskId, SysUser operator, String remark) {
		BizOutwardTask task;
		if (StringUtils.isBlank(remark) || (task = outwardTaskRepository.findById2(taskId)) == null) {
			return;
		}
		if (remark.contains("已完成交易最后一步")) {
			redisService.getStringRedisTemplate().boundSetOps(RedisKeys.TASK_REPORT_LAST_STEP).add(taskId + "");
		}
		remark = CommonUtils.genRemark(task.getRemark(), remSpecialChars(remark), new Date(),
				operator == null ? SYS : operator.getUid());
		outwardTaskRepository.updateStatusAndRemark(taskId, task.getStatus(), task.getStatus(), remark,
				task.getScreenshot());
	}

	@Override
	public void remark4Mgr(int accId, boolean isManual, boolean isThird, SysUser operator, String remark)
			throws Exception {
		BizOutwardTask task = outwardTaskRepository.applyTask(accId, OutwardTaskStatus.Undeposit.getStatus());
		if (Objects.nonNull(task)) {
			allocateTransService.llockUpdStatus(accId, task.getId().intValue(), TransLock.STATUS_DEL);
			remark4Mgr(task.getId(), isManual, isThird, operator, null, remark);
			return;
		}
		// 工具端只提供frAcc 查找一键转出或激活的任务上报
		TransferEntity entity = allocateTransService.activeAccByTest(accId, false);
		if (Objects.nonNull(entity)) {
			allocateTransService.ackTrans(entity);
		}
	}

	/**
	 * 备注 (主管)</br>
	 * 此处不要加事务处理，万一出现数据不一致情况，人工排查。</br>
	 * 加事务可能会出现重复出款现象
	 */
	@Override
	public void remark4Mgr(long taskId, boolean isManual, boolean isThird, SysUser operator, String[] bankTypeArr,
			String remark) throws Exception {
		BizOutwardTask task = outwardTaskRepository.findById2(taskId);
		// 校验： 只有Undeposit，ManagerDeal 才有重新分配功能;
		// 当状态为未出款时，如果分配给机器，则不能重新分配</br>
		// 当状态为未出款时，如果分配给人工，则本人可以点击重新分配</br>
		// 当状态为主管处理时候，则可以重新分配
		remark = StringUtils.trimToEmpty(remark);
		if (task == null
				|| (!OutwardTaskStatus.ManagerDeal.getStatus().equals(task.getStatus())
						&& !OutwardTaskStatus.Undeposit.getStatus().equals(task.getStatus()))
				|| (OutwardTaskStatus.Undeposit.getStatus().equals(task.getStatus()) && task.getOperator() == null
						&& task.getAccountId() != null
						&& (operator == null || operator.getId() != AppConstants.USER_ID_4_ADMIN))
				|| (OutwardTaskStatus.Undeposit.getStatus().equals(task.getStatus()) && task.getOperator() != null
						&& (operator == null || !Objects.equals(task.getOperator(), operator.getId())))
				|| (OutwardTaskStatus.ManagerDeal.getStatus().equals(task.getStatus())
						&& operator.getId() == AppConstants.USER_ID_4_ADMIN && !remark.contains("机器转出机器出款3"))) {
			log.info("AllocOTaskByMgr >> taskId:{} 校验失败 只有Undeposit，ManagerDeal 才有重新分配功能;当状态为未出款时，如果已分配，则不能重新分配",
					taskId);
			return;
		}
		if (!isManual && StringUtils.isNotBlank(remark)
				&& (remark.contains("无法识别转账银行") || ((remark.contains("机器转出机器出款") || remark.contains("超过15分钟,无结果机器转出"))
						&& Objects.nonNull(task.getRemark())
						&& CommonUtils.appearNumber(task.getRemark(), "机器转出") >= 1))) {
			isManual = true;
		}
		Date d = new Date();
		remark = remSpecialChars(remark);
		int thirdLowBal = findThirdLowBal();
		BizOutwardRequest o = outwardRequestService.get(task.getOutwardRequestId());
		isThird = AppConstants.OUTDREW_THIRD && isThird && task.getAmount().floatValue() >= thirdLowBal;
		if (AppConstants.OUTDREW_THIRD && !isThird && task.getAmount().floatValue() >= thirdLowBal) {
			String prefix = remark + "(第三方转" + (isManual ? "人工" : "机器") + ",本任务无效并拆单)";
			String rem = CommonUtils.genRemark(task.getRemark(), prefix, d, operator == null ? SYS : operator.getUid());
			int ret = outwardTaskRepository.updateStatusAndRemark(taskId, task.getStatus(),
					OutwardTaskStatus.Invalid.getStatus(), rem, task.getScreenshot());
			if (ret == 0) {
				log.info("AllocOTaskByMgr >> taskId:{} 第三方转人工或机器 历史任务置无效失败.", task.getId());
				return;
			}
			int splitAddAmount = 100;//拆单叠加基数，每单在上一单基础上增加100
			BizLevel l = levelService.findFromCache(o.getLevel());// 出款任务内外层
			Integer InOut = l != null && l.getCurrSysLevel() != null ? l.getCurrSysLevel() : OUTTER;
			int split = InOut == OUTTER ? findSplitOut() : findSplitIn();// 出款拆单金额
			List<BigDecimal> subAmtList = new ArrayList<>();// 子任务金额
			int multiples = (int) (task.getAmount().floatValue() / split);
			float remainder = task.getAmount().floatValue() % split;
			int splitAddAmountTotal = ((multiples - 1) * 50 + splitAddAmount) * multiples; //根据拆单的数量，计算出额外增加的金额总数
			//余数减去整单额外增加的基数总金额,如果为负数则第一单减去相应金额
			remainder = remainder - splitAddAmountTotal;
			for (int i = 0; i < multiples; i++) {
				BigDecimal amt = i == 0 && remainder < 5000 ? new BigDecimal(split).add(new BigDecimal(remainder))
						: new BigDecimal(split);
				//第一单从100开始，每单在上一单增加的金额上+100，防止拆单出现相同的金额(解决相同金额过多，机器无法连续出相同金额的任务)
				amt = amt.add(new BigDecimal(splitAddAmount * (i + 1)));
				subAmtList.add(amt);
			}
			if (remainder >= 5000) {
				subAmtList.add(new BigDecimal(remainder));
			}
			log.info("AllocOTaskByMgr >> taskId:{} 第三方转人工或机器 拆单任务数:subTaskSize:{}", taskId, subAmtList.size());
			prefix = remark + "(第三方转" + (isManual ? "人工" : "机器") + ",oId:" + task.getId() + ",oAmt:" + task.getAmount()
					+ ",nAmt:";
			for (BigDecimal sub : subAmtList) {
				BizOutwardTask newO = new BizOutwardTask();
				BeanUtils.copyProperties(newO, task);
				String newRem = addFlowChars(OutwardTaskStatus.Undeposit, prefix + sub + ")" + RE_ALLOCATE);// 请勿改动
				newRem = CommonUtils.genRemark(task.getRemark(), newRem, d, operator == null ? SYS : operator.getUid());
				newO.updProperties(null, sub, null, null, null, newRem, OutwardTaskStatus.Undeposit.getStatus());
				// 如果是人工出款，设置为人工，否则设置为返利网
				newO.setOutwardPayType(isManual ? OutWardPayType.MANUAL.getType() : OutWardPayType.REFUND.getType());
				outwardTaskService.save(newO);
				rpush(o, newO, false, isManual, isThird);
			}
		} else {
			log.info("AllocOTaskByMgr >> taskId:{} 重新分配 isManual:{},isThird:{}", taskId, isManual, isThird);
			String newRem = addFlowChars(OutwardTaskStatus.Undeposit, remark + RE_ALLOCATE);// 请勿改动
			newRem = CommonUtils.genRemark(task.getRemark(), newRem, d, operator == null ? SYS : operator.getUid());
			TransLock lock = allocateTransService.buildLockToId(false, task.getId().intValue());
			if (lock != null && lock.getFrId() != null) {
				allocateTransService.llockUpdStatus(lock.getFrId(), task.getId().intValue(), TransLock.STATUS_DEL);
			}
			task.updProperties(task.getId(), task.getAmount(), null, null, null, newRem,
					OutwardTaskStatus.Undeposit.getStatus());
			task.setOutwardPayType(isThird ? OutWardPayType.ThirdPay.getType()
					: (isManual ? OutWardPayType.MANUAL.getType() : OutWardPayType.REFUND.getType()));
			outwardTaskService.save(task);
			rpush(o, task, false, isManual, isThird);
		}
		saveBankTypeForTaskToUse(bankTypeArr, taskId);
	}

	@Override
	@Transactional
	public void updateBizOutwardTask(BizOutwardTask o, TransferEntity entity) {
		String remark = o.getRemark();
		remark = remSpecialChars(remark);
		remark = StringUtils.trimToEmpty(remark) + "出款成功";
		AccountBaseInfo base = accountService.getFromCacheById(entity.getFromAccountId());
		remark = addFlowChars(OutwardTaskStatus.Deposited, (remark + "-"
				+ (base == null || StringUtils.isBlank(base.getAlias()) ? entity.getFromAccountId() : base.getAlias())
				+ "-" + ROBOT));
		Date time = entity.getTime() == null ? new Date() : entity.getTime();
		o.setRemark(CommonUtils.genRemark(o.getRemark(), remark, time, SYS));
		o.setTimeConsuming(((int) (time.getTime() - o.getAsignTime().getTime())) / 1000);
		o.setScreenshot(entity.getScreenshot());
		outwardTaskRepository.saveAndFlush(o);
	}

	// 缓存为此任务选择的出款卡银行类型供分配时指定特定银行类型的出款卡
	private void saveBankTypeForTaskToUse(String[] bankTypeArr, long taskId) {
		String key = RedisKeys.gen4OutwardTaskBank(taskId);
		if (bankTypeArr != null && bankTypeArr.length > 0) {
			String banks = StringUtils.join(bankTypeArr, ";");
			redisService.getStringRedisTemplate().boundValueOps(key).set(banks, 5, TimeUnit.MINUTES);
		} else {
			redisService.getStringRedisTemplate().delete(key);
		}
	}

	/**
	 * 备注(财务确认)
	 *
	 * @throws Exception
	 *             操作者为空</br>
	 *             任务不存在;</br>
	 *             该任务未匹配流水</br>
	 *             出款请求不存在</br>
	 *             该任务非公司用款</br>
	 */
	@Transactional
	@Override
	public void remark4Ack(long taskId, SysUser operator, String remark) throws Exception {
		operator = Objects.requireNonNull(operator, "操作者为空");
		BizOutwardTask task = Objects.requireNonNull(outwardTaskRepository.findById2(taskId), "任务不存在");
		boolean finAcked = Objects.requireNonNull(checkFinAcked(task), "会员出款,财务不能确认");
		if (finAcked) {
			throw new Exception("财务已确认");
		}
		if (!Objects.equals(task.getStatus(), OutwardTaskStatus.Matched.getStatus())) {
			throw new Exception("该任务未匹配流水,不能确认");
		}
		log.info("出款任务->公司用款 财务确认 taskId:{} operator:{} amount:{} remark:{}", taskId, operator.getUid(),
				task.getAmount(), remark);
		remark = CommonUtils.genRemark(task.getRemark(), FIN_ACKED + remSpecialChars(remark), new Date(),
				operator.getUid());
		int ret = outwardTaskRepository.updateStatusAndRemark(taskId, task.getStatus(), task.getStatus(), remark,
				task.getScreenshot());
		if (ret == 0) {
			return;
		}
		// 检测：公司用款任务是否已全部经过财务确认；是,则更改OutwardRequest状态至OutwardRequestStatus.Acknowledged
		long unAckCount = outwardTaskRepository.findByOutwardRequestId(task.getOutwardRequestId()).stream()
				.filter(p -> {
					if (p.getId() == taskId) {
						return false;
					}
					Boolean acked = checkFinAcked(p);
					return Objects.nonNull(acked) && !acked;
				}).count();
		if (unAckCount == 0) {
			BizOutwardRequest oReq = outwardRequestService.get(task.getOutwardRequestId());
			oReq.setStatus(OutwardRequestStatus.Acknowledged.getStatus());
			outwardRequestService.save(oReq);
		}
	}

	/**
	 * 转无效 bankType指定新任务出款账号类型 drawManner 指定出款方式 人工 机器 第三方
	 */
	@Override
	@Transactional
	public void alterStatusToInvalid(long taskId, SysUser operator, String remark, String[] bankType,
			String drawManner) {
		try {
			BizOutwardTask task = outwardTaskRepository.findById2(taskId);
			// 只有待排查任务 才能转无效
			if (task == null || !OutwardTaskStatus.Failure.getStatus().equals(task.getStatus())) {
				return;
			}
			List<BizOutwardTask> hisList = outwardTaskRepository.findByOutwardRequestId(task.getOutwardRequestId());
			if (hisList.stream().filter((p) -> OutwardTaskStatus.Undeposit.getStatus().equals(p.getStatus())
					|| OutwardTaskStatus.ManagerDeal.getStatus().equals(p.getStatus())).count() > 0) {
				return;
			}
			Date d = new Date();
			String remarkOld = addFlowChars(OutwardTaskStatus.Invalid, remSpecialChars(remark));
			remarkOld = CommonUtils.genRemark(task.getRemark(), remark, d, operator == null ? SYS : operator.getUid());
			// 历史 记录
			int ret = outwardTaskRepository.updateStatusAndRemark(taskId, task.getStatus(),
					OutwardTaskStatus.Invalid.getStatus(), remarkOld, task.getScreenshot());
			if (ret == 0) {
				return;
			}
			// 新记录
			BizOutwardTask task1 = new BizOutwardTask();
			task1.setOutwardRequestId(task.getOutwardRequestId());
			task1.setAmount(task.getAmount());
			task1.setStatus(OutwardTaskStatus.Undeposit.getStatus());
			remark = CommonUtils.genRemark(remark, "由" + task.getId() + "生成新任务", d, SYS);
			task1.setRemark(remark);
			task1.setToAccount(task.getToAccount());
			task1.setToAccountOwner(task.getToAccountOwner());
			task1.setHandicap(task.getHandicap());
			task1.setLevel(task.getLevel());
			task1.setMember(task.getMember());
			task1.setOrderNo(task.getOrderNo());
			outwardTaskRepository.saveAndFlush(task1);
			// 把新任务直接重新分配
			if (StringUtils.isNotBlank(drawManner) || (bankType != null && bankType.length > 0)) {
				log.info(
						"AllocateOutwardTaskServiceImpl.alterStatusToInvalid turn to remark4Mgr ,oldTaskId:{},newTaskId:{},drawManner:{},bankType:{}",
						taskId, task1.getId(), drawManner, bankType.toString());
				boolean isManual = StringUtils.isNotBlank(drawManner) && "manual".equals(drawManner);
				if ("thirdOut".equals(drawManner)) {
					remark4Mgr(task1.getId(), isManual, true, operator, bankType, remark);
				} else {
					remark4Mgr(task1.getId(), isManual, false, operator, bankType, remark);
				}
			} else {
				log.info(
						"AllocateOutwardTaskServiceImpl.alterStatusToInvalid did not turn to remark4Mgr ,oldTaskId:{},newTaskId:{},drawManner:{},bankType:{}",
						taskId, task1.getId(), drawManner, bankType);
				// 将任务放到出款任务队列
				BizOutwardRequest req = outwardRequestService.get(task.getOutwardRequestId());
				rpush(req, task1, checkFirst(req.getReview()), true, false);
			}

		} catch (Exception e) {
			log.error("AllocateOutwardTaskServiceImpl.alterStatusToInvalid execute error occured:", e);
		}
	}

	/**
	 * 转失败
	 */
	@Override
	@Transactional
	public void alterStatusToFail(long taskId, SysUser operator, String remark) {
		BizOutwardTask task = outwardTaskRepository.findById2(taskId);
		if (task == null || (!OutwardTaskStatus.Deposited.getStatus().equals(task.getStatus())
				&& !OutwardTaskStatus.Matched.getStatus().equals(task.getStatus()))) {
			log.info("出款任务转待排查>>暂时不能转待排查 taskId:{}", taskId);
			return;
		}
		remark = addFlowChars(OutwardTaskStatus.Failure, remSpecialChars(remark));
		remark = CommonUtils.genRemark(task.getRemark(), remark, new Date(),
				operator == null ? SYS : operator.getUid());
		outwardTaskRepository.updateStatusAndRemark(taskId, task.getStatus(), OutwardTaskStatus.Failure.getStatus(),
				remark, task.getScreenshot());
	}

	/**
	 * 转主管
	 */
	@Override
	@Transactional
	public void alterStatusToMgr(int accId, String remark) {
		BizOutwardTask task = outwardTaskRepository.applyTask(accId, OutwardTaskStatus.Undeposit.getStatus());
		alterStatusToMgr(task, null, remark, null);
		asignFailedTaskService.asignOnTurnToFail(task.getId());
	}

	/**
	 * 转主管
	 */
	@Override
	@Transactional
	public void alterStatusToMgr(long taskId, SysUser operator, String remark, String screenshot) {
		BizOutwardTask task = outwardTaskRepository.findById2(taskId);
		alterStatusToMgr(task, operator, remark, screenshot);
		asignFailedTaskService.asignOnTurnToFail(taskId);
	}

	/**
	 * 转主管
	 */
	@Override
	@Transactional
	public void alterStatusToMgr(BizOutwardTask task, SysUser operator, String remark, String screenshot) {
		if (StringUtils.isNotBlank(remark) && remark.indexOf("第三方出款{") > -1) {
			// 第三方出款转主管
			if (task == null || !OutwardTaskStatus.Undeposit.getStatus().equals(task.getStatus())) {
				return;
			}
		} else {
			// 只能已分配的任务才能转给主管处理
			if (task == null
					|| !OutwardTaskStatus.Undeposit.getStatus().equals(task.getStatus())
							&& !OutwardTaskStatus.Unknown.getStatus().equals(task.getStatus())
					|| task.getAccountId() == null) {
				return;
			}
		}
		remark = addFlowChars(OutwardTaskStatus.ManagerDeal, remSpecialChars(remark));
		remark = CommonUtils.genRemark(task.getRemark(), remSpecialChars(remark), new Date(),
				operator == null ? SYS : operator.getUid());
		outwardTaskRepository.updateStatusAndRemark(task.getId(), task.getStatus(),
				OutwardTaskStatus.ManagerDeal.getStatus(), remark,
				StringUtils.isEmpty(screenshot) ? task.getScreenshot() : screenshot);
	}

	@Override
	@Transactional
	public void sreenshot(BizOutwardTask task, String sreenshot) {
		if (Objects.nonNull(task) && StringUtils.isNotEmpty(sreenshot))
			outwardTaskRepository.updateStatusAndRemark(task.getId(), task.getStatus(), task.getStatus(),
					task.getRemark(), sreenshot);
	}

	/**
	 * 转未知
	 */
	@Transactional
	@Override
	public void alterStatusToUnknown(long taskId, SysUser operator, String remark, String screenshot) {
		BizOutwardTask task = outwardTaskRepository.findById2(taskId);
		if (task == null || !OutwardTaskStatus.Undeposit.getStatus().equals(task.getStatus())
				|| task.getAccountId() == null || task.getAsignTime() == null) {
			return;
		}
		long timeConsume = (System.currentTimeMillis() - task.getAsignTime().getTime()) / 1000;
		remark = addFlowChars(OutwardTaskStatus.Unknown, remSpecialChars(remark));
		remark = CommonUtils.genRemark(task.getRemark(), remSpecialChars(remark), new Date(),
				operator == null ? SYS : operator.getUid());
		outwardTaskRepository.updateStatusAndRemark(task.getId(), task.getStatus(),
				OutwardTaskStatus.Unknown.getStatus(), remark,
				StringUtils.isEmpty(screenshot) ? task.getScreenshot() : screenshot, timeConsume);
	}

	/**
	 * 转取消
	 */
	@Transactional
	@Override
	public void alterStatusToCancel(Long reqId, Long taskId, SysUser operator, String remark) {
		alterStatusToCancelOrRefuse(reqId, taskId, operator, remark, OutwardTaskStatus.ManageCancel.getStatus());
	}

	/**
	 * 转拒接
	 */
	@Transactional
	@Override
	public void alterStatusToRefuse(Long reqId, Long taskId, SysUser operator, String remark) {
		alterStatusToCancelOrRefuse(reqId, taskId, operator, remark, OutwardTaskStatus.ManageRefuse.getStatus());
	}

	/**
	 * 转已匹配<br/>
	 * bankFlowId 可以为空
	 */
	@Transactional
	@Override
	public void alterStatusToMatched(Long taskId, Long bankFlowId, SysUser operator, String remark) {
		log.info("转已匹配>>start taskId:{} bankFlowId:{} remark:{}", taskId, bankFlowId, remark);
		BizOutwardTask task = outwardTaskRepository.findById2(taskId);
		// 只有已完成出款的任务，才能转匹配完成
		if (task == null || !OutwardTaskStatus.Deposited.getStatus().equals(task.getStatus())) {
			log.info("转已匹配>>只有已出款的出款任务才能参与匹配 taskId:{} bankFlowId:{} remark:{}", taskId, bankFlowId, remark);
			return;
		}
		if (bankFlowId == null) {
			BizOutwardRequest req = outwardRequestService.get(task.getOutwardRequestId());
			bankFlowId = findMatchingBankLogId(task.getAccountId(), req.getToAccount(), req.getToAccountOwner(),
					task.getAmount());
		}
		BizBankLog bankLog = bankFlowId == null ? null : bankLogService.get(bankFlowId);
		if (bankFlowId != null && bankLog == null) {
			log.info("转已匹配>>银行流水号对应银行流水不存在 taskId:{} bankFlowId:{} remark:{}", taskId, bankFlowId, remark);
			return;
		}
		// 第1步，第2步 顺序不能改变
		log.info("转已匹配 taskId:{} bankFlowId:{} operator:{},remark:{}", taskId, bankFlowId,
				operator == null ? SYS : operator.getUid(), remark);
		// 1.出款任务加备注
		remark4Custom(taskId, operator, remark);
		// 2.交易日志
		BizTransactionLog o = new BizTransactionLog();
		o.setAmount(bankLog != null ? bankLog.getAmount() : task.getAmount().multiply(new BigDecimal(-1)));
		o.setOrderId(task.getId());
		o.setConfirmor(operator == null ? null : operator.getId());
		o.setOperator(task.getOperator());
		o.setCreateTime(new Date());
		o.setType(TransactionLogType.OUTWARD.getType());
		o.setToAccount(0);
		o.setFromAccount(bankLog != null ? bankLog.getFromAccount() : task.getAccountId());
		o.setDifference(bankLog == null ? BigDecimal.ZERO : task.getAmount().subtract(bankLog.getAmount()));
		o.setRemark(CommonUtils.genRemark(o.getRemark(), remark, o.getCreateTime(),
				operator == null ? SYS : operator.getUid()));
		o.setFromBanklogId(bankFlowId);
		transactionLogService.save(o);
	}

	/**
	 * 从待排查或者已出款状态转完成
	 * <p>
	 * 1.如果该任务 有流水与其匹配，则该任务状态从待排查变为已匹配</br>
	 * 2.如果该任务 无流水与其匹配，则该任务状态从待排查变为完成出款
	 * </p>
	 *
	 * @param taskId
	 *            任务ID
	 * @param operator
	 *            操作人
	 * @param remarkOri
	 *            备注
	 */
	@Transactional
	@Override
	public void ToFinishFromFailOrManagerDeal(long taskId, SysUser operator, String remarkOri) {
		BizOutwardTask task = outwardTaskRepository.findById2(taskId);
		if (task == null || !OutwardTaskStatus.Failure.getStatus().equals(task.getStatus())
				&& !OutwardTaskStatus.Deposited.getStatus().equals(task.getStatus())) {
			log.info("从待排查或已出款状态转完成>>该任务不处于待排查或已出款状态，故无法从待排查转完成taskId:{} remarkOri:{}", taskId, remarkOri);
			return;
		}
		log.info("从待排查或已出款状态转已匹配>> taskId:{} remarkOri:{}", taskId, remarkOri);
		BizTransactionLog bizTransactionLog = transactionLogService.findByOrderIdAndType(taskId,
				TransactionLogType.OUTWARD.getType());
		Long bankLogId = bizTransactionLog != null ? bizTransactionLog.getFromBanklogId() : null;
		if (bankLogId == null) {
			BizOutwardRequest req = outwardRequestService.get(task.getOutwardRequestId());
			if (req == null) {
				log.info("从待排查或已出款状态转已匹配req不存在，参数:{}", taskId);
				return;
			}
			bankLogId = findMatchingBankLogId(task.getAccountId(), req.getToAccount(), req.getToAccountOwner(),
					task.getAmount());
		}
		OutwardTaskStatus finish = bizTransactionLog != null || bankLogId != null ? OutwardTaskStatus.Matched
				: OutwardTaskStatus.Deposited;
		String remark = addFlowChars(finish, remSpecialChars(remarkOri));
		Date d = new Date();
		remark = CommonUtils.genRemark(task.getRemark(), remark, d, operator == null ? SYS : operator.getUid());
		// 第1步，第2步 顺序不能颠倒0000000000000000
		// 1.出款任务 备注，状态更改
		int ret = outwardTaskRepository.updateStatusAndRemark(taskId, task.getStatus(), finish.getStatus(), remark,
				task.getScreenshot());
		if (ret == 0 || bizTransactionLog == null && bankLogId == null
				|| bizTransactionLog != null && bizTransactionLog.getFromBanklogId() != null) {
			return;
		}
		// 2.交易日志
		if (bizTransactionLog == null) {
			bizTransactionLog = new BizTransactionLog();
			bizTransactionLog.setOrderId(task.getId());
			bizTransactionLog.setType(TransactionLogType.OUTWARD.getType());
			bizTransactionLog.setOperator(task.getOperator());
			bizTransactionLog.setCreateTime(d);
			bizTransactionLog.setAmount(task.getAmount().multiply(new BigDecimal(-1)));
			bizTransactionLog.setDifference(BigDecimal.ZERO);
			bizTransactionLog.setToAccount(0);
			bizTransactionLog.setFromAccount(task.getAccountId());
		}
		if (operator != null) {
			bizTransactionLog.setConfirmor(operator.getId());
		}
		if (bizTransactionLog.getFromBanklogId() == null && bankLogId != null) {
			bizTransactionLog.setFromBanklogId(bankLogId);
		}
		bizTransactionLog.setRemark(CommonUtils.genRemark(bizTransactionLog.getRemark(), remarkOri,
				bizTransactionLog.getCreateTime(), operator == null ? SYS : operator.getUid()));
		transactionLogService.save(bizTransactionLog);
	}

	/**
	 * 获取正在匹配的银行流水ID
	 *
	 * @param fromAccountId
	 *            汇款账号
	 * @param toAccount
	 *            汇入账号
	 * @param toOwner
	 *            收款人
	 * @param amount
	 *            汇款金额
	 * @return 银行流水ID
	 * @since 1.8
	 */
	private Long findMatchingBankLogId(Integer fromAccountId, String toAccount, String toOwner, BigDecimal amount) {
		if (fromAccountId == null) {
			return null;
		}
		toAccount = StringUtils.trimToEmpty(toAccount);
		toOwner = StringUtils.trimToEmpty(toOwner);
		Integer status = BankLogStatus.Matching.getStatus();
		List<BizBankLog> logList = bankLogRepository.findByFromAccountAndAmount(fromAccountId,
				amount.abs().multiply(new BigDecimal(-1)));
		logList = logList.stream().filter(p -> p.getStatus() == null || p.getStatus().equals(status))
				.collect(Collectors.toList());
		logList.sort((o1, o2) -> -o1.getTradingTime().compareTo(o2.getTradingTime()));
		for (BizBankLog log : logList) {
			if (StringUtils.equals(StringUtils.trimToEmpty(log.getToAccount()), toAccount)
					|| StringUtils.equals(StringUtils.trimToEmpty(log.getToAccountOwner()), toOwner)) {
				return log.getId();
			}
		}
		return null;
	}

	/**
	 * 转取消/拒绝
	 *
	 * @param status
	 *            com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus
	 *            [ManageCancel,ManageRefuse]
	 */
	private void alterStatusToCancelOrRefuse(Long reqId, Long taskId, SysUser operator, String remarkOri,
			Integer status) {
		if (reqId == null && taskId == null) {
			return;
		}
		int mgrDeal = OutwardTaskStatus.ManagerDeal.getStatus();
		int cancel = OutwardTaskStatus.ManageCancel.getStatus();
		int refuse = OutwardTaskStatus.ManageRefuse.getStatus();
		// 校验 出款请求存在性,出款任务存在性
		BizOutwardRequest req = null;
		if (reqId != null) {
			req = outwardRequestService.get(reqId);
		}
		BizOutwardTask task = null;
		if (taskId != null) {
			task = outwardTaskRepository.findById2(taskId);
			if (task != null && req == null) {
				req = outwardRequestService.get(task.getOutwardRequestId());
			}
		}
		if (req == null || (taskId != null && task == null)) {
			log.info(" 转取消/拒绝 出款请求或出款任务不存在 reqId:{},taskId:{},status:{}", reqId, taskId, status);
			return;
		}
		if (reqId != null && taskId != null && !reqId.equals(task.getOutwardRequestId())) {
			log.info(" 转取消/拒绝 出款请求，reqId,taskId不匹配 reqId:{},taskId:{},status:{}", reqId, taskId, status);
			return;
		}
		// 公司用款:不能取消/拒绝
		if (checkComOutward(req)) {
			log.info(" 公司用款 不能转取消/拒绝 出款请求  reqId:{}, orderNo:{}", reqId, req.getOrderNo());
			return;
		}
		// 以下情况不能取消，拒绝
		// 该任务已分配 Undeposit & task.accountId is not null
		// 已出款 Deposited
		// 流水匹配 Matched
		// 转排查 Failure
		// 无效记录 Invalid
		// 主管取消 ManageCancel
		// 主管拒绝 ManageRefuse
		if (task != null && ((OutwardTaskStatus.Undeposit.getStatus().equals(task.getStatus())
				&& (task.getAccountId() != null || task.getOperator() != null))
				|| OutwardTaskStatus.Deposited.getStatus().equals(task.getStatus())
				|| OutwardTaskStatus.Matched.getStatus().equals(task.getStatus())
				|| OutwardTaskStatus.Failure.getStatus().equals(task.getStatus())
				|| OutwardTaskStatus.Invalid.getStatus().equals(task.getStatus())
				|| OutwardTaskStatus.ManageCancel.getStatus().equals(task.getStatus())
				|| OutwardTaskStatus.ManageRefuse.getStatus().equals(task.getStatus()))) {
			log.info(" 转取消/拒绝 出款请求，该任务 不能取消拒绝 reqId:{},taskId:{},status:{}", reqId, taskId, status);
			return;
		}
		long outReqId = req.getId();
		long outTaskId = taskId == null ? 0 : taskId;
		String memberCode = req.getMemberCode();
		String orderNo = req.getOrderNo();
		String ACTION = cancel == status ? OutwardTaskStatus.ManageCancel.getMsg()
				: OutwardTaskStatus.ManageRefuse.getMsg();
		// 备注：
		String remark = addFlowChars(status == cancel ? OutwardTaskStatus.ManageCancel : OutwardTaskStatus.ManageRefuse,
				remSpecialChars(remarkOri));
		if (remarkOri.contains("#ERROR#")) {
			remark = remark.replace("#ERROR#", "(会员收款账号异常)");
		}
		String hisRemark = task != null ? task.getRemark() : req.getRemark();
		String remarks = CommonUtils.genRemark(hisRemark, remark, new Date(),
				operator == null ? SYS : operator.getUid());
		AtomicInteger consumingtimeReq = new AtomicInteger(0);
		AtomicInteger consumingtimeTask = new AtomicInteger(0);
		if (reqId != null && taskId == null) {
			// 只有出款请求没有出款任务时候时候取消拒绝操作更新出款请求的 耗时
			Long time = System.currentTimeMillis() - req.getCreateTime().getTime();
			consumingtimeReq.set(time.intValue() / 1000);
		}
		if (task != null) {
			Long time;
			if (task.getAsignTime() != null) {
				// 已分配任务出去,主管取消或者拒绝
				time = task.getAsignTime() != null ? System.currentTimeMillis() - task.getAsignTime().getTime() : 0;
				consumingtimeTask.set(time.intValue() / 1000);
			} else {
				// 还没分配任务,在未出款取消或者拒绝 便于查询把当前时间设置为分配时间
				task.setAsignTime(new Date());
				time = req.getTimeConsuming() != null ? System.currentTimeMillis() - req.getTimeConsuming() * 1000 : 0;
				consumingtimeTask.set(time.intValue() / 1000);
				log.info("time :{},time.intValue() / 1000 :{},consumingtimeTask.get():{}", time, time.intValue() / 1000,
						consumingtimeTask.get());
			}
		}
		List<BizOutwardTask> tasks = outwardTaskRepository.findByOutwardRequestId(outReqId);
		// 检测：如果已有完成出款，已匹配，待排查 任务 不能继续进行
		int deposited = OutwardTaskStatus.Deposited.getStatus();
		int failure = OutwardTaskStatus.Failure.getStatus();
		int matched = OutwardTaskStatus.Matched.getStatus();
		if (tasks.stream()
				.filter(p -> deposited == p.getStatus() || failure == p.getStatus() || matched == p.getStatus())
				.count() > 0) {
			log.info(
					"alterStatusToCancelOrRefuse ( {} ) >> already have deposited|failure|matched tasks.  reqId: {},taskId: {},orderNo: {}",
					ACTION, outReqId, taskId, orderNo);
			return;
		}
		// 检测：outwardReq是否已取消/拒绝；如果已取消/拒绝，不必向 平台 确认 取消/拒绝 操作
		if (tasks.stream().filter(p -> cancel == p.getStatus() || refuse == p.getStatus()).count() > 0) {
			log.info("{}任务，该任务已向平台确认 reqId:{},taskId:{},orderNo:{}", ACTION, outReqId, taskId, orderNo);
			// outwardTaskRepository.cancelOrRefuse(outReqId, outTaskId, status,
			// remarks, mgrDeal,
			// consumingtimeTask.get());
			outwardTaskRepository.cancelOrRefuseById(outTaskId, status, remarks, consumingtimeTask.get());
			outwardTaskRepository.cancelOrRefuseByRequestIdAndNotAccountId(outReqId, outTaskId, status, remarks,
					consumingtimeTask.get());
			outwardTaskRepository.cancelOrRefuseByRequestIdAndStatus(outReqId, outTaskId, status, remarks, mgrDeal,
					consumingtimeTask.get());
			return;
		}
		// 检测：出款请求状态（非取消；拒绝；出款成功，平台已确认；出款成功，与平台确认失败）
		if (Arrays
				.asList(OutwardRequestStatus.Canceled.getStatus(), OutwardRequestStatus.Reject.getStatus(),
						OutwardRequestStatus.Acknowledged.getStatus(), OutwardRequestStatus.Failure.getStatus())
				.contains(req.getStatus())) {
			log.info("转取消/拒绝：reqId:{} orderNo:{} 已为最终状态 不能更改 finalStatus:{}", req.getId(), req.getOrderNo(),
					req.getStatus());
			return;
		}
		Action1<SimpleResponseData> simpleResponseData = o -> {
			log.info("{}任务，向new平台确认成功,operator:{}, reqId:{},taskId:{},orderNo: {}, Status:{},Message: {}", ACTION,
					operator.getUid(), reqId, taskId, orderNo, o.getStatus(), o.getMessage());
			if (o.getStatus() == 1) {
				int reqStatus = cancel == status ? OutwardRequestStatus.Canceled.getStatus()
						: OutwardRequestStatus.Reject.getStatus();
				try {
					if (reqId != null) {
						if (taskId == null) {
							// 只有出款请求没有出款任务时候时候取消拒绝操作更新出款请求的 耗时
							log.info("更新出款请求及耗时:outReqId:{},memberCode:{},remarks:{},reqStatus:{},operator:{},耗时:{}",
									outReqId, memberCode, remarks, reqStatus, operator.getId(), consumingtimeReq.get());
							outwardRequestRepository.cancelOrRefuse(outReqId, memberCode, remarks, reqStatus,
									OutwardRequestStatus.Canceled.getStatus(), OutwardRequestStatus.Reject.getStatus(),
									OutwardRequestStatus.Acknowledged.getStatus(),
									OutwardRequestStatus.Failure.getStatus(), operator.getId(), consumingtimeReq.get());
						} else {
							// 不更新出款请求耗时
							outwardRequestRepository.cancelOrRefuse(outReqId, memberCode, remarks, reqStatus,
									OutwardRequestStatus.Canceled.getStatus(), OutwardRequestStatus.Reject.getStatus(),
									OutwardRequestStatus.Acknowledged.getStatus(),
									OutwardRequestStatus.Failure.getStatus(), operator.getId());
							log.info("更新出款请求成功!");
						}
					}
					if (taskId != null) {
						// 要更新出款任务耗时
						log.info("更新出款任务及耗时:outReqId：{},outTaskId:{},status:{},remarks:{},mgrDeal:{},耗时:{} ", outReqId,
								outTaskId, status, remarks, mgrDeal, consumingtimeTask.get());
						// outwardTaskRepository.cancelOrRefuse(outReqId,
						// outTaskId, status, remarks, mgrDeal,
						// consumingtimeTask.get());
						outwardTaskRepository.cancelOrRefuseById(outTaskId, status, remarks, consumingtimeTask.get());
						outwardTaskRepository.cancelOrRefuseByRequestIdAndNotAccountId(outReqId, outTaskId, status,
								remarks, consumingtimeTask.get());
						outwardTaskRepository.cancelOrRefuseByRequestIdAndStatus(outReqId, outTaskId, status, remarks,
								mgrDeal, consumingtimeTask.get());
					}
					List<BizOutwardTask> taskList = outwardTaskRepository.findAllocated(outReqId);
					taskList.stream().filter(p -> p.getOperator() != null).forEach(p -> {
						String msg = "出款订单号:" + orderNo + " 停止出款，<br/>请转主管处理";
						String info = CommonUtils.genSysMsg4WS(p.getOperator(),
								SystemWebSocketCategory.OutwardTaskCancel, msg);
						redisService.convertAndSend(RedisTopics.BROADCAST, info);
					});
				} catch (Exception e) {
					log.info("更新异常:Exception:{},StackTrace:{}", e.getLocalizedMessage(), e.getStackTrace());
				}

			} else {
				log.info("{}任务，向new平台确认失败，可能已确认, orderNo: {}, response: {}", ACTION, orderNo, o.getMessage());
				if (operator != null) {
					String info = CommonUtils.genSysMsg4WS(operator.getId(), SystemWebSocketCategory.System,
							"操作失败，请联系技术人员处理");
					redisService.convertAndSend(RedisTopics.BROADCAST, info);
				}
			}
		};

		// 异常处理
		Action1<Throwable> throwable = e -> {
			log.error("{}任务，向平台确认异常， orderNo:{} {}", ACTION, orderNo, e);
			if (operator != null) {
				redisService.convertAndSend(RedisTopics.BROADCAST,
						CommonUtils.genSysMsg4WS(operator.getId(), SystemWebSocketCategory.System, "操作失败，请联系技术人员处理"));
			}
		};
		log.debug("出款任务，向平台确认 {} 操作 reqId:{},taskId:{},orderNo:{}", ACTION, outReqId, taskId, orderNo);
		String handicap = handicapService.findFromCacheById(req.getHandicap()).getCode();
		if (cancel == status) {// 向平台反馈取消操作
			HttpClientNew.getInstance().getPlatformServiceApi()
					.withdrawalCancel(requestBodyParser.buildRequestBody(handicap, req.getOrderNo(), remarkOri))
					.subscribe(simpleResponseData, throwable);

		} else {// 向平台反馈拒绝操作
			HttpClientNew.getInstance().getPlatformServiceApi()
					.withdrawalReject(requestBodyParser.buildRequestBody(handicap, req.getOrderNo(), remarkOri))
					.observeOn(Schedulers.io()).subscribe(simpleResponseData, throwable);
		}
	}

	/**
	 * 重置出款任务分配停止分配时间
	 */
	@Override
	public void alterHaltTimeToNull() {
		HALT_SERVICE_END_TIME = null;
		HALT_SERVICE_START_TIME = null;
	}

	/**
	 * 修改银行维护信息
	 */
	@Async
	@Override
	public void alterMaintainBank() {
		if (!incomeAccountAllocateService.checkHostRunRight()) {
			log.trace("该主机 {} 没有出款任务分配权限", CommonUtils.getInternalIp());
			return;
		}
		MemCacheUtils.getInstance().loadingPreferencesData();
		String maintainBanks = StringUtils.trimToEmpty(MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_SYS_MAINTAIN_BANKTYPE.getValue()));
		Date d = new Date();
		int thirdLowestBal = findThirdLowBal();
		while (true) {
			List<BizOutwardTask> taskList = outwardTaskRepository
					.findNotMaintain(OutwardTaskStatus.DuringMaintain.getStatus(), maintainBanks);
			if (CollectionUtils.isEmpty(taskList)) {
				break;
			}
			for (BizOutwardTask task : taskList) {
				String remark = addFlowChars(OutwardTaskStatus.Undeposit, BANK_MAINTAIN_RESORE);// 请勿改动
				remark = CommonUtils.genRemark(task.getRemark(), remark, d, SYS);
				BizOutwardRequest req = outwardRequestService.get(task.getOutwardRequestId());
				try {
					outwardTaskRepository.undepositFromMaintain(task.getId(), OutwardTaskStatus.Undeposit.getStatus(),
							OutwardTaskStatus.DuringMaintain.getStatus(), remark);
					rpush(req, task, task.getAmount().floatValue() >= thirdLowestBal);
				} catch (Exception e) {
					log.error("出款任务分配 银行维护恢复 taskId:{} orderNo:{} error:{}", task.getId(), req.getOrderNo(), e);
				}
			}
		}
	}

	/**
	 * 出款完成通知平台
	 * <p>
	 * 1.检测出款请求是否已确认，已确认：直接返回；未确认：流程继续</br>
	 * 2.检测出款请求的所有任务，是否全部完成出款，否：直接返回；是：流程继续</br>
	 * 3.通知平台，该出款请求已完成出款</br>
	 * 4.根据第三步返回结果，会写出款请求状态
	 * </p>
	 *
	 * @param operator
	 *            操作人ID
	 * @param req
	 *            出款请求
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void noticePlatIfFinished(Integer operator, BizOutwardRequest req) {
		// 公司用款:不用通知平台
		if (Objects.isNull(req) || checkComOutward(req)) {
			return;
		}
		// 状态已确认则返回
		if (OutwardRequestStatus.Acknowledged.getStatus().equals(req.getStatus())) {
			log.info("状态已确认则返回 reqId:{},orderNo:{} reqStatus:{}", req.getId(), req.getOrderNo(), req.getStatus());
			return;
		}
		// 检测是否拆单，且全部完成
		List<BizOutwardTask> taskList = outwardTaskRepository.findAll(DynamicSpecifications.build(null,
				BizOutwardTask.class, new SearchFilter("outwardRequestId", SearchFilter.Operator.EQ, req.getId())));
		boolean finish = taskList.stream().filter(p -> !OutwardTaskStatus.isComplete(p.getStatus())).count() == 0;
		if (!finish) {
			log.info("有任务未完成 reqId:{},orderNo:{} reqStatus:{}", req.getId(), req.getOrderNo(), req.getStatus());
			return;
		}
		String oldRemark = StringUtils.isNotBlank(req.getRemark()) ? req.getRemark() : "";
		SysUser opr = operator == null ? null : userService.findFromCacheById(operator);
		// 2.通知 平台
		String handicap = handicapService.findFromCacheById(req.getHandicap()).getCode();
		callWithDrawAck(null, req, handicap, operator, opr, oldRemark, false, null);
	}

	/**
	 * 出款完成 调用平台通知
	 * 
	 * @param req
	 * @param handicap
	 * @param operator
	 * @param opr
	 * @param oldRemark
	 * @param daifu
	 */
	@Transactional(rollbackFor = Exception.class)
	void callWithDrawAck(BizOutwardTask task, BizOutwardRequest req, String handicap, Integer operator, SysUser opr,
			String oldRemark, boolean daifu, String platPayCode) {
		try {
			RequestBody requestBody1 = null;
			if (daifu && null != task) {
				DaifuConfigRequest daifuConfigRequest = daifuConfigRequestRepository.findOne(task.getAccountId());
				log.debug("代付订单:{}, 账号 信息:{}", task, daifuConfigRequest);
				if (null != daifuConfigRequest) {
					requestBody1 = requestBodyParser.buildRequestBodyForDaifu(task.getHandicap(), req.getOrderNo(), "",
							daifuConfigRequest.getChannelName(), daifuConfigRequest.getMemberId(), platPayCode);
				}
			} else {
				log.debug("非代付订单:task:{},req:{}", task, req);
				requestBody1 = requestBodyParser.buildRequestBody(handicap, req.getOrderNo(), "");
			}
			if (requestBody1 == null) {
				log.debug("调用平台通知 requestBody为空!");
				return;
			}
			HttpClientNew.getInstance().getPlatformServiceApi().WithdrawalAck(requestBody1).observeOn(Schedulers.io())
					.subscribe(o -> {
						log.info("(new)Outward acknowledged success, orderNo: {}, response: {}", req.getOrderNo(),
								o.getMessage());
						if (!OutwardRequestStatus.Failure.getStatus().equals(req.getStatus())) {
							req.setUpdateTime(new Date());
						}
						int state = OutwardRequestStatus.Failure.getStatus();
						Date now = new Date();
						if (o.getStatus() == 1) {
							state = OutwardRequestStatus.Acknowledged.getStatus();
							if (operator == null || operator == 1 || opr == null) {
								req.setRemark(CommonUtils.genRemark(oldRemark, "通知平台响应成功:" + o.getMessage(), now, SYS));
							} else {
								req.setRemark(CommonUtils.genRemark(oldRemark, "通知平台响应成功:" + o.getMessage(), now,
										opr.getUid()));
							}
						} else {
							if (operator != null) {
								// 定时通知平台的时候 operator传null
								// 通知失败则不添加备注，防止备注过长无法入库。
								if (operator == 1) {
									req.setRemark(
											CommonUtils.genRemark(oldRemark, "通知平台响应结果:" + o.getMessage(), now, SYS));
								} else {
									req.setRemark(CommonUtils.genRemark(oldRemark, "通知平台响应结果:" + o.getMessage(), now,
											opr.getUid()));
								}
							}
						}
						req.setStatus(state);
						outwardRequestService.update(req);
						if (operator != null) {
							String info = CommonUtils.genSysMsg4WS(operator,
									SystemWebSocketCategory.SystemOutwardTaskOperation, o.getMessage());
							redisService.convertAndSend(RedisTopics.BROADCAST, info);
						}
					}, e -> {
						Date now = new Date();
						req.setRemark(CommonUtils.genRemark(oldRemark, "与平台确认失败" + e.getLocalizedMessage(), now, SYS));
						req.setStatus(OutwardRequestStatus.Failure.getStatus());
						// 再次确认的时候，存在与平台确认失败的情况。
						// req.setUpdateTime(now);
						outwardRequestService.update(req);
						log.error("(new)Outward acknowledged error. orderNo: " + req.getOrderNo(), e);
					});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 第三方代付 调用通知平台
	 * 
	 * @param task
	 * @param req
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void callPlatForDaifuSuccessOut(BizOutwardTask task, BizOutwardRequest req, String platPayCode) {
		log.debug("代付 状态确认通知平台:task:{},req:{},platPayCode:{}", ObjectMapperUtils.serialize(task),
				ObjectMapperUtils.serialize(req), platPayCode);
		// 状态已确认则返回
		if (OutwardRequestStatus.Acknowledged.getStatus().equals(req.getStatus())) {
			log.info("代付 状态已确认则返回 task:{}", task);
			return;
		}
		callWithDrawAck(task, req, task.getHandicap(), 1, null, req.getRemark(), true, platPayCode);
	}

	/**
	 * 保存 银行维护的任务
	 */
	private void saveMaintain(AllocatingOutwardTask tasking) {
		if (tasking == null || tasking.getTaskId() == null) {
			return;
		}
		BizOutwardTask outwardTask = outwardTaskRepository.findById2(tasking.getTaskId());
		if (outwardTask == null) {
			return;
		}
		String remark = addFlowChars(OutwardTaskStatus.DuringMaintain, BANK_MAINTAIN);
		remark = CommonUtils.genRemark(outwardTask.getRemark(), remark, new Date(), SYS);
		outwardTaskRepository.maintain(tasking.getTaskId(), OutwardTaskStatus.DuringMaintain.getStatus(),
				OutwardTaskStatus.Undeposit.getStatus(), remark);
		log.info("出款任务分配 银行维护 taskId:{} reqId:{} toAccountBank:{} amount:{}", tasking.getTaskId(), tasking.getReqId(),
				tasking.getToAccountBank(), tasking.getTaskAmount());
	}

	/**
	 * 保存分配信息
	 */
	private void saveAllocated(AllocatingOutwardTask ated) {
		// 备注：逻辑处理顺序：1，2，3 请勿改动
		// 基础数据
		int mgrDeal = OutwardTaskStatus.ManagerDeal.getStatus(), undeposit = OutwardTaskStatus.Undeposit.getStatus();
		int cancel = OutwardTaskStatus.ManageCancel.getStatus(), refuse = OutwardTaskStatus.ManageRefuse.getStatus();
		// 会员出款任务保存分配逻辑
		if (ated.getTaskType() != TaskType.RebateTask.getTypeId()) {
			BizOutwardRequest req = outwardRequestService.get(ated.getReqId());
			if (Objects.isNull(req)) {
				log.info("AllocOTask >> MemberOutTask (REQ reqId: {} taskId: {} ) OutwardRequest doesn't exist.",
						ated.getReqId(), ated.getTaskId());
				return;
			}
			String orderNo = req.getOrderNo();
			Integer handicap = req.getHandicap();
			String  memCode = req.getMemberCode();
			Date now = new Date();
			List<BizOutwardTask> tasks = outwardTaskRepository.findByOutwardRequestId(ated.getReqId());
			if (CollectionUtils.isEmpty(tasks)) {
				log.info("AllocOTask >> MemberOutTask (TASK reqId: {} taskId: {} ) OutwardRequest doesn't exist.",
						ated.getReqId(), ated.getTaskId());
				return;
			}
			List<BizOutwardTask> atedList = tasks.stream().filter(p -> p.getId().equals(ated.getTaskId()))
					.collect(Collectors.toList());
			String hisRem = CollectionUtils.isEmpty(atedList) ? StringUtils.EMPTY : atedList.get(0).getRemark();
			String theRem;
			Integer outwardPayType;
			if (ated.getManualOut() == THIRD_OUT_YES) {
				theRem = LOCK_REQ_SUCCESS + ated.getOperator() + "-" + THIRD;
				outwardPayType = OutWardPayType.ThirdPay.getType();
			} else {
				AccountBaseInfo base = accountService.getFromCacheById(ated.getFromId());
				theRem = LOCK_REQ_SUCCESS
						+ (base == null || StringUtils.isBlank(base.getAlias()) ? ated.getFromId() : base.getAlias())
						+ "-" + (ated.getOperator() == null ? ROBOT : ated.getOperator());
				outwardPayType = ated.getOperator() == null
						? (base.checkMobile() ? OutWardPayType.REFUND.getType() : OutWardPayType.PC.getType())
						: OutWardPayType.MANUAL.getType();
			}
			// 1.公司用款 2.历史记录 有 锁定成功
			if (checkComOutward(req) || tasks.stream()
					.filter(p -> StringUtils.isNotBlank(p.getRemark()) && p.getRemark().contains(LOCK_REQ_SUCCESS))
					.count() > 0) {
				String remark = CommonUtils.genRemark(hisRem, theRem, now, SYS);// 请勿去掉备注
				int rows = outwardTaskRepository.allocAccount(ated.getTaskId(), ated.getFromId(), ated.getOperator(),
						cancel, refuse, remark, undeposit, outwardPayType);// 分配
				if (rows == 1) {
					log.info(
							"AllocOTask >> MemberOutTask (REQ OrderNo: {} taskId: {} fromId: {} operator: {}) Allocate Success !!.",
							orderNo, ated.getTaskId(), ated.getFromId(), ated.getOperator());
				} else {
					BizOutwardTask task = outwardTaskRepository.findById2(ated.getTaskId());
					if (task == null) {
						log.error("AllocOTask >> task doesn't exists (REQ OrderNo: {} taskId: {})", orderNo,
								ated.getTaskId());
						return;
					}
					log.info("AllocOTask >> update error,task info(ID:{} ,account_id:{} ,operator:{} ,status:{} ) !!.",
							task.getId(), task.getAccountId(), task.getOperator(), task.getStatus());
				}
				return;
			}
			// 首次分配 判断 与 历史记录 有 重新分配 判断
			boolean check = tasks.stream().filter(p -> p.getAccountId() == null).count() == tasks.size();
			check = check || tasks.stream()
					.filter(p -> StringUtils.isNotBlank(p.getRemark()) && p.getRemark().contains(RE_ALLOCATE))
					.count() > 0;
			// 2.既不是首次分配 且 历史记录无重新分配 处理（转主管）
			if (!check) {
				String remark = CommonUtils.genRemark(StringUtils.EMPTY, LOCK_REQ_FAIL, now, SYS);// 请勿去掉备注
				remark = addFlowChars(OutwardTaskStatus.ManagerDeal, remark);// 备注中加流程标识（转主管）
				outwardTaskRepository.alterStatusToMgr(ated.getTaskId(), remark, mgrDeal, undeposit);
				asignFailedTaskService.asignOnTurnToFail(ated.getTaskId());
			}
			// 3.首次分配 与 历史记录有重新分配 处理
			// new and old api compatibility
			// String handicapCode =
			// handicapService.findFromCacheById(handicap).getCode();
			// if
			// (Arrays.asList(AppConstants.OLD_PLATFORM_HANDICAP.split(",")).contains(handicapCode))
			// {
			// HttpClient.getInstance().getIPlatformService(handicap)
			// .WithdrawMoneyLock(requestBodyParser.general(handicap, memCode,
			// orderNo,
			// StringUtils.EMPTY))
			// .subscribe(d -> {
			// if (d.contains("\"Result\":1")) {
			// String remark = CommonUtils.genRemark(hisRem, theRem, now,
			// SYS);//
			// 请勿去掉备注
			// outwardTaskRepository.allocAccount(ated.getTaskId(),
			// ated.getFromId(),
			// ated.getOperator(),
			// cancel, refuse, remark, undeposit);
			// } else {
			// String remark = CommonUtils.genRemark(hisRem, LOCK_REQ_FAIL, now,
			// SYS);//
			// 请勿去掉备注
			// remark = addFlowChars(OutwardTaskStatus.ManagerDeal, remark);//
			// 备注中加流程标识（转主管）
			// outwardTaskRepository.alterStatusToMgr(ated.getTaskId(), remark,
			// mgrDeal,
			// undeposit);
			// }
			// }, e -> {
			// String remark = CommonUtils.genRemark(hisRem, LOCK_REQ_FAIL, now,
			// SYS);//
			// 请勿去掉备注
			// remark = addFlowChars(OutwardTaskStatus.ManagerDeal, remark);//
			// 备注中加流程标识（转主管）
			// outwardTaskRepository.alterStatusToMgr(ated.getTaskId(), remark,
			// mgrDeal,
			// undeposit);
			// });
			// } else {
			// 新系统直接分配，不需锁定
			String remark = CommonUtils.genRemark(hisRem, theRem, now, SYS);// 请勿去掉备注
			outwardTaskRepository.allocAccount(ated.getTaskId(), ated.getFromId(), ated.getOperator(), cancel, refuse,
					remark, undeposit, outwardPayType);
			log.info(
					"AllocOTask >> MemberOutTask (REQ OrderNo: {} taskId: {} fromId: {} operator: {}) Allocate Success !!.",
					orderNo, ated.getTaskId(), ated.getFromId(), ated.getOperator());
			// }
		} else {
			// 返利任务分配
			BizAccountRebate req = accountRebateRepository.findById2(ated.getTaskId());
			if (Objects.isNull(req)) {
				log.info("AllocOTask >> RebateTask (REQ reqId: {} taskId: {} ) BizAccountRebate doesn't exist.",
						ated.getReqId(), ated.getTaskId());
				return;
			}
			String hisRem = req.getRemark();
			AccountBaseInfo base = accountService.getFromCacheById(ated.getFromId());
			String theRem = LOCK_REQ_SUCCESS
					+ (base == null || StringUtils.isBlank(base.getAlias()) ? ated.getFromId() : base.getAlias()) + "-"
					+ (ated.getOperator() == null ? ROBOT : ated.getOperator());
			Date now = new Date();
			String remark = CommonUtils.genRemark(hisRem, theRem, now, SYS);// 请勿去掉备注
			accountRebateRepository.allocAccount(req.getId(), ated.getFromId(), ated.getOperator(), remark,
					ated.getTaskId(), undeposit);
			log.info("AllocOTask >> RebateTask (taskId: {} fromId: {}) Allocate Success !!.", ated.getTaskId(),
					ated.getFromId());
		}
	}

	/**
	 * 出款任务分配
	 *
	 * @param tasking
	 *            待分配任务
	 * @param peerTrans
	 *            同行转账
	 * @return 分配结果 AllocatingOutwardTask#fromId AllocatingOutwardTask#operator
	 * @see this#checkPeerTrans(String) 校验同行转账 返回值说明
	 */
	private AllocatingOutwardTask allocate(AllocatingOutwardTask tasking, Boolean peerTrans) throws Exception {
		Set<ZSetOperations.TypedTuple<String>> getAll = cardCache.getUnchecked("OUTBANK");
		// 当不存在 被分配对象时：抛出特殊异常
		if (CollectionUtils.isEmpty(getAll) || getAll.size() == 1) {
			log.debug("allocate>> target set empty ,task {}", tasking);
			return null;
		}
		// 区域内是否区分盘口 true:区分盘口;不区分盘口
		boolean distHandi = handicapService.checkDistHandi(tasking.getZone());
		// 检测该任务是否已超时
		// 1.出款金额 < WAITING_AMT_MAX 需要等待</br>
		// 2.在满足1.的情况下,等待时间 > WAITING_MILIS_MAX,则不需要等待</br>
		boolean needWait = WAITING_AMT_MAX > tasking.getTaskAmount()
				&& System.currentTimeMillis() < (tasking.getCreateTime() + WAITING_MILIS_MAX);
		// 出款卡内外层
		BizLevel level = levelService.findFromCache(tasking.getLevel());
		if (tasking.getTaskType() != TaskType.RebateTask.getTypeId()
				&& (Objects.isNull(level) || Objects.isNull(level.getCurrSysLevel()))) {
			log.debug("allocate>> target level check false,task {}", tasking);
			return null;
		}
		// TODO 返利任务暂时当作内层任务
		Integer l = tasking.getTaskType() != TaskType.MemberOutTask.getTypeId() ? INNER : level.getCurrSysLevel();
		boolean mergeLevel = checkMergeLevel(tasking, l);
		// [0]tarVal[1]内层/外层[2]tarType[3]区域[4]handicapId[5]amount
		List<int[]> getFil = filterTarget(tasking, getAll, needWait, mergeLevel, l);
		AllocatingOutwardTask tasked = allocate(tasking, peerTrans, getFil, distHandi, mergeLevel, l);
		return tasked;
	}

	/**
	 * 人款卡出款任务分配
	 *
	 * @param tasking
	 *            待分配任务
	 * @param peerTrans
	 *            同行转账
	 * @return 分配结果 AllocatingOutwardTask#fromId AllocatingOutwardTask#operator
	 * @see this#checkPeerTrans(String) 校验同行转账 返回值说明
	 */
	private AllocatingOutwardTask allocate4In(AllocatingOutwardTask tasking, Boolean peerTrans) throws Exception {
		log.debug("allocate4In>> tasking info {}", tasking.getMsg());
		Set<ZSetOperations.TypedTuple<String>> getAll = cardCache.getUnchecked("OTHERBANK");
		// 当不存在 被分配对象时：抛出特殊异常
		if (CollectionUtils.isEmpty(getAll)) {
			log.debug("allocate4In>> target set is empty {}", tasking);
			return null;
		}
		// 盘口是否开启入款卡备用卡出款，如未开启不分配任务到入款卡、备用卡
		boolean isEnableInbankHandicap = CommonUtils.checkEnableInBankHandicap(tasking.getHandicap());
		if (!isEnableInbankHandicap) {
			log.debug("allocate4In>> handicap isn't open inbank outward task {}", tasking);
			return null;
		}
		// 入款卡内外层
		BizLevel level = levelService.findFromCache(tasking.getLevel());
		if (tasking.getTaskType() != TaskType.MemberOutTask.getTypeId()
				&& (Objects.isNull(level) || Objects.isNull(level.getCurrSysLevel()))) {
			log.debug("allocate4In>> level check false {}", tasking);
			return null;
		}
		// TODO 返利任务暂时当作内层任务
		Integer l = tasking.getTaskType() != TaskType.MemberOutTask.getTypeId() ? INNER : level.getCurrSysLevel();
		boolean mergeLevel = CommonUtils.isMergeMidInner(); // 入款卡备用卡出款时，中层和内层是否合并，取系统配置项
		// [0]tarVal[1]内层/外层[2]tarType[3]区域[4]handicapId[5]amount
		List<int[]> getFil = filterTarget4In(tasking, getAll, mergeLevel, l, false);
		AllocatingOutwardTask tasked = allocate(tasking, peerTrans, getFil, false, mergeLevel, l);
		return tasked;
	}

	/**
	 * 入款卡、出款卡公用分配逻辑
	 *
	 * @param tasking
	 *            待分配任务
	 * @param peerTrans
	 *            是否同行转账
	 * @param getFil
	 *            初步帅选结果集
	 * @param distHandi
	 *            是否区分盘口
	 * @param mergeLevel
	 *            是否中层内层合并
	 * @param l
	 *            层级
	 *
	 */
	private AllocatingOutwardTask allocate(AllocatingOutwardTask tasking, Boolean peerTrans, List<int[]> getFil,
			boolean distHandi, boolean mergeLevel, Integer l) throws Exception {
		if (CollectionUtils.isEmpty(getFil)) {
			log.debug("allocate>> suitable target set empty {}", tasking);
			return null;
		}
		// 黑名单集合
		Set<String> black = (Set<String>) getAllocNeetCache("BLACK");
		// 最后出款信息
		Map<Object, Object> last = (Map<Object, Object>) getAllocNeetCache("LAST");
		// 出款卡银行类别
		String tarBankk = RedisKeys.gen4OutwardTaskBank(tasking.getTaskId());
		String tarBanks = StringUtils.trimToNull(redisService.getStringRedisTemplate().boundValueOps(tarBankk).get());
		// 分配，历史记录
		// List<BizOutwardLog> his = FIRT_OUT_YES != tasking.getFirstOut() &&
		// TARGET_TYPE_THIRD != tasking.getManualOut()
		// ? findOutHisList(tasking) : null;
		List<BizOutwardLog> his = null;
		// 检测：用户，账号 是否具备出款权利
		Iterator<int[]> itFil = getFil.iterator();
		while (itFil.hasNext()) {
			int[] get = itFil.next();
			int tarType = get[2], tarVal = get[0];
			if ((TARGET_TYPE_USER == tarType && !checkManual(tarVal))
					|| (TARGET_TYPE_ROBOT == tarType && !checkRobot(tarVal))
					|| (TARGET_TYPE_THIRD == tarType && !checkThird(tarVal))
					|| (TARGET_TYPE_MOBILE == tarType && !checkMobile(tarVal))) {
				log.debug("allocate>> target is invalid {},target id {}", tasking, tarVal);
				itFil.remove();
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET)
						.remove(String.format(QUEUE_FORMAT_USER_OR_ROBOT, tarType, tarVal));
			}
		}
		if (!CollectionUtils.isEmpty(his)) {
			for (int[] get : getFil) {
				int tarType = get[2], tarVal = get[0];
				tasking.setTarget(String.format(QUEUE_FORMAT_USER_OR_ROBOT, tarType, tarVal));
				if ((TARGET_TYPE_ROBOT == tarType || TARGET_TYPE_MOBILE == tarType)
						&& his.stream().filter(p -> p.getFromId() == tarVal).count() > 0) {
					AccountBaseInfo base = accountService.getFromCacheById(tarVal);
					if (Objects.isNull(base))
						continue;
					AllocatingOutwardTask allocated = allocate(tarBanks, l, tasking, base, null, peerTrans, last, black,
							get[5], false, mergeLevel);
					if (allocated != null) {
						return allocated;
					}
				} else if (TARGET_TYPE_USER == tarType) {
					Map<Integer, BizAccount> acnts4Map = new HashMap<>();
					findOutAccList4Manual(distHandi, tarVal, tasking).forEach(p -> acnts4Map.put(p.getId(), p));
					for (BizOutwardLog log : his) {
						BizAccount acnt = acnts4Map.get(log.getFromId());
						AccountBaseInfo base = Objects.isNull(acnt) ? null
								: accountService.getFromCacheById(acnt.getId());
						int bal = acnt == null || acnt.getBankBalance() == null ? 0 : acnt.getBankBalance().intValue();
						AllocatingOutwardTask allocated = allocate(tarBanks, l, tasking, base, tarVal, peerTrans, last,
								black, bal, true, false);
						if (allocated != null) {
							return allocated;
						}
					}
				}
			}
		}
		// 随机分配( 第三方 人工/首次 机器)
		for (int[] get : getFil) {
			int tarType = get[2], tarVal = get[0];
			tasking.setTarget(String.format(QUEUE_FORMAT_USER_OR_ROBOT, tarType, tarVal));
			if (TARGET_TYPE_THIRD == tarType) {
				log.debug("allocate>> target third type check pass,task {},target {}", tasking, tarVal);
				tasking.setOperator(tarVal);
				tasking.setFromId(null);
				return tasking;
			} else if (TARGET_TYPE_USER == tarType) {
				Map<Integer, List<BizAccount>> userAccMap = (Map<Integer, List<BizAccount>>) getAllocNeetCache(
						"USERACCMAP");
				List<BizAccount> acnts4User = userAccMap == null ? null : userAccMap.get(tarVal);
				acnts4User = checkOutAcc4Manual(distHandi, tasking, acnts4User);
				AllocatingOutwardTask allocated = allocate(tarBanks, l, tasking, acnts4User, tarVal, peerTrans, last,
						black, true, false);
				if (allocated != null) {
					log.debug("allocate>> target user type check pass,task {},target id {}, user {}", tasking,
							allocated.getFromId(), allocated.getOperator());
					return allocated;
				}
			} else if (TARGET_TYPE_ROBOT == tarType || TARGET_TYPE_MOBILE == tarType) {
				AccountBaseInfo base = accountService.getFromCacheById(tarVal);
				if (Objects.isNull(base))
					continue;
				AllocatingOutwardTask allocated = allocate(tarBanks, l, tasking, base, null, peerTrans, last, black,
						get[5], false, mergeLevel);
				if (allocated != null) {
					log.debug("allocate>> target robot type check pass,task {},target id {}", tasking,
							allocated.getFromId());
					return allocated;
				}
			}
		}
		return null;
	}

	private AllocatingOutwardTask allocate(String tarBanks, Integer l, AllocatingOutwardTask tasking,
			List<BizAccount> acntList, Integer operator, Boolean peerTrans, Map<Object, Object> last, Set<String> black,
			boolean isManul, boolean mergeLevel) {
		if (Objects.nonNull(acntList) && acntList.size() > 0) {
			for (BizAccount acnt : acntList) {
				int bal = acnt == null || acnt.getBankBalance() == null ? 0 : acnt.getBankBalance().intValue();
				AllocatingOutwardTask tasked = allocate(tarBanks, l, tasking,
						accountService.getFromCacheById(acnt.getId()), operator, peerTrans, last, black, bal, isManul,
						mergeLevel);
				if (tasked != null) {
					return tasked;
				}
			}
		}
		return null;
	}

	private AllocatingOutwardTask allocate(String tarBanks, Integer l, AllocatingOutwardTask tasking,
			AccountBaseInfo acnt, Integer operator, Boolean peerTrans, Map<Object, Object> last, Set<String> black,
			int bal, boolean isManul, boolean mergeLevel) {
		if (acnt != null && checkBank(tarBanks, acnt) && checkLevel(l, acnt, isManul, mergeLevel)
				&& checkTrans(peerTrans, acnt.getBankType(), tasking.getToAccountBank())
				&& checkTask(last, acnt.getId(), tasking.getMemBank(), tasking.getTaskAmount())
				&& checkLimitOutOne(acnt, tasking) && checkBlack(acnt.getId(), bal, tasking, black)) {
			tasking.setFromId(acnt.getId());
			tasking.setOperator(operator);
			return tasking;
		}
		return null;
	}

	/**
	 * 从出款卡中过滤分配目标数据
	 *
	 * @param o
	 * @param gets
	 * @param needWait
	 * @param mergeLevel
	 * @param l
	 * @return
	 */
	private List<int[]> filterTarget(AllocatingOutwardTask o, Set<ZSetOperations.TypedTuple<String>> gets,
			boolean needWait, boolean mergeLevel, Integer l) {
		// 0:robot first;1:mobile first;2:only robot;3:only mobile
		String allocStrategy = buildAllocateStrategy();
		boolean allManual = Objects.equals(AppConstants.OUTTASK_ALLOCATE_TO_MANULE, allocStrategy);
		Integer zone = handicapService.findZoneByHandiId(o.getZone());
		Integer taskType = o.getManualOut();
		float taskAmount = o.getTaskAmount();
		List<int[]> resOver = new ArrayList<>(), retNeed = new ArrayList<>();
		for (ZSetOperations.TypedTuple<String> get : gets) {
			String[] tar = get.getValue().split(":");// [0]:tarType;[1]:typeVal
			int tarType = Integer.parseInt(tar[0]);
			// [0]tarVal[1]内层/外层/中层[2]tarType[3]区域[4]handicapId[5]amount
			int[] inf = descore(tarType, Integer.parseInt(tar[1]), get.getScore());
			if (atedTarget.getIfPresent(inf[0]) != null) {
				log.debug("filterTarget>> target already has task {},target id {}", o, inf[0]);
				continue;
			}
			// 1、机器出款卡所在的区域和任务区域一致
			// 2、非机器出款，用户出所拥有权限盘口的任务
			if (inf == null || inf.length < 6
					|| ((!allManual && taskType == ROBOT_OUT_YES && inf[3] != zone)
							|| ((taskType != ROBOT_OUT_YES || allManual)
									&& (tarType == TARGET_TYPE_THIRD || tarType == TARGET_TYPE_USER)
									&& !checkUserDataPermission(tar[1], o.getHandicap())))) {
				log.debug(
						"filterTarget>> zone check,taskType check,taskid {} ,inf {},taskType {}, tarType {},data right {},target id {}",
						o.getTaskId(), inf, taskType, tarType, !checkUserDataPermission(tar[1], o.getHandicap()),
						inf[0]);
				continue;
			}
			log.debug(
					"filterTarget>> all check passed except remain amount check! task {},taskType {},target id {},tarType {}",
					o, taskType, inf[0], tarType);
			// task为ROBOT_OUT_YES，策略为NOT_PC_AND_MOBILE时，将任务分配给人工
			if (MANUAL_OUT_YES == taskType || THIRD_OUT_YES == taskType || allManual) {
				if (MANUAL_OUT_YES == taskType && TARGET_TYPE_USER == tarType
						|| THIRD_OUT_YES == taskType && TARGET_TYPE_THIRD == tarType
						|| ROBOT_OUT_YES == taskType && TARGET_TYPE_USER == tarType)
					resOver.add(inf);
			} else if (checkAllocateStrategy(allocStrategy, tarType)) {// task为ROBOT_OUT_YES
				if (inf[1] == l || OUTTER != inf[1] && DESIGNATED != inf[1] && mergeLevel) {
					if (inf[5] - taskAmount >= 30) {
						resOver.add(inf);
					} else if (tarType != TARGET_TYPE_MOBILE) { // 手机出款卡不接收大于余额的任务
						retNeed.add(inf);
					}
				}
			}
		}
		if (MANUAL_OUT_YES == taskType || THIRD_OUT_YES == taskType || allManual) {
			return resOver;
		}
		if (resOver.size() > 0) {
			resOver = sortForFilter(resOver, taskAmount, allocStrategy, true);
		}
		if (retNeed.size() > 0) {
			retNeed = sortForFilter(retNeed, taskAmount, allocStrategy, false);
		}
		resOver.addAll(retNeed);
		return resOver;
	}

	/**
	 * 从入款卡中过滤分配目标数据
	 *
	 * @param o
	 *            待分配任务
	 * @param gets
	 *            非出款卡待分配数据集
	 * @param mergeLevel
	 *            是否层级合并
	 * @param l
	 *            层级
	 * @param distHandi
	 *            区域内是否分盘口
	 * @return
	 */
	private List<int[]> filterTarget4In(AllocatingOutwardTask o, Set<ZSetOperations.TypedTuple<String>> gets,
			boolean mergeLevel, Integer l, boolean distHandi) {
		String allocStrategy = buildAllocateStrategy();
		if (Objects.equals(AppConstants.OUTTASK_ALLOCATE_TO_MANULE, allocStrategy)) {
			log.debug("filterTarget4In>> all task allocate to manule {}", o);
			return null;
		}
		Integer taskType = o.getManualOut();
		if (MANUAL_OUT_YES == taskType || THIRD_OUT_YES == taskType) {
			log.debug("filterTarget4In>> task type check false {}", o);
			return null;
		}
		Integer zone = handicapService.findZoneByHandiId(o.getZone());
		float taskAmount = o.getTaskAmount();
		List<int[]> resOver = new ArrayList<>();
		for (ZSetOperations.TypedTuple<String> get : gets) {
			String tar = get.getValue();// 入款卡只存了账号ID typeVal
			Integer accId = Integer.parseInt(tar);
			AccountBaseInfo base = accountService.getFromCacheById(accId);
			int tarType = TARGET_TYPE_ROBOT;
			if (atedTarget.getIfPresent(accId) != null) {
				log.debug("filterTarget4In>> target id has task {},target id {}", o, accId);
				continue;
			}
			// TODO 以后返利网出款要改 非手机银行入款卡不出款
			if (base.getType() == INBANK && (base.getFlag() == null || base.getFlag() != 1)) {
				log.debug("filterTarget4In>> target id flag check false {},target id {}", o, accId);
				continue;
			}
			// TODO 外面的逻辑（入款卡不给首次出款的任务出款）移进来，后续 4440需求再改掉
			if (base.getType() == INBANK && o.getFirstOut() == FIRT_OUT_YES) {
				log.debug("filterTarget4In>> target first out check false {},target id {}", o, accId);
				continue;
			}
			log.debug("filterTarget4In>> target check pass1 {},target id {}", o, accId);
			// [0]tarVal[1]内层/外层/中层[2]tarType[3]区域[4]handicapId[5]amount
			int[] inf = deFrscore(tarType, accId, get.getScore());
			boolean tarEnableInbankHandicap = CommonUtils.checkEnableInBankHandicap(inf[4]);
			int minbal = buildLimitBalance(base);
			if (base.getType() == INBANK) {
				boolean overLimit = CommonUtils.getInbankOverLimitBeginAllocate();
				int maxAllocateBalance = CommonUtils.getMaxBalanceForInbankCommonAllocate();
				int minTaskAmount = CommonUtils.getMinTaskAmountForInbank();
				log.debug("filterTarget4In>> target type inbank {},target id {}", o, accId);
				// 入款卡超过余额告警时分配出款任务，入款卡余额大于正常分配最大金额或小于最小出款任务金额 不进行常规分配
				if (overLimit && inf[5] < minbal || inf[5] > maxAllocateBalance || inf[5] < minTaskAmount) {
					log.debug("filterTarget4In>> target id amount check false {},target id {}", o, accId);
					continue;
				}
				if (!checkBank(o, base)) {
					log.debug("filterTarget4In>> target id bank check false {},target id {}", o, accId);
					continue;
				}
			}
			if (inf == null || inf.length < 6 || inf[3] != zone || (distHandi && inf[4] != o.getHandicap())) {
				log.debug("filterTarget4In>> target id zone check false {},target id {}", o, accId);
				continue;
			}
			if (checkAllocateStrategy(allocStrategy, tarType) && tarEnableInbankHandicap) {// task为ROBOT_OUT_YES
				if (inf[1] == l || mergeLevel && OUTTER != inf[1] && DESIGNATED != inf[1]) {
					log.debug("filterTarget4In>> target type check level {},target id {}", o, accId);
					int multiple = CommonUtils.getInReserveOutwardMultiple();
					/**
					 * 1、备用卡余额小于余额告警值和5000两者之间的较小值或余额小于任务金额的 INBANK_RESERVEBANK_ALLOCATE_MULTIPLE
					 * 倍数<br>
					 * 2、入款卡被选为出款条件：PC转账 手机抓流水 模式 <br>
					 * 3、客户绑定卡被选为出款条件：客户绑定卡余额 - 任务金额 > 50 <br>
					 **/
					if (inf[5] - taskAmount >= 50 && ((base.getType() == RESERVEBANK
							&& (inf[5] < taskAmount * multiple || inf[5] < minbal || taskAmount >= 5000))
							|| base.getType() == INBANK || base.getType() == BINDCUSTOMER)) {
						log.debug("filterTarget4In>> balance check pass {},target id {}", o, accId);
						resOver.add(inf);
					}
				}
			}
		}
		if (resOver.size() > 0) {
			resOver = sortForFilter(resOver, taskAmount, allocStrategy, true);
		}
		return resOver;
	}

	@Override
	public boolean checkInv(AccountBaseInfo base, Set<Integer> ids) {
		if (Objects.isNull(base) || Objects.isNull(base.getId()) || CollectionUtils.isEmpty(ids)
				|| Objects.nonNull(base.getHolder()))
			return true;
		return !ids.contains(base.getId());
	}

	private boolean checkInvokeTm(AccountBaseInfo acnt, Set<Integer> accIdSetInValTm) {
		if (Objects.nonNull(acnt) && Objects.isNull(acnt.getHolder()) && Objects.equals(acnt.getFlag(), 1))
			return !CollectionUtils.isEmpty(accIdSetInValTm) && accIdSetInValTm.contains(acnt.getId());
		return true;
	}

	/**
	 * 检测 账号(c：屏蔽旧逻辑备用卡接出款任务)
	 */
	private boolean checkRobot(int accountId) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		Integer type = base.getType();
		return base != null && base.getHolder() == null && type != null
				&& AccountStatus.Normal.getStatus().equals(base.getStatus());
	}

	/**
	 * 检测 用户
	 */
	private boolean checkManual(int userId) {
		SysUser user = userService.findFromCacheById(userId);
		return user != null && user.getCategory() != null
				&& com.xinbo.fundstransfer.domain.pojo.UserCategory.Outward.getCode() == user.getCategory()
				&& userService.online(userId) && SUSPEND_OPERATOR.getIfPresent(userId) == null;
	}

	/**
	 * 检测 账号
	 */
	private boolean checkMobile(int accountId) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		Integer type = base.getType();
		return base.getHolder() == null && type != null && AccountStatus.Normal.getStatus().equals(base.getStatus())
				&& (OUTBANK == type || OUTTHIRD == type || INBANK == type || RESERVEBANK == type
						|| BINDCUSTOMER == type)
				&& base.getFlag() != null && (base.getFlag() == 1 || base.getFlag() == 2)
				&& SUSPEND_ACCOUNT.getIfPresent(accountId) == null;
	}

	/**
	 * 检测 账号
	 */
	private boolean checkMobileNew(int accountId) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		Integer type = base.getType();
		return base.getHolder() == null && type != null && AccountStatus.Normal.getStatus().equals(base.getStatus())
				&& (OUTBANK == type || OUTTHIRD == type || INBANK == type || RESERVEBANK == type
						|| BINDCUSTOMER == type)
				&& base.getFlag() != null && (base.getFlag() == 1 || base.getFlag() == 2);
	}

	/**
	 * 检测 银行类别
	 */
	private boolean checkBank(String tarBanks, AccountBaseInfo p) {
		return (StringUtils.isBlank(tarBanks) || tarBanks.contains(p.getBankType())) && checkBank(p.getBankType());
	}

	public boolean checkBank(String bankType) {
		if (!CommonUtils.isEnableCheckBank()) {
			return true;
		}
		if (StringUtils.isBlank(bankType) || !"民生银行,交通银行,华夏银行,建设银行,中国银行,工商银行,内蒙古银行,农业银行".contains(bankType))
			return true;
		Long[] stTmEdTm = BANK_TYPE_HALT_SERVICE.get(bankType);
		Long stTm = null, edTm = null;
		if (Objects.nonNull(stTmEdTm) && stTmEdTm.length >= 2) {
			stTm = stTmEdTm[0];
			edTm = stTmEdTm[1];
		}
		long curr = System.currentTimeMillis();
		if (stTm != null && edTm != null && curr < edTm)
			return curr < stTm;
		Calendar cal = Calendar.getInstance();
		int stD = 0, stH = 0, stM = 0, stS = 0, edD = 0, edH = 0, edM = 0, edS = 0;
		if ("民生银行".contains(bankType)) {// 0:00:00-02:00:00->~-02:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR) - 1;
			stH = 23;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR);
			edH = 2;
			edM = 4;
			edS = 59;
		} else if ("交通银行".contains(bankType)) {// 00:00:00-01:00:00->~-01:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR) - 1;
			stH = 23;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR);
			edH = 1;
			edM = 4;
			edS = 59;
		} else if ("华夏银行".contains(bankType)) {// 21:30:00-24:00:00->21:26:59-00:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 21;
			stM = 26;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 0;
			edM = 4;
			edS = 59;
		} else if ("建设银行".contains(bankType)) {// 22:00:00-24:00:00->21:56:59-00:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 21;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 0;
			edM = 4;
			edS = 59;
		} else if ("工商银行".contains(bankType)) {// 23:30:00-24:00:00->23:26:59-00:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 23;
			stM = 26;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 0;
			edM = 4;
			edS = 59;
		} else if ("内蒙古银行".contains(bankType)) {// 20:30:00-24:00:00->20:26:59-00:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 20;
			stM = 26;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 0;
			edM = 4;
			edS = 59;
		} else if ("中国银行".contains(bankType)) {// 22:00:00-03:00:00->21:56:59-03:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 21;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 3;
			edM = 4;
			edS = 59;
		} else if ("农业银行".contains(bankType)) {// 下午4点、晚上12点整，银行维护
			stD = cal.get(Calendar.DAY_OF_YEAR);// 下午4点
			stH = 15;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR);
			edH = 16;
			edM = 4;
			edS = 59;
		}
		cal.set(Calendar.DAY_OF_YEAR, stD);
		cal.set(Calendar.HOUR_OF_DAY, stH);
		cal.set(Calendar.MINUTE, stM);
		cal.set(Calendar.SECOND, stS);
		stTm = cal.getTimeInMillis();
		cal.set(Calendar.DAY_OF_YEAR, edD);
		cal.set(Calendar.HOUR_OF_DAY, edH);
		cal.set(Calendar.MINUTE, edM);
		cal.set(Calendar.SECOND, edS);
		edTm = cal.getTimeInMillis();
		if (edTm <= curr) {
			if ("农业银行".contains(bankType)) {// 晚上12点整
				cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 56);
				cal.set(Calendar.SECOND, 59);
				stTm = cal.getTimeInMillis();
				cal.add(Calendar.DAY_OF_YEAR, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 4);
				cal.set(Calendar.SECOND, 59);
				edTm = cal.getTimeInMillis();
			} else {
				stTm = stTm + 86400000;
				edTm = edTm + 86400000;
			}
		}
		if (Objects.isNull(stTmEdTm)) {
			BANK_TYPE_HALT_SERVICE.put(bankType, new Long[] { stTm, edTm });
		} else {
			stTmEdTm[0] = stTm;
			stTmEdTm[1] = edTm;
		}
		return curr < stTm;
	}

	private boolean checkUnmatch(Integer id, Set<Integer> unMatched) {
		return Objects.isNull(id) || CollectionUtils.isEmpty(unMatched) || !unMatched.contains(id);
	}

	/**
	 * 检测 内外层
	 */
	private boolean checkLevel(int lTask, AccountBaseInfo acnt, boolean isMunal, boolean mergeLevel) {
		if (isMunal)
			return true;
		Integer levelP = acnt.getCurrSysLevel() == null ? OUTTER : acnt.getCurrSysLevel();
		return mergeLevel
				? ((lTask & levelP) == OUTTER || (lTask & levelP) == DESIGNATED
						|| lTask != OUTTER && levelP != OUTTER && lTask != DESIGNATED && levelP != DESIGNATED)
				: ((lTask & levelP) == OUTTER || (lTask & levelP) == INNER || (lTask & levelP) == MIDDLE
						|| (lTask & levelP) == DESIGNATED);
	}

	/**
	 * 检测 当日出款
	 */
	private boolean checkDailyOut(AccountBaseInfo acnt) {
		boolean flag = true;
		if (acnt == null) {
			return flag;
		}
		AccountBaseInfo base = accountService.getFromCacheById(acnt.getId());
		// 入款卡\备用卡\客户绑定卡不校验当日出款限额
		if (base == null || INBANK == base.getType() || RESERVEBANK == base.getType()
				|| BINDCUSTOMER == base.getType()) {
			return flag;
		}
		int income0outward1 = 1;
		float outDaily = accountService.findAmountDailyByTotal(income0outward1, acnt.getId()).floatValue();
		if (acnt.getLimitOut() != null) {
			flag = acnt.getLimitOut() > outDaily;
			if (!flag) {
				log.debug("卡的最高出款限额:{},当日累计出款:{}", acnt.getLimitOut(), outDaily);
				// 暂停
				if (acnt.getFlag() == 2) {
					accountService.savePauseOrResumeOrOnlineForMobile(acnt.getId(), 88);
				} else {
					accountService.saveOnlineAccontIds(acnt.getId(), false);
					accountService.savePauseOrResumeAccountId(acnt.getId(), 88);
				}
			}
			return flag;
		}
		String proVal = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_LIMIT_CHECKOUT_TODAY.getValue());
		if (StringUtils.isBlank(proVal)) {
			log.trace("当日出款限额检测  请设置系统当日出款限额");
			return flag;
			// return true;
		}
		flag = Float.parseFloat(proVal) > outDaily;
		if (!flag) {
			log.debug("系统设置的最高限额:{},当日累计出款:{}", proVal, outDaily);
			// 暂停
			if (acnt.getFlag() == 2) {
				accountService.savePauseOrResumeOrOnlineForMobile(acnt.getId(), 88);
			} else {
				accountService.saveOnlineAccontIds(acnt.getId(), false);
				accountService.savePauseOrResumeAccountId(acnt.getId(), 88);
			}
		}
		return flag;
	}

	/**
	 * 检测机器出款限额
	 *
	 * @param amount
	 *            待分配出款任务
	 * @return true:通过；false:不通过
	 */
	private boolean checkRobotLimit(float amount) {
		String val = MemCacheUtils.getInstance().getSystemProfile().get(UserProfileKey.OUTDRAW_MONEYLIMIT.getValue());
		return !StringUtils.isNumeric(val) || amount <= Integer.parseInt(val);
	}

	/**
	 * 检测首次出款
	 * <p>
	 * 检测出款审核信息中是否包含"首次出款"或"出款银行卡不一致"
	 * </p>
	 *
	 * @param review
	 *            出款请求审核记录
	 */
	@Override
	public boolean checkFirst(String review) {
		return StringUtils.isNotBlank(review) && (review.contains("首次出款") || review.contains("出款银行卡不一致"));
	}

	/**
	 * 检测是否需要终止分配服务
	 * <p>
	 * 1.如果停服开始时间为空，或停服结束时间为空：则：设置停服开始时间，与停服结束时间</br>
	 * 2.如果当前时间处于停服时间段，则停服</br>
	 * 3.如果当前时间已停服时间段，则：置 停服开始时间，停服结束时间为空</br>
	 * </p>
	 */
	private void checkHaltService() {
		long curr = System.currentTimeMillis();
		if (HALT_SERVICE_START_TIME == null || HALT_SERVICE_END_TIME == null) {
			// 获取停服配置信息
			String val = MemCacheUtils.getInstance().getSystemProfile()
					.get(UserProfileKey.OUTDRA_HALT_ALLOC_START_TIME.getValue());
			val = StringUtils.isNotBlank(val) ? val : "06:50:00";
			String[] args = val.split(":");
			Calendar cal = Calendar.getInstance();
			// 设置停服开始时间
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(args[0]));
			cal.set(Calendar.MINUTE, Integer.parseInt(args[1]));
			cal.set(Calendar.SECOND, Integer.parseInt(args[2]));
			HALT_SERVICE_START_TIME = cal.getTime().getTime();
			// 设置停服结束时间
			cal.add(Calendar.HOUR_OF_DAY, 1);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			HALT_SERVICE_END_TIME = cal.getTime().getTime();
			// 如果当前时间已过停服时间段，则：停服时间段加一天
			if (curr >= HALT_SERVICE_END_TIME) {
				HALT_SERVICE_START_TIME = HALT_SERVICE_START_TIME + 86400000;
				HALT_SERVICE_END_TIME = HALT_SERVICE_END_TIME + 86400000;
			}
		}
		if (HALT_SERVICE_START_TIME <= curr && curr <= HALT_SERVICE_END_TIME) {// 不能把if改为elseif
			log.info("出款任务分配:停止服务开始...");
			try {
				Thread.sleep(HALT_SERVICE_END_TIME - System.currentTimeMillis() + 3000);
			} catch (Exception e) {
				log.debug("", e);
			}
			log.info("出款任务分配:停止服务结束...");
		} else if (curr > HALT_SERVICE_END_TIME) {
			HALT_SERVICE_START_TIME = null;
			HALT_SERVICE_END_TIME = null;
		}
	}

	/**
	 * 检测当前银行是否处于维护状态
	 *
	 * @param currBank
	 *            当前银行
	 * @return true:维护中；false:非维护中
	 */
	@Override
	public boolean checkMaintain(String currBank) {
		if (StringUtils.isBlank(currBank)) {
			return false;
		}
		String banks = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_SYS_MAINTAIN_BANKTYPE.getValue());
		return StringUtils.isNotBlank(banks) && banks.contains(currBank);
	}

	/**
	 * 检测 该出款任务财务是否已进行确认
	 *
	 * @return null:不需要财务确认(会员出款)</br>
	 *         true:财务人员已确认</br>
	 *         false:财务人员未确认
	 */
	private Boolean checkFinAcked(BizOutwardTask task) {
		return StringUtils.isBlank(task.getMember())
				? (Objects.nonNull(task.getRemark()) && task.getRemark().contains(FIN_ACKED))
				: null;
	}

	/**
	 * 检测 该出款请求是否为公司用款
	 *
	 * @param oReq
	 *            出款请求
	 * @return true：是;false:否
	 */
	private boolean checkComOutward(BizOutwardRequest oReq) {
		return Objects.nonNull(oReq) && StringUtils.isBlank(oReq.getMember());
	}

	/**
	 * 检测银行间转账
	 *
	 * @param peerTrans
	 *            toBank是否处于同行转账模式
	 * @param fromBank
	 *            汇出银行
	 * @param toBank
	 *            汇入银行
	 * @return true:fromBank可以转账到toBank；false:fromBank不可以转账到toBank
	 */
	@Override
	public boolean checkTrans(Boolean peerTrans, String fromBank, String toBank) {
		return peerTrans == null || (peerTrans && StringUtils.isNotBlank(fromBank) && StringUtils.isNotBlank(toBank))
				&& (toBank.contains(fromBank) || fromBank.equals("云南农信") && "云南省农村信用社".contains(toBank));
	}

	/**
	 * 检测同行转账
	 * <p>
	 * 1.检测同行转账模式是否处于打开状态,否：返回null,是：流程继续</br>
	 * 2.检测该银行是否处于同行转账模式中,否：返回false,是：返回true</br>
	 * </p>
	 *
	 * @param currBank
	 *            当前银行全称 或 当前银行简称
	 * @return null:同行转账模式关闭；true:同行转账模式打开且处于同行转账模式；false:同行转账模式打开且不处于同行转账模式
	 */
	@Override
	public Boolean checkPeerTrans(String currBank) {
		String banks = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_SYS_PEER_TRANSFER.getValue());// 同行转账银行简称集，以英文逗号分割
		if (StringUtils.isBlank(banks)) {
			return null;
		}
		if (StringUtils.isBlank(currBank)) {
			return false;
		}
		for (String bank : banks.split(",")) {
			if (currBank.contains(bank)) {
				return true;
			}
			if (bank.equals("云南农信") && "云南省农村信用社".contains(currBank)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取第三方最低限额
	 */
	@Override
	public int findThirdLowBal() {
		String profile = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_THIRD_LOWEST_BAL.getValue());
		return StringUtils.isNumeric(profile) ? Integer.valueOf(profile) : 10000;
	}

	/**
	 * 外层拆单金额
	 */
	@Override
	public int findSplitOut() {
		String splitOut = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_SPLIT_AMOUNT_OUTSIDE.getValue());
		return StringUtils.isNumeric(splitOut) ? Integer.parseInt(splitOut) : 1000;
	}

	/**
	 * 内层拆单金额
	 */
	@Override
	public int findSplitIn() {
		String splitIn = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_SPLIT_AMOUNT_INSIDE.getValue());
		return StringUtils.isNumeric(splitIn) ? Integer.parseInt(splitIn) : 1000;
	}

	/**
	 * set the attribute value for <tt>MergeLevel</tt> program.
	 *
	 * @param broad
	 *            true:broadcast the <tt>MergeLevel</tt> program settings to all web
	 *            server hosts;false:update the native host's
	 *            <tt>MergeLevel</tt>program settings;
	 * @param on
	 *            0:close the <tt>MergeLevel</tt> program;1:open the
	 *            <tt>MergeLevel</tt> program;
	 * @param durTm
	 *            the hours the <tt>MergeLevel</tt> program lasts from now on.
	 * @see this#getMergeLevel(int)
	 * @see this#checkMergeLevel(AllocatingOutwardTask, Integer)
	 */
	@Override
	public void setMergeLevel(boolean broad, int zone, boolean on, int durTm) {
		if (broad) {
			redisService.convertAndSend(RedisTopics.REFRESH_OTASK_MERGE_LEVEL,
					String.format("%d:%d:%d", zone, (on ? 1 : 0), durTm));
		} else if (on && durTm > 0) {
			Cache<String, Long> MERGE_LEVEL = CacheBuilder.newBuilder().maximumSize(4)
					.expireAfterWrite(durTm, TimeUnit.HOURS).build();
			long edTm = System.currentTimeMillis() + durTm * 3600000;
			MERGE_LEVEL.put(ALLOC_MERGE_LEVEL_LAST_TIME, Long.valueOf(durTm));
			MERGE_LEVEL.put(ALLOC_MERGE_LEVEL_DEADLINE, edTm);
			MERGE_LEVEL_SET[zone] = MERGE_LEVEL;
		} else {
			MERGE_LEVEL_SET[zone] = null;
		}

	}

	/**
	 * Returns the attribute value setting for <tt>MergeLevel</tt>.If the attributes
	 * haven't been set or expired in cache, then this method returns
	 * <code>new long[]{0,0,0}</code>
	 * <p>
	 * <p>
	 * the return value is a long array which contains only 3 elements. the first
	 * one :0:close the <tt>MergeLevel</tt> program;1:open the <tt>MergeLevel</tt>
	 * program; the second one:the hours the <tt>MergeLevel</tt> program lasts if
	 * the program is opened</br>
	 * the third one:the deadline's milliseconds
	 *
	 * @return the attribute value setting for <tt>MergeLevel</tt> or
	 *         <code>new long[]{0,0,0}</code> if the attributes haven't been set or
	 *         expired in cache.
	 * @see this#setMergeLevel(boolean, int, boolean, int)
	 * @see this#checkMergeLevel(AllocatingOutwardTask, Integer)
	 */
	@Override
	public long[] getMergeLevel(int zone) {
		Cache<String, Long> MERGE_LEVEL = MERGE_LEVEL_SET[zone];
		if (Objects.isNull(MERGE_LEVEL) || MERGE_LEVEL.size() == 0)
			return new long[] { 0, 0, 0 };
		Long lastTime = MERGE_LEVEL.getIfPresent(ALLOC_MERGE_LEVEL_LAST_TIME);
		Long deadline = MERGE_LEVEL.getIfPresent(ALLOC_MERGE_LEVEL_DEADLINE);
		if (Objects.isNull(lastTime) || Objects.isNull(deadline))
			return new long[] { 0, 0, 0 };
		return new long[] { 1, lastTime, deadline };
	}

	/**
	 * store account outward time.
	 *
	 * @param accId
	 *            account's identity
	 * @param taskId
	 *            task's identity
	 */
	public void recordLog(Integer accId, Long taskId) {
		if (Objects.nonNull(accId) && Objects.nonNull(taskId) && accId != 0 && taskId != 0) {
			redisService.getStringRedisTemplate().opsForHash().put(RedisKeys.LAST_TIME_OUTWARD, accId.toString(),
					String.valueOf(System.currentTimeMillis()));
			allocTransSer.addToBlackList(accId, taskId, 240);
		}
	}

	/**
	 * check if <tt>MergeLevel</tt> program already started.
	 *
	 * @return <code>true</code> if the <tt>MergeLevel</tt> program already
	 *         started,otherwise return <code>false</code>
	 * @see this#setMergeLevel(boolean, int, boolean, int) ;
	 * @see this#getMergeLevel(int)
	 */
	private boolean checkMergeLevel(AllocatingOutwardTask tasking, Integer l) {
		Cache<String, Long> MERGE_LEVEL = MERGE_LEVEL_SET[handicapService.findZoneByHandiId(tasking.getZone())];
		return l != OUTTER && l != DESIGNATED && Objects.equals(tasking.getManualOut(), ROBOT_OUT_YES)
				&& (Objects.nonNull(MERGE_LEVEL)
						&& Objects.nonNull(MERGE_LEVEL.getIfPresent(ALLOC_MERGE_LEVEL_LAST_TIME))
						&& Objects.nonNull(MERGE_LEVEL.getIfPresent(ALLOC_MERGE_LEVEL_DEADLINE)));
	}

	/**
	 * 检测 防止该出款卡连续给同一银行卡出相同金额
	 *
	 * @param last
	 *            上次出款记录集
	 * @param frId
	 *            出款卡ID
	 * @param acc
	 *            出款账号
	 * @param amt
	 *            出款金额
	 * @return true:可以出款；false:不可以出款
	 */
	private boolean checkTask(Map<Object, Object> last, Integer frId, String acc, Float amt) {
		if (last != null && frId != null && acc != null && amt != null) {
			Object val = last.get(String.valueOf(frId));
			return !(val != null && val.toString().endsWith(acc + ":" + amt.intValue()));
		}
		return true;
	}

	/**
	 * 检测出款账号单笔最高/最低限额
	 *
	 * @param acc
	 *            账号
	 * @param tasking
	 *            待分配任务
	 * @return true:可以出款;false:不可以出款
	 */
	private boolean checkLimitOutOne(AccountBaseInfo acc, AllocatingOutwardTask tasking) {
		return (!(acc != null && acc.getLimitOutOne() != null && acc.getLimitOutOne() > 0
				&& ((acc.getFlag() != null && acc.getFlag() == 1) && acc.getLimitOutOne() < tasking.getTaskAmount()
						|| (acc.getFlag() == null || acc.getFlag() != 1)
								&& (acc.getLimitOutOne() + 5000) <= tasking.getTaskAmount())))
				&& (!(acc != null && acc.getLimitOutOneLow() != null && acc.getLimitOutOneLow() > 0
						&& acc.getLimitOutOneLow() > tasking.getTaskAmount()));
	}

	private boolean checkOutCtnDaily(AccountBaseInfo acc, Map<String, Float> outCtnDaily) {
		if (Objects.isNull(acc) || Objects.isNull(acc.getLimitOutCount()) || acc.getLimitOutCount() <= 0
				|| Objects.isNull(outCtnDaily)) {
			return true;
		}
		Float ctn = outCtnDaily.get(String.valueOf(acc.getId()));
		boolean flag = Objects.isNull(ctn) || ctn <= acc.getLimitOutCount();
		if (!flag) {
			if (acc.getFlag() == 2) {
				accountService.savePauseOrResumeOrOnlineForMobile(acc.getId(), 88);
			} else {
				accountService.saveOnlineAccontIds(acc.getId(), false);
				accountService.savePauseOrResumeAccountId(acc.getId(), 88);
			}
		}
		return flag;
	}

	/**
	 * 下发黑名单检测
	 *
	 * @param accId
	 *            账号
	 * @param bal
	 *            accId余额
	 * @param tasking
	 *            待分配任务
	 * @param black
	 *            黑名单列表
	 * @return true:通过检测；</br>
	 *         false:未通过检测</br>
	 */
	private boolean checkBlack(int accId, int bal, AllocatingOutwardTask tasking, Set<String> black) {
		return (allocTransSer.checkBlack(black, AllocateTransferService.WILD_CARD_ACCOUNT, accId)
				|| (bal > tasking.getTaskAmount())) && allocTransSer.checkBlack(black, accId, tasking.getTaskId());
	}

	/**
	 * 人工出款时，获取出款账号
	 * <p>
	 * 当
	 * <code>allocating.getHandicap() == null || allocating.getHandicap() == WILD_CARD_HANDICAP</code>
	 * </br>
	 * 表示该 出款任务不区分盘口</br>
	 * 当
	 * <code>BizAccount.getHandicap() == null || BizAccount.getHandicap() == WILD_CARD_HANDICAP</code>
	 * </br>
	 * 表示该出款卡可以给任何盘口的出款任务出款</br>
	 * </p>
	 *
	 * @param userId
	 *            用户ID
	 * @param allocating
	 *            待出款任务
	 * @return 出款账号
	 */
	private List<BizAccount> findOutAccList4Manual(boolean distHandi, int userId, AllocatingOutwardTask allocating) {
		List<BizAccount> accList = accountService.find4OutwardAsign(userId);
		if (distHandi) {
			accList = accList.stream()
					.filter(p -> Objects.isNull(p.getHandicapId())
							|| Objects.equals(p.getHandicapId(), WILD_CARD_HANDICAP)
							|| Objects.equals(p.getHandicapId(), allocating.getHandicap()))
					.collect(Collectors.toList());
		}
		return accList;
	}

	private Map<Integer, List<BizAccount>> findOutAccList4Manual() {
		List<BizAccount> accList = accountService.findOutAccList4Manual();
		Map<Integer, List<BizAccount>> accMap = new HashMap<>();
		for (BizAccount acc : accList) {
			List<BizAccount> temp = accMap.get(acc.getHolder());
			if (temp == null) {
				temp = new ArrayList<>();
			}
			temp.add(acc);
			accMap.put(acc.getHolder(), temp);
		}
		return accMap;
	}

	private List<BizAccount> checkOutAcc4Manual(boolean distHandi, AllocatingOutwardTask allocating,
			List<BizAccount> accList) {
		if (accList == null || accList.size() == 0) {
			return null;
		}
		if (distHandi) {
			accList = accList.stream()
					.filter(p -> Objects.isNull(p.getHandicapId())
							|| Objects.equals(p.getHandicapId(), WILD_CARD_HANDICAP)
							|| Objects.equals(p.getHandicapId(), allocating.getHandicap()))
					.collect(Collectors.toList());
		}
		return accList;
	}

	/**
	 * 出款确认
	 * <p>
	 * 1.检测该任务是否已确认,已确认直接返回，未确认流程继续</br>
	 * 2.更改该出款任务的备注，状态，截图信息，出款失败，直接返回，出款成功，流程继续</br>
	 * 3.累计当日出款，按自然日统计</br>
	 * 4.通知平台
	 * </p>
	 *
	 * @param success
	 *            出款结果 ture:成功;出款失败
	 * @param taskId
	 *            出款任务ID
	 * @param operator
	 *            操作者（机器出款：null）
	 * @param remark
	 *            备注
	 * @param accountId
	 *            出款账号ID
	 * @param time
	 *            出款时间
	 * @param screenshot
	 *            出款操作截图（人工出款:null）
	 */
	@Transactional(rollbackFor = Exception.class)
	void ack(boolean success, Long taskId, Integer operator, String remark, Integer accountId, Date time,
			String screenshot, boolean thirdOut, String platPayCode) {
		try {
			BizOutwardTask o = outwardTaskRepository.findById2(taskId);
			// 检测是否已分配,是否已经确认
			if (o == null || (o.getAccountId() == null && o.getOperator() == null)
					|| (!Arrays
							.asList(OutwardTaskStatus.Undeposit.getStatus(), OutwardTaskStatus.ManagerDeal.getStatus())
							.contains(o.getStatus()))) {
				log.error("出款确认 未分配/重复确认 taskId：{},accountId:{},操作人:{}", taskId, accountId, operator);
				return;
			}
			String oprUid = SYS;
			if (operator != null && operator != AppConstants.USER_ID_4_ADMIN) {
				SysUser opr = userService.findFromCacheById(operator);
				if (opr == null) {
					log.error("出款确认 操作人不存在 taskId:{} operator:{}", taskId, operator);
					return;
				}
				oprUid = userService.findFromCacheById(operator).getUid();
			}
			OutwardTaskStatus status = success ? OutwardTaskStatus.Deposited : OutwardTaskStatus.ManagerDeal;
			// 备注信息
			remark = remSpecialChars(remark);
			remark = StringUtils.trimToEmpty(remark) + (success ? "出款成功" : "出款失败");
			if (thirdOut) {
				remark = addFlowChars(status, (remark + "-" + THIRD + "-" + (operator != null ? operator : THIRD)));
			} else {
				AccountBaseInfo base = accountService.getFromCacheById(accountId);
				remark = addFlowChars(status,
						(remark + "-"
								+ (base == null || StringUtils.isBlank(base.getAlias()) ? accountId : base.getAlias())
								+ "-" + (operator != null ? operator : ROBOT)));
			}
			// 保存确认信息
			o.setAccountId(accountId);
			o.setStatus(status.getStatus());
			o.setRemark(CommonUtils.genRemark(o.getRemark(), remark, time, oprUid));
			o.setTimeConsuming(((int) (time.getTime() - o.getAsignTime().getTime())) / 1000);
			if (StringUtils.isNotEmpty(screenshot)) {
				o.setScreenshot(screenshot);
			}
			// 此处不能替换为 save(o)
			outwardTaskRepository.saveAndFlush(o);
			log.info("Transfer(outAck) >>save taskId:{} accountId:{} operator:{} amount:{} status:{} success:{}",
					o.getId(), o.getAccountId(), o.getOperator(), o.getAmount(), o.getStatus(), success);
			if (!success) {
				// success ==false 转主管 自动分配任务排查
				asignFailedTaskService.asignOnTurnToFail(o.getId());
				// 如果确认转账失败，直接返回（不在累计当日出款，与通知平台）
				return;
			}
			// if (!thirdOut) {
			// // 2. 累计当日出款
			// AccountBaseInfo base = accountService.getFromCacheById(o.getAccountId());
			// if (Objects.nonNull(base) && !base.checkMobile()) {
			// RedisTemplate<String, String> template =
			// redisService.getFloatRedisTemplate();
			// redisService.increment(RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD,
			// String.valueOf(accountId),
			// o.getAmount().floatValue());
			// Long expire =
			// template.boundHashOps(RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD).getExpire();
			// if (expire != null && expire < 0) {
			// expire = CommonUtils.getExpireTime4AmountDaily() -
			// System.currentTimeMillis();
			// // 当日清零
			// template.expire(RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD, expire,
			// TimeUnit.MILLISECONDS);
			// }
			// }
			// }
			BizOutwardRequest req = outwardRequestService.get(o.getOutwardRequestId());
			log.debug("第三方代付的任务:{},platPayCode:{}通知平台", ObjectMapperUtils.serialize(o), platPayCode);
			if (null != o.getThirdInsteadPay() && o.getThirdInsteadPay() == 1 && StringUtils.isNotBlank(platPayCode)) {
				callPlatForDaifuSuccessOut(o, req, platPayCode);
			} else {
				// 3. 通知平台
				log.debug("非第三方代付的任务:{},通知平台", o);
				noticePlatIfFinished(operator, req);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 前端输入信息特殊字符处理
	 * <p>
	 * 特殊字符全部由空串代替
	 * </p>
	 *
	 * @param remark
	 *            前端输入信息
	 * @return 过滤后的信息
	 */
	private String remSpecialChars(String remark) {
		if (remark == null) {
			return StringUtils.EMPTY;
		}
		return remark.replace(LOCK_REQ_SUCCESS, StringUtils.EMPTY).replace(LOCK_REQ_FAIL, StringUtils.EMPTY)
				.replace(RE_ALLOCATE, StringUtils.EMPTY).replace(FIN_ACKED, StringUtils.EMPTY);
	}

	/**
	 * 备注信息添加流程说明
	 * <p>
	 * 返回信息格式： 备注信息（转+流程信息）
	 * </p>
	 *
	 * @param next
	 *            即将进入的流程，若无则为:null
	 * @param remark
	 *            备注信息
	 * @return 含流程说明的备注信息
	 */
	private String addFlowChars(OutwardTaskStatus next, String remark) {
		if (next == null) {
			return remark;
		}
		return StringUtils.trimToEmpty(remark) + "(转" + next.getMsg() + ")";
	}

	/**
	 * 根据当前时间生产一个分值（八位整数）
	 * <p>
	 * 根据当前时间的毫秒数的后八位生成一个<tt>Double</tt>分值
	 * </p>
	 *
	 * @return score
	 */
	private double score4zset() {
		String t = String.valueOf(System.currentTimeMillis());
		t = t.substring(t.length() - 8);
		return Double.parseDouble(t);
	}

	/**
	 * 根据内层/外层；被分配对象类型；盘口；金额生成一个分值
	 * <p>
	 * 分值格式：1200000.12345678
	 * </p>
	 * <p>
	 * 整数部分:共七位，从左往右</br>
	 * 第一位：内层/外层；</br>
	 * 第二位：人工出款/机器出款;/第三方出款</br>
	 * 第三，四位：盘口所在区域</br>
	 * 第五，六，七:盘口ID [001~999],公司盘口最大数量为999个</br>
	 * </p>
	 * <p>
	 * 小数部分：共8位，金额的整数部分
	 * </p>
	 *
	 * @param l
	 *            内层/外层/中层
	 * @param target
	 *            被分配对象类型 TARGET_TYPE_USER TARGET_TYPE_ROBOT TARGET_TYPE_START
	 * @param zone
	 *            区域
	 * @param handicapId
	 *            盘口ID
	 * @param amount
	 *            金额
	 * @return 分值
	 * @see com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel#Outter
	 *      #getValue();
	 * @see com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel#Inner
	 *      #getValue();
	 * @see com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel#Middle
	 *      #getValue();
	 */
	private Double score4zset(int l, int target, int zone, int handicapId, BigDecimal amount) {
		amount = Objects.isNull(amount) ? BigDecimal.ZERO : amount;
		return Double.valueOf(String.format("%d%d%02d%03d.%08d", l, target, zone, handicapId,
				amount.setScale(0, BigDecimal.ROUND_DOWN).intValue()));
	}

	/**
	 * 解析分值
	 * <p>
	 * 参数<code>score</code>必须由函数</br>
	 * <code>this#score4zset(int l, int target, int handicapId, BigDecimal amount)</code>
	 * 生成
	 * </p>
	 *
	 * @param score
	 *            分值
	 * @return int[0] 内层/外层/中层 l</br>
	 *         int[1] tarType</br>
	 *         int[2] 区域</br>
	 *         int[3] handicapId</br>
	 *         int[4] amount</br>
	 */
	private int[] descore(double score) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumIntegerDigits(7);
		nf.setMinimumIntegerDigits(7);
		nf.setMaximumFractionDigits(8);
		nf.setMinimumFractionDigits(8);
		nf.setGroupingUsed(false);
		String sc = nf.format(score);
		return new int[] { Integer.parseInt(sc.substring(0, 1)), Integer.parseInt(sc.substring(1, 2)),
				Integer.parseInt(sc.substring(2, 4)), Integer.parseInt(sc.substring(4, 7)),
				Integer.parseInt(sc.substring(8, 16)) };
	}

	/**
	 * 解析分值
	 * <p>
	 * 参数<code>score</code>必须由函数</br>
	 * <code>this#score4zset(int l, int target, int handicapId, BigDecimal amount)</code>
	 * 生成
	 * </p>
	 *
	 * @return int[0] tarVal</br>
	 *         int[1] 内层/外层/中层 l</br>
	 *         int[2] tarType</br>
	 *         int[3] 区域</br>
	 *         int[4] handicapId</br>
	 *         int[5] amount</br>
	 */
	private int[] descore(int tarType, int tarVal, double score) {
		int[] infs = descore(score);
		int[] des = Arrays.copyOf(new int[] { tarVal }, infs.length + 1);
		System.arraycopy(infs, 0, des, 1, infs.length);
		des[2] = tarType;
		return des;
	}

	/**
	 * 解析入款卡分值
	 * <p>
	 * 参数<code>score</code>必须由函数</br>
	 * <code>allocateTransService#enScore4Fr(int type, int zone, int l, int handi, int bal)</code>
	 * 生成
	 * </p>
	 *
	 * @return int[0] tarVal</br>
	 *         int[1] 内层/外层/中层 l</br>
	 *         int[2] tarType</br>
	 *         int[3] 区域</br>
	 *         int[4] handicapId</br>
	 *         int[5] amount</br>
	 */
	private int[] deFrscore(int tarType, int tarVal, double score) {
		// 0:type 1;zone 2:l 3:handi 4:bal
		Integer[] infs = allocateTransService.deScore4Fr(score);
		if (infs.length == 5) {
			int[] des = new int[6];
			des[0] = tarVal;
			des[1] = infs[2];
			des[2] = tarType;
			des[3] = infs[1];
			des[4] = infs[3];
			des[5] = infs[4];
			return des;
		}
		return null;
	}

	/**
	 * 描述:此方法用于将出款任务组装成符合{@link AllocatingOutwardTask#genMsg(int, BizOutwardRequest, BizOutwardTask, boolean, boolean, boolean)}
	 * } 格式的任务并放入redis队列(ZSetOps) 并更新该条记录的zsetops分值以便后台线程识别并分配任务到相应的卡出款
	 *
	 * @param req
	 *            出款请求
	 * @param task
	 *            出款任务
	 * @param isFirstOut
	 *            是否首次出款
	 * @param isManual
	 *            是否人工出款
	 * @param isThird
	 *            是否需要第三方出款
	 */
	private void rpush(BizOutwardRequest req, BizOutwardTask task, boolean isFirstOut, boolean isManual,
			boolean isThird) {
		// isFirstOut = false;// 屏蔽 首次出款 用 人工出款
		BizHandicap handi = handicapService.findFromCacheById(req.getHandicap());
		int zone = Objects.isNull(handi.getZone()) ? handi.getId() : handi.getZone();
		String msg = AllocatingOutwardTask.genMsg(zone, req, task, isFirstOut, isManual, isThird);
		// 任务审核时，判断盘口是否开启走新逻辑
		String handicap = req.getHandicap() == null ? StringUtils.EMPTY : req.getHandicap().toString();
		if (CommonUtils.checkDistHandicapNewVersion(Integer.valueOf(handicap))) {
			rpushnew(msg);
		} else {
			rpush(msg);
		}
	}

	/**
	 * 执行lua script:批量取出待出款任务
	 * <p>
	 * 1.从 分配队列 移动一定数量的任务 到临时队列</br>
	 * 2.返回 从分配队列 移动到 临时队列 中的 出款任务
	 * </p>
	 *
	 * @return 待分配出款任务 返回信息格式请参考@see 说明
	 * @see com.xinbo.fundstransfer.domain.pojo.AllocatingOutwardTask#genMsg(int
	 *      zone, BizOutwardRequest r, BizOutwardTask t, boolean firstout, boolean
	 *      manualout, boolean thirdout)
	 */
	private String lpop() {
		String ret = redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_POP";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_POP;
			}
		}, null, NUM_LPOP, RedisKeys.ALLOCATING_OUTWARD_TASK_CAL, RedisKeys.ALLOCATING_OUTWARD_TASK_TMP);
		return StringUtils.trimToNull(ret);
	}

	/**
	 * 执行lua script:返回未分配的出款任务
	 * <p>
	 * 1.已分配任务 从 临时队列中 删除</br>
	 * 2.未分配任务 从 临时队列中 移动到 待分配队列
	 * </p>
	 *
	 * @param allocated
	 *            已分配任务
	 * @param allocating
	 *            未分配任务
	 * @param target
	 *            被分配对象，当allocated为空，则：target为空；当当allocated不为空，则：target不为空
	 */
	private void lback(String[] allocated, String[] allocating, String target) {
		if ((allocated == null || allocated.length == 0) && (allocating == null || allocating.length == 0)) {
			return;
		}
		String ated = allocated == null || allocated.length == 0 ? StringUtils.EMPTY : StringUtils.join(allocated, ";");
		String ating = allocating == null || allocating.length == 0 ? StringUtils.EMPTY
				: StringUtils.join(allocating, ";");
		String tar = target == null ? "" : target;
		redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_BACK";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_BACK;
			}
		}, null, RedisKeys.ALLOCATING_OUTWARD_TASK, RedisKeys.ALLOCATING_OUTWARD_TASK_TMP,
				RedisKeys.ALLOCATING_OUTWARD_TARGET, ated, ating, tar);
	}

	/**
	 * 提交本次交易信息
	 *
	 * @param trans
	 *            交易信息
	 */
	private void lack(TransferEntity trans) {
		if (trans == null || trans.getFromAccountId() == null || trans.getAccount() == null
				|| trans.getAmount() == null) {
			return;
		}
		String frAcc = String.valueOf(trans.getFromAccountId());
		String inf = (System.currentTimeMillis() + EIGHT_HOURS_MILIS) + ":" + frAcc + ":" + trans.getAccount() + ":"
				+ trans.getAmount().intValue();
		redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_LAST";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_LAST;
			}
		}, null, String.valueOf(System.currentTimeMillis()), frAcc, inf, RedisKeys.ALLOC_OTASK_LAST);
	}

	private Integer lorder() {
		String ret = redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_ORDER";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_ORDER;
			}
		}, null, RedisKeys.ALLOCATING_OUTWARD_TASK, RedisKeys.ALLOCATING_OUTWARD_TASK_CAL);
		return Integer.valueOf(ret);
	}

	/**
	 * 执行lua script:系统初始化
	 * <p>
	 * 把临时队列 中的待分配任务 移动到 待分配队列
	 * </p>
	 */
	@PostConstruct
	private void linit() {
		redisService.getStringRedisTemplate().delete("AllocMergeLevel");
		redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_INIT";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_INIT;
			}
		}, null, RedisKeys.ALLOCATING_OUTWARD_TASK, RedisKeys.ALLOCATING_OUTWARD_TASK_TMP);
	}

	/**
	 * 检查是否存在流水
	 */
	@Override
	public Long checkBankLog(long taskId) {
		BizOutwardTask task = outwardTaskRepository.findById2(taskId);
		BizOutwardRequest req = outwardRequestService.get(task.getOutwardRequestId());
		return findMatchingBankLogId(task.getAccountId(), req.getToAccount(), req.getToAccountOwner(),
				task.getAmount());
	}

	/**
	 * 检测 第三方出款用户
	 */
	@Override
	public boolean checkThird(int userId) {
		SysUser user = userService.findFromCacheById(userId);
		return AppConstants.OUTDREW_THIRD && user != null && user.getCategory() != null
				&& UserCategory.Finance.getCode() == user.getCategory() && userService.online(userId)
				&& SUSPEND_OPERATOR.getIfPresent(userId) == null;
	}

	@Override
	public boolean checkManual(BizOutwardRequest req, Float taskAmt) {
		boolean isManual = !checkRobotLimit(taskAmt);
		if (!isManual) {
			isManual = outwardTaskService.checkManualOut4Member(req.getToAccount());
		}
		return isManual;
	}

	/***
	 *
	 * @return true:fr->to 可以转账;false:不可以转账
	 */
	@Override
	public boolean checkPeer(Boolean frPeer, AccountBaseInfo fr, AccountBaseInfo to) {
		if (frPeer != null) {
			Boolean t = checkPeerTrans(to.getBankType());
			if (t != null && !t) {
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.TRANS_AMT)
						.remove(String.valueOf(to.getId()));
				return false;
			}
			boolean trans = checkTrans(frPeer, fr.getBankType(), to.getBankType());
			if (!trans) {
				return false;
			}
		}
		return true;
	}

	private boolean checkNonExpire(Set<Integer> ids, Integer target) {
		if (Objects.isNull(ids) || Objects.isNull(target))
			return true;
		return !ids.contains(target);
	}

	/**
	 * 将task封装成TransferEntity信息 TODO 目前只封装了分配给机器的返利任务，分配人工的暂时没有修改，后续开放分配人工接口时，再做相应修改
	 *
	 * @param accountId
	 * @return
	 */
	private TransferEntity buildTransEntity(int accountId, boolean mobile, BigDecimal bankBalance) {
		BizOutwardTask task = outwardTaskRepository.applyTask4Robot(accountId, OutwardTaskStatus.Undeposit.getStatus());
		TransferEntity ret = null;
		if (task != null) {
			// 封装 返回信息
			BizOutwardRequest req = outwardRequestService.get(task.getOutwardRequestId());
			ret = new TransferEntity();
			ret.setFromAccountId(accountId);
			ret.setAccount(StringUtils.trimToNull(req.getToAccount()));
			ret.setTaskId(task.getId());
			ret.setAmount(task.getAmount().floatValue());
			ret.setOwner(StringUtils.trimToNull(req.getToAccountOwner()));
			ret.setBankType(StringUtils.trimToNull(req.getToAccountBank()));// Type对应的是ToAccountBank、BankAddr对应的是ToAccountName
			ret.setBankAddr(StringUtils.trimToNull(req.getToAccountName()));
			if (Objects.nonNull(task.getAsignTime())) {
				ret.setAcquireTime(task.getAsignTime().getTime());
			} else {
				ret.setAcquireTime(System.currentTimeMillis());
			}
			systemAccountManager.regist(ret, bankBalance);
			return ret;
		}
		BizAccountRebate rebate = accountRebateRepository.applyTask4Robot(accountId,
				OutwardTaskStatus.Undeposit.getStatus());
		if (rebate != null) {
			ret = new TransferEntity();
			ret.setFromAccountId(accountId);
			ret.setAccount(StringUtils.trimToNull(rebate.getToAccount()));
			ret.setTaskId(rebate.getId());
			ret.setAmount(rebate.getAmount().floatValue());
			ret.setOwner(StringUtils.trimToNull(rebate.getToHolder()));
			ret.setBankType(StringUtils.trimToNull(rebate.getToAccountType()));// Type对应的是ToAccountBank、BankAddr对应的是ToAccountName
			ret.setBankAddr(StringUtils.trimToNull(rebate.getToAccountInfo()));
			if (Objects.nonNull(rebate.getAsignTime())) {
				ret.setAcquireTime(rebate.getAsignTime().getTime());
			} else {
				ret.setAcquireTime(System.currentTimeMillis());
			}
			ret.setRemark("返利任务");
			systemAccountManager.regist(ret, bankBalance);
		}
		if (ret == null) {
			ret = allocateTransService.applyTrans(accountId, false, null, false);
		}
		return ret;
	}

	private String buildAllocateStrategy() {
		return MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTWARD_TASK_ALLOCATE_STRATEGY", "0");
	}

	private boolean checkAllocateStrategy(String allocStrategy, int tarType) {
		/**
		 * 1、OUTTASK_ALLOCATE_AMT_FIRST PC和手机都可以出款<br>
		 * 2、OUTTASK_ALLOCATE_PC_FIRST PC和手机都可以出款<br>
		 * 3、OUTTASK_ALLOCATE_MOBILE_FIRSTPC和手机都可以出款<br>
		 * 4、OUTTASK_ALLOCATE_ONLY_PC 仅PC可以出款<br>
		 * 5、OUTTASK_ALLOCATE_ONLY_MOBILE 仅手机可以出款<br>
		 */
		return ((AppConstants.OUTTASK_ALLOCATE_AMT_FIRST.equals(allocStrategy)
				&& (TARGET_TYPE_ROBOT == tarType || TARGET_TYPE_MOBILE == tarType))
				|| ((AppConstants.OUTTASK_ALLOCATE_PC_FIRST.equals(allocStrategy)
						|| AppConstants.OUTTASK_ALLOCATE_MOBILE_FIRST.equals(allocStrategy))
						&& (TARGET_TYPE_ROBOT == tarType || TARGET_TYPE_MOBILE == tarType))
				|| (AppConstants.OUTTASK_ALLOCATE_ONLY_PC.equals(allocStrategy) && TARGET_TYPE_ROBOT == tarType)
				|| (AppConstants.OUTTASK_ALLOCATE_ONLY_MOBILE.equals(allocStrategy) && TARGET_TYPE_MOBILE == tarType));
	}

	private List<int[]> sortForFilter(List<int[]> result, float taskAmt, String allocStrategy, boolean over) {
		boolean isAmtFirst = AppConstants.OUTTASK_ALLOCATE_AMT_FIRST.equals(allocStrategy)
				|| AppConstants.OUTTASK_ALLOCATE_ONLY_PC.equals(allocStrategy)
				|| AppConstants.OUTTASK_ALLOCATE_ONLY_MOBILE.equals(allocStrategy);
		boolean asc = over && taskAmt < 2000 || !over;
		boolean isMobileFirst = !isAmtFirst && AppConstants.OUTTASK_ALLOCATE_MOBILE_FIRST.equals(allocStrategy);
		result.sort((o1, o2) -> {
			if (isAmtFirst) {
				return asc ? (o1[5] - o2[5]) : (o2[5] - o1[5]);
			} else {
				if (isMobileFirst) {
					if (o1[2] != o2[2])
						return o2[2] - o1[2];
					else
						return asc ? (o1[5] - o2[5]) : (o2[5] - o1[5]);
				} else {
					if (o1[2] != o2[2])
						return o1[2] - o2[2];
					else
						return asc ? (o1[5] - o2[5]) : (o2[5] - o1[5]);
				}
			}
		});// 按照分值升序序排序
		return result;
	}

	/**
	 * 移除已经分配出去的数据
	 *
	 * @param atedId
	 *            已经分配出去的任务
	 * @param addOrRemove
	 *            添加或移除
	 */
	public void addOrRemoveAllocated(Integer atedId, boolean addOrRemove) {
		if (addOrRemove)
			atedTarget.put(atedId, atedId);
		else
			atedTarget.invalidate(atedId);
	}

	/**
	 * 移除lock中已经存在的数据
	 *
	 * @param getAll
	 */
	private void removeExitLock(Set<ZSetOperations.TypedTuple<String>> getAll) {
		if (Objects.nonNull(getAll)) {
			Iterator<ZSetOperations.TypedTuple<String>> itReGet = getAll.iterator();
			// 删除备用卡中已经有转出的卡的数据
			while (itReGet.hasNext()) {
				ZSetOperations.TypedTuple<String> val = itReGet.next();
				String value = val.getValue();
				TransLock lock = allocateTransService.buildLock(false, Integer.parseInt(value));
				if (lock != null) {
					itReGet.remove();
				}
			}
		}
	}

	/**
	 * 获取余额告警值
	 *
	 * @param base
	 * @return
	 */
	public int buildLimitBalance(AccountBaseInfo base) {
		int minBal = CommonUtils.getReserveTOReserveMinAmount();
		if (Objects.isNull(base) || Objects.isNull(base.getLimitBalance())) {
			return minBal;
		}
		return base.getLimitBalance();
	}

	/**
	 * 将超时的出款任务转出去
	 */
	public void turnExpireOutwardTask() throws InterruptedException {
		if (!incomeAccountAllocateService.checkHostRunRight()) {
			log.trace("AllocOTask >> the host has no right to allocate. {}", CommonUtils.getInternalIp());
			Thread.sleep(5000L);
			return;
		}
		long currTime = System.currentTimeMillis() - 420000;
		Date curDate = new Date(currTime);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currTimeStr = format.format(curDate);
		// 甩出给会员出款任务
		List<BizOutwardTask> taskList = outwardTaskRepository.getExpireOutwardTask(currTimeStr);
		SysUser operator = userService.findFromCacheById(AppConstants.USER_ID_4_ADMIN);
		Set<String> lastKeys = redisService.getStringRedisTemplate().boundSetOps(RedisKeys.TASK_REPORT_LAST_STEP)
				.members();
		if (!CollectionUtils.isEmpty(taskList)) {
			for (BizOutwardTask task : taskList) {
				try {
					if (lastKeys != null && lastKeys.contains(task.getId() + "")) {
						alterStatusToMgr(task, null, "超过7分钟，并且完成最后一步，转主管", null);
						log.info("turnExpireOutwardTask>>转主管处理,taskid {} accid {}", task.getId(), task.getAccountId());
					} else {
						remark4Mgr(task.getId(), false, false, operator, null,
								StringUtils.trimToEmpty(task.getRemark()) + "超过7分钟,无结果机器转出");
						log.info("turnExpireOutwardTask>>重新分配,taskid {} accid {}", task.getId(), task.getAccountId());
					}
				} catch (Exception e) {
					log.error("turnExpireOutwardTask>>task turn out error,taskid {} accid {}", task.getId(),
							task.getAccountId());
				}
			}
		}
		// 甩出返利任务
		List<BizAccountRebate> rebateList = accountRebateRepository.getExpireRebateTask(currTimeStr);
		if (!CollectionUtils.isEmpty(rebateList)) {
			for (BizAccountRebate task : rebateList) {
				try {
					if (lastKeys != null && lastKeys.contains(task.getId() + "")) {
						accRebateDao.updRemarkOrStatus(task.getId(), "超过15分钟，并且完成最后一步，转待排查",
								OutwardTaskStatus.Failure.getStatus(), task.getStatus(), null);
						log.info("turnExpireOutwardTask>>待排查,taskid {} accid {}", task.getId(), task.getAccountId());
					} else {
						accountRebateService.reAssignDrawing(operator, task.getId(),
								StringUtils.trimToEmpty(task.getRemark()) + "超过15分钟,无结果机器转出");
						log.info("turnExpireOutwardTask>>重新分配,taskid {} accid {}", task.getId(), task.getAccountId());
					}
				} catch (Exception e) {
					log.error("turnExpireOutwardTask>>rebate task turn out error,taskid {} accid {}", task.getId(),
							task.getAccountId());
				}
			}
		}
	}

	public boolean isManualTask(BizOutwardRequest req, Float taskAmt) {
		return checkFirst(req.getReview()) || checkManual(req, taskAmt)
				|| CommonUtils.checkOutWardBankTypeKeywordsFilter(req.getToAccountBank());
	}

	private boolean checkTask(BizOutwardTask outTask, BizAccountRebate rebateTask) {
		Integer accId = Objects.nonNull(outTask) ? outTask.getAccountId() : rebateTask.getAccountId();
		AccountBaseInfo base = accountService.getFromCacheById(accId);
		// 返利网未出款超时的任务，转出
		if (Objects.isNull(base) || !Objects.equals(base.getFlag(), AccountFlag.REFUND.getTypeId())) {
			log.debug("turnExpireOutwardTask>>acc is null or is not refund acc,accid {}", accId);
			return false;
		}
		return true;
	}

	private Set<Integer> buildIdByNonExprie() {
		String rateLimitStr = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTDRAW_RATE_LIMIT", "0");
		int rateLimit = StringUtils.isNumeric(rateLimitStr) ? Integer.parseInt(rateLimitStr) : 0;
		if (rateLimit <= 0)
			return Collections.EMPTY_SET;
		rateLimit = (rateLimit > 3600 ? 3600 : rateLimit) * 1000;
		Object obj = redisService.getStringRedisTemplate().opsForHash().entries(RedisKeys.LAST_TIME_OUTWARD);
		Map<String, String> data = (Map<String, String>) obj;
		if (CollectionUtils.isEmpty(data))
			return Collections.EMPTY_SET;
		Set<Integer> ret = new HashSet<>();
		long curr = System.currentTimeMillis();
		Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> item = it.next();
			String key = item.getKey(), value = item.getValue();
			if (Objects.nonNull(key) && Objects.nonNull(value)) {
				if (curr < (Long.parseLong(value) + rateLimit)) {
					ret.add(Integer.valueOf(key));
					it.remove();
				}
			}
		}
		if (!CollectionUtils.isEmpty(data))
			redisService.getStringRedisTemplate().opsForHash().delete(RedisKeys.LAST_TIME_OUTWARD,
					data.keySet().toArray());
		return ret;
	}

	@Override
	public void reAssignDrawingTask(BizOutwardTask task, boolean manner, String[] targetBankType, SysUser sysUser,
			String remark) {
		remark = StringUtils.trimToEmpty(remark) + "(重新分配,原出款账号" + task.getAccountId() + ")";
		remark = CommonUtils.genRemark(task.getRemark(), remark, new Date(), sysUser.getUid());
		task.updProperties(task.getId(), task.getAmount(), null, null, null, remark,
				OutwardTaskStatus.Undeposit.getStatus());
		outwardTaskService.save(task);
		rpush(outwardRequestService.get(task.getOutwardRequestId()), task, false, manner, false);
		TransLock lock = allocateTransService.buildLockToId(false, task.getId().intValue());
		if (lock != null && lock.getFrId() != null) {
			allocateTransService.llockUpdStatus(lock.getFrId(), task.getId().intValue(), TransLock.STATUS_DEL);
		}
		saveBankTypeForTaskToUse(targetBankType, task.getId());
	}

	private boolean checkUserDataPermission(String userId, Integer handicap) {
		if (StringUtils.isBlank(userId)) {
			return false;
		}
		String userHandicap = sysDataPermissionService.findUserHandicapFromCache(Integer.parseInt(userId));
		log.debug("Alloc checkUserData userId {} handicap {} data right {}", userId, handicap, userHandicap);
		return userHandicap.contains(";" + handicap + ";");
	}

	private void rpush(String msg) {
		log.info("出款任务 {} 放入队列.", msg);
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		// 1.把出款任务放到分配队列
		template.boundListOps(RedisKeys.ALLOCATING_OUTWARD_TASK).rightPush(msg);
		// 2.改变分配任务分值（启动分配任务）
		template.boundZSetOps(RedisKeys.ALLOCATING_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE, score4zset());
	}

	private void rpushnew(String msg) {
		log.info("出款任务 {} 放入队列.", msg);
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		// 1.把出款任务放到分配队列
		template.boundListOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TASK).rightPush(msg);
		// 2.改变分配任务分值（启动分配任务）
		template.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).add(DUPLICATE_ALLOCATE, score4zset());
	}

	/**
	 * 入款卡出款银行校验，非工行入款卡通过校验，工行只出部分银行的出款任务
	 *
	 * @param task
	 * @param base
	 * @return
	 */
	private boolean checkBank(AllocatingOutwardTask task, AccountBaseInfo base) {
		String inbank = base.getBankType();
		if (!"工商银行".equals(inbank)) {
			return true;
		}
		String taskbank = task.getToAccountBank();
		if (taskbank.contains("中国银行") || taskbank.contains("农业银行") || taskbank.contains("工商银行")
				|| taskbank.contains("建设银行") || taskbank.contains("交通银行") || taskbank.contains("中信银行")
				|| taskbank.contains("光大银行") || taskbank.contains("华夏银行") || taskbank.contains("民生银行")
				|| taskbank.contains("广发银行") || taskbank.contains("招商银行") || taskbank.contains("平安银行")
				|| taskbank.contains("兴业银行") || taskbank.contains("浦发银行") || taskbank.contains("邮政"))
			return true;
		else
			return false;
	}

	private Set<Integer> buildAllUnMatched() {
		String sql = String.format(
				"select case when operator is null then account_id else operator end from biz_outward_task where status in (0,1,2) and account_id is not null and asign_time>='%s' group by case when operator is null then account_id else operator end having count(1) >5",
				CommonUtils.getStartTimeOfCurrDay());
		List<Object> result = entityManager.createNativeQuery(sql).getResultList();
		if (!CollectionUtils.isEmpty(result)) {
			Set<Integer> unmatched = new HashSet<>();
			for (Object obj : result) {
				unmatched.add(Integer.parseInt(obj.toString()));
			}
			return unmatched;
		}
		return null;
	}

	/**
	 * 获取任务分配所需的缓存信息
	 *
	 * @param key
	 * @return
	 */
	@Override
	public Object getAllocNeetCache(String key) {
		Object o = allocNeedCache.getIfPresent(key);
		if (o != null) {
			return o;
		}
		if ("USERACCMAP".equals(key)) {
			Map<Integer, List<BizAccount>> userAccMap = findOutAccList4Manual();
			if (!CollectionUtils.isEmpty(userAccMap)) {
				allocNeedCache.put("USERACCMAP", userAccMap);
				return userAccMap;
			}
		} else if ("LAST".equals(key)) {
			Map<Object, Object> last = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ALLOC_OTASK_LAST)
					.entries();
			if (!CollectionUtils.isEmpty(last)) {
				allocNeedCache.put("LAST", last);
				return last;
			}
		} else if ("BLACK".equals(key)) {
			Set<String> black = allocTransSer.findBlackList();
			if (!CollectionUtils.isEmpty(black)) {
				allocNeedCache.put("BLACK", black);
				return black;
			}
		}
		return null;
	}
}