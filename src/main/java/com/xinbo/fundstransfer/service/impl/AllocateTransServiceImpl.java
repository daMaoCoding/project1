package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.assign.OtherCache;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.enums.UserCategory;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.newinaccount.service.InAccountService;
import com.xinbo.fundstransfer.newpay.inputdto.SyncBankBalanceInputDTO;
import com.xinbo.fundstransfer.newpay.service.NewPayService;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("WeakerAccess unused")
public class AllocateTransServiceImpl implements AllocateTransService {
	private static final Logger log = LoggerFactory.getLogger(AllocateTransServiceImpl.class);
	@Autowired
	@Qualifier("updateAccountRealBalanceScript")
	private RedisScript<String> updateAccountRealBalanceScript;
	@Value("${funds.transfer.version}")
	private String CURR_VERSION;
	@Autowired
	private AccountRepository accDao;
	@Autowired
	private OutwardTaskRepository oTaskDao;
	@Autowired
	private OutwardRequestRepository oReqDao;
	@Autowired
	@Lazy
	private AccountService accSer;
	@Autowired
	private HandicapService handiSer;
	@Autowired
	private RedisService redisSer;
	@Autowired
	@Lazy
	private IncomeRequestService inReqSer;
	@Autowired
	@Lazy
	private AllocateIncomeAccountService alloIAcntSer;
	@Autowired
	@Lazy
	private AllocateOutwardTaskService allOTaskSer;
	@Autowired
	@Lazy
	private AllocateTransferService oldTransSer;
	@Autowired
	@Lazy
	private TransMonitorService transMonitorService;
	@PersistenceContext
	private EntityManager entityMgr;
	@Autowired
	@Lazy
	private NewPayService newPayService;
	@Autowired
	@Lazy
	private TransactionLogService transactionService;
	@Autowired
	@Lazy
	private AccountChangeService accountChangeService;
	@Autowired
	@Lazy
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	@Lazy
	private CabanaService cabanaService;
	@Autowired
	@Lazy
	private SystemAccountManager systemAccountManager;
	@Autowired
	@Lazy
	private OtherCache otherCache;
	@Autowired
	@Lazy
	private InAccountService inAccountService;
	@Autowired
	@Lazy
	private SysUserService userService;
	private ObjectMapper mapper = new ObjectMapper();
	/**
	 * 常量：</br>
	 * ###出款卡:OUTBANK</br>
	 * ###入款卡:INBANK</br>
	 * ###入款第三方:INTHIRD</br>
	 * ###入款微信:INWECHAT</br>
	 * ###入款支付宝:INALI</br>
	 * ###支付宝体现卡:BINDALI</br>
	 * ###微信体现卡:BINDWECHAT</br>
	 * ###第三方体现卡:THIRDCOMMON</br>
	 * ###公共体现卡:BINDCOMMON</br>
	 * ###备用卡:RESERVEBANK</br>
	 * ###入款账号客户绑定卡:RESERVEBANK</br>
	 */
	private static final int OUTBANK = AccountType.OutBank.getTypeId();
	private static final int INBANK = AccountType.InBank.getTypeId();
	private static final int INTHIRD = AccountType.InThird.getTypeId();
	private static final int INWECHAT = AccountType.InWechat.getTypeId();
	private static final int INALI = AccountType.InAli.getTypeId();
	private static final int BINDALI = AccountType.BindAli.getTypeId();
	private static final int BINDCOMMON = AccountType.BindCommon.getTypeId();
	private static final int BINDWECHAT = AccountType.BindWechat.getTypeId();
	private static final int THIRDCOMMON = AccountType.ThirdCommon.getTypeId();
	private static final int RESERVEBANK = AccountType.ReserveBank.getTypeId();
	private static final int BINDCUSTOMER = AccountType.BindCustomer.getTypeId();
	/**
	 * 最大误差 MAX_TOLERANCE(生产环境，测试环境：BigDecimal.TEN)</br>
	 */
	private static int MAX_TOLERANCE = 500;

	/**
	 * 常量：</br>
	 * 单笔转账最大金额 TRANS_MAX_PER</br>
	 */
	private static final int TRANS_MAX_PER = 49000;

	private static final int O_SCR_N_MONY_Y_TASK_N_BAL = 2;
	private static final int O_SCR_MANUAL_N_MONY = 3;
	private static final int O_SCR_N_MONY_Y_TASK = 4;
	private static final int O_SCR_N_MONY_N_TASK = 5;

	private static final int Outter = CurrentSystemLevel.Outter.getValue();

	private static final int Inner = CurrentSystemLevel.Inner.getValue();

	private static final int NORMAL = AccountStatus.Normal.getStatus();

	private static final int UNDEPOSIT = OutwardTaskStatus.Undeposit.getStatus();

	private static String LAST_ALLOC_TRANS_CNST = StringUtils.SPACE;

	// 下发锁定过期时间，毫秒
	private static final int TRANS_ACCOUNT_LOCK_EXPIRE = 300000;
	// 人工卡下发锁定过期时间，毫秒
	private static final int TRANS_ACCOUNT_LOCK_EXPIRE_MANUL = 180000;
	// 测试转账激活锁定过期时间，毫秒
	private static final int TEST_TRANS_ACCOUNT_LOCK_EXPIRE = 300000;

	private volatile static boolean ALLOC_TRANS_DUPLICATE = false;
	private volatile static boolean ALLOC_NEW_TRANS_DUPLICATE = false;
	private static final String ALLOC_TRANS_CNST = "AllocTransCnst";
	private static final String ALLOC_NEW_TRANS_CNST = "AllocNewTransCnst";

	private static final Cache<Integer, BigDecimal> ACC_AMT = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(90, TimeUnit.SECONDS).build();
	private static final Cache<Integer, BigDecimal> ACC_NEW_AMT = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(90, TimeUnit.SECONDS).build();

	/**
	 * 配置：上次无效数据清理时间；清理周期</br>
	 * ###CLEAN_INVLD_DATA_LAST_TM 上次清理时间 </br>
	 * ###CLEAN_INVLD_DATA_INTR 清理周期
	 */
	private static volatile long CLEAN_INVLD_DATA_LAST_TM = 0L;
	private static final long CLEAN_INVLD_DATA_INTR = 30000;
	/**
	 * CLEAN_INVLD_DATA_ONETIME_LAST_TM 一天只清理一次，清理时间
	 */
	private static volatile long CLEAN_INVLD_DATA_ONETIME_LAST_TM = 0L;
	/**
	 * 24小时：毫秒数
	 */
	private final static Long ONE_DAY_TIMESTAMP = 86400000L;
	/**
	 * 配置：出款账号下发一键配置 </br>
	 * UP_LIM_UP_LIM 金额上限</br>
	 * UP_LIM_DUR_TM 持续时间</br>
	 * UP_LIM_ED_TM 结束时间</br>
	 */
	private static final String UP_LIM_UP_LIM = "UPLIM", UP_LIM_TRIG_LIM = "TRIGLIM", UP_LIM_DUR_TM = "DUR_TM",
			UP_LIM_ED_TM = "EDTM";
	private static final ConcurrentLinkedQueue<Integer> ACC_QUENE = new ConcurrentLinkedQueue<>();
	private static final ConcurrentLinkedQueue<Integer> ACC_NEW_QUENE = new ConcurrentLinkedQueue<>();
	private volatile static Thread THREAD_ACC_REAL_AMT = null;
	private volatile static Thread THREAD_ACC_NEW_REAL_AMT = null;

	/**
	 * 常量：锁定时间</br>
	 * 分配锁定时间:LOCK_ROBOT_ALLOC_SECONDS</br>
	 * 认领锁定时间:LOCK_ROBOT_CLAIM_SECONDS</br>
	 * 人工锁定时间:LOCK_MANUAL_SECONDS</br>
	 */
	private static final int LOCK_ROBOT_ALLOC_SECONDS = 65, LOCK_ROBOT_CLAIM_SECONDS = 300,
			LOCK_ROBOT_ACK_SECONDS = 1200, LOCK_ROBOT_DEL_SECONDS = 1800, LOCK_MANUAL_SECONDS = 1800;

	private static final int TARGET_TYPE_ROBOT = 1, TARGET_TYPE_MOBILE = 4;

	/**
	 * lua script:转账 锁定</br>
	 * 下发锁定Redis Key format: prefix:fromId:toId:operatorId:transRadix:transInt<br/>
	 * 机器锁定业务逻辑：</br>
	 * 1.获取toId锁定记录</br>
	 * 2.检测是否已锁定 （A.operatorId 不同，说明：人工已锁定，返回；B.fromId不同，说明：其他机器已锁定，返回）</br>
	 * 3.通过2后，删除fromId锁定记录</br>
	 * 4.计算本次转账锁定，转账小数位</br>
	 * 5.计算锁定时间</br>
	 * 6.组装锁定信息 prefix:fromId:toId:operatorId:transRadix:transInt</br>
	 * 7.保存锁定信息</br>
	 * 人工下发锁定业务逻辑</br>
	 * 1.获取toId锁定记录</br>
	 * 2.检测是否已锁定(operatorId 不同,说明：已锁定 返回)</br>
	 * 3.通过2后，删除operatorId锁定记录</br>
	 * 4.计算本次转账锁定，转账小数位</br>
	 * 5.计算锁定时间</br>
	 * 6.组装锁定信息 prefix:fromId:toId:operatorId:transRadix:transInt</br>
	 * </br>
	 * 7.保存锁定信息</br>
	 */
	private static final String LUA_SCRIPT_ALLOC_TRANS_LOCK = "local fromId = ARGV[1];\n" + "local toId = ARGV[2];\n"
			+ "local opr = ARGV[3];\n" + "local transInt = ARGV[4];\n" + "local robot = ARGV[5];\n"
			+ "local timeRobot = ARGV[6];\n" + "local timeManual = ARGV[7];\n" + "local ptnFromId = ARGV[8];\n"
			+ "local ptnToId = ARGV[9];\n" + "local ptnOpr = ARGV[10];\n" + "local keyRadix = ARGV[11];\n"
			+ "local keyPtn = ARGV[12];\n" + "local dNow = ARGV[13];\n" + "local stAlc = ARGV[14];\n"
			+ "local stAck = ARGV[15];\n" + "local stDel = ARGV[16];\n " + "local thirdToOutcard=ARGV[17];\n"
			+ "local isRobot = opr ~= nil and opr == robot;\n" + "local keysToId = redis.call('keys',ptnToId);\n"
			+ "if keysToId ~= nil and next(keysToId) ~= nil then\n" + " for i0,v0 in pairs(keysToId) do\n"
			+ "  local inf = {};\n" + "  string.gsub(v0, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "  local op_ = table.concat(inf, ':', 4, 4);\n" + "  local st_ = table.concat(inf, ':', 8, 8);\n"
			+ "  if st_ ~= nil and st_ ~= stAck and st_ ~= stDel and opr ~= nil and opr ~= op_ then\n"
			+ "   return 'error';\n" + "  end\n" + " end\n" + "end\n" + "if isRobot then\n"
			+ " local keysFromId = redis.call('keys',ptnFromId);\n"
			+ " if keysFromId ~= nil and next(keysFromId) ~= nil then\n" + "  for i0,v0 in pairs(keysFromId) do\n"
			+ "   local inf = {};\n" + "   string.gsub(v0, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "   local op_ = table.concat(inf, ':', 4, 4);\n" + "   local st_ = table.concat(inf,':',8,8);\n"
			+ "   if st_ ~= nil and st_ ~= stAck and st_ ~= stDel and opr ~= nil and op_ == robot then\n"
			+ "    return 'error';\n" + "   end\n" + "  end\n" + " end\n"
			+ "elseif thirdToOutcard ~=nil and thirdToOutcard ~= '1' then \n"
			+ " local keysOpr = redis.call('keys',ptnOpr);\n" + " if keysOpr ~= nil and next(keysOpr) ~= nil then\n"
			+ "  return 'error';\n" + " end\n" + "end\n" + "local radix = nil;"
			+ "local radixStr = redis.call('hget',keyRadix,fromId);\n"
			+ "if radixStr == nil or radixStr == '' or radixStr == false or radixStr == true or tonumber(radixStr) >=0.99 then\n"
			+ " radix = 0.01;\n" + "else\n" + " radix = tonumber(radixStr) + 0.01;\n" + "end\n"
			+ "radix = string.format('%.2f',radix);\n" + "redis.call('hset',keyRadix,fromId,radix);\n"
			+ "local inf = {};\n" + "string.gsub(keyPtn, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "local info = table.concat(inf, ':', 1, 4);\n"
			+ "info = info..':'..radix..':'..transInt..':'..dNow..':'..stAlc..':'..dNow;\n" + "local extime = 1800;\n"
			+ "if isRobot then\n" + " extime = tonumber(timeRobot);\n" + "else\n" + " extime = tonumber(timeManual);\n"
			+ "end\n" + "redis.call('set',info,'','EX',extime);\n" + "return 'ok';\n";

	private static final String LUA_SCRIPT_ALLOC_TRANS_LOCK_ = "local fromId = ARGV[1];\n" + "local toId = ARGV[2];\n"
			+ "local opr = ARGV[3];\n" + "local transInt = ARGV[4];\n" + "local robot = ARGV[5];\n"
			+ "local timeRobot = ARGV[6];\n" + "local timeManual = ARGV[7];\n" + "local ptnFromId = ARGV[8];\n"
			+ "local ptnToId = ARGV[9];\n" + "local ptnOpr = ARGV[10];\n" + "local keyRadix = ARGV[11];\n"
			+ "local keyPtn = ARGV[12];\n" + "local dNow = ARGV[13];\n" + "local stAlc = ARGV[14];\n"
			+ "local stAck = ARGV[15];\n" + "local stDel = ARGV[16];local thirdToOutcard=ARGV[17];\n"
			+ "local isRobot = opr ~= nil and opr == robot;\n" + "local keysToId = redis.call('keys',ptnToId);\n"
			+ "if keysToId ~= nil and next(keysToId) ~= nil then\n" + " for i0,v0 in pairs(keysToId) do\n"
			+ "  local inf = {};\n" + "  string.gsub(v0, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "  local op_ = table.concat(inf, ':', 4, 4);\n" + "  local st_ = table.concat(inf, ':', 8, 8);\n"
			+ "  if st_ ~= nil and st_ ~= stAck and st_ ~= stDel and opr ~= nil and opr ~= op_ then\n"
			+ "   return 'error';\n" + "  end\n"
			+ "  if st_ ~= nil and st_ ~= stAck and st_ ~= stDel and op_ == robot then\n" + "   return 'error';\n"
			+ "  end\n" + " end\n" + "end\n" + "if isRobot then\n"
			+ " local keysFromId = redis.call('keys',ptnFromId);\n"
			+ " if keysFromId ~= nil and next(keysFromId) ~= nil then\n" + "  for i0,v0 in pairs(keysFromId) do\n"
			+ "   local inf = {};\n" + "   string.gsub(v0, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "   local op_ = table.concat(inf, ':', 4, 4);\n" + "   local st_ = table.concat(inf,':',8,8);\n"
			+ "   if st_ ~= nil and st_ ~= stAck and st_ ~= stDel and opr ~= nil and op_ == robot then\n"
			+ "    return 'error';\n" + "   end\n" + "  end\n" + " end\n"
			+ "elseif thirdToOutcard ~=nil and thirdToOutcard ~= '1' then \n"
			+ " local keysOpr = redis.call('keys',ptnOpr);\n" + " if keysOpr ~= nil and next(keysOpr) ~= nil then\n"
			+ "  return 'error';\n" + " end\n" + "end\n" + "local radix = nil;"
			+ "local radixStr = redis.call('hget',keyRadix,fromId);\n"
			+ "if radixStr == nil or radixStr == '' or radixStr == false or radixStr == true or tonumber(radixStr) >=0.99 then\n"
			+ " radix = 0.01;\n" + "else\n" + " radix = tonumber(radixStr) + 0.01;\n" + "end\n"
			+ "radix = string.format('%.2f',radix);\n" + "redis.call('hset',keyRadix,fromId,radix);\n"
			+ "local inf = {};\n" + "string.gsub(keyPtn, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "local info = table.concat(inf, ':', 1, 4);\n"
			+ "info = info..':'..radix..':'..transInt..':'..dNow..':'..stAlc..':'..dNow;\n" + "local extime = 1800;\n"
			+ "if isRobot then\n" + " extime = tonumber(timeRobot);\n" + "else\n" + " extime = tonumber(timeManual);\n"
			+ "end\n" + "redis.call('set',info,'','EX',extime);\n" + "return 'ok';\n";

	private static final String LUA_SCRIPT_ALLOC_TRANS_LOCK_UPDATE_STATUS = "local fr = ARGV[1];\n"
			+ "local to = ARGV[2];\n" + "local st = ARGV[3];\n" + "local exTm = tonumber(ARGV[4]);\n"
			+ "local stDel = ARGV[5];\n" + "local ptnFr = ARGV[6];\n" + "local stAck = ARGV[7];\n"
			+ "local rbt = ARGV[8];\n" + "local preDel = ARGV[9];\n" + "local dNow = ARGV[10];\n"
			+ "local keysFr = redis.call('keys',ptnFr);\n" + "if keysFr ~= nil and next(keysFr) ~= nil then\n"
			+ " for i0,v0 in pairs(keysFr) do\n" + "  local inf = {};\n"
			+ "  string.gsub(v0, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "  local fr_ = table.concat(inf,':',2,2);\n" + "  local to_ = table.concat(inf,':',3,3);\n"
			+ "  local op_ = table.concat(inf,':',4,4);\n" + "  local st_ = table.concat(inf,':',8,8);\n"
			+ "  if op_ ~= nil and op_ == rbt then\n"
			+ "   if fr_ == fr and to_ == to and st_ ~= stAck and st_ ~= stDel then\n" + "    redis.call('del',v0);\n"
			+ "    local info = table.concat(inf, ':', 1, 7)..':'..st..':'..dNow;\n" + "    if st == stDel then\n"
			+ "     info = preDel..table.concat(inf, ':', 2, 7)..':'..st..':'..dNow;\n" + "    end\n"
			+ "    redis.call('set',info,'','EX',exTm);\n" + "   end\n" + "  else\n"
			+ "   if fr_ == fr and to_ == to then\n" + "    redis.call('del',v0);\n" + "   end\n" + "  end\n" + " end\n"
			+ "end\n" + "return 'ok';\n";

	/**
	 * lua script:更新银行卡真实余额
	 */
	private static final String LUA_SCRIPT_UPD_REL_BAL = "local accId = ARGV[1];\n" + "local relBal = ARGV[2];\n"
			+ "local keyRelBal = ARGV[3];\n" + "local keyRelBalRptTm = ARGV[4];\n" + "local keyApiRevokeTm = ARGV[5];\n"
			+ "local currMillis = tonumber(ARGV[6]);\n" + "local updApiRevokeTm = tonumber(ARGV[7]);\n"
			+ "local keyAccBalChgTm = ARGV[8];\n" + "local relBal_ = redis.call('hget',keyRelBal,accId);\n"
			+ "if relBal == '' or relBal == nil then\n" + " relBal = nil;\n" + "else\n"
			+ " relBal = tonumber(relBal);\n" + "end\n" + "if relBal_ == '' or relBal_ == nil then\n"
			+ " relBal_ = nil;\n" + "else\n" + " relBal_ = tonumber(relBal_);\n" + "end\n"
			+ "if relBal == nil and relBal_ == nil then\n" + " redis.call('hdel',keyRelBal,accId);\n"
			+ " redis.call('hdel',keyApiRevokeTm,accId);\n" + " redis.call('hdel',keyRelBalRptTm,accId);\n"
			+ " return 'error';\n" + "end\n" + "if relBal_ == nil then\n"
			+ " redis.call('hset',keyRelBal,accId,relBal);\n" + " redis.call('hset',keyApiRevokeTm,accId,currMillis);\n"
			+ " redis.call('hset',keyRelBalRptTm,accId,currMillis);\n"
			+ " redis.call('hset',keyAccBalChgTm,accId,currMillis);\n" + " return 'ok';\n" + "end\n"
			+ "if relBal == nil then\n" + " if updApiRevokeTm == 1 then\n"
			+ "  redis.call('hset',keyApiRevokeTm,accId,currMillis);\n" + " end\n" + " return 'error';\n" + "end\n"
			+ "if relBal_ < relBal then\n" + " redis.call('hset',keyRelBal,accId,relBal);\n"
			+ " redis.call('hset',keyApiRevokeTm,accId,currMillis);\n"
			+ " redis.call('hset',keyRelBalRptTm,accId,currMillis);\n"
			+ " redis.call('hset',keyAccBalChgTm,accId,currMillis);\n" + " return 'ok';\n" + "end\n"
			+ "if relBal_ >= relBal then\n" + " redis.call('hset',keyRelBal,accId,relBal);\n"
			+ " if updApiRevokeTm == 1 then\n" + "  redis.call('hset',keyApiRevokeTm,accId,currMillis);\n" + " end\n"
			+ " if relBal_ > relBal then\n" + "  redis.call('hset',keyAccBalChgTm,accId,currMillis);\n" + " end\n"
			+ " return 'ok';\n" + "end\n" + "return 'error';\n";

	@Override
	public void schedule() throws InterruptedException {
		String nativeHost = CommonUtils.getInternalIp();
		if (!alloIAcntSer.checkHostRunRight()) {
			log.trace("the host {} have no right to execute the allocation transfer schedule at present.", nativeHost);
			Thread.sleep(5000L);
			return;
		}
		long currTm = System.currentTimeMillis();
		if (cleanInvalidData(currTm, CLEAN_INVLD_DATA_LAST_TM, CLEAN_INVLD_DATA_INTR)) {
			CLEAN_INVLD_DATA_LAST_TM = currTm;
			log.debug("the host {} executed the clean the invalid data in cache.", nativeHost);
		}
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		String allocTransCnst = template.boundValueOps(ALLOC_TRANS_CNST).get();
		if (Objects.equals(allocTransCnst, LAST_ALLOC_TRANS_CNST)) {
			log.trace("the host {} have right , but no need to execute the allocation transfer schedule at present.",
					nativeHost);
			Thread.sleep(2000L);
			return;
		}
		LAST_ALLOC_TRANS_CNST = Objects.isNull(allocTransCnst) ? String.valueOf(System.currentTimeMillis())
				: allocTransCnst;
		if (Objects.isNull(allocTransCnst)) {
			template.boundValueOps(ALLOC_TRANS_CNST).set(LAST_ALLOC_TRANS_CNST);
		}
		log.trace("the host {}  execute the allocation transfer schedule.", nativeHost);
		if (!control()) {
			log.trace("the host {} have no result of the allocation transfer.", nativeHost);
			Thread.sleep(2000L);
		}
	}

	/**
	 * get the transfer task
	 * <p>
	 * from account includes InBank|ReserveBank|IssueBank account
	 * </p>
	 *
	 * @param fromId
	 *            from account ID
	 * @param reBal
	 *            inclusive null
	 * @return transfer information </br>
	 *         if no available transfer at present, null as result.
	 */
	@Override
	public TransferEntity applyByFrom(int fromId, BigDecimal reBal) {
		AccountBaseInfo fr = accSer.getFromCacheById(fromId);
		if (fr == null) {
			log.info("AskTrans{} relBal:{} >> account doesn't exist", fromId, reBal);
			return null;
		}
		int type = fr.getType();
		if (INBANK != type && INWECHAT != type && INALI != type && BINDALI != type && BINDWECHAT != type
				&& BINDCOMMON != type && THIRDCOMMON != type && RESERVEBANK != type && BINDCUSTOMER != type) {
			log.info(
					"AskTrans{} relBal:{} >> account type not in InBank|InWechat|InAlipay|Issue|Reserve|BindCustomer. real type:{}",
					fromId, reBal, type);
			return null;
		}
		if (!Objects.equals(fr.getStatus(), NORMAL)) {
			log.info("AskTrans{} relBal:{} >> account status not Normal. real status:{}", fromId, reBal, type);
			return null;
		}
		TransLock lock = buildLock(false, fromId);
		if (Objects.nonNull(lock)) {
			log.info("AskTrans{} relBal:{} >> TransLock frId:{} toId:{} transInt:{} transRadix:{}", fromId, reBal,
					lock.getFrId(), lock.getToId(), lock.getTransInt(), lock.getTransRadix());
			TransferEntity ret;
			boolean isNewVersion = CommonUtils.checkDistHandicapNewVersion(fr.getHandicapId());
			if (isNewVersion) {
				TransferEntity entity = allOTaskSer.applyTask4RobotNew(fromId, reBal);
				if (entity != null) {
					return entity;
				}
			}
			boolean isEnableInbankHandicap = CommonUtils.checkEnableInBankHandicap(fr.getHandicapId());
			// 盘口开启入款卡出款，由于出款任务在lock中存的是taskid，没有toAccountId，所以调applyTask4Robot接口查询
			if (isEnableInbankHandicap) {
				ret = allOTaskSer.applyTask4Robot(fromId, reBal);
				if (ret != null) {
					log.info("AskTrans{} relBal:{} >> taskid:{} amt:{}", fr.getId(), reBal, ret.getTaskId(),
							ret.getAmount());
					return ret;
				}
			}
			ret = new TransferEntity();
			AccountBaseInfo to = accSer.getFromCacheById(lock.getToId());
			if (Objects.isNull(to) || !Objects.equals(to.getStatus(), NORMAL)) {
				redisSer.getStringRedisTemplate().delete(lock.getMsg());
				return null;
			}
			ret.setFromAccountId(fr.getId());
			ret.setToAccountId(to.getId());
			ret.setAccount(to.getAccount());
			ret.setOwner(to.getOwner());
			ret.setBankType(to.getBankType());
			ret.setBankAddr(to.getBankName());
			ret.setAcquireTime(lock.getTime());
			BigDecimal amt = BigDecimal.ZERO;
			amt = amt.add(Objects.nonNull(lock.getTransInt()) ? lock.getTransInt() : BigDecimal.ZERO);
			amt = amt.add(Objects.nonNull(lock.getTransRadix()) ? lock.getTransRadix() : BigDecimal.ZERO);
			ret.setAmount(amt.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
			BigDecimal relBal = accountChangeService.buildRelBal(fromId);
			systemAccountManager.regist(ret, relBal);
			if (Objects.equals(TransLock.STATUS_ALLOC, lock.getStatus())) {
				llockUpdStatus(fromId, lock.getToId(), TransLock.STATUS_CLAIM, LOCK_ROBOT_CLAIM_SECONDS);
				log.info("AskTrans{} relBal:{} >> first.to:{} amt:{}", fr.getId(), reBal, to.getId(), ret.getAmount());
			} else {
				log.error("AskTrans{} relBal:{} >> duplicate . to:{} amt:{}", fr.getId(), reBal, to.getId(),
						ret.getAmount());
			}
			flushLockLog(lock.getFrId(), lock.getToId(), lock.getTransInt(), lock.getTransRadix());
			return ret;
		} else {
			if (CommonUtils.checkDistHandicapNewVersion(fr.getHandicapId())) {
				return allOTaskSer.applyTask4RobotNew(fromId, reBal);
			}
			log.info("AskTrans{} relBal:{} >> TransLock is empty.", fromId, reBal);
		}
		return null;
	}

	/**
	 * get the transfer task （new version）
	 * <p>
	 * from account includes InBank|ReserveBank|IssueBank account
	 * </p>
	 *
	 * @param fromId
	 *            from account ID
	 * @param reBal
	 *            inclusive null
	 * @return transfer information </br>
	 *         if no available transfer at present, null as result.
	 */
	@Override
	public TransferEntity applyByFromNew(int fromId, BigDecimal reBal) {
		AccountBaseInfo fr = accSer.getFromCacheById(fromId);
		if (fr == null) {
			log.debug("AskTrans{} relBal:{} >> account doesn't exist", fromId, reBal);
			return null;
		}
		int type = fr.getType();
		if (INBANK != type && RESERVEBANK != type && BINDCOMMON != type) {
			log.debug(
					"AskTrans{} relBal:{} >> account type not in InBank|InWechat|InAlipay|Issue|Reserve|BindCustomer. real type:{}",
					fromId, reBal, type);
			return null;
		}
		if (!Objects.equals(fr.getStatus(), NORMAL)) {
			log.debug("AskTrans{} relBal:{} >> account status not Normal. real status:{}", fromId, reBal, type);
			return null;
		}
		TransLock lock = buildLock(false, fromId);
		if (Objects.nonNull(lock)) {
			log.debug("AskTrans{} relBal:{} >> TransLock frId:{} toId:{} transInt:{} transRadix:{}", fromId, reBal,
					lock.getFrId(), lock.getToId(), lock.getTransInt(), lock.getTransRadix());
			TransferEntity ret;
			Object model = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
					.get(fr.getId().toString());
			int modelVal = model == null ? Constants.YSF_MODEL_IN : Integer.valueOf(model.toString());
			// 边入边出入款卡、非专注入款卡（出款模式）请求出款任务
			if (INBANK == fr.getType() && (Objects.equals(fr.getSubType(), 3)
					|| (!fr.getFlag().equals(AccountFlag.PC.getTypeId()) && modelVal == Constants.YSF_MODEL_OUT))) {
				ret = allOTaskSer.applyTask4RobotNew(fromId, reBal);
				if (ret != null) {
					log.debug("AskTrans{} relBal:{} >> taskid:{} amt:{}", fr.getId(), reBal, ret.getTaskId(),
							ret.getAmount());
					return ret;
				}
			}
			ret = new TransferEntity();
			AccountBaseInfo to = accSer.getFromCacheById(lock.getToId());
			if (Objects.isNull(to) || !Objects.equals(to.getStatus(), NORMAL)) {
				redisSer.getStringRedisTemplate().delete(lock.getMsg());
				return null;
			}
			ret.setFromAccountId(fr.getId());
			ret.setToAccountId(to.getId());
			ret.setAccount(to.getAccount());
			ret.setOwner(to.getOwner());
			ret.setBankType(to.getBankType());
			ret.setBankAddr(to.getBankName());
			ret.setAcquireTime(lock.getTime());
			BigDecimal amt = BigDecimal.ZERO;
			amt = amt.add(Objects.nonNull(lock.getTransInt()) ? lock.getTransInt() : BigDecimal.ZERO);
			amt = amt.add(Objects.nonNull(lock.getTransRadix()) ? lock.getTransRadix() : BigDecimal.ZERO);
			ret.setAmount(amt.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
			BigDecimal relBal = accountChangeService.buildRelBal(fromId);
			systemAccountManager.regist(ret, relBal);
			if (Objects.equals(TransLock.STATUS_ALLOC, lock.getStatus())) {
				llockUpdStatus(fromId, lock.getToId(), TransLock.STATUS_CLAIM, LOCK_ROBOT_CLAIM_SECONDS);
				log.debug("AskTrans{} relBal:{} >> first.to:{} amt:{}", fr.getId(), reBal, to.getId(), ret.getAmount());
			} else {
				log.debug("AskTrans{} relBal:{} >> duplicate . to:{} amt:{}", fr.getId(), reBal, to.getId(),
						ret.getAmount());
			}
			flushLockLog(lock.getFrId(), lock.getToId(), lock.getTransInt(), lock.getTransRadix());
			return ret;
		} else {
			log.debug("AskTrans{} relBal:{} >> TransLock is empty.", fromId, reBal);
		}
		return null;
	}

	/**
	 * get the transfer task by cloud funds transfer system
	 * <p>
	 * if {@code  relBal == null || relBal.compareTo(BigDecimal.ZERO)<=0},
	 * {@code null} would be as result.
	 *
	 * @param bindType
	 *            10:weichat;11：alipay
	 * @param acc
	 *            the from-account's card number.
	 * @param handi
	 *            handicap code defined by platform system.
	 * @param l
	 *            current system level.
	 * @param relBal
	 *            the real balance at present.
	 * @return TransferEntity#getToAccountId();</br>
	 *         TransferEntity#getAccount();</br>
	 *         TransferEntity#getOwner();</br>
	 *         TransferEntity#getBankType();</br>
	 *         TransferEntity#getBankAddr();</br>
	 *         TransferEntity#getAcquireTime();</br>
	 *         TransferEntity#getAmount();</br>
	 * @see BizHandicap#getCode()
	 * @see CurrentSystemLevel#getValue()
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#BindAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#BindWechat
	 */
	@Override
	public TransferEntity applyByCloud(int bindType, String acc, String handi, Integer l, BigDecimal relBal) {
		int toler = buildToler();
		if (StringUtils.isEmpty(acc) || StringUtils.isEmpty(handi) || Objects.isNull(l) || Objects.isNull(relBal)
				|| relBal.intValue() < toler) {
			log.error(
					"CloudAskTrans{} >> (handi:{} l:{} relBal:{}) acc|handi|l|relBal empty , || real balance is littler than toler ({}).",
					acc, handi, l, relBal, toler);
			return null;
		}

		if (!CommonUtils.checkProEnv(CURR_VERSION)) {// 非生产环境 TransferEntity
			TransferEntity ret = new TransferEntity();
			AccountBaseInfo to = accSer.getFromCacheById(2344);
			ret.setToAccountId(to.getId());
			ret.setAccount(to.getAccount());
			ret.setOwner(to.getOwner());
			ret.setBankType(to.getBankType());
			ret.setBankAddr(to.getBankName());
			ret.setAcquireTime(System.currentTimeMillis());
			BigDecimal amt = relBal.subtract(buildMinBal(true));
			ret.setAmount(amt.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
			return ret;
		}
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Set<String> bks = findBlackList();// black collection.
		Map<Integer, Integer> isb = new HashMap<>();// reserveAccountID->realBalance.
		Double[] fil = new Double[] { enScore4Fr(bindType, 0, 0, 0, 0), enScore4Fr(bindType + 1, 0, 0, 0, 0) };
		template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM).rangeByScoreWithScores(fil[0], fil[1])
				.forEach(p -> isb.put(Integer.valueOf(p.getValue()), deScore4Fr(p.getScore())[4]));
		List<Map.Entry<Integer, Integer>> tarl = new ArrayList<>(buildExpAmt(isb).entrySet());
		Collections.sort(tarl, Comparator.comparing(Map.Entry::getValue));
		for (Map.Entry<Integer, Integer> target : tarl) {
			AccountBaseInfo to = accSer.getFromCacheById(target.getKey());
			if (Objects.isNull(to) || !Objects.equals(NORMAL, to.getStatus()) || !checkBlack(bks, acc, to.getId()))
				continue;
			int transInt = relBal.subtract(buildMinBal(true)).intValue();
			transInt = Math.min(transInt, TRANS_MAX_PER + (new Random().nextInt(1000)));
			try {
				lockTrans(acc, to.getId(), AppConstants.USER_ID_4_ADMIN, transInt);
				log.info("CloudAskTrans{} >> Alloc ( acc:{} bal:{} toId:{} amt:{} ) Alloc toId:{} transInt:{}", acc,
						acc, relBal, to.getId(), transInt);
				break;
			} catch (Exception e) {
				log.debug("CloudAskTrans{} >> Fail ( acc:{} bal:{} toId:{} amt:{} ) Alloc toId:{} transInt:{}", acc,
						acc, relBal, to.getId(), transInt);
			}
		}
		Set<String> keys = template.keys(RedisKeys.genPattern4TransferAccountLock_from(acc));
		TransLock lock = buildLock(true, keys);
		if (Objects.nonNull(lock)) {
			TransferEntity ret = new TransferEntity();
			AccountBaseInfo to = accSer.getFromCacheById(lock.getToId());
			ret.setToAccountId(to.getId());
			ret.setAccount(to.getAccount());
			ret.setOwner(to.getOwner());
			ret.setBankType(to.getBankType());
			ret.setBankAddr(to.getBankName());
			ret.setAcquireTime(System.currentTimeMillis());
			BigDecimal amt = BigDecimal.ZERO;
			amt = amt.add(Objects.nonNull(lock.getTransInt()) ? lock.getTransInt() : BigDecimal.ZERO);
			amt = amt.add(Objects.nonNull(lock.getTransRadix()) ? lock.getTransRadix() : BigDecimal.ZERO);
			ret.setAmount(amt.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
			if (Objects.equals(TransLock.STATUS_ALLOC, lock.getStatus())) {
				llockUpdStatus(acc, lock.getToId(), TransLock.STATUS_CLAIM, LOCK_ROBOT_CLAIM_SECONDS);
				log.info("CloudAskTrans{} >> Ret ( acc:{} bal:{} toId:{} amt:{} ) Alloc toId:{} amt:{}", acc, acc,
						relBal, to.getId(), amt);
			} else {
				log.info("CloudAskTrans{} >> duplicate ( acc:{} bal:{} toId:{} amt:{} ) Alloc toId:{} amt:{}", acc, acc,
						relBal, to.getId(), amt);
			}
			flushLockLog(lock.getFrId(), lock.getToId(), lock.getTransInt(), lock.getTransRadix());
			return ret;
		} else {
			log.info("CloudAskTrans{} >> ( acc:{} bal:{} ) has no result.", acc, acc, relBal);
		}
		return null;
	}

	/**
	 * acknowledge the robot transfer result.
	 *
	 * @param entity
	 *            transfer entity
	 */
	@Override
	@Transactional
	public void ackByRobot(TransferEntity entity) {
		boolean isOutwardTask = Objects.nonNull(entity.getTaskId());
		if (!isOutwardTask && Objects.isNull(entity.getToAccountId())) {
			log.trace("AckByRobot >> trans entity is empty | to-account Id is empty.& isn't outward task ");
			return;
		}
		long consume = 0;// 耗时(秒)
		if (Objects.nonNull(entity.getAcquireTime())) {
			consume = (System.currentTimeMillis() - entity.getAcquireTime()) / 1000;
		}
		boolean result = Objects.nonNull(entity.getResult()) && entity.getResult() == 1;// True:成功；False:失败；
		log.info("AckByRobot >> {},consume:{} seconds. fr:{} , to:{} amt:{} reBal:{}", result, consume,
				entity.getFromAccountId(), entity.getToAccountId(), entity.getAmount(), entity.getBalance());
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		BigDecimal[] divAndRemain = new BigDecimal(entity.getAmount()).divideAndRemainder(BigDecimal.ONE);
		try {
			if (Objects.equals(3, entity.getResult())) {
				return;// 状态为3：超时
			}
			if (isOutwardTask) {
				// 入款卡完成出款
				allOTaskSer.ack4Robot(entity);
			}
			// 下发上报
			if (!isOutwardTask) {
				String keyOrderNo = RedisKeys.genKey4LockRem(buildOrderNo(entity.getFromAccountId(),
						entity.getToAccountId(), divAndRemain[0], divAndRemain[1]));
				String remark = template.boundValueOps(keyOrderNo).get();
				AccountBaseInfo fr = accSer.getFromCacheById(entity.getFromAccountId());
				AccountBaseInfo to = accSer.getFromCacheById(entity.getToAccountId());
				int status = result ? IncomeRequestStatus.Matching.getStatus()
						: IncomeRequestStatus.Canceled.getStatus();
				Date time = entity.getTime() == null ? new Date() : entity.getTime();
				BizIncomeRequest o = new BizIncomeRequest();
				o.setFromId(entity.getFromAccountId());
				o.setToId(entity.getToAccountId());
				o.setLevel(0);
				o.setHandicap(0);
				o.setToAccount(to.getAccount());
				o.setOperator(null);
				o.setAmount(new BigDecimal(entity.getAmount().toString()));
				o.setCreateTime(new Date(Objects.isNull(entity.getAcquireTime()) ? System.currentTimeMillis()
						: entity.getAcquireTime()));
				o.setOrderNo(String.valueOf(time.getTime()));
				o.setRemark(CommonUtils.genRemark(remark, (result ? "下发成功" : "下发失败"), new Date(), "机器"));
				if (Objects.equals(fr.getType(), RESERVEBANK) && Objects.equals(to.getType(), RESERVEBANK)) {
					o.setType(IncomeRequestType.ReserveToReserve.getType());
				} else {
					o.setType(oldTransSer.transToReqType(fr.getType()));
				}
				o.setFromAccount(fr.getAccount());
				o.setMemberUserName(StringUtils.EMPTY);
				o.setMemberRealName(to.getOwner());
				o.setStatus(status);
				o.setToAccountBank(to.getBankName());
				o.setTimeconsume(consume);
				inReqSer.save(o, true);
				// 如果下发成功，toAccountId增加五分钟时间锁，保证下发五分钟之内不会下发第二笔
				if (result && entity.getAmount() >= Constants.MAX_TOLERANCE) {
					dealWithTransLock(entity.getToAccountId());
				}
				// 由于一键转出和测试任务没有标识，上报的逻辑在下发上报时处理
				ackTrans(entity);
			}
		} catch (Exception e) {
			log.info("AckByRobot >> {}, Fail. consume:{} seconds. fr:{} , to:{} amt:{} reBal:{}.", result, consume,
					entity.getFromAccountId(), entity.getToAccountId(), entity.getAmount(), entity.getBalance(), e);
		} finally {
			int TRANS_LOCK_STATUS = isOutwardTask ? TransLock.STATUS_DEL
					: result ? TransLock.STATUS_ACK : TransLock.STATUS_DEL;
			int ACK_SECONDS = isOutwardTask ? LOCK_ROBOT_ALLOC_SECONDS
					: result ? LOCK_ROBOT_ACK_SECONDS : LOCK_ROBOT_DEL_SECONDS;
			int toId = isOutwardTask ? entity.getTaskId().intValue() : entity.getToAccountId();
			llockUpdStatus(entity.getFromAccountId(), toId, TRANS_LOCK_STATUS, ACK_SECONDS);
			if (!isOutwardTask) {
				if (!result) {
					addToBlackList(entity.getFromAccountId(), entity.getToAccountId(), 150, TimeUnit.SECONDS);
				}
				flushLockLog(entity.getFromAccountId(), entity.getToAccountId(), divAndRemain[0], divAndRemain[1]);
			}
		}
	}

	/**
	 * confirm the result of transaction from frAcc account to toId account.
	 *
	 * @param orderNo
	 *            the order No
	 * @param handicapCode
	 *            the handicap code defined by plat form system.
	 * @param frAcc
	 *            the card number of frAcc
	 * @param toId
	 *            the identity of toAcc defined in funds transfer system.
	 * @param amt
	 *            transfer amount.
	 * @param acquireTime
	 *            the time the cloud funds transfer system claim task.
	 * @param ret
	 *            true: transfer success</br>
	 *            false: transfer failure</br>
	 * @see TransferEntity#getAcquireTime()
	 */
	@Override
	public void ackByCloud(String orderNo, String handicapCode, String frAcc, int toId, BigDecimal amt,
			long acquireTime, boolean ret) {
		if (StringUtils.isBlank(frAcc) || Objects.isNull(amt) || amt.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		BizHandicap handi = handiSer.findFromCacheByCode(handicapCode);
		if (Objects.isNull(handi)) {
			return;
		}
		long consume = (System.currentTimeMillis() - acquireTime) / 1000;
		log.info("AckByCloud >> {},consume:{} seconds. fr:{} , to:{} amt:{}", ret, consume, frAcc, toId, amt);
		try {
			AccountBaseInfo to = accSer.getFromCacheById(toId);
			int status = ret ? IncomeRequestStatus.Matching.getStatus() : IncomeRequestStatus.Canceled.getStatus();
			BizIncomeRequest o = new BizIncomeRequest();
			o.setToId(toId);
			o.setLevel(0);
			o.setHandicap(handi.getId());
			o.setHandicapName(handi.getName());
			o.setToAccount(to.getAccount());
			o.setOperator(null);
			o.setAmount(amt.setScale(2, BigDecimal.ROUND_HALF_UP));
			o.setCreateTime(new Date());
			o.setOrderNo("p" + orderNo);
			o.setRemark(CommonUtils.genRemark(null, (ret ? "下发成功" : "下发失败"), new Date(), "云端"));
			o.setType(IncomeRequestType.PlatFormToFundsTransfer.getType());
			o.setFromAccount(frAcc);
			o.setMemberUserName(StringUtils.EMPTY);
			o.setMemberRealName(to.getOwner());
			o.setStatus(status);
			o.setToAccountBank(to.getBankName());
			o.setTimeconsume(consume);
			inReqSer.save(o, true);
		} catch (Exception e) {
			log.info("AckByCloud >> {},Fail consume:{} seconds. fr:{} , to:{} amt:{}", ret, consume, frAcc, toId, amt);
		} finally {
			int TRANS_LOCK_STATUS = ret ? TransLock.STATUS_ACK : TransLock.STATUS_DEL;
			int ACK_SECONDS = ret ? LOCK_ROBOT_ACK_SECONDS : LOCK_ROBOT_DEL_SECONDS;
			llockUpdStatus(frAcc, toId, TRANS_LOCK_STATUS, ACK_SECONDS);
		}
	}

	/**
	 * 描述:入款卡上报余额 适用于入款卡可以转账到出款卡的时候 上报余额场景:1.确认转账结果 2.流水上报 3.获取下发任务 4.下发完成上报结果
	 * 5.获取出款任务 6.出款卡请求转账 7.手机端上报流水时间 8.第三方流水上报
	 * 
	 * @param id
	 *            identity of account.
	 * @param bal
	 */
	@Override
	public void applyRelBal(int id, BigDecimal bal) {
		if (CommonUtils.checkDistHandicapNewVersion(accSer.getFromCacheById(id).getHandicapId())) {
			log.debug("applyRelBal>> use the new version,id {},bal {}", id, bal);
			this.applyRelBalNew(id, bal, null, true);
		} else {
			log.debug("applyRelBal>> haven't use the new version,id {},bal {}", id, bal);
			this.applyRelBal(id, bal, null, true);
		}
	}

	/**
	 * 描述：入款卡(可以下发到出款卡)余额上报处理方法
	 * 
	 * @param id
	 *            上报余额的卡号id
	 * @param bal
	 *            上报的余额
	 * @param repTm
	 *            上报时间
	 * @param updApiInvokeTm
	 *            是否更新api调用时间标识
	 */
	@Override
	public void applyRelBal(int id, BigDecimal bal, Long repTm, boolean updApiInvokeTm) {
		AccountBaseInfo base1 = accSer.getFromCacheById(id);
		if (base1 != null && CommonUtils.checkDistHandicapNewVersion(base1.getHandicapId())) {
			log.debug("applyRelBal>>:id:{},bal:{},repTm:{},updApiInvokeTm:{},use the new version", id, bal, repTm,
					updApiInvokeTm);
			applyRelBalNew(id, bal, repTm, updApiInvokeTm);
			return;
		}
		log.debug("余额上报,参数:id:{},bal:{},repTm:{},updApiInvokeTm:{}", id, bal, repTm, updApiInvokeTm);
		redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_REAL_BAL_TM).put(String.valueOf(id),
				String.valueOf(System.currentTimeMillis()));
		if (setRealBalInCache(id, bal != null ? bal.setScale(2, BigDecimal.ROUND_HALF_UP) : null, repTm,
				updApiInvokeTm)) {
			log.debug("调用脚本更新余额,放入队列!参数:id:{},bal:{},repTm:{},updApiInvokeTm:{}", id, bal, repTm, updApiInvokeTm);
			try {
				ACC_AMT.put(id, bal);
				ACC_QUENE.add(id);
			} catch (Exception e) {
				log.error("Error,report account's real balance. id:{} bal:{} msg:{}", id, bal, e);
			}
		}
		if (AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_REAL_AMT)) {
			try {
				THREAD_ACC_REAL_AMT.interrupt();
			} catch (Exception e) {
				log.error("Error,stop thread for account balance. id:{} bal:{}", id, bal);
			} finally {
				THREAD_ACC_REAL_AMT = null;
			}
			return;
		} else if (Objects.nonNull(THREAD_ACC_REAL_AMT) && THREAD_ACC_REAL_AMT.isAlive()) {
			log.trace("the thread for account real amount reporting  already exist. id:{},bal:{}", id, bal);
			return;
		}
		THREAD_ACC_REAL_AMT = new Thread(() -> {
			for (;;) {
				Integer accId = null;
				try {
					accId = ACC_QUENE.poll();
					if (Objects.isNull(accId)) {
						if (!AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_REAL_AMT)) {
							try {
								Thread.sleep(2000);
							} catch (Exception e) {
								log.error("Thread Account Balance Exception.");
							}
							continue;
						} else {
							break;
						}
					}
					AccountBaseInfo base = accSer.getFromCacheById(accId);
					BigDecimal relBal = ACC_AMT.getIfPresent(accId);
					if (base == null || relBal == null) {
						log.debug("applyRelBal>> base | relBal is null,base {},relBal {}", base, base);
						continue;
					}
					log.debug("applyRelBal>> accId {},accType {},status {}", base.getId(), base.getType(),
							base.getStatus());
					// 1.更新账号数据库余额(放在对账benchmark方法中)
					accDao.updateBankBalance(relBal, accId);
					ALLOC_TRANS_DUPLICATE = true;
					int type = base.getType(), status = base.getStatus();
					StringRedisTemplate template = redisSer.getStringRedisTemplate();
					String tar = String.valueOf(accId);
					// 1.检查最近的下发记录是否有未匹配的且所有金额大于100的记录,以便告警和决定是否继续下发
					BigDecimal[] amtAndCnt = buildFlowMatching(base.getId());
					if (BigDecimal.ZERO.compareTo(amtAndCnt[1]) != 0) {
						log.debug("applyRelBal>> acc has matching flow,accId {},accType {}", base.getId(),
								base.getType());
						ldel4ONeed(tar);
						template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM).remove(tar);
						continue;
					}
					// 2. check status :Normal
					if (NORMAL != status) {
						if (OUTBANK == type) {
							log.debug(
									"applyRelBal>> accId {},accType {},acc status isn't normal,remove from ALLOC_OUT_NEED_ORI",
									base.getId(), base.getType());
							ldel4ONeed(tar);
						} else {
							log.debug(
									"applyRelBal>> accId {},accType {},acc status isn't normal,remove from ALLOC_APPLY_BY_FROM",
									base.getId(), base.getType());
							template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM).remove(tar);
						}
						continue;
					}
					// 3. update
					if (INBANK == type || INWECHAT == type || INALI == type || BINDALI == type || BINDWECHAT == type
							|| BINDCOMMON == type || THIRDCOMMON == type || RESERVEBANK == type
							|| BINDCUSTOMER == type) {
						log.debug("applyRelBal>> accId {},accType {},inbank type calls applyByIn ", base.getId(),
								base.getType());
						applyByIn(base, relBal);
					}
					// 4. update the needing amount collection for out account.
					if (OUTBANK == type) {
						String enableInbankHandicap = CommonUtils.getEnableInBankHandicap();
						// 所有盘口开启入款卡、备用卡出款时，出款卡的数据不用加入到ALLOC_APPLY_BY_FROM结果集中
						if (!"ALL".equals(enableInbankHandicap)) {
							log.debug("applyRelBal>> accId {},accType {},outbank type calls applyByIn ", base.getId(),
									base.getType());
							applyByIn(base, relBal);
						}
						applyByOut(base, relBal);
					}
					// 5 bindcustomer account report bal to platform
					if (BINDCUSTOMER == type) {
						String code = handiSer.findFromCacheById(base.getHandicapId()).getCode();
						if (StringUtils.isNotBlank(code)) {
							String balance = relBal.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
							SyncBankBalanceInputDTO balanceInputDTO = new SyncBankBalanceInputDTO();
							balanceInputDTO.setOid(Integer.parseInt(code));
							balanceInputDTO.setAccount(base.getAccount());
							balanceInputDTO.setBalance(relBal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
							balanceInputDTO.setSysBalance(relBal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
							ResponseDataNewPay responseDataNewPay = newPayService.syncBankBalance(balanceInputDTO);
							if (responseDataNewPay.getCode() == 200) {
								log.info("上报客户绑定卡余额到平台成功，账号:{}，余额:{}", base.getAccount(), balance);
							} else {
								log.error("上报客户绑定卡余额到平台失败，原因: {}，账号: {}，余额: {}", responseDataNewPay.getMsg(),
										base.getAccount(), balance);
							}
						}
					}
				} catch (Exception e) {
					log.error("Thread Account Balance Exception account id:{} ", accId);
					log.error("Thread Account Balance Exception. ", e);
				} finally {
					if (ALLOC_TRANS_DUPLICATE | (ALLOC_TRANS_DUPLICATE = false)) {
						redisSer.getStringRedisTemplate().boundValueOps(ALLOC_TRANS_CNST)
								.set(String.valueOf(System.currentTimeMillis()));
					}
				}
			}
		});
		THREAD_ACC_REAL_AMT.setName("THREAD_ACC_REAL_AMT");
		THREAD_ACC_REAL_AMT.start();
	}

	/**
	 * 描述：新版本入款卡(可以下发到出款卡)余额上报处理方法
	 * 
	 * @param id
	 *            上报余额的卡号id
	 * @param bal
	 *            上报的余额
	 * @param repTm
	 *            上报时间
	 * @param updApiInvokeTm
	 *            是否更新api调用时间标识
	 */
	@Override
	public void applyRelBalNew(int id, BigDecimal bal, Long repTm, boolean updApiInvokeTm) {
		log.debug("applyRelBalNew>>余额上报,参数:id:{},bal:{},repTm:{},updApiInvokeTm:{}", id, bal, repTm, updApiInvokeTm);
		redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_REAL_BAL_TM).put(String.valueOf(id),
				String.valueOf(System.currentTimeMillis()));
		if (setRealBalInCache(id, bal, repTm, updApiInvokeTm)) {
			log.debug("applyRelBalNew>>调用脚本更新余额,放入队列!");
			try {
				ACC_NEW_AMT.put(id, bal);
				ACC_NEW_QUENE.add(id);
			} catch (Exception e) {
				log.error("Error,report account's real balance. id:{} bal:{} msg:{}", id, bal, e);
			}
		}
		if (AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_NEW_REAL_AMT)) {
			try {
				THREAD_ACC_NEW_REAL_AMT.interrupt();
			} catch (Exception e) {
				log.error("Error,stop thread for account balance. id:{} bal:{}", id, bal);
			} finally {
				THREAD_ACC_NEW_REAL_AMT = null;
			}
			return;
		} else if (Objects.nonNull(THREAD_ACC_NEW_REAL_AMT) && THREAD_ACC_NEW_REAL_AMT.isAlive()) {
			log.trace("the thread for account real amount reporting  already exist. id:{},bal:{}", id, bal);
			return;
		}
		THREAD_ACC_NEW_REAL_AMT = new Thread(() -> {
			for (;;) {
				try {
					Integer accId = ACC_NEW_QUENE.poll();
					if (Objects.isNull(accId)) {
						if (!AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_NEW_REAL_AMT)) {
							try {
								Thread.sleep(2000);
							} catch (Exception e) {
								log.error("Thread Account Balance Exception.");
							}
							continue;
						} else {
							break;
						}
					}
					AccountBaseInfo base = accSer.getFromCacheById(accId);
					BigDecimal relBal = ACC_NEW_AMT.getIfPresent(accId);
					if (base == null || relBal == null) {
						continue;
					}
					log.debug("applyRelBalNew>> accId {},accType {},status {}", base.getId(), base.getType(),
							base.getStatus());
					// 1.更新账号数据库余额(放在对账benchmark方法中)
					accDao.updateBankBalance(relBal, accId);
					ALLOC_NEW_TRANS_DUPLICATE = true;
					int type = base.getType(), status = base.getStatus();
					StringRedisTemplate template = redisSer.getStringRedisTemplate();
					String tar = String.valueOf(accId);
					// 1.检查最近的下发记录是否有未匹配的且所有金额大于100的记录,以便告警和决定是否继续下发
					BigDecimal[] amtAndCnt = buildFlowMatching(base.getId());
					// 修改流水告警逻辑以及是否下发或者出款
					if (BigDecimal.ZERO.compareTo(amtAndCnt[1]) != 0) {
						log.debug("applyRelBalNew>> acc has matching flow,accId {},accType {}", base.getId(),
								base.getType());
						ldel4ONeedNew(tar);
						template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(tar);
						continue;
					}
					// 2. check status :Normal
					if (NORMAL != status) {
						if (OUTBANK == type) {
							log.debug(
									"applyRelBalNew>> accId {},accType {},acc status isn't normal,remove from ALLOC_OUT_NEED_ORI",
									base.getId(), base.getType());
							ldel4ONeedNew(tar);
						} else {
							log.debug(
									"applyRelBalNew>> accId {},accType {},acc status isn't normal,remove from ALLOC_NEW_APPLY_BY_FROM",
									base.getId(), base.getType());
							template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(tar);
						}
						continue;
					}
					// 3. update
					if (INBANK == type || BINDCOMMON == type || RESERVEBANK == type) {
						log.debug(
								"applyRelBalNew>> acc is inbank|bindcommn|reservebank,call applyByInNew,accId {},accType {}",
								base.getId(), base.getType());
						applyByInNew(base, relBal);
					}
					// 4. update the needing amount collection for out account.
					if (OUTBANK == type) {
						log.debug("applyRelBalNew>> acc is outbank,call applyByOutNew,accId {},accType {}",
								base.getId(), base.getType());
						applyByOutNew(base, relBal);
					}
				} catch (Exception e) {
					log.error("Thread Account Balance Exception account id:{} ", id);
					log.error("Thread Account Balance Exception. ", e);
				} finally {
					if (ALLOC_NEW_TRANS_DUPLICATE | (ALLOC_NEW_TRANS_DUPLICATE = false)) {
						redisSer.getStringRedisTemplate().boundValueOps(ALLOC_NEW_TRANS_CNST)
								.set(String.valueOf(System.currentTimeMillis()));
					}
				}
			}
		});
		THREAD_ACC_NEW_REAL_AMT.setName("THREAD_ACC_NEW_REAL_AMT");
		THREAD_ACC_NEW_REAL_AMT.start();
	}

	/**
	 * set the upper limit of out account
	 * <p>
	 * <code>if on == false </code> the params upLim,durTm are in invalid.
	 * <code>if on == true</code> the params upLim ,durTm are valid
	 * </p>
	 *
	 * @param on
	 *            true: start </br>
	 *            false:stop
	 * @param upLim
	 *            the upper limit of out account
	 * @param triglimit
	 *            the trigger limit of out account
	 * @param durTm
	 *            time of duration. mintue is time unit.</br>
	 */
	@Override
	public void setUpLim4ONeed(int zone, boolean on, int upLim, int triglimit, int durTm) {
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		String key = RedisKeys.genKey4AllocTransUpLim(zone);
		if (on) {
			long edTm = System.currentTimeMillis() + durTm * 60000;
			template.boundZSetOps(key).add(UP_LIM_UP_LIM, upLim);
			template.boundZSetOps(key).add(UP_LIM_DUR_TM, durTm);
			template.boundZSetOps(key).add(UP_LIM_ED_TM, edTm);
			template.boundZSetOps(key).add(UP_LIM_TRIG_LIM, triglimit);
			template.boundZSetOps(key).expire(durTm, TimeUnit.MINUTES);
		} else {
			template.delete(key);
		}
	}

	/**
	 * get the upper limit of out account
	 *
	 * @return int[0] 1:start;otherwise:0</br>
	 *         int[1] upLim </br>
	 *         int[2] trigLim</br>
	 *         int[3] durTm</br>
	 *         int[4] expire Time</br>
	 * @see this#setUpLim4ONeed(int, boolean, int, int, int)
	 */
	@Override
	public long[] getUpLim4ONeed(int zone) {
		String key = RedisKeys.genKey4AllocTransUpLim(zone);
		Set<ZSetOperations.TypedTuple<String>> set = redisSer.getStringRedisTemplate().boundZSetOps(key)
				.rangeWithScores(0, -1);
		if (CollectionUtils.isEmpty(set)) {
			return new long[] { 0, 0, 0, 0, 0 };
		}
		long[] ret = new long[] { 1, 0, 0, 0, 0 };
		set.forEach(p -> {
			String val = p.getValue();
			long scr = p.getScore().longValue();
			if (Objects.equals(UP_LIM_UP_LIM, val))
				ret[1] = scr;
			else if (Objects.equals(UP_LIM_TRIG_LIM, val))
				ret[2] = scr;
			else if (Objects.equals(UP_LIM_DUR_TM, val))
				ret[3] = scr;
			else if (Objects.equals(UP_LIM_ED_TM, val))
				ret[4] = scr;
		});
		return ret;
	}

	/**
	 * get the information that transfer to to-account
	 *
	 * @param toId
	 *            to-account's ID</br>
	 *            exclusive :OutBank
	 */
	@Override
	public List<TransTo> buildTransTo(int toId) {
		List<TransTo> ret = new ArrayList<>();
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Set<String> keys = template.keys(RedisKeys.genPattern4TransferAccountLock_to(toId));
		keys.addAll(template.keys(RedisKeys.genPattern4LockDel_to(toId)));
		Long rptTm = buildRealBalRptTm(toId);
		Long currMillis = System.currentTimeMillis();
		if (Objects.isNull(rptTm)) {
			rptTm = currMillis - 600000;
		}
		for (String key : keys) {
			TransLock lock = new TransLock(key);
			if (lock.getLtTime() <= rptTm) {
				continue;
			}
			TransTo to = new TransTo();
			AccountBaseInfo base = accSer.getFromCacheById(lock.getFrId());
			BizHandicap frHandi = handiSer.findFromCacheById(base.getHandicapId());
			to.setFrHandicapName(Objects.isNull(frHandi) ? StringUtils.EMPTY : frHandi.getName());
			to.setFrCsl(Objects.isNull(base.getCurrSysLevel()) ? Outter : base.getCurrSysLevel());
			to.setFrAlias(base.getAlias());
			to.setFrAcc(base.getAccount());
			to.setFrBankType(base.getBankType());
			to.setFrOwner(base.getOwner());
			to.setToId(lock.getToId());
			to.setTransAmt(lock.getTransInt().add(lock.getTransRadix()).setScale(2, BigDecimal.ROUND_HALF_UP));
			to.setStatus(lock.getStatus());
			to.setStatusMsg(TransLock.getStatusMsg(lock.getStatus()));
			if (isTCH(currMillis, rptTm, lock)) {
				to.setStatus(TransLock.STATUS_ACK_);
			}
			to.setCreateTime(new Date(lock.getTime()));
			to.setLtTime(new Date(lock.getLtTime()));
			ret.add(to);
		}
		Collections.sort(ret, (o1, o2) -> o2.getLtTime().compareTo(o1.getLtTime()));
		return ret;
	}

	/**
	 * get all transaction which transfer to out-account at present.
	 *
	 * @param operator
	 *            the operator at current action.
	 * @see com.xinbo.fundstransfer.domain.pojo.TransTo
	 */
	@Override
	public List<TransTo> buildTransTo4Transing(SysUser operator) {
		List<TransTo> ret = new ArrayList<>();
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Set<String> keys = template.keys(RedisKeys.genPattern4TransferAccountLock());
		Integer zone = handiSer.findZoneByHandiId(operator.getHandicap());
		for (String key : keys) {
			TransLock lock = new TransLock(key);
			AccountBaseInfo tobase = accSer.getFromCacheById(lock.getToId());
			// check the OutBank| Admin|Zone
			if (Objects.isNull(tobase) || !Objects.equals(operator.getCategory(), UserCategory.ADMIN.getValue())
					&& !Objects.equals(zone, handiSer.findZoneByHandiId(tobase.getHandicapId()))) {
				continue;
			}
			AccountBaseInfo frbase = accSer.getFromCacheById(lock.getFrId());
			if (Objects.isNull(frbase)) {
				continue;
			}
			if (!Objects.equals(lock.getStatus(), TransLock.STATUS_ALLOC)
					&& !Objects.equals(lock.getStatus(), TransLock.STATUS_CLAIM)) {
				continue;
			}
			TransTo to = new TransTo();
			to.setFrId(lock.getFrId());
			to.setFrType(frbase.getType());
			to.setFrHandicapId(frbase.getHandicapId());
			BizHandicap frHandi = handiSer.findFromCacheById(frbase.getHandicapId());
			to.setFrHandicapName(Objects.isNull(frHandi) ? StringUtils.EMPTY : frHandi.getName());
			to.setFrCsl(Objects.isNull(frbase.getCurrSysLevel()) ? Outter : frbase.getCurrSysLevel());
			to.setFrAlias(frbase.getAlias());
			to.setFrAcc(frbase.getAccount());
			to.setFrBankType(frbase.getBankType());
			to.setFrOwner(frbase.getOwner());
			SysUser user = userService.findFromCacheById(lock.getOprId());
			to.setUsername(user.getUid());

			to.setToId(lock.getToId());
			to.setToHandicapId(tobase.getHandicapId());
			BizHandicap toHandi = handiSer.findFromCacheById(tobase.getHandicapId());
			to.setToHandiCapName(Objects.isNull(toHandi) ? StringUtils.EMPTY : toHandi.getName());
			to.setToType(tobase.getType());
			to.setToCsl(Objects.isNull(tobase.getCurrSysLevel()) ? Outter : tobase.getCurrSysLevel());
			to.setToAlias(tobase.getAlias());
			to.setToAcc(tobase.getAccount());
			to.setToBankType(tobase.getBankType());
			to.setToOwner(tobase.getOwner());
			to.setOrderNo(buildOrderNo(lock.getFrId(), lock.getToId(), lock.getTransInt(), lock.getTransRadix()));
			to.setTransAmt(lock.getTransInt().add(lock.getTransRadix()));
			to.setLtTime(new Date(lock.getLtTime()));
			to.setStatus(lock.getStatus());
			to.setStatusMsg(TransLock.getStatusMsg(lock.getStatus()));
			to.setCreateTime(new Date(lock.getTime()));
			to.setPriority(null);
			to.setTimeConsume(CommonUtils.convertTime2String(System.currentTimeMillis() - lock.getTime()));
			to.setRemark(
					StringUtils.trimToEmpty(template.boundValueOps(RedisKeys.genKey4LockRem(to.getOrderNo())).get())
							.replace("\r\n", "<br>"));
			ret.add(to);
		}
		Collections.sort(ret, Comparator.comparing(TransTo::getCreateTime));
		return ret;
	}

	/**
	 * get all out-account needing funds at present.
	 *
	 * @param operator
	 *            the operator at current action.
	 * @see com.xinbo.fundstransfer.domain.pojo.TransTo
	 */
	@Override
	public List<TransTo> buildTransTo4Needing(SysUser operator) {
		List<TransTo> ret = new ArrayList<>();
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Set<ZSetOperations.TypedTuple<String>> getAll = template.boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI)
				.rangeWithScores(0, -1);
		Integer category = operator.getCategory();
		Integer zone = handiSer.findZoneByHandiId(operator.getHandicap());
		Set<String> evading = findBlackList();
		for (ZSetOperations.TypedTuple<String> zet : getAll) {
			Number[] ifs = deScore4Out(zet.getScore());// [0]priority[1]tm[2]need
			AccountBaseInfo tobase = accSer.getFromCacheById(Integer.valueOf(zet.getValue()));
			Integer frZone = handiSer.findZoneByHandiId(tobase.getHandicapId());
			if (category != UserCategory.ADMIN.getValue() && !Objects.equals(zone, frZone)) {
				continue;// 不是超级管理员只能看自己所在区域的数据
			}
			if (!checkBlack(evading, "0", tobase.getId())) {
				continue;// 转入黑名单的账号不纳入统计
			}
			TransTo to = new TransTo();
			to.setToId(tobase.getId());
			to.setToHandicapId(tobase.getHandicapId());
			BizHandicap toHandi = handiSer.findFromCacheById(tobase.getHandicapId());
			to.setToHandiCapName(Objects.isNull(toHandi) ? StringUtils.EMPTY : toHandi.getName());
			to.setToType(tobase.getType());
			to.setToCsl(Objects.isNull(tobase.getCurrSysLevel()) ? Outter : tobase.getCurrSysLevel());
			to.setToAlias(tobase.getAlias());
			to.setToAcc(tobase.getAccount());
			to.setToBankType(tobase.getBankType());
			to.setToOwner(tobase.getOwner());
			// to.setOrderNo(buildOrderNo(lock));
			to.setTransAmt(new BigDecimal(ifs[2].intValue()));
			to.setCreateTime(new Date(ifs[1].longValue()));
			to.setLtTime(to.getCreateTime());
			// 待下发由第三方下发还是其他卡下发
			List<Integer> needThirdList = accSer.needThirdDrawToOutCardIds();
			if (needThirdList.contains(tobase.getId())) {
				to.setPriority(1);
			} else {
				to.setPriority(2);
			}
			// to.setPriority(ifs[0].intValue());
			to.setTimeConsume(
					CommonUtils.convertTime2String(System.currentTimeMillis() - to.getCreateTime().getTime()));
			// to.setRemark(
			// StringUtils.trimToEmpty(template.boundValueOps(RedisKeys.genKey4LockRem(to.getOrderNo())).get())
			// .replace("\r\n", "<br>"));
			ret.add(to);
		}
		Collections.sort(ret,
				(o1, o2) -> Objects.equals(o1.getPriority(), o2.getPriority())
						? o1.getCreateTime().compareTo(o2.getCreateTime())
						: o1.getPriority().compareTo(o2.getPriority()));
		return ret;
	}

	/**
	 * fill up remark to TransLock
	 *
	 * @param operator
	 *            the user filling up remark to TransLock
	 * @param orderNo
	 *            orderNo the TransLock's order Number.
	 * @param remark
	 *            the remark from the operator
	 */
	@Override
	public void remark4TransLock(SysUser operator, String orderNo, String remark) {
		if (Objects.isNull(operator) || StringUtils.isBlank(orderNo) || StringUtils.isBlank(remark)) {
			log.error("remark4TransLock (orderNo : {} , remark : {} ) >> the operator | orderNo | remark is empty.",
					orderNo, remark);
			return;
		}
		String key = RedisKeys.genKey4LockRem(orderNo);
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		remark = CommonUtils.genRemark(template.boundValueOps(key).get(), remark, new Date(), operator.getUid());
		template.boundValueOps(key).set(remark, 90, TimeUnit.MINUTES);
	}

	/**
	 * fill up remark to Trans Request.
	 *
	 * @param operator
	 *            the user filling up remark to Trans Request.
	 * @param reqId
	 *            the identity of the trans request.
	 * @param remark
	 *            the remark from the operator
	 */
	@Override
	public void remark4TransReq(SysUser operator, Long reqId, String remark) {
		if (Objects.isNull(operator) || Objects.isNull(reqId) || StringUtils.isBlank(remark)) {
			log.error("remark4TransReq (reqId : {} , remark : {} ) >> the operator | orderNo | remark is empty.", reqId,
					remark);
			return;
		}
		BizIncomeRequest req = inReqSer.get(reqId);
		if (Objects.isNull(req)) {
			log.error("remark4TransReq (reqId : {} , remark : {} ) >> the trans request doesn't exist..", reqId,
					remark);
			return;
		}
		req.setRemark(CommonUtils.genRemark(req.getRemark(), remark, new Date(), operator.getUid()));
		inReqSer.update(req);
	}

	/**
	 * cancel the transaction acknowledge record
	 *
	 * @param operator
	 *            the operator at present
	 * @param reqId
	 *            the income request ID
	 */
	@Override
	public void cancelTransAck(SysUser operator, long reqId) {
		if (Objects.isNull(operator)) {
			log.info("cancelTransAck >> (reqId:{}) the operator is empty. ", reqId);
			return;
		}
		BizIncomeRequest req = inReqSer.get(reqId);
		if (Objects.isNull(req)) {
			log.info("cancelTransAck >> (operator:{} reqId:{}) the income request doesn't exist. ", operator.getUid(),
					reqId);
			return;
		}
		String kp = RedisKeys.genPattern4TransferAccountLock(req.getFromId(), req.getToId(),
				AppConstants.USER_ID_4_ADMIN);
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Set<String> keys = template.keys(kp);
		if (!CollectionUtils.isEmpty(keys)) {
			for (String key : keys) {
				TransLock lock = new TransLock(key);
				if (Objects.equals(TransLock.STATUS_ACK, lock.getStatus())
						&& lock.getTransInt().intValue() == req.getAmount().intValue()) {
					template.delete(key);
				}
			}
		}
		req.setRemark(CommonUtils.genRemark(req.getRemark(), "成功转失败", new Date(), operator.getUid()));
		req.setStatus(IncomeRequestStatus.Canceled.getStatus());
		inReqSer.update(req);
		// 转失败后要更新biz_transaction_log中对应的数据
		transactionService.updateByFromIdToIdAmount(req.getFromId(), req.getToId(), req.getAmount());

	}

	/**
	 * get the account's real balance in cache
	 * <p>
	 * if the real balance doesn't exist. null as result.
	 * </p>
	 *
	 * @param accId
	 *            the account's ID
	 * @return the account's real balance
	 */
	@Override
	public BigDecimal buildRealBalInCache(Integer accId) {
		if (Objects.isNull(accId)) {
			return null;
		}

		Object ret = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_REAL_BAL).get(accId.toString());
		return Objects.isNull(ret) ? null : new BigDecimal((String) ret).setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * generate an orderNo for TransLock
	 */
	private String buildOrderNo(Object frId, int toId, BigDecimal transInt, BigDecimal transRadix) {
		return frId + "O" + toId + "O" + transInt.intValue() + "O"
				+ transRadix.multiply(BigDecimal.TEN.multiply(BigDecimal.TEN)).divideAndRemainder(BigDecimal.ONE)[0]
						.intValue();
	}

	private void flushLockLog(Object frId, int toId, BigDecimal transInt, BigDecimal transRadix) {
		redisSer.getStringRedisTemplate()
				.delete(RedisKeys.genKey4LockRem(buildOrderNo(frId, toId, transInt, transRadix)));
	}

	/**
	 * get transfer statistics information
	 */
	@Override
	public List<MonitorStat> buildTransStat(SysUser user) {
		List<MonitorStat> dataList = new ArrayList<>();
		Map<String, MonitorStat> restmp = new HashMap<>();
//		Map<Integer, MonitorStat> restmp = new HashMap<>();
		Integer category = user.getCategory();
		Integer zone = handiSer.findZoneByHandiId(user.getHandicap());
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		// 出款卡\入款\下发\备用卡数账户信息
		List<SearchFilter> filterToList = new ArrayList<>();
		List<Integer> accType = new ArrayList<>();
		accType.add(OUTBANK);
		accType.add(INBANK);
		accType.add(INTHIRD);
		accType.add(RESERVEBANK);
		accType.add(BINDCOMMON);
		accType.add(THIRDCOMMON);
		accType.add(BINDALI);
		accType.add(BINDWECHAT);
		accType.add(BINDWECHAT);
		filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, accType.toArray()));
		List<Integer> accStatus = new ArrayList<>();
		accStatus.add(AccountStatus.Normal.getStatus());
		accStatus.add(AccountStatus.Enabled.getStatus());
		accStatus.add(AccountStatus.StopTemp.getStatus());
		filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, accStatus.toArray()));
		SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
		List<Object[]> accountIdAndBalList = accSer.findAccountIdAndBalList(filterToArray);
		String data = sysDataPermissionService.findUserHandicapFromCache(user.getId());
		for (Object[] item : accountIdAndBalList) {
			Integer id = (Integer) item[0];
			BigDecimal bal = (BigDecimal) item[1];
			if (Objects.isNull(id))
				continue;
			if (Objects.isNull(bal)) {
				bal = BigDecimal.ZERO;
			}
			AccountBaseInfo base = accSer.getFromCacheById(id);
			Integer type = base.getType();
			if (category != -1 && !data.contains(";" + base.getHandicapId() + ";")) {
				continue;// 不是超级管理员只能看自己所拥有的盘口权限的数据
			}
			MonitorStat stat = restmp.get(type + "-" + base.getStatus());
//			MonitorStat stat = restmp.get(type);
			if (Objects.isNull(stat)) {
				stat = new MonitorStat();
				stat.setAccType(type);
				stat.setAccTypeAndStatus(type + "-" + base.getStatus());
				restmp.put(type + "-" + base.getStatus(), stat);
//				restmp.put(type, stat);
			}
			stat.setTotalBal(stat.getTotalBal() + bal.floatValue());
			stat.setTotalNum(stat.getTotalNum() + 1);
		}
		// 出款卡需要的数据
		Set<ZSetOperations.TypedTuple<String>> getAll = template.boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI)
				.rangeWithScores(0, -1);
		Set<String> evading = findBlackList();
		for (ZSetOperations.TypedTuple<String> zet : getAll) {
			float need = deScore4Out(zet.getScore())[2].floatValue();// [0]priority[1]tm[2]need
			AccountBaseInfo base = accSer.getFromCacheById(Integer.valueOf(zet.getValue()));
			// 转入黑名单的账号不纳入统计
			if (!checkBlack(evading, "0", base.getId())) {
				continue;
			}
			// 不是超级管理员只能看自己所在区域的数据
			if (category != -1 && zone != handiSer.findZoneByHandiId(base.getHandicapId())) {
				continue;
			}
			MonitorStat stat = restmp.get(OUTBANK + "-" + base.getStatus());
//			MonitorStat stat = restmp.get(OUTBANK);
			if (Objects.isNull(stat)) {
				stat = new MonitorStat();
//				stat.setAccType(OUTBANK);
				stat.setAccTypeAndStatus(OUTBANK + "-" + base.getStatus());
			}
			stat.setAccNum(stat.getAccNum() + 1);
			stat.setAmount(stat.getAmount() + need);
			restmp.put(OUTBANK + "-" + base.getStatus(), stat);
//			restmp.put(OUTBANK, stat);
		}
		for (Map.Entry<String, MonitorStat> entry : restmp.entrySet()) {
			dataList.add(entry.getValue());
		}
		return dataList;
	}

	public boolean checkDailyIn(AccountBaseInfo base) {
		if (base == null) {
			return true;
		}
		// 入款卡才校验入款限额
		if (base == null || !Objects.equals(Constants.INBANK, base.getType())) {
			return true;
		}
		int income0outward1 = 0;
		float InDaily = accSer.findAmountDailyByTotal(income0outward1, base.getId()).floatValue();
		if (base.getLimitIn() != null) {
			return base.getLimitIn() > InDaily + CommonUtils.getLessThenSumDailyIncome();
		}
		return true;
	}

	@Override
	public boolean checkDailyIn2(Map<Integer, BigDecimal> dailyIn, AccountBaseInfo base) {
		Assert.notNull(base, "账号信息为空");
		Assert.notNull(dailyIn, "查询的所有账号的日入款数据为空");

		if (!Objects.equals(Constants.INBANK, base.getType()) || base.getLimitIn() == null)
			return true;

		if (!CollectionUtils.isEmpty(dailyIn)) {
			BigDecimal dailyInALready = dailyIn.get(base.getId());
			log.debug("账号id:{} 当日入款总金额:{}", base.getId(), dailyInALready);
			if (dailyInALready != null) {
				return base.getLimitIn() > dailyInALready.floatValue() + CommonUtils.getLessThenSumDailyIncome();
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * get the system upper|trigger limit of out account
	 * <p>
	 * if the setting doesn't exist, return null.
	 * </p>
	 *
	 * @return [0]:the system upper limit amount</br>
	 *         [1]:the system trigger limit amount</br>
	 * @see this#getUpLim4ONeed(int)
	 * @see this#setUpLim4ONeed(int, boolean, int, int, int)
	 */
	private Integer[] buildUpAndTrigLim(int zone) {
		String key = RedisKeys.genKey4AllocTransUpLim(zone);
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Double scr4up = template.boundZSetOps(key).score(UP_LIM_UP_LIM);
		if (Objects.isNull(scr4up))
			return null;
		Double scr4trig = template.boundZSetOps(key).score(UP_LIM_TRIG_LIM);
		if (Objects.isNull(scr4trig))
			return null;
		return new Integer[] { scr4up.intValue(), scr4trig.intValue() };
	}

	/**
	 * 下发调度器
	 *
	 * @return false:无调度任务;</br>
	 *         true:有调度任务</br>
	 */
	private boolean control() {
		// 有问题流水
		Set<Integer> accAlarm = buildAcc4Alarm();
		accAlarm = accAlarm == null ? new HashSet<>() : accAlarm;
		Set<Integer> invSet = systemAccountManager.alarm4AccountingInOut(false);
		Set<Integer> outvSet = systemAccountManager.alarm4AccountingInOut(true);
		if (!CollectionUtils.isEmpty(invSet)) {
			for (Integer id : invSet) {
				AccountBaseInfo base = accSer.getFromCacheById(id);
				if (base != null && base.getHolder() == null) {
					accAlarm.add(id);
				}
			}
		}
		log.debug("下发黑名单：" + accAlarm + " 转出黑名单：" + outvSet);
		// 出款卡>>
		int minBal = buildMinBal(true).intValue();
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Double[] infrto = new Double[] { enScore4Fr(INBANK, 0, 0, 0, 0), enScore4Fr(INBANK + 1, 0, 0, 0, 0) };
		Set<ZSetOperations.TypedTuple<String>> inBankS = template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
				.rangeByScoreWithScores(infrto[0], infrto[1]);
		// 入款卡:>>
		// 1、取出入款卡数据
		boolean enableOut = CommonUtils.isEnableInBankOutTask();
		if (enableOut) {
			alloc4InOverCom(inBankS, outvSet);
		}
		List<ZSetOperations.TypedTuple<String>> inBankL = inBankS.stream().collect(Collectors.toList());
		List<Number[]> oneedList = new ArrayList<>();// [0]ID;[1]score;[2]priority;[3]tm;[4]need
		Set<ZSetOperations.TypedTuple<String>> valScrSet = template.boundZSetOps(RedisKeys.ALLOC_OUT_NEED_ORI)
				.rangeWithScores(0, -1);
		if (!CollectionUtils.isEmpty(valScrSet)) {
			for (ZSetOperations.TypedTuple<String> valScr : valScrSet) {
				double scr = valScr.getScore();
				Number[] infs = deScore4Out(scr);
				Number[] ret = Arrays.copyOf(new Number[] { Integer.valueOf(valScr.getValue()), scr }, infs.length + 2);
				System.arraycopy(infs, 0, ret, 2, infs.length);
				oneedList.add(ret);
			}
			oneedList.sort((o1, o2) -> {
				double d = o1[1].doubleValue() - o2[1].doubleValue();
				return d > 0 ? 1 : (d < 0 ? -1 : 0);
			});
		}
		int len4o = oneedList.size();
		Set<String> evading = findBlackList();
		// 支付宝入款卡下发到出款卡
		if (len4o > 0) {
			alloc4MobileInOver(inBankL, oneedList, accAlarm, evading, outvSet);
		}
		for (Number[] alloc : oneedList) {// [0]ID;[1]score;[2]priority;[3]tm;[4]need
			if (Objects.isNull(alloc)) {
				break;
			}
			int oId = alloc[0].intValue(), oPri = alloc[2].intValue(), oNd = alloc[4].intValue();
			long oTm = alloc[3].longValue();
			String tar = String.valueOf(oId);
			AccountBaseInfo oAcc = accSer.getFromCacheById(oId);
			if (Objects.isNull(oAcc) || !Objects.equals(oAcc.getStatus(), NORMAL)) {
				ldel4ONeed(tar);
				log.trace("AllocOAcc >> the oAcc doesn't exist | not Normal Status. id:{}", oId);
				continue;
			}
			if (accAlarm.contains(oId)) {
				log.trace("AllocOAcc >> the acc （{}） have flows that need handle.", oId);
				continue;
			}
			boolean dsct = handiSer.checkDistHandi(oAcc.getHandicapId());
			Integer frl = oAcc.getCurrSysLevel();
			frl = Objects.isNull(frl) ? CurrentSystemLevel.Outter.getValue() : frl;
			Integer frz = handiSer.findZoneByHandiId(oAcc.getHandicapId());
			int shareSize = buildShare(), shareNo = oNd / shareSize;
			int inMin = oNd + minBal;
			int inMax = inMin + shareSize;
			int inMax_ = 99999;
			boolean ret = false;
			for (int no = shareNo; no >= 0; no--) {
				if (no != shareNo) {
					inMax = inMin;
					inMax_ = inMax;
					inMin = shareNo == 0 ? 0 : inMax - shareSize;
				}
				ret = alloc4ONeed(oId, oPri, oTm, oNd, false, frl, frz, BINDCOMMON, inMin, inMax_, evading, true,
						outvSet);// 下发卡（下发卡）
				if (ret)
					break;
				ret = alloc4ONeed(oId, oPri, oTm, oNd, false, frl, frz, THIRDCOMMON, inMin, inMax_, evading, true,
						outvSet);// 下发卡(第三方专用)
				if (ret)
					break;
			}
			if (ret)
				continue;
			inMin = oNd + minBal;
			inMax = inMin + shareSize;
			inMax_ = 99999;
			for (int no = shareNo; no >= 0; no--) {
				if (no != shareNo) {
					inMax = inMin;
					inMax_ = inMax;
					inMin = shareNo == 0 ? 0 : inMax - shareSize;
				}
				ret = alloc4ONeed(oId, oPri, oTm, oNd, dsct, frl, frz, INBANK, inMin, inMax, evading, false, outvSet);// 入款卡
				if (ret)
					break;
				ret = alloc4ONeed(oId, oPri, oTm, oNd, false, frl, frz, BINDALI, inMin, inMax_, evading, false,
						outvSet);// 下发卡（支付宝）
				if (ret)
					break;
				ret = alloc4ONeed(oId, oPri, oTm, oNd, false, frl, frz, BINDWECHAT, inMin, inMax_, evading, false,
						outvSet);// 下发卡(微信)
				if (ret)
					break;
				ret = alloc4ONeed(oId, oPri, oTm, oNd, dsct, frl, frz, RESERVEBANK, inMin, inMax_, evading, false,
						outvSet);// 备用卡
				if (ret)
					break;
				ret = alloc4ONeed(oId, oPri, oTm, oNd, false, frl, frz, BINDCOMMON, inMin, inMax_, evading, false,
						outvSet);// 下发卡（下发卡）
				if (ret)
					break;
				ret = alloc4ONeed(oId, oPri, oTm, oNd, false, frl, frz, THIRDCOMMON, inMin, inMax_, evading, false,
						outvSet);// 下发卡(第三方专用)
				if (ret)
					break;
			}
		}
		int len4i = inBankL.size();
		boolean needAlloIn = len4i > 0;
		// 2、取出备用卡数据
		Double[] refrto = new Double[] { enScore4Fr(RESERVEBANK, 0, 0, 0, 0), enScore4Fr(RESERVEBANK + 1, 0, 0, 0, 0) };
		Set<ZSetOperations.TypedTuple<String>> reBankS = template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
				.rangeByScoreWithScores(refrto[0], refrto[1]);
		// 先备用卡余额大于20K卡先找出款任务
		if (enableOut) {
			alloc4ROver20K(reBankS, outvSet);
		}
		Map<Integer, Integer> ids = new HashMap<>();
		Map<ZSetOperations.TypedTuple<String>, Integer> rBal = new HashMap<>(); // 备用卡金额超过峰值的数据
		Integer peekBal = CommonUtils.getReserveToReserveBalance();
		Integer minAmount = CommonUtils.getReserveTOReserveMinAmount();
		for (ZSetOperations.TypedTuple<String> id : reBankS) {
			Integer bal = deScore4Fr(id.getScore())[4];
			Integer subR = bal - peekBal;
			if (subR > minAmount) {
				rBal.put(id, subR);
				needAlloIn = true;
			}
			Integer accId = Integer.parseInt(id.getValue());
			AccountBaseInfo base = accSer.getFromCacheById(accId);
			// 手机银行备用卡下发多笔未到账时，不再给对应的备用卡下发
			if (base.checkMobile() && !transactionService.checkNonCredit(base)) {
				log.debug("reserve bank has more than one trade not arrive,acc id {}", accId);
				continue;
			}
			ids.put(accId, bal);
		}
		int len4r = 0;
		// 要进行下发分配时，才触发下发分配操作
		if (needAlloIn) {
			// 3、取出备用卡预计金额数据
			Map<Integer, Integer> retMap = buildExpAmt(ids); // 备用卡的预计金额在备用卡转备用卡时还需要使用
			buildExpAmtWithWeight(retMap); // 根据备用卡日收款累计金额加权计算预期金额
			String enableInbankHandicap = CommonUtils.getEnableInBankHandicap();
			// 所有盘口都开启入款卡、备用卡出款功能时，在入款卡、备用卡下发时不需要取出款卡的数据
			if (!"ALL".equals(enableInbankHandicap)) {
				Double[] outfrto = new Double[] { enScore4Fr(OUTBANK, 0, 0, 0, 0),
						enScore4Fr(OUTBANK + 1, 0, 0, 0, 0) };
				Set<ZSetOperations.TypedTuple<String>> outBankS = template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
						.rangeByScoreWithScores(outfrto[0], outfrto[1]);
				// 出款卡数据加到被帅选的集合中
				reBankS.addAll(outBankS);
				// 5、取出出款卡预计金额数据
				Map<Integer, Integer> outids = new HashMap<>();
				for (ZSetOperations.TypedTuple<String> id : outBankS) {
					outids.put(Integer.parseInt(id.getValue()), deScore4Fr(id.getScore())[4]);
				}
				Map<Integer, Integer> outMapTmp = buildExpAmt(outids);
				// 出款卡的预计金额为出款卡峰值-出款卡预计金额，大于3000时，才放入Map中，放入的数据为 出款卡预计金额 - 峰值，负数
				for (Map.Entry<Integer, Integer> acc : outMapTmp.entrySet()) {
					AccountBaseInfo base = accSer.getFromCacheById(acc.getKey());
					Integer outPeak = buildPeak(base);
					if (outPeak - acc.getValue() > 3000 && checkBankStatement(base)) {
						log.debug("acc {} can receive {},add to the select map", acc.getKey(),
								peekBal - acc.getValue());
						retMap.put(acc.getKey(), acc.getValue() - outPeak);
					}
				}
			}
			len4i = inBankL.size();
			// 6、入款卡下发分配
			for (int index = len4i - 1; index >= 0; index--) {
				ZSetOperations.TypedTuple<String> tasking = inBankL.get(index);
				if (tasking == null) {
					continue;
				}
				alloc4IOver(tasking, evading, reBankS, retMap, accAlarm, outvSet);
			}

			// 7、备用卡下发分配
			List<Map.Entry<ZSetOperations.TypedTuple<String>, Integer>> ret = new ArrayList<>(rBal.entrySet());
			Collections.sort(ret, Comparator.comparing(Map.Entry::getValue));
			len4r = ret.size();
			for (int index = len4r - 1; index >= 0; index--) {
				ZSetOperations.TypedTuple<String> tasking = ret.get(index).getKey();
				if (tasking == null) {
					continue;
				}
				alloc4IOver(tasking, evading, reBankS, retMap, accAlarm, outvSet);
			}
		}
		return len4o > 0 || len4i > 0 || len4r > 0;
	}

	/**
	 * allocate transfer task for out-account
	 *
	 * @param oId
	 *            out-account ID
	 * @param oPri
	 *            priority
	 * @param oTm
	 *            time
	 * @param oNd
	 *            the amount the out-account needed.
	 * @param dsct
	 *            true: distinguish handicap</br>
	 *            false: indistinguish handicap
	 * @param frl
	 *            the from-account level(Ouuter,Middle,Inner)
	 * @param frz
	 *            the from-account zone
	 * @param frt
	 *            the from-account type
	 * @param stAmt
	 *            下发金额起始范围
	 * @param edAmt
	 *            下发金额结束范围
	 * @return 已分配金额; 0：未分配
	 * @see com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo
	 * @see com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType
	 */
	private boolean alloc4ONeed(Integer oId, Integer oPri, long oTm, Integer oNd, boolean dsct, int frl, int frz,
			int frt, int stAmt, int edAmt, Set<String> evading, Boolean mobileFirst, Set<Integer> outvSet) {
		AccountBaseInfo oAcc = accSer.getFromCacheById(oId);
		if (Objects.isNull(oAcc)) {
			ldel4ONeed(String.valueOf(oId));
			log.trace(
					"alloc4ONeed  >> the out account doesn't exist. oId:{},oPri:{},oTm:{},oNd:{},dist:{},frL:{},frZ:{},frT:{}",
					oId, oPri, oTm, oNd, dsct, frl, frz, frt);
			return true;
		}
		int oHandi = buildHandi(oAcc.getHandicapId());
		Double[] scrFil = score4OFil(oHandi, dsct, frl, frz, frt, stAmt, edAmt);
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Set<ZSetOperations.TypedTuple<String>> getAll = template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
				.rangeByScoreWithScores(scrFil[0], scrFil[1]);
		if (CollectionUtils.isEmpty(getAll)) {
			log.trace("alloc4ONeed  >> no available from-account at present. frL:{},frZ:{},frT:{}", frl, frz, frt);
			return false;
		}
		int toler = buildToler(), minBal = buildMinBal(true).intValue(), over = buildOver(oAcc, false);
		String tar = String.valueOf(oId);
		List<Number[]> gets = order4ONeed(frt, minBal, oNd, getAll, mobileFirst);// [0]id;[1]type;[2]zone;[3]l;[4]handi;[5]bal
		for (Number[] get : gets) {
			int frId = get[0].intValue();
			AccountBaseInfo fr = accSer.getFromCacheById(frId);
			// verify null
			if (Objects.isNull(fr)) {
				log.trace("alloc4ONeed  >> the from account ({}) doesn't exist. ", frId);
				continue;
			}
			if (outvSet != null && outvSet.contains(frId)) {
				continue;
			}
			// verify account type & account status
			if (!Objects.equals(fr.getType(), frt) || NORMAL != fr.getStatus()) {
				log.trace("alloc4ONeed  >> the from account ({}) type|status not right. frType:{},frStatus:{},frT:{}",
						frId, fr.getType(), fr.getStatus(), frt);
				continue;
			}
			int frHandi = buildHandi(fr.getHandicapId());
			// 入款卡、备用卡所在盘口开通入款卡、备用卡出款，则不将对应的数据分配到出款卡中
			if (frt == RESERVEBANK || frt == INBANK || frt == BINDCUSTOMER) {
				if (CommonUtils.checkEnableInBankHandicap(frHandi)) {
					continue;
				}
			}
			// verify hanicap
			if (dsct && (WILD_CARD_HANDI == frHandi || WILD_CARD_HANDI == oHandi || frHandi != oHandi)) {
				continue;
			}
			// verify black List
			if (!checkBlack(evading, String.valueOf(fr.getId()), oId)) {
				continue;
			}
			// verify peer agency tranfer
			if (Objects.nonNull(allOTaskSer.checkPeerTrans(oAcc.getBankType()))) {
				continue;
			}
			// verify maintenance
			if (allOTaskSer.checkMaintain(oAcc.getBankType())) {
				continue;
			}
			Integer iReal = get[5].intValue();
			Integer tInt = Math.min(iReal - minBal, oNd);
			tInt = (iReal - minBal - tInt) > over ? tInt : (iReal - minBal);
			// verify the transfer lowest limit amount.
			if (tInt < MAX_TOLERANCE) {
				continue;
			}
			try {
				lockTrans(fr.getId(), oAcc.getId(), AppConstants.USER_ID_4_ADMIN, tInt);
				log.info("Alloc4Trans >> O( {} , {} ) transFr{} transTo{} transInt:{}", fr.getId(), oAcc.getId(),
						fr.getId(), oAcc.getId(), tInt);
				// post operation for from account
				if (RESERVEBANK != frt || get[5].intValue() >= buildHigh(fr)) {
					template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM).remove(String.valueOf(fr.getId()));
					log.debug("alloc4ONeed  >> delete the fr-acc allocated. frId:{},toId:{},transInt:{}", fr.getId(),
							oAcc.getId(), tInt);
				}
				// post operation for to account
				ldel4ONeed(tar);
				return true;
			} catch (Exception e) {
				log.debug("alloc4ONeed  >> lock trans fail. frId:{},toId:{},amt:{}", fr.getId(), oAcc.getId(), tInt);
			}
		}
		return false;
	}

	/**
	 * 入款卡、备用卡超额时下发到备用卡
	 *
	 * @param tasking
	 *            待分配的入款/备用卡数据
	 * @param blacklist
	 *            黑名单数据
	 * @param all
	 *            待分配的备用卡数据
	 * @param expBal
	 *            预计备用卡金额数据
	 */
	private void alloc4IOver(ZSetOperations.TypedTuple<String> tasking, Set<String> blacklist,
			Set<ZSetOperations.TypedTuple<String>> all, Map<Integer, Integer> expBal, Set<Integer> accAlarm4Flow,
			Set<Integer> outvSet) {
		int frAccId = Integer.valueOf(tasking.getValue());
		if (CollectionUtils.isEmpty(all)) {
			log.trace("Issued Alloc{} Regular all is null", frAccId);
			return;
		}
		if (outvSet != null && outvSet.contains(frAccId)) {
			log.trace("Issued Alloc{} Acc has one trans out haven't been confirmed", frAccId);
			return;
		}
		AccountBaseInfo frAb = accSer.getFromCacheById(frAccId);
		// [0]type [1]zone [2]level [3]handicap [4]bal
		Integer[] n = deScore4Fr(tasking.getScore());
		int frType = frAb.getType();
		// 账户余额没超过余额告警值 不进行下发
		int limitBalance = buildHigh(frAb);
		if (frType == INBANK && n[4] < limitBalance) {
			log.trace(
					"acc {} balance {} is not bigger then limitbalance {} | limitbalance is null and bal less then 5000",
					frAccId, n[4], limitBalance);
			return;
		}
		if (frType != INBANK && frType != RESERVEBANK && frType != BINDCUSTOMER) {
			log.trace("acc {} Type in cache {} is invalid,Type in redis {}", frAccId, frType, n[0]);
			return;
		}
		Boolean peerTrans = allOTaskSer.checkPeerTrans(frAb.getBankType());
		if (peerTrans == null) {
			boolean maintain = allOTaskSer.checkMaintain(frAb.getBankType());
			if (maintain) {
				log.debug("Issued Alloc{} Regular Bank {} is maintain", frAccId, frAb.getBankType());
				return;
			}
		}
		// 开启入款卡、备用卡出款时，入款卡转备用卡是区分盘口的
		Integer frHandicap = frAb.getHandicapId();
		boolean isEnableInBankHandicap = CommonUtils.checkEnableInBankHandicap(frHandicap);
		// 开启入款备用客户绑定卡出款时，备用卡只有大于峰值 + 备用转备用最低金额时，才走下发逻辑
		if (isEnableInBankHandicap && frType == RESERVEBANK
				&& n[4] < frAb.getPeakBalance() + CommonUtils.getReserveTOReserveMinAmount()) {
			return;
		}
		if (frType == RESERVEBANK && !systemAccountManager.check4AccountingOut(frAccId)) {
			log.info("applyByIn (id:{}) >> There is trans out have not confirm", frAccId);
			return;
		}
		int handistart = isEnableInBankHandicap ? frHandicap : 0;
		int handiend = isEnableInBankHandicap ? frHandicap : 999;
		// [0]type [1]zone [2]level [3]handicap [4]bal
		Double[] reFrTo = new Double[] { enScore4Fr(RESERVEBANK, n[1], n[2], handistart, 0),
				enScore4Fr(RESERVEBANK, n[1], n[2], handiend, 999999) };

		// 1、取备用卡的数据
		List<ZSetOperations.TypedTuple<String>> retId = all.stream()
				.filter(p -> p.getScore() >= reFrTo[0] && p.getScore() <= reFrTo[1])
				.sorted((o1, o2) -> o2.getScore().compareTo(o1.getScore())).collect(Collectors.toList());
		Map<Integer, Integer> retM = new TreeMap<>();
		// 入款卡、备用卡所在盘口未开启入款卡备用卡出款时，才能往出款卡下发
		if (!isEnableInBankHandicap) {
			// 2、取出款卡的数据
			Double[] outFrTo = new Double[] { enScore4Fr(OUTBANK, n[1], n[2], handistart, 0),
					enScore4Fr(OUTBANK, n[1], n[2], handiend, 999999) };
			List<ZSetOperations.TypedTuple<String>> outId = all.stream()
					.filter(p -> p.getScore() >= outFrTo[0] && p.getScore() <= outFrTo[1])
					.sorted((o1, o2) -> o2.getScore().compareTo(o1.getScore())).collect(Collectors.toList());
			// 出款卡数据放入结果集中
			for (ZSetOperations.TypedTuple<String> set : outId) {
				Integer key = Integer.parseInt(set.getValue());
				if (Objects.nonNull(expBal.get(key))) {
					retM.put(key, expBal.get(key));
				}
			}
		}
		// 备用卡数据放入结果集中
		for (ZSetOperations.TypedTuple<String> set : retId) {
			Integer key = Integer.parseInt(set.getValue());
			if (Objects.nonNull(expBal.get(key))) {
				retM.put(key, expBal.get(key));
			}
		}
		if (CollectionUtils.isEmpty(retM)) {
			log.trace("Issued Alloc{} Suitable retId is null", frAccId);
			return;
		}
		// map转换成list进行排序
		List<Map.Entry<Integer, Integer>> ret = new ArrayList<>(retM.entrySet());
		Collections.sort(ret, Comparator.comparing(Map.Entry::getValue));
		boolean allocated = false;
		int count = 0;
		do {
			// 分配不出去以后将权重数据清除掉
			if (count > 0) {
				for (Map.Entry<Integer, Integer> ent : expBal.entrySet()) {
					Integer bal = ent.getValue();
					if (bal > 100000000) {
						expBal.put(ent.getKey(), bal % 100000000);
					}
				}
			}
			for (Map.Entry<Integer, Integer> target : ret) {
				int tarAcc = target.getKey(); // 目标卡账号ID
				AccountBaseInfo tarAb = accSer.getFromCacheById(tarAcc);
				if (Objects.isNull(tarAb) || !Objects.equals(tarAb.getStatus(), NORMAL)) {
					log.debug("TransBlack{} InForce frId:{} >> the account doesn't exist | status isn't right.", tarAcc,
							frAccId);
					continue;
				}
				// 银行流水告警校验
				if (accAlarm4Flow.contains(tarAcc)) {
					log.debug("tar acc {} has problem with banklog or has one trans in haven't been confirmed", tarAcc);
					continue;
				}
				// 校验黑名单
				if (!checkBlack(blacklist, String.valueOf(frAccId), tarAcc)) {
					log.debug("TransBlack{} InForce frId:{}", tarAcc, frAccId);
					continue;
				}
				// 检测 同行转账
				if (!allOTaskSer.checkPeer(peerTrans, frAb, tarAb)) {
					log.debug("TransPeer{} InForce", tarAb.getId());
					continue;
				}
				// 备用卡找备用卡时，如果找到自身时则匹配不成功
				if (Objects.equals(frAb.getId(), tarAb.getId())) {
					log.debug("ReserveBank to ReserveBank from Acc {} is same as to Acc {}", frAb.getId(),
							tarAb.getId());
					continue;
				}
				// 备用卡所在盘口开启入款卡备用卡出款时，只能本盘口的入款、备用卡转入本备用卡
				int tarHandicapId = tarAb.getHandicapId();
				if (tarAb.getType() == RESERVEBANK) {
					boolean isTarEnableInBankHandicap = CommonUtils.checkEnableInBankHandicap(tarHandicapId);
					if (isTarEnableInBankHandicap && tarHandicapId != frHandicap) {
						log.debug(
								"Target Bank is ReserveBank & the handicap enable InBank to Outward & from handicap isn't equal tar handicap,fr acc {} handicap {} to acc {} handicap{}",
								frAccId, frHandicap, tarAcc, tarHandicapId);
						continue;
					}
				}
				// 备用卡手机银行峰值取卡本身设置的峰值，卡接收的余额不能超过峰值
				int peakBalance = tarAb.getType() == RESERVEBANK && tarAb.checkMobile() ? buildPeak(tarAb)
						: CommonUtils.getReserveToReserveBalance();
				// 检测通过能进行转账，处理转账信息
				int frBal = n[4]; // 转出账号余额
				int transInt = frBal - buildMinBal(true).intValue();// 转出账户能转出金额
				int expBalance = expBal.get(tarAb.getId());
				int toInt = expBalance > 100000000 ? (expBalance % 100000000) : expBalance; // 转入备用卡预计余额（由于存数据时有加权，这边取与100000000的余数为实际的预期金额）、转入出款卡能接收金额（负数）

				// 转出金额 + 出款卡能接收金额 > 2000 时，不下发到这张卡，下发到其他卡中
				if (tarAb.getType() == OUTBANK && transInt + toInt > 2000) {
					log.debug(
							"Target Bank is OutBank,The real bal add transInt more then peekBalance,frAcc {} toAcc {} can receive {} transInt {}",
							frAb.getId(), tarAb.getId(), Math.abs(toInt), transInt);
					continue;
				}
				// 防止一直给一张备用卡下发，超过峰值时，取下一个权重的备用卡数据，再次分配时不管是否超过峰值都可以分配
				// 手机银行备用卡不允许金额超过峰值
				if (tarAb.getType() == RESERVEBANK && toInt > peakBalance && (count == 0 || tarAb.checkMobile())) {
					log.debug(
							"Target Bank is ReserveBank,The exp bal is more then peekBalance,frAcc {} toAcc {} can receive {} transInt {}",
							frAb.getId(), tarAb.getId(), Math.abs(toInt), transInt);
					continue;
				}
				transInt = Math.min(transInt, (frAb.getBankType().equals(tarAb.getBankType()) ? 99000 : TRANS_MAX_PER)
						+ (new java.util.Random().nextInt(1000))); // 转账金额和最大单笔转出限制取最小值
				// 转入出款卡能接收金额、转入备用卡能接收的金额
				// 备用卡预期金额大于峰值时并且不是手机，取备用卡预期金额，否则取峰值 - 预期金额
				toInt = tarAb.getType() == OUTBANK ? Math.abs(toInt)
						: toInt > peakBalance && !tarAb.checkMobile() ? toInt : (peakBalance - toInt);
				if (frAb.getType() == RESERVEBANK) {
					transInt = frBal - peakBalance;// 转出账户能转出金额
					if (toInt < MAX_TOLERANCE && (frBal < 30000 || tarAb.checkMobile())) {
						log.debug(
								"ReserveBank to ReserveBank from Acc {} to Acc {} can't receive more then {} real need {}",
								frAb.getId(), tarAb.getId(), MAX_TOLERANCE, toInt);
						continue;
					}
					// 备用转备用时，如果两张备用卡的余额都超过峰值，并且余额差不超过20000，不进行互转
					if (toInt > peakBalance && tarAb.getType() == RESERVEBANK
							&& (frBal < toInt || frBal - toInt < 20000)) {
						log.debug(
								"ReserveBank to ReserveBank from Acc {} to Acc {} are more then peakBalance,the diference less then 20000,frBal {} toBal {}",
								frAb.getId(), tarAb.getId(), frBal, toInt);
						continue;
					}
					transInt = Math.min(transInt, toInt); // 转出金额和接收金额取最小值
					// 备用卡金额太大时，在两张备用卡中平分
					if (frBal > 30000 && tarAb.getType() == RESERVEBANK) {
						// (转出备用卡余额+转入备用卡余额/2)-转入备用卡当前余额
						// (frBal+(peakBalance-toInt))/2-(peakBalance-toInt)
						if (toInt > peakBalance) {
							transInt = Math.min(
									(frAb.getBankType().equals(tarAb.getBankType()) ? 99000 : TRANS_MAX_PER),
									(frBal - toInt) / 2);
						} else {
							// 不是手机银行时，在两张备用卡中平分，否则取之前算出的转账金额
							if (!tarAb.checkMobile()) {
								transInt = Math.min(
										frAb.getBankType().equals(tarAb.getBankType()) ? 99000 : TRANS_MAX_PER,
										frBal / 2);
							}
						}
					}
				} else if (tarAb.checkMobile() && frAb.getType() == INBANK && tarAb.getType() == RESERVEBANK) {
					transInt = Math.min(transInt, toInt); // 转出金额和接收金额取最小值
					if (transInt < MAX_TOLERANCE) {
						log.debug("InBank to ReserveBank from Acc {} to Acc {} can't trans less then {} real trans {}",
								frAb.getId(), tarAb.getId(), MAX_TOLERANCE, transInt);
						continue;
					}
				}
				// 备用转备用最低金额校验
				if (tarAb.getType() == RESERVEBANK && frType == RESERVEBANK
						&& transInt < CommonUtils.getReserveTOReserveMinAmount()) {
					continue;
				}
				try {
					lockTrans(frAccId, tarAcc, AppConstants.USER_ID_4_ADMIN, transInt);
					log.info("Alloc4Trans >> I( {} , {} ) transFr{} frHandicap {} transTo{} toHandicap {} transInt:{}",
							frAccId, tarAcc, frAccId, frHandicap, tarAcc, tarHandicapId, transInt);
					// 分配出去以后，将备用卡预计金额更新
					expBal.put(target.getKey(), target.getValue() + transInt);
					// 分配出去以后入款集合中的数据移除掉
					redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
							.remove(tasking.getValue());
					allocated = true;
					break;
				} catch (Exception e) {
					log.debug("AskTrans{} TransLockError{} Regular", frAccId, tarAcc);// 锁定失败：抛出异常
				}
			}
			count++;
		} while (!allocated && count < 2);
	}

	private void applyByOut(AccountBaseInfo base, BigDecimal bankBal) {
		log.debug("applyByOut (id:{},relBal:{}) >>", base.getId(), bankBal);
		if (!Objects.equals(OUTBANK, base.getType()) || !Objects.equals(NORMAL, base.getStatus())) {
			log.debug("ApplyByOut (id:{},bal:{}) >> the acc not outBank | Normal.", base.getId(), bankBal);
			return;
		}
		if (redisSer.getStringRedisTemplate().boundSetOps(RedisKeys.APP_NEED_UPGRADE).members()
				.contains(base.getId().toString())) {
			log.info("applyByOut (id:{}) >> the app need to upgrade", base.getId());
			return;
		}

		if (Objects.isNull(base.getHolder()) && !systemAccountManager.check4AccountingIn(base.getId())) {
			ldel4ONeed(String.valueOf(base.getId()));
			redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
					.remove(String.valueOf(base.getId()));
			log.info("applyByOut (id:{}) >> there is trans in have not confirm", base.getId());
			return;
		}
		ActionEventEnum model = accSer.getModel4PC(base.getId());
		boolean trans4PC = Objects.nonNull(model) && model.ordinal() != ActionEventEnum.CAPTUREMODE.ordinal();
		Integer sysTrigrLim = null;
		Integer sysUpperLim = null;
		if (trans4PC) {
			int zone = handiSer.findZoneByHandiId(base.getHandicapId());
			Integer[] sysUpTrigLim = buildUpAndTrigLim(zone);
			sysUpperLim = Objects.isNull(sysUpTrigLim) ? null : sysUpTrigLim[0];
			sysTrigrLim = Objects.isNull(sysUpTrigLim) ? null : sysUpTrigLim[1];
		}
		int tolerance = buildToler();
		int oHigh = Objects.nonNull(sysUpperLim) ? sysUpperLim : accountChangeService.margin(base);
		BizOutwardTask task = oTaskDao.applyTask(base.getId(), UNDEPOSIT);
		Long rptTm = buildRealBalRptTm(base.getId());
		long curr = System.currentTimeMillis();
		if (base.checkMobile() || checkPCFlowTimeFactor()) {
			if (Objects.nonNull(rptTm) && curr - rptTm < 600000) {
				rptTm = curr - 300000;
			} else {
				Long t = buildBalChgTm(base);
				rptTm = Objects.nonNull(t) ? t : rptTm;
				if (Objects.nonNull(rptTm) && curr - rptTm < 600000) {
					rptTm = curr - 300000;
				}
			}
		}
		int[] ostat = buildOStat(base.getId(), Objects.isNull(rptTm) ? new Date(curr - 600000) : new Date(rptTm));
		String val = String.valueOf(base.getId());
		if (!base.checkMobile() && Objects.nonNull(task)) {
			int taskAmt = task.getAmount().intValue(), mapMax = 35000;
			if (ostat[1] >= mapMax) {
				ldel4ONeed(val);
				log.error(
						"ApplyByOut (id:{} , bal:{} , taskId:{} , orderNo:{} , taskAmt:{}) >> exceed the limit amount in mapping. mapMax:{} mapping:{} ",
						base.getId(), bankBal, task.getId(), task.getOrderNo(), task.getAmount(), mapMax, ostat[1]);
				return;
			}
			int overNeed = buildOver(base, true), overIn = buildOver(base, false);
			int virBal = bankBal.intValue() + ostat[0] + (base.checkMobile() ? ostat[1] : 0) - overNeed;
			if (virBal < taskAmt) {
				int need = oHigh > taskAmt ? (oHigh - virBal) : taskAmt - virBal;
				if (need <= tolerance) {
					log.debug("applyByOut (id:{},relBal:{}) >>need amount less than tolerance", base.getId(), bankBal);
					ldel4ONeed(val);
				} else {
					boolean bankStmt = checkBankStatement(base);
					int v1 = ostat[1] + bankBal.intValue();
					need = v1 >= oHigh && v1 >= taskAmt && need > overIn ? overIn : need;
					boolean checkNonCredit = transactionService.checkNonCredit(base);
					if (bankStmt && checkNonCredit) {
						Date tm = buildOTm(task);
						if (Objects.isNull(base.getHolder()) || base.checkMobile()) {
							Integer limitout = base.getLimitOut();
							if (Objects.nonNull(limitout)) {
								BigDecimal amount = accSer.findAmountDailyByTotal(1, base.getId());
								if (Objects.nonNull(amount)) {
									limitout = limitout - 2000 - amount.intValue();
								}
								need = Math.min(need, limitout);
								if (need <= tolerance) {
									log.info("applyByOut (id:{},relBal:{}) >>need amount less than tolerance",
											base.getId(), bankBal);
									ldel4ONeed(val);
								} else {
									log.info(
											"ApplyByOut (id:{},relBal:{}) >> taskAmt:{} need:{}  bankStmt:true checkNonCredit:true",
											base.getId(), bankBal, taskAmt, need);
									ladd4ONeed(val, enScore4Out(O_SCR_N_MONY_Y_TASK, tm, need));
								}
							} else {
								log.info(
										"ApplyByOut (id:{},relBal:{}) >> taskAmt:{} need:{}  bankStmt:true checkNonCredit:true",
										base.getId(), bankBal, taskAmt, need);
								ladd4ONeed(val, enScore4Out(O_SCR_N_MONY_Y_TASK, tm, need));
							}
						} else {
							log.info(
									"ApplyByOut (id:{},relBal:{}) >> taskAmt:{} need:{}  bankStmt:true checkNonCredit:true",
									base.getId(), bankBal, taskAmt, need);
							ladd4ONeed(val, enScore4Out(O_SCR_MANUAL_N_MONY, tm, need));
						}
					} else {
						log.info(
								"ApplyByOut (id:{},relBal:{}) >> taskAmt:{} need:{}  bankStmt: {} checkNonCredit: {} .",
								base.getId(), bankBal, taskAmt, need, bankStmt, checkNonCredit);
					}
				}
			} else {
				log.debug("applyByOut (id:{},relBal:{}) >>don't need money", base.getId(), bankBal);
				ldel4ONeed(val);
			}
		} else if (bankBal.intValue() <= buildLowest(sysTrigrLim, base)) {
			int need = oHigh - bankBal.intValue() - ostat[0] - ostat[1];
			if (need <= tolerance) {
				log.debug("applyByOut (id:{},relBal:{}) >>need amount less than tolerance", base.getId(), bankBal);
				ldel4ONeed(val);
			} else {
				boolean bankStmt = checkBankStatement(base);
				boolean checkNonCredit = transactionService.checkNonCredit(base);
				if (bankStmt && checkNonCredit) {
					if (Objects.isNull(base.getHolder()) || base.checkMobile()) {
						Integer limitout = base.getLimitOut();
						if (Objects.nonNull(limitout)) {
							BigDecimal amount = accSer.findAmountDailyByTotal(1, base.getId());
							if (Objects.nonNull(amount)) {
								limitout = limitout - 2000 - amount.intValue();
							}
							need = Math.min(need, limitout);
							if (need <= tolerance) {
								log.debug("applyByOut (id:{},relBal:{}) >>need amount less than tolerance",
										base.getId(), bankBal);
								ldel4ONeed(val);
							} else {
								log.info(
										"ApplyByOut (id:{},relBal:{}) >> no task need:{} bankStmt:true checkNonCredit:true type:M. limitOut:{} amount:{}",
										base.getId(), bankBal, need, (limitout + amount.intValue() + 2000), amount);
								ladd4ONeed(val, enScore4Out(O_SCR_N_MONY_Y_TASK, null, need));
							}
						} else {
							log.info(
									"ApplyByOut (id:{},relBal:{}) >> no task need:{} bankStmt:true checkNonCredit:true type:M. limitOut:null",
									base.getId(), bankBal, need);
							ladd4ONeed(val, enScore4Out(O_SCR_N_MONY_Y_TASK, null, need));
						}
					} else {
						log.info("ApplyByOut (id:{},relBal:{}) >> no task need:{} bankStmt:true checkNonCredit:true",
								base.getId(), bankBal, need);
						ladd4ONeed(String.valueOf(base.getId()), enScore4Out(O_SCR_MANUAL_N_MONY, null, need));
					}
				} else {
					log.info("ApplyByOut (id:{},relBal:{}) >> no task need:{} bankStmt:{} checkNonCredit:{} .",
							base.getId(), bankBal, need, bankStmt, checkNonCredit);
				}
			}
		} else {
			log.debug("applyByOut (id:{},relBal:{}) >>don't need money", base.getId(), bankBal);
			ldel4ONeed(val);
		}
	}

	private void applyByOutNew(AccountBaseInfo base, BigDecimal bankBal) {
		log.debug("ApplyByOutNew (id:{},relBal:{}) >>", base, bankBal);
		if (base == null || !Objects.equals(OUTBANK, base.getType()) || !Objects.equals(NORMAL, base.getStatus())) {
			log.debug("ApplyByOutNew (id:{},bal:{}) >> the acc is null | not outBank | Normal.", base.getId(), bankBal);
			return;
		}
		if (redisSer.getStringRedisTemplate().boundSetOps(RedisKeys.APP_NEED_UPGRADE).members()
				.contains(base.getId().toString())) {
			log.info("ApplyByOutNew (id:{}) >> the app need to upgrade", base.getId());
			return;
		}

		Set<Integer> accountingException = systemAccountManager.accountingException();
		accountingException.addAll(systemAccountManager.accountingSuspend());
		if (accountingException.contains(base.getId())) {
			redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
					.remove(String.valueOf(base.getId()));
			ldel4ONeedNew(String.valueOf(base.getId()));
			log.info("出款卡余额上报处理，卡编号{} >> the accountingException", base.getId());
			return;
		}

		if (Objects.isNull(base.getHolder()) && !systemAccountManager.check4AccountingIn(base.getId())) {
			log.info("ApplyByOutNew (id:{}) >> there is trans in have not confirm", base.getId());
			ldel4ONeedNew(String.valueOf(base.getId()));
			redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
					.remove(String.valueOf(base.getId()));
			return;
		}
		Set<String> keys = redisSer.getStringRedisTemplate()
				.keys(RedisKeys.genPattern4TransferAccountLock_to(base.getId()));
		for (String k : keys) {
			TransLock l = new TransLock(k);
			if (Objects.equals(TransLock.STATUS_ALLOC, l.getStatus())
					|| Objects.equals(TransLock.STATUS_CLAIM, l.getStatus())) {
				ldel4ONeedNew(String.valueOf(base.getId()));
				log.debug("allocateTrans ( {} ) >>  genPattern4TransferAccountLock_to . ALLOC|CLAIM .", base.getId());
				return;
			}
		}
		// 出款卡默认都写入from队列
		String val = String.valueOf(base.getId());
		// 入款限额设置为1，不写入from与need中，不下发
		if (Objects.nonNull(base.getLimitIn()) && base.getLimitIn() == 1) {
			redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(val);
			ldel4ONeedNew(val);
			log.debug("ApplyByOutNew (id:{},bal:{}) >> this limitIn value is 1", base.getId(), bankBal);
			return;
		}
		int toCl = Objects.isNull(base.getCurrSysLevel()) ? Outter : base.getCurrSysLevel();// 银行卡所属层级
		int handiId = handiSer.findHandiByHandiId(base.getHandicapId());
		int zone = handiSer.findZoneByHandiId(handiId);
		if (ObjectUtils.isEmpty(base.getType()) || ObjectUtils.isEmpty(zone) || ObjectUtils.isEmpty(toCl)
				|| ObjectUtils.isEmpty(handiId) || ObjectUtils.isEmpty(bankBal.intValue())
				|| bankBal.compareTo(BigDecimal.ZERO) < 1) {
			log.info("ApplyByOutNew account:{} data is null, type:{},zone:{},level:{},handiId:{},bankBal:{}",
					base.getId(), base.getType(), zone, toCl, handiId, bankBal.intValue());
			return;
		}
		Double scr = enScore4Fr(base.getType(), zone, toCl, handiId, bankBal.intValue());
		redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).add(val, scr);
		int tolerance = buildToler();
		int margin = accountChangeService.margin(base);
		int oHigh = 0;
		// 返利网出款卡缺钱允许超过信用额度的2%（可配置）
		if (Objects.equals(AccountFlag.REFUND.getTypeId(), base.getFlag())) {
			BigDecimal percentage = BigDecimal.valueOf(CommonUtils.getIssuedToOutExceedCreditsPercentage() / 100);
			int toler = BigDecimal.valueOf(margin).multiply(percentage).intValue();
			oHigh = margin + toler;
		} else {
			oHigh = margin;
		}
		BizOutwardTask task = oTaskDao.applyTask(base.getId(), UNDEPOSIT);
		Long rptTm = buildRealBalRptTm(base.getId());
		long curr = System.currentTimeMillis();
		if (base.checkMobile() || checkPCFlowTimeFactor()) {
			if (Objects.nonNull(rptTm) && curr - rptTm < 600000) {
				rptTm = curr - 300000;
			} else {
				Long t = buildBalChgTm(base);
				rptTm = Objects.nonNull(t) ? t : rptTm;
				if (Objects.nonNull(rptTm) && curr - rptTm < 600000) {
					rptTm = curr - 300000;
				}
			}
		}
		int[] ostat = buildOStat(base.getId(), Objects.isNull(rptTm) ? new Date(curr - 600000) : new Date(rptTm));
		if (!base.checkMobile() && Objects.nonNull(task)) {
			int taskAmt = task.getAmount().intValue(), mapMax = accountChangeService.margin(base) * 75 / 100;
			if (ostat[1] >= mapMax) {
				ldel4ONeedNew(val);
				log.error(
						"ApplyByOutNew (id:{} , bal:{} , taskId:{} , orderNo:{} , taskAmt:{}) >> exceed the limit amount in mapping. mapMax:{} mapping:{} ",
						base.getId(), bankBal, task.getId(), task.getOrderNo(), task.getAmount(), mapMax, ostat[1]);
				return;
			}
			int overNeed = buildOver(base, true), overIn = buildOver(base, false);
			int virBal = bankBal.intValue() + ostat[0] - overNeed;
			if (virBal < taskAmt) {
				int need = oHigh > taskAmt ? (oHigh - virBal) : taskAmt - virBal;
				if (need <= tolerance) {
					log.debug("ApplyByOutNew (id:{},relBal:{}) >>need amount less than tolerance", base.getId(),
							bankBal);
					ldel4ONeedNew(val);
				} else {
					boolean bankStmt = checkBankStatement(base);
					int v1 = ostat[1] + bankBal.intValue();
					need = v1 >= oHigh && v1 >= taskAmt && need > overIn ? overIn : need;
					boolean checkNonCredit = transactionService.checkNonCredit(base);
					if (bankStmt && checkNonCredit) {
						Date tm = buildOTm(task);
						if (Objects.isNull(base.getHolder())) {
							Integer limitout = base.getLimitOut();
							if (Objects.nonNull(limitout)) {
								BigDecimal amount = accSer.findAmountDailyByTotal(1, base.getId());
								if (Objects.nonNull(amount)) {
									limitout = limitout - amount.intValue() - bankBal.intValue();
								}
								need = Math.min(need, limitout);
								if (need <= tolerance) {
									log.info("ApplyByOutNew (id:{},relBal:{}) >>need amount less than tolerance",
											base.getId(), bankBal);
									ldel4ONeedNew(val);
								} else {
									log.info(
											"ApplyByOutNew (id:{},relBal:{}) >> taskAmt:{} need:{}  bankStmt:true checkNonCredit:true",
											base.getId(), bankBal, taskAmt, need);
									ladd4ONeedNew(val, enScore4Out(O_SCR_N_MONY_Y_TASK, tm, need));
									// 调用需要第三方下发到出款卡
									// accSer.addNeedThirdDrawToOutCardList(base.getId(),
									// need, base.getType());
								}
							} else {
								log.info(
										"ApplyByOutNew (id:{},relBal:{}) >> taskAmt:{} need:{}  bankStmt:true checkNonCredit:true",
										base.getId(), bankBal, taskAmt, need);
								ladd4ONeedNew(val, enScore4Out(O_SCR_N_MONY_Y_TASK, tm, need));
								// 调用需要第三方下发到出款卡
								// accSer.addNeedThirdDrawToOutCardList(base.getId(),
								// need, base.getType());
							}
						} else {
							log.info(
									"ApplyByOutNew (id:{},relBal:{}) >> taskAmt:{} need:{}  bankStmt:true checkNonCredit:true",
									base.getId(), bankBal, taskAmt, need);
							ladd4ONeedNew(val, enScore4Out(O_SCR_MANUAL_N_MONY, tm, need));
							// 调用需要第三方下发到出款卡
							// accSer.addNeedThirdDrawToOutCardList(base.getId(),
							// need, base.getType());
						}
					} else {
						log.info(
								"ApplyByOutNew (id:{},relBal:{}) >> taskAmt:{} need:{}  bankStmt: {} checkNonCredit: {} .",
								base.getId(), bankBal, taskAmt, need, bankStmt, checkNonCredit);
					}
				}
			} else {
				log.debug("ApplyByOutNew (id:{},relBal:{}) >>don't need money", base.getId(), bankBal);
				ldel4ONeedNew(val);
			}
		} else if (bankBal.intValue() <= buildLowest(null, base)
				|| (Objects.nonNull(task) && bankBal.intValue() < task.getAmount().intValue())) {
			int need = oHigh - bankBal.intValue() - ostat[0] - ostat[1];
			if (need <= tolerance) {
				log.debug(
						"ApplyByOutNew (id:{},relBal:{}, ostat[0]:{}, ostat[1]:{}, oHigh:{}) >>need amount less than tolerance",
						base.getId(), bankBal, ostat[0], ostat[1], oHigh);
				ldel4ONeedNew(val);
			} else {
				boolean bankStmt = checkBankStatement(base);
				boolean checkNonCredit = transactionService.checkNonCredit(base);
				if (bankStmt && checkNonCredit) {
					if (Objects.isNull(base.getHolder()) || base.checkMobile()) {
						Integer limitout = base.getLimitOut();
						if (Objects.nonNull(limitout)) {
							BigDecimal amount = accSer.findAmountDailyByTotal(1, base.getId());
							if (Objects.nonNull(amount)) {
								limitout = limitout - amount.intValue() - bankBal.intValue();
							}
							need = Math.min(need, limitout);
							if (need <= tolerance) {
								log.debug("ApplyByOutNew (id:{},relBal:{}) >>need amount less than tolerance",
										base.getId(), bankBal);
								ldel4ONeedNew(val);
							} else {
								double scoreOut = 0;
								if (Objects.nonNull(task)) {
									if (bankBal.intValue() > task.getAmount().intValue()) {
										scoreOut = enScore4Out(O_SCR_N_MONY_Y_TASK, null, need);
									} else {
										scoreOut = enScore4Out(O_SCR_N_MONY_Y_TASK_N_BAL, null, need);
									}
								} else {
									scoreOut = enScore4Out(O_SCR_N_MONY_N_TASK, null, need);
								}
								log.info(
										"ApplyByOutNew (id:{},relBal:{}) >> no task need:{} bankStmt:true checkNonCredit:true type:M. limitOut:{} amount:{}",
										base.getId(), bankBal, need, (limitout + amount.intValue() + 2000), amount);
								ladd4ONeedNew(val, scoreOut);
								// 调用需要第三方下发到出款卡
								// accSer.addNeedThirdDrawToOutCardList(base.getId(),
								// need, base.getType());
							}
						} else {
							double scoreOut = 0;
							if (Objects.nonNull(task)) {
								if (bankBal.intValue() > task.getAmount().intValue()) {
									scoreOut = enScore4Out(O_SCR_N_MONY_Y_TASK, null, need);
								} else {
									scoreOut = enScore4Out(O_SCR_N_MONY_Y_TASK_N_BAL, null, need);
								}
							} else {
								scoreOut = enScore4Out(O_SCR_N_MONY_N_TASK, null, need);
							}
							log.info(
									"ApplyByOutNew (id:{},relBal:{}) >> no task need:{} bankStmt:true checkNonCredit:true type:M. limitOut:null",
									base.getId(), bankBal, need);
							ladd4ONeedNew(val, scoreOut);
							// 调用需要第三方下发到出款卡
							// accSer.addNeedThirdDrawToOutCardList(base.getId(),
							// need, base.getType());
						}
					} else {
						log.info("ApplyByOutNew (id:{},relBal:{}) >> no task need:{} bankStmt:true checkNonCredit:true",
								base.getId(), bankBal, need);
						ladd4ONeedNew(String.valueOf(base.getId()), enScore4Out(O_SCR_MANUAL_N_MONY, null, need));
						// 调用需要第三方下发到出款卡
						// accSer.addNeedThirdDrawToOutCardList(base.getId(),
						// need, base.getType());
					}
				} else {
					log.info("ApplyByOutNew (id:{},relBal:{}) >> no task need:{} bankStmt:{} checkNonCredit:{} .",
							base.getId(), bankBal, need, bankStmt, checkNonCredit);
				}
			}
		} else {
			log.debug("ApplyByOutNew (id:{},relBal:{}) >>don't need money", base.getId(), bankBal);
			ldel4ONeedNew(val);
		}
	}

	/**
	 * 描述 ： 入款卡余额上报 处理
	 * 
	 * @param base
	 *            入款信息
	 * @param bankBal
	 *            入款卡上报余额
	 */
	private void applyByIn(AccountBaseInfo base, BigDecimal bankBal) {
		if (Objects.isNull(base) || Objects.isNull(bankBal)) {
			log.info("入款卡余额上报处理，存在空的参数:base:{},bankBal:{}", base, bankBal);
			return;
		}
		if (redisSer.getStringRedisTemplate().boundSetOps(RedisKeys.APP_NEED_UPGRADE).members()
				.contains(base.getId().toString())) {
			log.info("入款卡余额上报处理，卡编号：{}对应app需要升级！", base.getAlias());
			return;
		}
		int type = base.getType(),
				/** 余额峰值差额区间值 */
				toler = buildToler(),
				/** 余额峰值 */
				limitBalance = buildHigh(base);
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		template.boundHashOps(RedisKeys.REAL_BAL_LASTTIME).put(String.valueOf(base.getId()),
				String.valueOf(System.currentTimeMillis()));
		if ((type == BINDALI || type == BINDWECHAT || type == BINDCOMMON || type == THIRDCOMMON)) {
			if (!systemAccountManager.check4AccountingOut(base.getId())) {
				redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
						.remove(String.valueOf(base.getId()));
				log.info("入款卡余额上报处理,有未完成的下发任务:{}", base.getId());
				return;
			}
		}
		boolean isOverLimit = CommonUtils.getInbankOverLimitBeginAllocate();
		if (type == INBANK && isOverLimit) {
			toler = Math.max(toler, limitBalance);
		}
		// subtype为非0用于区分普通入款卡和手机入款卡
		/**
		 * TODO 4491 判断入款卡能否出款或下发要走新逻辑 outEnable 是否可以出款 达到信用额度百分比 limit_percentage/100
		 * 分单纯的PC入款卡和手机入款卡
		 */

		if (type == INBANK && base.getSubType() != null && base.getSubType() != 0) {
			toler = CommonUtils.getMobileInbankBeginTransBalance();
		}
		String val = String.valueOf(base.getId());
		if (type != OUTBANK) {
			if (type != RESERVEBANK && type != BINDCUSTOMER && bankBal.intValue() < toler) {
				redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM_OUT).remove(val);
				log.debug("ApplyByIn (id:{},bal:{}) >> real balance doesn't reach the low limit ({})", base.getId(),
						bankBal.intValue(), toler);
				return;
			}
			TransLock lock = buildLock(false, base.getId());
			if (Objects.nonNull(lock)
					&& (type != RESERVEBANK || bankBal.intValue() >= CommonUtils.getReserveToReserveBalance())) {
				redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM).remove(val);
				redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM_OUT).remove(val);
				template.boundHashOps(RedisKeys.INBANK_BAL_MORE_LIMITBAL_TM).delete(val);
				log.info("ApplyByIn (id:{},bal:{}) >> exist transfer task. (toId:{} amtInt:{} ,radix:{})", base.getId(),
						bankBal.intValue(), lock.getToId(), lock.getTransInt(), lock.getTransRadix());
				return;
			}
		}
		int toCl = Objects.isNull(base.getCurrSysLevel()) ? Outter : base.getCurrSysLevel();// 银行卡所属层级
		int handiId = handiSer.findHandiByHandiId(base.getHandicapId());
		int zone = handiSer.findZoneByHandiId(handiId);
		if (type == BINDALI || type == BINDWECHAT || type == BINDCOMMON || type == THIRDCOMMON) {
			handiId = zone;
		}
		Double scr = enScore4Fr(base.getType(), zone, toCl, handiId, bankBal.intValue());
		template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM).add(val, scr);
		if (Objects.equals(base.getType(), BINDCOMMON)) {
			Double scrNew = enScore4Fr(base.getType(), zone, toCl, handiId, bankBal.intValue());
			template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).add(val, scrNew);
		}
		// if (type == INBANK && Objects.isNull(buildUpLimitBalTm(base))) {
		// template.boundHashOps(RedisKeys.INBANK_BAL_MORE_LIMITBAL_TM).put(val,
		// String.valueOf(System.currentTimeMillis()));
		// }
		log.info("ApplyByIn (id:{},bal:{}) >> update the real balance in redis.", base.getId(), bankBal);
		boolean isEnableInbankHandicap = CommonUtils.checkEnableInBankHandicap(base.getHandicapId());
		if (!isEnableInbankHandicap) {
			return;
		}
		if (type == INBANK || type == RESERVEBANK || type == BINDCUSTOMER) {
			if (type == INBANK && !checkCanOutwardTask(base, bankBal)) {
				return;
			}
			template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM_OUT).add(val, scr);
		}
	}

	/**
	 * 描述 ： 新逻辑入款卡余额上报 处理
	 * 
	 * @param base
	 *            入款信息
	 * @param bankBal
	 *            入款卡上报余额
	 */
	private void applyByInNew(AccountBaseInfo base, BigDecimal bankBal) {
		log.debug("applyByInNew>> base {},bankBal {}", base, bankBal);
		if (Objects.isNull(base) || Objects.isNull(bankBal)) {
			log.info("入款卡余额上报处理，存在空的参数:base:{},bankBal:{}", base, bankBal);
			return;
		}
		if (redisSer.getStringRedisTemplate().boundSetOps(RedisKeys.APP_NEED_UPGRADE).members()
				.contains(base.getId().toString())) {
			log.info("入款卡余额上报处理，卡编号：{}对应app需要升级！", base.getAlias());
			return;
		}
		String val = String.valueOf(base.getId());
		Set<Integer> accountingException = systemAccountManager.accountingException();
		accountingException.addAll(systemAccountManager.accountingSuspend());
		if (accountingException.contains(base.getId())) {
			redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(val);
			log.info("入款卡余额上报处理，卡编号{} >> the accountingException", base.getId());
			return;
		}
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		template.boundHashOps(RedisKeys.REAL_BAL_LASTTIME).put(String.valueOf(base.getId()),
				String.valueOf(System.currentTimeMillis()));
		// 专注入款卡
		if ((Objects.equals(base.getType(), INBANK) && Objects.equals(base.getFlag(), AccountFlag.PC.getTypeId()))
				|| Objects.equals(base.getType(), BINDCOMMON) || Objects.equals(base.getType(), RESERVEBANK)) {
			// 入款卡是否有锁
			if (!Objects.equals(base.getType(), RESERVEBANK)) {
				TransLock lock = buildLock(false, base.getId());
				if (Objects.nonNull(lock)) {
					redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).remove(val);
					log.debug("ApplyByInNew (id:{},bal:{}) >> exist transfer task. (toId:{} amtInt:{} ,radix:{})",
							base.getId(), bankBal.intValue(), lock.getToId(), lock.getTransInt(), lock.getTransRadix());
					return;
				}
			}
			log.debug("applyByInNew >>id {},bal {}", base.getId(), bankBal.intValue());
			int limitBalance = buildHigh(base);
			if (bankBal.intValue() < limitBalance && Objects.equals(base.getType(), INBANK)) {
				log.debug("applyByInNew >>id {},bal {}, limitBalance {}", base.getId(), bankBal.intValue(),
						limitBalance);
				return;
			}
			int toCl = Objects.isNull(base.getCurrSysLevel()) ? Outter : base.getCurrSysLevel();// 银行卡所属层级
			int handiId = handiSer.findHandiByHandiId(base.getHandicapId());
			int zone = handiSer.findZoneByHandiId(handiId);
			if (Objects.equals(base.getType(), BINDCOMMON)) {
				handiId = zone;
			}
			Double scr = enScore4Fr(base.getType(), zone, toCl, handiId, bankBal.intValue());
			template.boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM).add(val, scr);
			log.info("applyByInNew (id:{},bal:{}) >> update the real balance in redis.", base.getId(), bankBal);
			return;
		}
	}

	/**
	 * clean the invalid data in cache
	 *
	 * @param currTm
	 *            the millis of the current time.
	 * @param lastTm
	 *            the cleaning time last time.
	 * @param internal
	 *            the cleaning internal | period
	 * @return true: executed the cleaning action.</br>
	 *         false: didn't execute the cleaning action.
	 */
	@SuppressWarnings("unckecked")
	private boolean cleanInvalidData(long currTm, long lastTm, long internal) {
		if (lastTm + internal > currTm) {
			return false;
		}
		int EXPR_INBANK = 300000, EXPR_ISSUE = 180000, EXPR_RSV = 180000, EXPR_OUTBANK = 300000;
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		template.boundHashOps(RedisKeys.ACC_API_REVOKE_TM).entries().forEach((k, v) -> {
			boolean clean = false;
			AccountBaseInfo base = Objects.isNull(k) ? null : accSer.getFromCacheById(Integer.valueOf((String) k));
			if (Objects.isNull(base) || Objects.isNull(v)) {
				clean = true;
			} else {
				int type = base.getType();
				if (INBANK == type)
					clean = currTm - Long.valueOf(v.toString()) >= EXPR_INBANK;
				else if (RESERVEBANK == type)
					clean = currTm - Long.valueOf(v.toString()) >= EXPR_RSV;
				else if (OUTBANK == type)
					clean = currTm - Long.valueOf(v.toString()) >= EXPR_OUTBANK;
				else
					clean = currTm - Long.valueOf(v.toString()) >= EXPR_ISSUE;
			}
			if (clean) {
				template.boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM).remove(k);
				ldel4ONeed(String.valueOf(k));
				template.boundHashOps(RedisKeys.ACC_REAL_BAL).delete(k);
				template.boundHashOps(RedisKeys.ACC_REAL_BAL_RPT_TM).delete(k);
				template.boundHashOps(RedisKeys.ACC_API_REVOKE_TM).delete(k);
				template.boundHashOps(RedisKeys.REAL_BAL_LASTTIME).delete(k);
				template.boundHashOps(RedisKeys.INBANK_ONLINE).delete(k);
			}
		});
		if (CLEAN_INVLD_DATA_ONETIME_LAST_TM == 0L) {
			long difTm = currTm % ONE_DAY_TIMESTAMP;
			// 第一次凌晨4点到5点之前清理测试转账的数据
			if (difTm > 72000000 && difTm < 75600000) {
				template.delete(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS);
				template.delete(RedisKeys.ACTIVE_ACCOUNT_KEYS);
				CLEAN_INVLD_DATA_ONETIME_LAST_TM = currTm;
				log.info("记录清理一键转出、激活数据的时间");
			}
		}
		// 非第一次清理，在上次清理时间往后推一天再进行清理
		else if (CLEAN_INVLD_DATA_ONETIME_LAST_TM + ONE_DAY_TIMESTAMP < currTm
				&& CLEAN_INVLD_DATA_ONETIME_LAST_TM + ONE_DAY_TIMESTAMP + 1800000L > currTm) {
			template.delete(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS);
			template.delete(RedisKeys.ACTIVE_ACCOUNT_KEYS);
			CLEAN_INVLD_DATA_ONETIME_LAST_TM = currTm;
			log.info("记录清理一键转出、激活数据的时间");
		}
		return true;
	}

	/**
	 * find the account's expect amount
	 *
	 * @param relAmt
	 *            the collection of account's real amount
	 * @return key:id; </br>
	 *         value:the expect amount
	 */
	private Map<Integer, Integer> buildExpAmt(Map<Integer, Integer> relAmt) {
		if (CollectionUtils.isEmpty(relAmt)) {
			return relAmt;
		}
		long currMillis = System.currentTimeMillis();
		redisSer.getStringRedisTemplate().keys(RedisKeys.genPattern4TransferAccountLock()).forEach(p -> {
			TransLock l = new TransLock(p);
			if (l.getTransInt() != null
					&& (l.getStatus() != TransLock.STATUS_ACK || currMillis - l.getLtTime() <= 180000)
					&& relAmt.containsKey(l.getToId())) {
				relAmt.put(l.getToId(), relAmt.get(l.getToId()) + l.getTransInt().intValue());
			}
		});
		return relAmt;
	}

	/**
	 * get the time when the account's real balance is reported at latest.
	 * <p>
	 * the time doesn't exist, the null as result.
	 * </p>
	 *
	 * @param accId
	 *            the account's ID
	 * @return the time's millis.
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private Long buildRealBalRptTm(Integer accId) {
		if (Objects.isNull(accId)) {
			return null;
		}
		Object ret = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_REAL_BAL_RPT_TM)
				.get(String.valueOf(accId));
		return Objects.isNull(ret) ? null : Long.valueOf((String) ret);
	}

	private int buildShare() {
		return CommonUtils.checkProEnv(CURR_VERSION) ? 5000 : 50;
	}

	/**
	 * set the real balance in cache .
	 *
	 * @param accId
	 *            the account's ID
	 * @param realBal
	 *            the account's real balance
	 * @param repTm
	 *            report time
	 * @return true: updated;</br>
	 *         false: non update;
	 */
	@SuppressWarnings({ "unchecked" })
	private boolean setRealBalInCache(int accId, BigDecimal realBal, Long repTm, boolean updRevoKeApiTm) {
		if (realBal == null || realBal.compareTo(BigDecimal.ZERO) == 0) {
			return false;
		}
		// Object ret = redisSer.getStringRedisTemplate().execute(new
		// RedisScript<String>() {
		// @Override
		// public String getSha1() {
		// return "LUA_SCRIPT_UPD_REL_BAL";
		// }
		//
		// @Override
		// public Class<String> getResultType() {
		// return String.class;
		// }
		//
		// @Override
		// public String getScriptAsString() {
		// return LUA_SCRIPT_UPD_REL_BAL;
		// }
		// }, null, String.valueOf(accId), String.valueOf(realBal.setScale(2,
		// BigDecimal.ROUND_HALF_UP)),
		// RedisKeys.ACC_REAL_BAL, RedisKeys.ACC_REAL_BAL_RPT_TM,
		// RedisKeys.ACC_API_REVOKE_TM,
		// (Objects.isNull(repTm) ? String.valueOf(System.currentTimeMillis()) :
		// repTm.toString()),
		// (updRevoKeApiTm ? "1" : "0"), RedisKeys.ACC_BAL_CHG_TM);

		Assert.notNull(accId, "accountId must not be null");
		List keys = new LinkedList();
		keys.add(RedisKeys.ACC_REAL_BAL);
		keys.add(RedisKeys.ACC_REAL_BAL_RPT_TM);
		keys.add(RedisKeys.ACC_API_REVOKE_TM);
		keys.add(RedisKeys.ACC_BAL_CHG_TM);
		String[] argvs = new String[] { String.valueOf(accId),
				String.valueOf(realBal.setScale(2, BigDecimal.ROUND_HALF_UP)),
				(Objects.isNull(repTm) ? String.valueOf(System.currentTimeMillis()) : repTm.toString()),
				(updRevoKeApiTm ? "1" : "0"), };
		String ret = redisSer.getStringRedisTemplate().execute(updateAccountRealBalanceScript, keys, argvs);

		return Objects.equals("ok", ret);
	}

	/**
	 * get the lowest limit balance of out account
	 * <p>
	 * if real balacne is lower than the limit, </br>
	 * it will trigger the transfer event .
	 * </p>
	 *
	 * @param sysTrigLim
	 *            the system trigger limit amount
	 * @param base
	 *            the current account's base inf
	 * @see this#setUpLim4ONeed(int, boolean, int, int, int)
	 * @see this#getUpLim4ONeed(int)
	 * @see this#buildUpAndTrigLim(int)
	 */
	private Integer buildLowest(Integer sysTrigLim, AccountBaseInfo base) {
		if (Objects.nonNull(sysTrigLim))
			return sysTrigLim;
		if (base.checkMobile() && accountChangeService.margin(base) != null) {
			return accountChangeService.margin(base) * CommonUtils.getMobileBeginIssuedPercent() / 100;
		}
		if (base.getLowestOut() != null)
			return base.getLowestOut();
		return Integer.parseInt(
				MemCacheUtils.getInstance().getSystemProfile().get(UserProfileKey.OUTDRAW_SYSMONEY_LOWEST.getValue()));
	}

	/**
	 * get the high limit balance of out account
	 * <p>
	 * <code>if base.getLimitBalance == null || base.getLimitBalance < 0  </code>
	 * <br/>
	 * 12000 as the result
	 * </p>
	 *
	 * @see AccountBaseInfo#getLimitBalance()
	 */
	private Integer buildHigh(AccountBaseInfo base) {
		return Objects.nonNull(base) && base.getLimitBalance() != null && base.getLimitBalance() > 0
				? base.getLimitBalance()
				: 12000;
	}

	/**
	 * get the peak limit balance of out account
	 * <p>
	 * <code>if base.getPeakBalance == null || base.getPeakBalance < 0 </code> </br>
	 * <code>this#buildHiht()</code> as the result.
	 * </p>
	 */
	private Integer buildPeak(AccountBaseInfo base) {
		return accountChangeService.margin(base);
	}

	/**
	 * get the lowest limit balance if transfer every time.
	 * <p>
	 * TEST ENV ：10</br>
	 * PRO EVN ：MAX_TOLERANCE
	 * </p>
	 */
	private int buildToler() {
		return CommonUtils.checkProEnv(CURR_VERSION) ? MAX_TOLERANCE : BigDecimal.ONE.intValue();
	}

	/**
	 * 获取：会员提单时间
	 *
	 * @param task
	 *            出款任务
	 */
	private Date buildOTm(BizOutwardTask task) {
		BizOutwardRequest oReq = oReqDao.findOne(task.getOutwardRequestId());
		return Objects.nonNull(oReq) ? oReq.getCreateTime() : task.getAsignTime();
	}

	/**
	 * 获取：超额金额
	 */
	private int buildOver(AccountBaseInfo base, boolean oNeed) {
		return base.checkMobile() ? (CommonUtils.checkProEnv(CURR_VERSION) ? (oNeed ? MAX_TOLERANCE / 2 : 0) : 10)
				: (CommonUtils.checkProEnv(CURR_VERSION) ? (oNeed ? MAX_TOLERANCE * 2 : MAX_TOLERANCE * 3) : 10);
	}

	/**
	 * get the total amount in transfering status & mapping status
	 *
	 * @param toId
	 *            to-account's ID
	 * @param asignTm
	 *            the time that a current task asign to the to-account (toId)
	 * @return int[0] the total amount in transfering status</br>
	 *         int[1] the total amount in mapping status</br>
	 */
	private int[] buildOStat(int toId, Date asignTm) {
		int trans = 0, map = 0;
		long currMillis = System.currentTimeMillis();
		String ptn = RedisKeys.genPattern4TransferAccountLock_to(toId);
		for (String p : redisSer.getStringRedisTemplate().keys(ptn)) {
			TransLock l = new TransLock(p);
			if (TransLock.STATUS_ALLOC == l.getStatus() || TransLock.STATUS_CLAIM == l.getStatus()) {
				trans = trans + l.getTransInt().intValue();
			} else if (isTCH(currMillis, Objects.isNull(asignTm) ? null : asignTm.getTime(), l)) {
				map = map + l.getTransInt().intValue();
			}
		}
		return new int[] { trans, map };
	}

	private boolean isTCH(long currMillis, Long asignMillis, TransLock l) {
		return TransLock.STATUS_ACK == l.getStatus() && asignMillis != null && currMillis - asignMillis >= 60000
				&& l.getLtTime() > asignMillis;
	}

	public TransLock buildLock(boolean ignore, int fromId) {
		Set<String> keys = redisSer.getStringRedisTemplate()
				.keys(RedisKeys.genPattern4TransferAccountLock_from(fromId));
		return buildLock(ignore, keys);
	}

	public TransLock buildLockToId(boolean ignore, int toId) {
		Set<String> keys = redisSer.getStringRedisTemplate().keys(RedisKeys.genPattern4TransferAccountLock_to(toId));
		return buildLock(ignore, keys);
	}

	@Override
	public Map<Integer, TransLock> allToIdsTransLocks(List<Integer> accountIds) {
		Set<String> keys = redisSer.getStringRedisTemplate().keys(RedisKeys.genPattern4TransferAccountLock_All());
		// TRANSFER_ACCOUNT_LOCK + fromId + ":" + toId + ":" + operatorId + ":" + radix
		// + ":" + transInt;
		// TransferAccountLock:2147483645:266802:207:0.17:0:1568600901596:0:1568600901596
		if (!CollectionUtils.isEmpty(keys))
			return Maps.newHashMap();
		return buildLockAll(true, keys, accountIds);
	}

	private Map<Integer, TransLock> buildLockAll(boolean ignore, Set<String> keys, List<Integer> accountIds) {
		Assert.notNull(keys, "TransLock的key不存在");
		Map<Integer, TransLock> res = Maps.newLinkedHashMap();
		for (String k : keys) {
			String toIdStr = k.split(":")[2];
			if (StringUtils.isBlank(toIdStr))
				continue;
			Integer toId = Integer.valueOf(toIdStr);
			if (!accountIds.contains(toId)) {
				continue;
			}
			TransLock lock = new TransLock(ignore, k);
			if (Objects.equals(lock.getStatus(), TransLock.STATUS_DEL)
					|| Objects.equals(lock.getStatus(), TransLock.STATUS_ACK)) {
				continue;
			}
			res.put(toId, lock);
		}
		return res;
	}

	private TransLock buildLock(boolean ignore, Set<String> keys) {
		for (String k : keys) {
			TransLock lock = new TransLock(ignore, k);
			if (Objects.equals(lock.getStatus(), TransLock.STATUS_DEL)
					|| Objects.equals(lock.getStatus(), TransLock.STATUS_ACK)) {
				continue;
			}
			return lock;
		}
		return null;
	}

	/**
	 * * 根据内层/外层；银行卡区域；银行卡类型；盘口；金额生成一个分值
	 * <p>
	 * 分值格式：12000000.12345678
	 * </p>
	 * <p>
	 * 整数部分:共七位，从左往右</br>
	 * 第一、二位：银行卡类型</br>
	 * 第三、四位：区域</br>
	 * 第 五 位 ：内层/中层/外层；</br>
	 * 第六，七，八:盘口ID [001~999],公司盘口最大数量为999个</br>
	 * </p>
	 * <p>
	 * 小数部分：共8位，金额的整数部分
	 * </p>
	 *
	 * @param type
	 *            银行卡类型
	 * @param zone
	 *            区域
	 * @param l
	 *            内层/外层/中层
	 * @param handi
	 *            盘口
	 * @param bal
	 *            银行卡余额
	 */
	public Double enScore4Fr(int type, int zone, int l, int handi, int bal) {
		int l_ = CommonUtils.isMergeMidInner() ? (l == Outter ? Outter : Inner) : l;
		return Double.valueOf(String.format("%02d%02d%d%03d.%08d", type, zone, l_, handi, bal));
	}

	/**
	 * 解析下发账号分值
	 *
	 * @return Number[0] type </br>
	 *         Number[1] zone </br>
	 *         Number[2] l</br>
	 *         Number[3] handi</br>
	 *         Number[4] bal</br>
	 * @see this#deScore4Out(Double)
	 */
	public Integer[] deScore4Fr(Double score) {
		String sc = CommonUtils.getNumberFormat(8, 8).format(score);
		return new Integer[] { Integer.valueOf(sc.substring(0, 2)), Integer.valueOf(sc.substring(2, 4)),
				Integer.valueOf(sc.substring(4, 5)), Integer.valueOf(sc.substring(5, 8)),
				Integer.valueOf(sc.substring(9, 17)) };
	}

	/**
	 * 解析出款卡补钱分值
	 *
	 * @return Number[0] priority </br>
	 *         Number[1] tm </br>
	 *         Number[2] need
	 * @see this#deScore4Out(Double)
	 */
	private Number[] deScore4Out(Double score) {
		String sc = CommonUtils.getNumberFormat(7, 9).format(score);
		return new Number[] { Integer.valueOf(sc.substring(0, 1)),
				Long.valueOf(sc.substring(1, 7) + sc.substring(8, 12) + "000"), Integer.valueOf(sc.substring(12, 17)) };
	}

	/**
	 * 生成出款卡补钱分值
	 * <p>
	 * 整数部分:共七位；小数部分：共九位</br>
	 * 第一位：优先级；</br>
	 * 第二，三，四，五，六，七位，小数位：第一，二，三，四位:共10位，表示时间</br>
	 * 小数位：第五，六，七，八，九位：表示金额</br>
	 * </p>
	 *
	 * @param priority
	 *            [1,9] 值越小,越优先 <br/>
	 *            缺钱 && 有出款任务:4 </br>
	 *            缺钱 && 无出款任务:5;<br/>
	 *            不缺钱 && 有出款任务:8;</br>
	 *            不缺钱 && 无出款任务:9;</br>
	 *            <p>
	 *            保留数字:1,2,3,6,7
	 * @param tm
	 *            <code>if priority == 4 </code> tm 为 会员出款时间
	 *            <code>if priority == 5 </code> tm 为 出款卡缺钱上报时间
	 *            <code>if priority == 8 </code> tm 为 出款卡最近一次余额上报时间
	 *            <code>if priority == 9 </code> tm 为 出款卡最近一次余额上报时间
	 * @param need
	 *            所需金额
	 */
	private Double enScore4Out(int priority, Date tm, Integer need) {
		long d = (Objects.isNull(tm) ? System.currentTimeMillis() : tm.getTime()) / 1000;
		return Double.valueOf(String.format("%d%06d.%04d%05d", priority, d / 10000, d % 10000, need));
	}

	/**
	 * @return Number[0] id</br>
	 *         Number[1] type</br>
	 *         Number[2] zone</br>
	 *         Number[3] l </br>
	 *         Number[4] handi </br>
	 *         Number[5] bal
	 */
	private List<Number[]> order4ONeed(Integer frt, Integer minBal, Integer oNd,
			Set<ZSetOperations.TypedTuple<String>> gets, Boolean mobileFirst) {
		List<Number[]> result = new ArrayList<>();
		gets.forEach(p -> {
			Number[] infs = deScore4Fr(p.getScore());
			Number[] tmp = Arrays.copyOf(new Number[] { Integer.valueOf(p.getValue()) }, infs.length + 1);
			System.arraycopy(infs, 0, tmp, 1, infs.length);
			if (Objects.isNull(mobileFirst)) {
				result.add(tmp);
			} else {
				AccountBaseInfo fr = accSer.getFromCacheById(tmp[0].intValue());
				if (Objects.nonNull(fr)) {
					if (mobileFirst) {
						if (fr.checkMobile())
							result.add(tmp);
					} else {
						if (!fr.checkMobile())
							result.add(tmp);
					}
				}
			}

		});
		result.sort((o1, o2) -> {
			int d1 = o1[5].intValue() - oNd - minBal, d2 = o2[5].intValue() - oNd - minBal;
			if ((BINDALI == frt || BINDWECHAT == frt || BINDCOMMON == frt || THIRDCOMMON == frt) && oNd >= 8000) {
				return d2 - d1;
			}
			if (d1 > 0 && d2 > 0 || d1 > 0 && d2 < 0)
				return d1 - d2;
			if (d1 < 0 && d2 < 0 || d1 < 0 && d2 > 0)
				return d2 - d1;
			return 0;
		});
		return result;
	}

	private int WILD_CARD_HANDI = 0;

	private int buildHandi(Integer handi) {
		return Objects.isNull(handi) ? WILD_CARD_HANDI : handi;
	}

	/**
	 * 生成 分值过滤区间
	 *
	 * @param handi
	 *            盘口ID
	 * @param dsct
	 *            true:区分盘口;false:不区分盘口
	 * @param frl
	 *            下发账号：内层/中层/外层
	 * @param frz
	 *            下发账号：区域
	 * @param frt
	 *            下发账号：分类
	 * @param stAmt
	 *            金额起始范围
	 * @param edAmt
	 *            金额结束范围
	 * @return [fromScore, toScore]
	 * @see com.xinbo.fundstransfer.domain.entity.BizHandicap
	 * @see com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType
	 */
	private Double[] score4OFil(int handi, boolean dsct, int frl, int frz, int frt, int stAmt, int edAmt) {
		if (dsct && !Objects.equals(handi, WILD_CARD_HANDI)) {
			return new Double[] { enScore4Fr(frt, frz, frl, handi, stAmt), enScore4Fr(frt, frz, frl, handi, edAmt) };
		} else {
			return new Double[] { enScore4Fr(frt, frz, frl, 0, stAmt), enScore4Fr(frt, frz, frl, 999, edAmt) };
		}
	}

	/**
	 * 描述:检测本次上报余额的最近的所有流水记录,是否有下发总额超过100以上未匹配的记录
	 * 
	 * @param accId
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public BigDecimal[] buildFlowMatching(int accId) {
		String stTm = CommonUtils.getStartTimeOfCurrDay();
		BigDecimal[] ret = new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
		String fmt = "select l.from_account frAcc,l.amount amt,l.status from biz_bank_log l,biz_account a where l.from_account = a.id and a.status=1 and l.amount<0 and l.status=0 and l.create_time>='%s' and l.from_account=%d";
		entityMgr.createNativeQuery(String.format(fmt, stTm, accId)).getResultList().forEach(p -> {
			Object[] vals = (Object[]) p;
			BigDecimal amt = (BigDecimal) vals[1];
			ret[0] = ret[0].add(amt.abs());
			ret[1] = ret[1].add(BigDecimal.ONE);
		});
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		if (ret[0].intValue() >= 100) {
			String temp = ret[0].intValue() + "=" + ret[1].intValue();
			template.boundHashOps(RedisKeys.ACC_MACH_ALARM).put(String.valueOf(accId), temp);
		} else {
			ret[0] = BigDecimal.ZERO;
			ret[1] = BigDecimal.ZERO;
			template.boundHashOps(RedisKeys.ACC_MACH_ALARM).delete(String.valueOf(accId));
		}
		sendAlarmToFrontDesk();
		return ret;
	}

	@Override
	public Map<Integer, BigDecimal[]> buildFlowOutMatching() {
		Map<Integer, BigDecimal[]> ret = new HashMap<>();
		Map<Object, Object> kv = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_MACH_ALARM).entries();
		for (Map.Entry<Object, Object> en : kv.entrySet()) {
			String[] hv = ((String) en.getValue()).split("=");
			ret.put(Integer.valueOf((String) en.getKey()),
					new BigDecimal[] { new BigDecimal(hv[0]), new BigDecimal(hv[1]) });
		}
		return ret;
	}

	/**
	 * get all account's ID that has the matching flows transferred to other
	 * account.
	 */
	@Override
	public Set<Integer> buildAcc4FlowOutMatching() {
		return redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_MACH_ALARM).keys().stream()
				.map(p -> Integer.valueOf(p.toString())).collect(Collectors.toSet());
	}

	/**
	 * get all factors that resist other accounts transfer funds to the account.
	 *
	 * @param accId
	 *            the account that received funds transferred from other accounts
	 * @return the factors that resist other accounts transfer funds to the
	 *         accounts. the factors is separated by <tt>;</tt>
	 */
	@Override
	public String buildResistFactors(int accId) {
		List<String> ret = new ArrayList<>();
		if (!oldTransSer.checkBlack(accId))
			ret.add("已经回收");
		if (!transMonitorService.checkAccAlarm4Flow(accId))
			ret.add("账号告警");
		if (Objects.isNull(buildRealBalRptTm(accId)))
			ret.add("未报余额");
		if (CollectionUtils.isEmpty(ret)) {
			AccountBaseInfo base = accSer.getFromCacheById(accId);
			if (Objects.nonNull(base) && !checkBankStatement(base)) {
				Object log = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_LOG_TM)
						.get(String.valueOf(base.getId()));
				if (Objects.nonNull(log))
					ret.add(String.format("抓流水[上次抓取于:%s]",
							CommonUtils.getDateStr(new Date(Long.parseLong(log.toString())))));
				else
					ret.add("未抓流水");
			}
		}
		if (CollectionUtils.isEmpty(ret)) {
			AccountBaseInfo base = accSer.getFromCacheById(accId);
			if (!transactionService.checkNonCredit(base)) {
				ret.add("未到账");
			}
		}
		return String.join(";", ret);
	}

	/**
	 * 描述:所有未完成的下发金额超过一定的金额通知页面
	 */
	@Override
	public void sendAlarmToFrontDesk() {
		Set<Integer> acc4Alarm = buildAcc4Alarm();
		Map<Integer, Integer> zoneCount = new HashMap<>();
		if (!CollectionUtils.isEmpty(acc4Alarm)) {
			for (Integer acc : acc4Alarm) {
				AccountBaseInfo base = accSer.getFromCacheById(acc);
				if (Objects.isNull(base) || !Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus()))
					continue;
				zoneCount.merge(0, 1, (o1, o2) -> Objects.isNull(o1) ? o2 : o1 + o2);
				int zone = handiSer.findZoneByHandiId(base.getHandicapId());
				zoneCount.merge(zone, 1, (o1, o2) -> Objects.isNull(o1) ? o2 : o1 + o2);
			}
		}
		try {
			String info = CommonUtils.genSysMsg4WS(null, SystemWebSocketCategory.AccountAlarmCount,
					mapper.writeValueAsString(zoneCount));
			redisSer.convertAndSend(RedisTopics.BROADCAST, info);
		} catch (Exception e) {
			log.error("");
		}
	}

	/**
	 * account's ID that has matching flows transferred to other account ,either has
	 * been in Risk Status by monitoring the real balance transformation.
	 *
	 * @see this#buildAcc4FlowOutMatching()
	 * @see TransMonitorServiceImpl#buildAcc4MonitorRisk()
	 */
	@Override
	public Set<Integer> buildAcc4Alarm() {
		// Set<Integer> acc4Flow = buildAcc4FlowOutMatching();
		// acc4Flow.addAll(transMonitorService.buildAcc4MonitorRisk());
		return buildAcc4FlowOutMatching();
	}

	@Override
	public Set<Integer> buildValidAcc() {
		return redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_API_REVOKE_TM).keys().stream()
				.map(p -> Integer.valueOf(p.toString())).collect(Collectors.toSet());
	}

	/**
	 * 账号当前余额
	 * 
	 * @param target
	 * @return
	 */
	public BigDecimal getCurrBalance(Integer target) {
		if (Objects.isNull(target))
			return BigDecimal.ZERO;
		String val = (String) redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_REAL_BAL)
				.get(target.toString());
		return StringUtils.isEmpty(val) ? new BigDecimal("99999999") : SysBalUtils.radix2(new BigDecimal(val));
	}

	@Override
	public Map<Integer, BigDecimal> allCurrBalance(@Nullable List<Integer> accountIds) {
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		String key = RedisKeys.ACC_REAL_BAL;
		Map<Integer, BigDecimal> res = Maps.newLinkedHashMap();
		if (template.hasKey(key)) {
			HashOperations hashOperations = template.opsForHash();
			if (!CollectionUtils.isEmpty(accountIds)) {
				List list = hashOperations.multiGet(key,
						accountIds.stream().map(p -> p.toString()).collect(Collectors.toList()));
				for (int i = 0, size = accountIds.size(); i < size; i++) {
					Object o = list.get(i);
					if (o != null) {
						res.put(accountIds.get(i), SysBalUtils.radix2(new BigDecimal(o.toString())));
					} else {
						res.put(accountIds.get(i), new BigDecimal("99999999"));
					}
				}
			} else {
				Map<String, String> entries = hashOperations.entries(key);
				if (!CollectionUtils.isEmpty(entries)) {
					for (Map.Entry<String, String> entry : entries.entrySet()) {
						String hkey = entry.getKey();
						String hval = entry.getValue();
						if (StringUtils.isBlank(hkey) && !"null".equals(hkey)) {
							if (StringUtils.isBlank(hval) && !"null".equals(hval))
								res.put(Integer.valueOf(hkey), SysBalUtils.radix2(new BigDecimal(hval)));
							else
								res.put(Integer.valueOf(hkey), new BigDecimal("99999999"));
						}
					}
				}
			}

		}
		return res;
	}

	/**
	 * 账号是否存在转账任务
	 * 
	 * @param accid
	 * @return
	 */
	public boolean hasTrans(int accid) {
		TransLock lock = buildLock(false, accid);
		if (Objects.nonNull(lock)) {
			log.debug("检查是否在转账:accid:{},是否在转账:{},转账详细信息:{}", accid, Objects.nonNull(lock), lock.getMsg());
		} else {
			log.debug("检查是否在转账:accid:{},转账详细信息:null", accid);
		}
		return Objects.nonNull(lock);
	}

	/**
	 * 当前余额 + 当日出款 是否超过当日出款限额
	 *
	 * @param accId
	 * @return
	 */
	@Override
	public boolean exceedAmountSumDailyOutward(int accId) {
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		if (base == null || base.getLimitOut() == null || base.getLimitOut() == 0) {
			return false;
		}
		BigDecimal currBalance = getCurrBalance(accId);
		BigDecimal outDaily = accSer.findAmountDailyByTotal(1, base.getId());
		if (currBalance == null || outDaily == null) {
			return false;
		}
		if (outDaily.intValue() + currBalance.intValue() + CommonUtils.getLessThenSumDailyOutward() >= base
				.getLimitOut()) {
			log.info("当日出款已达到日出款限额，该卡：{}当日已出款：{}", accId, outDaily.intValue());
		}
		return outDaily.intValue() + currBalance.intValue() + CommonUtils.getLessThenSumDailyOutward() >= base
				.getLimitOut();
	}

	@Override
	public boolean exceedAmountSumDailyOutward2(Integer accId) {
		if (accId == null)
			return false;
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		if (base == null || base.getLimitOut() == null || base.getLimitOut() == 0) {
			return false;
		}
		return false;
	}

	/**
	 * 当前余额 + 当日出款 是否超过当日出款限额
	 *
	 * @param accId
	 * @return
	 */
	@Override
	public boolean exceedAmountSumDailyOutwardNew(int accId) {
		AccountBaseInfo base = accSer.getFromCacheById(accId);

		if (base == null || base.getLimitOut() == null || base.getLimitOut() == 0) {
			return false;
		}
		BigDecimal outDaily = accSer.findAmountDailyByTotal(1, base.getId());
		if (outDaily == null) {
			return false;
		}
		return outDaily.intValue() + CommonUtils.getTriggerClearBalance4OtherCard()
				+ CommonUtils.getLessThenSumDailyOutward() >= base.getLimitOut();
	}

	/**
	 * 账号收款时，用于判断账号是否可以收款
	 * 
	 * @param accId
	 * @return
	 */
	public boolean isOnline(int accId) {
		if (onLine(accId)) {
			return redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.REAL_BAL_LASTTIME).hasKey(accId + "");
		}
		return false;
	}

	/**
	 * 账号收款时，用于判断账号是否在线
	 *
	 * @param accId
	 * @return
	 */
	public boolean onLine(int accId) {
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		if (base == null) {
			return false;
		}
		if (!base.checkMobile()) {
			return true;
		}
		boolean isOnline = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.INBANK_ONLINE).hasKey(accId + "");
		log.debug("是否在线:accId:{},{}", accId, isOnline);
		return isOnline;
	}

	/**
	 * 给定账号Id集，获取在线的银行卡号结果集
	 *
	 * @param accountIds
	 * @return
	 */
	public List<String> onLineAcc(List<Integer> accountIds) {
		if (CollectionUtils.isEmpty(accountIds)) {
			return null;
		}
		List<Integer> onLine = accountIds.stream().filter(p -> onLine(p)).collect(Collectors.toList());
		List<String> result = new ArrayList<>();
		for (Integer id : onLine) {
			result.add(accSer.getFromCacheById(id).getAccount());
		}
		return result;
	}

	/**
	 * 查询所有黑名单集
	 */
	private Set<String> findBlackList() {
		return redisSer.getStringRedisTemplate().keys(RedisKeys.genPattern4TransBlack());
	}

	private boolean checkBlack(Set<String> black, String frAcc, int toId) {
		boolean ret = CollectionUtils.isEmpty(black)
				|| (black.stream().filter(p -> p.startsWith(RedisKeys.gen4TransBlack(0, toId, 0))
						|| p.startsWith(RedisKeys.gen4TransBlack(frAcc, toId, 0))).count() == 0);
		return ret;
	}

	private void addToBlackList(int frId, int toId, long expir, TimeUnit timeUnit) {
		redisSer.getStringRedisTemplate().boundValueOps(RedisKeys.gen4TransBlack(frId, toId, 0) + ":0")
				.set(StringUtils.EMPTY, expir, timeUnit);
	}

	/**
	 * 获取:账号预留余额设置</br>
	 * 生产环境：isRobot ? TRANS_ROBOT_BAL : TRANS_MANUAL_BAL</br>
	 * 测试环境：BigDecimal.ONE
	 */
	private BigDecimal buildMinBal(boolean isRobot) {
		if (CommonUtils.checkProEnv(CURR_VERSION)) {
			return isRobot ? new BigDecimal(50) : new BigDecimal(20);
		} else {
			return BigDecimal.ONE;
		}
	}

	@Override
	public void lockTrans(Object fromId, Integer toId, Integer operator, Integer transInt, Integer expTM)
			throws Exception {
		if (fromId == null || toId == null || operator == null || transInt == null) {
			throw new Exception("lock trans record failed , due to incompleteness of input information.");
		}
		llock(fromId, toId, operator, transInt, expTM, false);
	}

	@Override
	public boolean llockUpdStatus(Object fromId, Integer toId, int status) {
		if (toId != null) {
			redisSer.getStringRedisTemplate().boundSetOps(RedisKeys.TASK_REPORT_LAST_STEP).remove(toId + "");
		}
		return llockUpdStatus(fromId, toId, status, LOCK_ROBOT_DEL_SECONDS);
	}

	public TransferEntity activeAccByTest(int fromId, boolean create) {
		AccountBaseInfo fr = accSer.getFromCacheById(fromId);
		if (create && (fr == null || !Objects.equals(fr.getStatus(), AccountStatus.Inactivated.getStatus()))) {
			log.info("activeAccByTest>> acc is null | status isn't inactivated,accId {}", fromId);
			return null;
		}
		if (!create) {
			return applyTrans(fromId, false, null, true);
		} else {
			log.info("activeAccByTest>> active account add to redis,accId {}", fromId);
			try {
				String k = RedisKeys.ACCOUNT_ACTIVE + fromId;
				redisSer.getStringRedisTemplate().boundValueOps(k).set(StringUtils.EMPTY, 120, TimeUnit.MINUTES);
			} catch (Exception e) {
				log.error("Error,acc add to active queue. id:{} msg:{}", fromId, e);
			}
			return null;
		}
	}

	/**
	 * 从redis中取出需激活的卡，分配激活任务
	 * 
	 * @throws InterruptedException
	 */
	@Scheduled(fixedDelay = 5000)
	protected void accActive() throws InterruptedException {
		// 1.检测 是否具有 出款任务分配权限
		if (!alloIAcntSer.checkHostRunRight()) {
			log.trace("AllocOTask >> the host has no right to allocate. {}", CommonUtils.getInternalIp());
			Thread.sleep(5000L);
			return;
		}
		Set<String> keys = redisSer.getStringRedisTemplate().keys(RedisKeys.ACCOUNT_ACTIVE + "*");
		if (!CollectionUtils.isEmpty(keys)) {
			for (String key : keys) {
				String id = key.replace(RedisKeys.ACCOUNT_ACTIVE, "");
				if (StringUtils.isNotBlank(id)) {
					AccountBaseInfo base = accSer.getFromCacheById(Integer.parseInt(id));
					if (base != null && Objects.equals(base.getStatus(), AccountStatus.Inactivated.getStatus())) {
						TransferEntity entity = applyTrans(Integer.parseInt(id), true, null, true);
						if (entity != null) {
							redisSer.getStringRedisTemplate().delete(key);
						}
					} else {
						redisSer.getStringRedisTemplate().delete(key);
					}
				} else {
					redisSer.getStringRedisTemplate().delete(key);
				}
			}
		} else {
			Thread.sleep(5000L);
		}
	}

	public TransferEntity getTestTrans(int fromId, boolean create) {
		return applyTrans(fromId, create, null, false);
	}

	/**
	 * 根据给定id，分配相应的转账任务，
	 *
	 * @param fromId
	 * @param create
	 * @param amount
	 * @return
	 */
	public TransferEntity applyTrans(int fromId, boolean create, Float amount, boolean filterBank) {
		StringRedisTemplate redisTemplate = redisSer.getStringRedisTemplate();
		String pattern4fromId = RedisKeys.genPattern4ActiveAccTrans_from(fromId);
		Set<String> keys = redisTemplate.keys(pattern4fromId);
		String result = "";
		if (CollectionUtils.isEmpty(keys)) {
			if (!create) {
				return null;
			}
			AccountBaseInfo baseInfo = accSer.getFromCacheById(fromId);
			Set<String> fails = redisTemplate.keys(RedisKeys.genPattern4ActiveAccTransFail_from(fromId));
			log.debug("applyTrans fails:{}", fails);
			String failIds = "";
			// 失败的key的格式：ActiveAccountTestTransFail:frId:toId1,toId2,...
			if (!CollectionUtils.isEmpty(fails)) {
				for (String key : fails) {
					failIds = key.split(":")[2];
					break;
				}
			}
			StringRedisTemplate template = redisSer.getStringRedisTemplate();
			// 过滤掉的Id集合
			failIds = failIds + "," + template.boundValueOps(RedisKeys.ONE_TIME_TRANS_OUT_KEYS).get();
			log.debug("applyTrans failIds:{}", failIds);
			result = buildActiveTransTo(baseInfo, "," + failIds + ",", amount, filterBank);
			if (StringUtils.isEmpty(result)) {
				log.info("can't get trans entity {} ", fromId);
				return null;
			} else {
				Map trans = redisSer.getStringRedisTemplate().opsForHash().entries(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS);
				String[] str = result.split(":");
				Object o = trans.get(str[1]);
				String amt;
				if (Objects.isNull(amount)) {
					// val 的格式为：amount:timestamp
					if (Objects.nonNull(o)) {
						amt = getTransDecimal(o.toString());
						// 存放用于计算下次小数位的整数信息
						redisSer.getStringRedisTemplate().opsForHash().put(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS, str[1],
								(amt.split("\\."))[1] + ":" + System.currentTimeMillis());
					} else {
						// 存放用于计算下次小数位的整数信息
						redisSer.getStringRedisTemplate().opsForHash().put(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS, str[1],
								"01:" + System.currentTimeMillis());
						amt = "10.01";
					}
				} else {
					amt = BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
					String oneTime = template.boundValueOps(RedisKeys.ONE_TIME_TRANS_OUT_KEYS).get();
					if (StringUtils.isNotEmpty(oneTime)) {
						oneTime = oneTime + str[1] + ",";
					} else {
						oneTime = "," + str[1] + ",";
					}
					template.boundValueOps(RedisKeys.ONE_TIME_TRANS_OUT_KEYS).set(oneTime, 30, TimeUnit.MINUTES);
				}
				result = RedisKeys.ACTIVE_ACCOUNT_TEST_TRANS + result + ":" + amt + ":" + System.currentTimeMillis();
				// 存放用于激活是匹配的转账信息
				// key toId:amount
				// val
				// ActiveAccountTestTrans:frId:toId:frAccount:toAccount:toOwner:toBankType:bankname:amount:timestamp
				if (filterBank) {
					redisSer.getStringRedisTemplate().opsForHash().put(RedisKeys.ACTIVE_ACCOUNT_KEYS,
							str[1] + ":" + amt, result);
					// key frId:-amount
					redisSer.getStringRedisTemplate().opsForHash().put(RedisKeys.ACTIVE_ACCOUNT_KEYS,
							fromId + ":-" + amt, result);
					log.info("applyTrans>> get active trans entity frId {} trans info {}", fromId, result);
				} else {
					redisSer.getStringRedisTemplate().opsForHash().put(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS,
							str[1] + ":" + amt, result);
					// key frId:-amount
					redisSer.getStringRedisTemplate().opsForHash().put(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS,
							fromId + ":-" + amt, result);
					log.info("applyTrans>> get test trans entity frId {} trans info {}", fromId, result);
				}
				redisSer.getStringRedisTemplate().boundValueOps(result).set(StringUtils.EMPTY, 10, TimeUnit.MINUTES);
			}
		} else {
			for (String key : keys) {
				result = key;
				break;
			}
		}
		if (StringUtils.isNotEmpty(result)) {
			String[] strs = result.split(":");
			TransferEntity ret = new TransferEntity();
			ret.setAmount(new BigDecimal(strs[8]).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
			ret.setFromAccountId(fromId);
			ret.setToAccountId(Integer.parseInt(strs[2]));
			ret.setAccount(strs[4]);
			ret.setOwner(strs[5]);
			ret.setBankType(strs[6]);
			ret.setBankAddr(strs[7]);
			ret.setAcquireTime(System.currentTimeMillis());
			ret.setRemark("一键转出");
			BigDecimal realBal = accountChangeService.buildRelBal(fromId);
			systemAccountManager.regist(ret, realBal);
			return ret;
		}
		return null;
	}

	public void ackTrans(TransferEntity entity) {
		llockUpdStatus(entity.getFromAccountId(), entity.getToAccountId() == null ? 0 : entity.getToAccountId(),
				Objects.equals(entity.getResult(), 3) ? TransLock.STATUS_DEL : TransLock.STATUS_ACK);
		StringRedisTemplate redisTemplate = redisSer.getStringRedisTemplate();
		String pattern4fromId = RedisKeys.genPattern4ActiveAccTrans_from(entity.getFromAccountId());
		Set<String> keys = redisTemplate.keys(pattern4fromId);
		if (!CollectionUtils.isEmpty(keys)) {
			String result = "";
			for (String key : keys) {
				result = key;
				break;
			}
			redisTemplate.delete(result);
			if (entity.getResult() == null || entity.getResult() != 1) {
				// key的格式为：ActiveAccountTestTrans:frId:toId:frAccount:toAccount:toOwner:toBankType:amount
				String[] keyArr = result.split(":");
				// 删除用于激活的key，由于客户端可能上报不准确，这里先不删除对应的key，日终能够自动删除
				redisTemplate.boundHashOps(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS).delete(keyArr[2] + ":" + keyArr[7]);
				// 添加到失败的结果集中
				// 失败的key的格式：ActiveAccountTestTransFail:frId:toId1,toId2,...
				String pattern4Fail = RedisKeys.genPattern4ActiveAccTransFail_from(entity.getFromAccountId());
				Set<String> fail = redisTemplate.keys(pattern4Fail);
				if (!CollectionUtils.isEmpty(fail)) {
					for (String key : fail) {
						redisSer.getStringRedisTemplate().delete(key);
						redisSer.getStringRedisTemplate().boundValueOps(key + "," + keyArr[2]).set(StringUtils.EMPTY,
								30, TimeUnit.MINUTES);
						break;
					}
				} else {
					redisSer.getStringRedisTemplate()
							.boundValueOps(
									RedisKeys.ACTIVE_ACCOUNT_TEST_FAIL + entity.getFromAccountId() + ":" + keyArr[2])
							.set(StringUtils.EMPTY, 30, TimeUnit.MINUTES);
				}
			}
		}
	}

	/**
	 * 给指定账户分配指定金额区间的任务
	 *
	 * @param baseInfo
	 * @param min
	 * @param max
	 */
	@Override
	public boolean allocByAccAndBal(AccountBaseInfo baseInfo, Integer min, Integer max) {
		if (Objects.isNull(baseInfo) || !Objects.equals(baseInfo.getStatus(), NORMAL) || Objects.isNull(min)
				|| Objects.isNull(max)) {
			return false;
		}
		if (!systemAccountManager.check4AccountingOut(baseInfo.getId())) {
			log.info("allocByAccAndBal >> id {} has one trans out hasn't been confirmed ", baseInfo.getId());
			return false;
		}
		TransLock lock = buildLock(false, baseInfo.getId());
		if (Objects.nonNull(lock)) {
			log.info("allocByAccAndBal >> id {} already has trans task, trans to {} amount {} ", baseInfo.getId(),
					lock.getToId(), lock.getTransInt());
			return true;
		}
		if (baseInfo.getType() == BINDCOMMON) {
			TransferEntity entity = applyTrans(baseInfo.getId(), true, Float.parseFloat(max.toString() + ".00"), false);
			if (Objects.nonNull(entity)) {
				return true;
			}
			return false;
		}
		int undeposit = OutwardTaskStatus.Undeposit.getStatus();
		int cancel = OutwardTaskStatus.ManageCancel.getStatus(), refuse = OutwardTaskStatus.ManageRefuse.getStatus();
		Integer zone = handiSer.findZoneByHandiId(baseInfo.getHandicapId());
		Integer accId = baseInfo.getId();
		String alias = baseInfo.getAlias();
		BizOutwardTask task = null;
		String handicapids = CommonUtils.getDistHandicapNewVersion();
		String[] handicap = new String[] { "0" };
		if (StringUtils.isNotBlank(handicapids)) {
			// 全部开启新版本后，不能通过这种方式取出款任务
			if (!"ALL".equals(handicapids)) {
				if (handicapids.contains(";" + baseInfo.getHandicapId() + ";")) {
					TransferEntity entity = applyTrans(baseInfo.getId(), true, Float.parseFloat(max.toString() + ".00"),
							false);
					if (Objects.nonNull(entity)) {
						return true;
					}
					return false;
				} else {
					handicapids = handicapids.substring(1);
					handicapids = handicapids.substring(0, handicapids.length() - 1);
					handicap = handicapids.split(";");
				}
			} else {
				TransferEntity entity = applyTrans(baseInfo.getId(), true, Float.parseFloat(max.toString() + ".00"),
						false);
				if (Objects.nonNull(entity)) {
					return true;
				}
				return false;
			}
		}
		/**
		 * if (max > 50100) { task = oTaskDao.findSameBankBigOutwardTask(50000, max,
		 * zone, baseInfo.getBankType(), alias, handicap); } if (Objects.isNull(task)) {
		 * task = oTaskDao.findBigOutwardTask(min, max, zone, alias, handicap); } if
		 * (Objects.nonNull(task)) { String hisRem = task.getRemark(); String theRem =
		 * "锁定成功" + (StringUtils.isBlank(alias) ? accId : alias) + "-" + "机器"; String
		 * remark = CommonUtils.genRemark(hisRem, theRem, new Date(), "系统"); Integer
		 * outwardPayType = baseInfo.checkMobile() ? OutWardPayType.REFUND.getType() :
		 * OutWardPayType.PC.getType(); try { lockTrans(baseInfo.getId(),
		 * task.getId().intValue(), AppConstants.USER_ID_4_ADMIN,
		 * task.getAmount().intValue(), 86400); oTaskDao.allocAccount(task.getId(),
		 * accId, null, cancel, refuse, remark, undeposit, outwardPayType); log.info(
		 * "allocByAccAndBal >>(REQ OrderNo: {} taskId: {} task amount {} fromId: {} bal
		 * {} operator: {}) Allocate Success !!.", task.getOrderNo(), task.getId(),
		 * task.getAmount(), accId, max, null); return true; } catch (Exception e) {
		 * log.debug( "allocByAccAndBal >> log exception (REQ OrderNo: {} taskId: {}
		 * fromId: {} operator: {} msg:{}) !!.", task.getOrderNo(), task.getId(), accId,
		 * null, e); return false; } }
		 */
		return false;
	}

	/**
	 * 该账号是否在下发黑名单中
	 *
	 * @param toId
	 *            账号ID
	 * @return true:不在下发黑名单中</br>
	 *         false:在下发黑名单中</br>
	 */
	@Override
	public boolean checkBlack(int toId) {
		return !redisSer.getStringRedisTemplate().hasKey(RedisKeys.gen4TransBlack(WILD_CARD_ACCOUNT, toId, 0))
				&& systemAccountManager.check4AccountingIn(toId);
	}

	/**
	 * 第三方入款卡 下发到出款卡 锁定
	 *
	 * @param fromId
	 * @param toId
	 * @param operator
	 * @param transInt
	 * @param expireTimeSeconds
	 * @return
	 */
	@Override
	public boolean lockForThirdDrawToOutCard(Object fromId, Integer toId, Integer operator, Integer transInt,
			Integer expireTimeSeconds) {
		if (fromId == null || toId == null || operator == null || transInt == null || expireTimeSeconds == null) {
			log.debug("参数有空 不能锁定!");
			return false;
		}
		try {
			llockForManual(fromId, toId, operator, transInt, expireTimeSeconds, true);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean lockForDrawTaskToOutCard(Object fromId, Integer toId, Integer operator, Integer transInt,
			Integer expireTimeSeconds) {
		if (fromId == null || toId == null || operator == null || transInt == null || expireTimeSeconds == null) {
			log.debug("参数有空 不能锁定!");
			return false;
		}
		try {
			llockForManual(fromId, toId, operator, transInt, expireTimeSeconds, true);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 第三方入款卡 下发到出款卡 解锁
	 *
	 * @param fromId
	 * @param toId
	 * @return
	 */
	@Override
	public boolean unLockForThirdDrawToOutCard(Object fromId, Integer toId) {
		log.info("第三方款卡 下发到出款卡 解锁 参数  fromId :{}, toId :{}", fromId, toId);
		if (fromId == null || toId == null) {
			return true;
		}
		boolean del = false;
		try {
			del = llockUpdStatus(fromId, toId, TransLock.STATUS_DEL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("解锁 结果  :{}", del);
		return del;
	}

	@Override
	public boolean unLockForDrawTaskToOutCard(Object fromId, Integer toId) {
		log.info("下发任务解锁  出款卡 解锁 参数  fromId :{}, toId :{}", fromId, toId);
		if (fromId == null || toId == null) {
			return true;
		}
		boolean del = false;
		try {
			del = llockUpdStatus(fromId, toId, TransLock.STATUS_DEL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("解锁 结果  :{}", del);
		return del;
	}

	/**
	 * 第三方下发完成之后 移除出款卡
	 *
	 * @param toId
	 * @return
	 */
	@Override
	public void removeNeedAmountOutCard(Integer toId) {
		log.debug("第三方入款卡 锁定出款卡 之后 移除 toId:{}", toId);
		ldel4ONeedNew(String.valueOf(toId));
	}

	private void lockTrans(Object fromId, Integer toId, Integer operator, Integer transInt) throws Exception {
		if (fromId == null || toId == null || operator == null || transInt == null) {
			throw new Exception("lock trans record failed , due to incompleteness of input information.");
		}
		llock(fromId, toId, operator, transInt, LOCK_ROBOT_CLAIM_SECONDS, false);
	}

	/**
	 * 执行 lua script: 锁定
	 */
	private void llock(Object fromId, Integer toId, Integer operator, Integer transInt, Integer expTM,
			boolean thirdDrawToOutcardFlag) throws Exception {
		String thirdDrawToOutcardFlagStr = "2";
		if (thirdDrawToOutcardFlag) {
			thirdDrawToOutcardFlagStr = "1";
		}
		String ret = redisSer.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOC_TRANS_LOCK";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return CommonUtils.getTransLockFromTo() ? LUA_SCRIPT_ALLOC_TRANS_LOCK_ : LUA_SCRIPT_ALLOC_TRANS_LOCK;
			}
		}, null, String.valueOf(fromId), String.valueOf(toId), String.valueOf(operator), String.valueOf(transInt),
				String.valueOf(AppConstants.USER_ID_4_ADMIN), String.valueOf(expTM),
				String.valueOf(LOCK_MANUAL_SECONDS), RedisKeys.genPattern4TransferAccountLock_from(fromId),
				RedisKeys.genPattern4TransferAccountLock_to(toId),
				RedisKeys.genPattern4TransferAccountLock_operator(operator), RedisKeys.FROM_ACCOUNT_TRANS_RADIX_AMOUNT,
				RedisKeys.genPattern4TransferAccountLock(fromId, toId, operator),
				String.valueOf(System.currentTimeMillis()), String.valueOf(TransLock.STATUS_ALLOC),
				String.valueOf(TransLock.STATUS_ACK), String.valueOf(TransLock.STATUS_DEL), thirdDrawToOutcardFlagStr);
		if (!Objects.equals("ok", ret)) {
			throw new Exception(String.format("the account %s already locked.", String.valueOf(fromId)));
		}
	}

	/**
	 * 执行 lua script: 锁定 第三方下发 到出款卡 使用
	 */
	private void llockForManual(Object fromId, Integer toId, Integer operator, Integer transInt, Integer expTM,
			boolean thirdDrawToOutcardFlag) throws Exception {
		String thirdDrawToOutcardFlagStr = "2";
		if (thirdDrawToOutcardFlag) {
			thirdDrawToOutcardFlagStr = "1";
		}
		String ret = redisSer.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOC_TRANS_LOCK";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return CommonUtils.getTransLockFromTo() ? LUA_SCRIPT_ALLOC_TRANS_LOCK_ : LUA_SCRIPT_ALLOC_TRANS_LOCK;
			}
		}, null, String.valueOf(fromId), String.valueOf(toId), String.valueOf(operator), String.valueOf(transInt),
				String.valueOf(AppConstants.USER_ID_4_ADMIN), String.valueOf(expTM), String.valueOf(expTM),
				RedisKeys.genPattern4TransferAccountLock_from(fromId),
				RedisKeys.genPattern4TransferAccountLock_to(toId),
				RedisKeys.genPattern4TransferAccountLock_operator(operator), RedisKeys.FROM_ACCOUNT_TRANS_RADIX_AMOUNT,
				RedisKeys.genPattern4TransferAccountLock(fromId, toId, operator),
				String.valueOf(System.currentTimeMillis()), String.valueOf(TransLock.STATUS_ALLOC),
				String.valueOf(TransLock.STATUS_ACK), String.valueOf(TransLock.STATUS_DEL), thirdDrawToOutcardFlagStr);
		if (!Objects.equals("ok", ret)) {
			throw new Exception(String.format("the account %s already locked.", String.valueOf(fromId)));
		}
	}

	/**
	 * 执行 lua script: 锁定
	 */
	private boolean llockUpdStatus(Object fromId, Integer toId, int status, long ex) {
		String ret = redisSer.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOC_TRANS_LOCK_UPDATE_STATUS";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOC_TRANS_LOCK_UPDATE_STATUS;
			}
		}, null, String.valueOf(fromId), String.valueOf(toId), String.valueOf(status), String.valueOf(ex),
				String.valueOf(TransLock.STATUS_DEL), RedisKeys.genPattern4TransferAccountLock_from(fromId),
				String.valueOf(TransLock.STATUS_ACK), String.valueOf(AppConstants.USER_ID_4_ADMIN),
				RedisKeys.TRANS_LOCK_DEL, String.valueOf(System.currentTimeMillis()));
		return StringUtils.equals("ok", ret);
	}

	private void ladd4ONeed(String tar, double scr) {
		redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_OUT_NEED_ORI).add(tar, scr);
	}

	private void ldel4ONeed(String tar) {
		redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_OUT_NEED_ORI).remove(tar);
	}

	private void ladd4ONeedNew(String tar, double scr) {
		redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI).add(tar, scr);
	}

	private void ldel4ONeedNew(String tar) {
		redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI).remove(tar);
	}

	/**
	 * 根据备用卡日累计转入总额加权
	 */
	private void buildExpAmtWithWeight(Map<Integer, Integer> retMap) {
		List<Object> idList = new ArrayList<>();
		for (Map.Entry ent : retMap.entrySet()) {
			idList.add(ent.getKey().toString());
		}
		Map<Integer, BigDecimal> incomeDailyMap = accSer.findAmountDaily(0, idList);
		for (Map.Entry<Integer, Integer> ent : retMap.entrySet()) {
			Integer key = ent.getKey();
			Integer value = ent.getValue();
			int sumAmt = Objects.isNull(incomeDailyMap.get(key)) ? 0 : incomeDailyMap.get(key).intValue();
			if (sumAmt < 100000) {
				retMap.put(key, value);
			} else if (sumAmt < 200000) {
				retMap.put(key, value + 100000000);
			} else if (sumAmt < 300000) {
				retMap.put(key, value + 200000000);
			} else if (sumAmt < 400000) {
				retMap.put(key, value + 300000000);
			} else if (sumAmt < 500000) {
				retMap.put(key, value + 400000000);
			} else if (sumAmt < 600000) {
				retMap.put(key, value + 500000000);
			} else if (sumAmt < 700000) {
				retMap.put(key, value + 600000000);
			} else if (sumAmt < 800000) {
				retMap.put(key, value + 700000000);
			} else if (sumAmt < 900000) {
				retMap.put(key, value + 800000000);
			} else {
				retMap.put(key, value + 900000000);
			}
		}
	}

	/**
	 * check bank statement.
	 * <p>
	 * <p>
	 * if param is null, return {@code false}
	 *
	 * @param base
	 *            account's base information.
	 * @return {@code true} if the account doesn't exist unmatching bank
	 *         statement,otherwise,{@code false}
	 */
	private boolean checkBankStatement(AccountBaseInfo base) {
		if (Objects.isNull(base) || !Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus()))
			return false;
		if (!base.checkMobile() && !checkPCFlowTimeFactor())
			return true;
		String id = String.valueOf(base.getId());
		Object log = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_LOG_TM).get(id);
		Object chg = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_BAL_CHG_TM).get(id);
		if (Objects.isNull(log) || Objects.isNull(chg))
			return false;
		Long logTm = Long.parseLong(log.toString()), chgTm = Long.parseLong(chg.toString());
		return (System.currentTimeMillis() - logTm <= 600000) && logTm >= (chgTm - 10000);
	}

	/**
	 * get change time of balance
	 *
	 * @param base
	 *            account's base information
	 * @return change time of balance
	 */
	private Long buildBalChgTm(AccountBaseInfo base) {
		if (Objects.isNull(base) || !Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus()))
			return null;
		Object chgTm = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_BAL_CHG_TM)
				.get(String.valueOf(base.getId()));
		return Objects.nonNull(chgTm) ? Long.valueOf(chgTm.toString()) : null;
	}

	/**
	 * inspect whether the factor <tt>PCFlowTime</tt> is opened.
	 *
	 * @return {@code true} open,otherwise,{@code false}
	 */
	private boolean checkPCFlowTimeFactor() {
		return Objects.equals("1", MemCacheUtils.getInstance().getSystemProfile()
				.getOrDefault(UserProfileKey.PC_FLOW_TIME_FACTOR.getValue(), "0"));
	}

	/**
	 * get time of inbank bal more the limitbal
	 *
	 * @param base
	 *            account's base information
	 * @return change time of balance
	 */
	private Long buildUpLimitBalTm(AccountBaseInfo base) {
		if (Objects.isNull(base) || !Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus()))
			return null;
		Object upLimitBal = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.INBANK_BAL_MORE_LIMITBAL_TM)
				.get(String.valueOf(base.getId()));
		return Objects.nonNull(upLimitBal) ? Long.valueOf(upLimitBal.toString()) : null;
	}

	/**
	 * 获取激活任务字符串
	 *
	 * @param base
	 * @param failIds
	 * @return
	 */
	private String buildActiveTransTo(AccountBaseInfo base, String failIds, Float amount, boolean filterBank) {
		Integer frZone = handiSer.findZoneByHandiId(base.getHandicapId());
		String result;
		// 一键转出amount不为空，只转给备用卡
		String targetKey = RedisKeys.ALLOCATING_OUTWARD_TARGET;
		String fromKey = RedisKeys.ALLOC_APPLY_BY_FROM;
		if (CommonUtils.checkDistHandicapNewVersion(base.getHandicapId())) {
			targetKey = RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET;
			fromKey = RedisKeys.ALLOC_NEW_APPLY_BY_FROM;
		}
		Set<Integer> exceptionIds = systemAccountManager.accountingException();
		Set<Integer> transUnExpire = buildUnExpireTransId();
		if (!CollectionUtils.isEmpty(transUnExpire)) {
			exceptionIds.addAll(transUnExpire);
		}
		log.debug("buildActiveTransTo exceptionIds:{}", exceptionIds);
		if (Objects.isNull(amount)) {
			Set<ZSetOperations.TypedTuple<String>> getAll = redisSer.getStringRedisTemplate().boundZSetOps(targetKey)
					.rangeWithScores(0, -1);
			result = buildActiveTransTo(base, getAll, failIds, frZone, true, amount, filterBank, exceptionIds);
			if (StringUtils.isNotEmpty(result)) {
				return result;
			}
		}
		Double[] refrto = new Double[] { enScore4Fr(RESERVEBANK, frZone, 0, 0, 0),
				enScore4Fr(RESERVEBANK + 1, frZone, 0, 0, 0) };
		Set<ZSetOperations.TypedTuple<String>> reBankS = redisSer.getStringRedisTemplate().boundZSetOps(fromKey)
				.rangeByScoreWithScores(refrto[0], refrto[1]);
		return buildActiveTransTo(base, reBankS, failIds, frZone, false, amount, filterBank, exceptionIds);
	}

	/**
	 * 获取激活任务字符串
	 *
	 * @param base
	 * @param set
	 * @param failIds
	 * @param frZone
	 * @return
	 */
	private String buildActiveTransTo(AccountBaseInfo base, Set<ZSetOperations.TypedTuple<String>> set, String failIds,
			Integer frZone, boolean isOutBank, Float amount, boolean isfilterBank, Set<Integer> exceptionIds) {
		String filterBank = CommonUtils.getActiveAccFilterBank();
		List<ZSetOperations.TypedTuple<String>> toList = new ArrayList<>();
		if (!CollectionUtils.isEmpty(set)) {
			toList.addAll(set);
			Collections.shuffle(toList);
		}
		for (ZSetOperations.TypedTuple<String> zet : toList) {
			Integer accId;
			if (isOutBank) {
				String value = zet.getValue();
				int type = Integer.parseInt(value.split(":")[0]);
				// 激活任务（amount为空）可以转给手机
				if (!(type == TARGET_TYPE_ROBOT || (Objects.isNull(amount) && type == TARGET_TYPE_MOBILE))) {
					log.debug("type equals robot | amount is null and type not mobile, id:{}", value);
					continue;
				}
				accId = Integer.parseInt(value.split(":")[1]);
			} else {
				accId = Integer.parseInt(zet.getValue());
			}
			log.debug("buildActiveTransTo is accId:{}", accId);
			if (Objects.equals(base.getId(), accId)) {
				log.debug("fromId:{} and toId:{} equal", base.getId(), accId);
				continue;
			}
			if (exceptionIds.contains(accId)) {
				log.debug("account is contains exceptionIds, toId:{}", accId);
				continue;
			}
			AccountBaseInfo tobase = accSer.getFromCacheById(accId);
			// 一键转出 一键下发（amount不为空）不转给手机
			// 目前一键转出和下发的只转给PC的出款卡和备用卡，有可能导致出款卡超过峰值
			if (Objects.isNull(tobase) || (tobase.checkMobile() && amount != null)
					|| (Objects.nonNull(tobase.getCurrSysLevel())
							&& !tobase.getCurrSysLevel().equals(CurrentSystemLevel.Designated.getValue())
							&& (Objects.isNull(amount) && !tobase.checkMobile()))
					|| Objects.equals(tobase.getType(), AccountType.InBank.getTypeId())) {
				log.debug(
						"tobase is null | tobase is mobile and amount is null | tobase amount is null and tobase is not mobile | tobase type is inbank, toId:{}",
						tobase.getId());
				continue;
			}
			Integer toZone = handiSer.findZoneByHandiId(tobase.getHandicapId());
			if (frZone == toZone
					&& (Objects.isNull(base.getCurrSysLevel()) || base.getCurrSysLevel() == tobase.getCurrSysLevel())
					&& (Objects.nonNull(amount)
							|| (!isfilterBank || isfilterBank && filterBank.contains(tobase.getBankType())))
					&& tobase.getStatus() == AccountStatus.Normal.getStatus()
					&& !failIds.contains("," + tobase.getId() + ",")
					&& !redisSer.getStringRedisTemplate().hasKey(RedisKeys.genTransExcludeAccount(tobase.getId()))) {
				log.debug("dealWIthTransLock is toId:{}", tobase.getId());
				if (Objects.nonNull(amount)) {
					dealWithTransLock(tobase.getId());
				} else {
					dealWithTransLock(tobase.getId(), true);
				}
				return base.getId() + ":" + tobase.getId() + ":" + base.getAccount() + ":" + tobase.getAccount() + ":"
						+ tobase.getOwner() + ":" + tobase.getBankType() + ":" + tobase.getBankName();
			}
		}
		return "";
	}

	private String getTransDecimal(String decimal) {
		// decimal的格式为 amount:timestamp
		int i = Integer.parseInt(decimal.split(":")[0]);
		if (i < 9) {
			i = i + 1;
			return "10.0" + i;
		} else if (i < 99) {
			i = i + 1;
			if (i == 50) {
				i = i + 1;
			}
			return "10." + i;
		} else {
			return "10.01";
		}
	}

	/**
	 * 备用卡金额超过2万时，找出款任务进行分配
	 *
	 * @param rSet
	 */
	private void alloc4ROver20K(Set<ZSetOperations.TypedTuple<String>> rSet, Set<Integer> outvSet) {
		if (CollectionUtils.isEmpty(rSet)) {
			return;
		}
		List<ZSetOperations.TypedTuple<String>> temp = new ArrayList<>(rSet);
		for (ZSetOperations.TypedTuple<String> r : temp) {
			// Number[0] type Number[1] zone Number[2] l Number[3] handi
			// Number[4] bal
			Integer bal = deScore4Fr(r.getScore())[4];
			if (bal >= 20000) {
				bal = Math.min(49500, bal - 50);
				Integer accId = Integer.parseInt(r.getValue());
				AccountBaseInfo frbase = accSer.getFromCacheById(Integer.valueOf(accId));
				boolean enable = CommonUtils.checkEnableInBankHandicap(frbase.getHandicapId());
				if (frbase == null) {
					continue;
				}
				if (!CollectionUtils.isEmpty(outvSet) && outvSet.contains(accId.toString())) {
					continue;
				}
				if (!enable) {
					continue;
				}
				if (frbase.getType() != RESERVEBANK) {
					continue;
				}
				boolean result = allocByAccAndBal(frbase, CommonUtils.getMinAmount4ReserveOver20K(), bal);
				if (result) {
					Iterator<ZSetOperations.TypedTuple<String>> itGet = rSet.iterator();
					while (itGet.hasNext()) {
						ZSetOperations.TypedTuple<String> val = itGet.next();
						if (Objects.equals(val.getValue(), accId.toString())) {
							itGet.remove();
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * 入款卡非正常分配任务
	 *
	 * @param inSet
	 */
	private void alloc4InOverCom(Set<ZSetOperations.TypedTuple<String>> inSet, Set<Integer> outvSet) {
		try {
			Map<Object, Object> accModel = accSer.getModel();
			if (CollectionUtils.isEmpty(inSet)) {
				return;
			}
			List<ZSetOperations.TypedTuple<String>> temp = new ArrayList<>(inSet);
			for (ZSetOperations.TypedTuple<String> r : temp) {
				Integer accId = Integer.parseInt(r.getValue());
				AccountBaseInfo frbase = accSer.getFromCacheById(Integer.valueOf(accId));
				if (Objects.isNull(frbase)) {
					continue;
				}
				if (!CollectionUtils.isEmpty(outvSet) && outvSet.contains(accId.toString())) {
					continue;
				}
				boolean enable = CommonUtils.checkEnableInBankHandicap(frbase.getHandicapId());
				if (!enable) {
					continue;
				}
				// subtype为非0用于区分普通入款卡和手机入款卡
				boolean mobileInBank = checkMobileInBank(frbase);
				Object model = accModel.get(r.getValue());
				if (Objects.isNull(model) && !mobileInBank) {
					continue;
				}
				if (frbase.getType() != INBANK) {
					continue;
				}
				String modelStr = model.toString();
				// model数据格式 两位，第一位为转账 第二位为抓流水 1表示手机 2表示PC
				if (!("2".equals(modelStr.substring(0, 1)) && "1".equals(modelStr.substring(1, 2))) && !mobileInBank) {
					continue;
				}
				// Number[0] type Number[1] zone Number[2] l Number[3] handi
				// Number[4] bal
				Integer bal = deScore4Fr(r.getScore())[4];
				int minBal = mobileInBank ? CommonUtils.getMobileInbankBeginTransBalance()
						: CommonUtils.getMaxBalanceForInbankCommonAllocate();
				int maxBal = CommonUtils.getMinBalanceForInbankToReserve();
				if (bal >= minBal && bal < maxBal) {
					bal = Math.min(49500, bal - 50);
					int transMin = mobileInBank ? bal - 50 : CommonUtils.getMinAmount4ReserveOver20K();
					boolean result = allocByAccAndBal(frbase, transMin, bal);
					Iterator<ZSetOperations.TypedTuple<String>> itGet = inSet.iterator();
					if ((mobileInBank && result) || !mobileInBank) {
						while (itGet.hasNext()) {
							ZSetOperations.TypedTuple<String> val = itGet.next();
							if (Objects.equals(val.getValue(), accId.toString())) {
								redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
										.remove(val.getValue());
								itGet.remove();
								break;
							}
						}
					}
				} else if (bal < minBal) {
					// 小于大额出款最低任务时删除，让走正常出款逻辑
					// isAliPay 小于 minBal时不会加入到 ALLOC_APPLY_BY_FROM中
					// 非 isAliPay 时，通过出款任务分配进行分配任务，这里删除其数据，使之不进行下发
					Iterator<ZSetOperations.TypedTuple<String>> itGet = inSet.iterator();
					while (itGet.hasNext()) {
						ZSetOperations.TypedTuple<String> val = itGet.next();
						if (Objects.equals(val.getValue(), accId.toString())) {
							itGet.remove();
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			log.debug("alloc4InOverCom >> exception msg" + e);
		}
	}

	/**
	 * 支付宝入款卡下发到出款卡
	 *
	 * @param inBankL
	 *            可转出结果集
	 * @param oneedList
	 *            差钱结果集
	 * @param accAlarm
	 *            流水告警结果集
	 * @param black
	 *            下发黑名单
	 * @param outvSet
	 *            转出黑名单
	 */
	private void alloc4MobileInOver(List<ZSetOperations.TypedTuple<String>> inBankL, List<Number[]> oneedList,
			Set<Integer> accAlarm, Set<String> black, Set<Integer> outvSet) {
		if (CollectionUtils.isEmpty(inBankL) || CollectionUtils.isEmpty(oneedList)) {
			log.trace("alloc4AliOver Alloc Regular all is null");
			return;
		}
		int len4i = inBankL.size();
		int minBal = buildMinBal(true).intValue();
		int toler = buildToler();
		for (int index = len4i - 1; index >= 0; index--) {
			ZSetOperations.TypedTuple<String> tasking = inBankL.get(index);
			if (tasking == null) {
				continue;
			}
			int frAccId = Integer.valueOf(tasking.getValue());
			AccountBaseInfo frAb = accSer.getFromCacheById(frAccId);
			if (Objects.isNull(frAb) || Objects.isNull(frAb.getType()) || !checkMobileInBank(frAb)
					|| frAb.getStatus() != AccountStatus.Normal.getStatus()) {
				continue;
			}
			if (!CollectionUtils.isEmpty(outvSet) && outvSet.contains(frAb.getId().toString())) {
				continue;
			}
			Boolean peerTrans = allOTaskSer.checkPeerTrans(frAb.getBankType());
			if (peerTrans == null) {
				boolean maintain = allOTaskSer.checkMaintain(frAb.getBankType());
				if (maintain) {
					continue;
				}
			}
			// [0]type [1]zone [2]level [3]handicap [4]bal
			Integer[] n = deScore4Fr(tasking.getScore());
			for (int j = 0; j < oneedList.size(); j++) {
				// [0]ID;[1]score;[2]priority;[3]tm;[4]need
				Number[] need = oneedList.get(j);
				int toId = need[0].intValue();
				AccountBaseInfo tobase = accSer.getFromCacheById(toId);
				if (Objects.isNull(tobase) || tobase.getStatus() != AccountStatus.Normal.getStatus()) {
					continue;
				}
				int toZone = handiSer.findZoneByHandiId(tobase.getHandicapId());
				if (n[1] != toZone) {
					continue;
				}
				if (n[2] != tobase.getCurrSysLevel()) {
					continue;
				}
				int toInt = n[4] - minBal;
				int oNd = need[4].intValue();
				if (toInt > oNd) {
					continue;
				}
				// 银行流水告警校验
				if (accAlarm.contains(need[0].intValue())) {
					log.debug("tar acc {} has problem with banklog", toId);
					continue;
				}
				// 校验黑名单
				if (!checkBlack(black, String.valueOf(frAccId), toId)) {
					log.debug("TransBlack{} InForce frId:{}", toId, frAccId);
					continue;
				}
				// 检测 同行转账
				if (!allOTaskSer.checkPeer(peerTrans, frAb, tobase)) {
					log.debug("TransPeer{} InForce", tobase.getId());
					continue;
				}
				try {
					lockTrans(frAccId, toId, AppConstants.USER_ID_4_ADMIN, toInt);
					log.info("alloc4AliOver >> O( {} , {} ) transFr{} transTo{} transInt:{}", frAccId, toId, frAccId,
							toId, toInt);
					redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_APPLY_BY_FROM)
							.remove(String.valueOf(frAccId));
					// post operation for to account
					ldel4ONeed(String.valueOf(toId));
					oneedList.remove(j);
					inBankL.remove(index);
					break;
				} catch (Exception e) {
					log.debug("alloc4AliOver  >> lock trans fail. frId:{},toId:{},amt:{}", frAccId, toId, toInt);
					continue;
				}
			}
		}
	}

	private boolean checkMobileInBank(AccountBaseInfo base) {
		if (Objects.isNull(base)) {
			return false;
		}
		if (Objects.isNull(base.getType()) || base.getType() != INBANK) {
			return false;
		}
		if ((base.getSubType() != null && base.getSubType() != 0) || (base.getFlag() != null && base.getFlag() == 2)) {
			return true;
		}
		return false;
	}

	// TODO 目前入款卡PC转账，手机抓流水，可以出款 后面要改
	private boolean checkCanOutwardTask(AccountBaseInfo base, BigDecimal bal) {
		if (base == null || bal == null || base.getFlag() == null || base.getFlag() != 1) {
			return false;
		}
		Object model = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_MODEL).get(base.getId().toString());
		if (model == null) {
			return false;
		}
		String modelStr = model.toString();
		// model数据格式 两位，第一位为转账 第二位为抓流水 1表示手机 2表示PC
		if ("2".equals(modelStr.substring(0, 1)) && "1".equals(modelStr.substring(1, 2))) {
			int maxAllocateBalance = CommonUtils.getMaxBalanceForInbankCommonAllocate();
			int minTaskAmount = CommonUtils.getMinTaskAmountForInbank();
			boolean overLimit = CommonUtils.getInbankOverLimitBeginAllocate();
			if (bal.intValue() >= minTaskAmount && bal.intValue() <= maxAllocateBalance
					&& (overLimit && bal.intValue() > allOTaskSer.buildLimitBalance(base) || !overLimit)) {
				return true;
			}
		}
		return false;
	}

	@Scheduled(fixedDelay = 60000)
	public void initQuickPay() throws InterruptedException {
		String nativeHost = CommonUtils.getInternalIp();
		if (!alloIAcntSer.checkHostRunRight()) {
			log.trace("the host {} have no right to execute the allocation transfer schedule at present.", nativeHost);
			Thread.sleep(5000L);
			return;
		}
		Set<String> keyList = redisSer.getStringRedisTemplate().keys(RedisKeys.YSF_INIT_TIME + "*");
		for (String strKey : keyList) {
			Set<ZSetOperations.TypedTuple<String>> timeSet = redisSer.getStringRedisTemplate().opsForZSet()
					.rangeWithScores(strKey, 0, -1);
			for (ZSetOperations.TypedTuple<String> valScr : timeSet) {
				Integer accountId = Integer.valueOf(valScr.getValue());
				BigDecimal bd = new BigDecimal(valScr.getScore());
				String initTime = bd.toString().substring(0, 8);
				Calendar c = Calendar.getInstance();
				String dateTime = String.valueOf(c.getTimeInMillis()).substring(0, 8);
				if (initTime.equals(dateTime)) {
					cabanaService.initQuickPay(accountId);
				}
			}
		}
	}

	@Override
	public void inOutModelCheck(String accountId, String oldModel) {
		AccountBaseInfo base = accSer.getFromCacheById(Integer.valueOf(accountId));
		log.debug("inOutModelCheck>> accountId {},oldModel {},accType {},subType {}", accountId, oldModel,
				base.getType(), base.getSubType());
		if (!base.getType().equals(INBANK)) {
			return;
		}
		if (base.getSubType().equals(InBankSubType.IN_BANK_YSF.getSubType())) {
			if (!(Constants.YSF_MODEL_OUT + StringUtils.EMPTY).equals(oldModel)) {
				cabanaService.inOutModel(Integer.valueOf(accountId), Constants.YSF_MODEL_OUT + StringUtils.EMPTY);
			}
			return;
		}
		if (base.getFlag().equals(AccountFlag.PC.getTypeId())) {
			return;
		}
		if (base.getFlag().equals(AccountFlag.PC.getTypeId())) {
			if (!(Constants.YSF_MODEL_IN + StringUtils.EMPTY).equals(oldModel)) {
				cabanaService.inOutModel(Integer.valueOf(accountId), Constants.YSF_MODEL_IN + StringUtils.EMPTY);
			}
			return;
		}
		Object model = redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL).get(accountId);
		int newModel = model == null ? Constants.YSF_MODEL_IN : Integer.valueOf(model.toString());
		log.debug("newModel {}, oldModel {} ", newModel, oldModel);
		if (Integer.valueOf(oldModel) != newModel) {
			cabanaService.inOutModel(Integer.valueOf(accountId), newModel + "");
		}
	}

	/**
	 * 连续转账失败的账号
	 *
	 * @return
	 */
	@Override
	public Set<Integer> buildFailureTrans() {
		int failure = CommonUtils.getFailureTransCount();
		if (failure == 0) {
			return new HashSet<>();
		}
		Set<Integer> res = redisSer.getFloatRedisTemplate().boundHashOps(RedisKeys.COUNT_FAILURE_TRANS).entries()
				.entrySet().stream().filter(p -> ((Float) p.getValue()).intValue() >= failure)
				.map(p -> Integer.parseInt(p.getKey().toString())).collect(Collectors.toSet());
		if (res == null) {
			res = new HashSet<>();
		}
		return res;
	}

	/**
	 * 校验连续转账失败次数
	 *
	 * @param failure
	 * @param accountId
	 * @return
	 */
	@Override
	public boolean checkFailureTrans(Set<Integer> failure, Integer accountId) {
		int counts = CommonUtils.getFailureTransCount();
		if (counts == 0) {
			return true;
		}
		if (CollectionUtils.isEmpty(failure)) {
			return true;
		}
		boolean flag = !failure.contains(accountId);
		log.debug("是否连续多次转账失败 :连续转账失败集合:{},id:{},是否在!flag:{}", failure, accountId, flag);
		return flag;
	}

	/**
	 * 处理下发锁数据，删除过期的锁和新增需锁的数据
	 *
	 * @param accId
	 */
	private void dealWithTransLock(Integer accId) {
		dealWithTransLock(accId, false);
	}

	/**
	 * 处理下发锁数据，删除过期的锁和新增需锁的数据
	 *
	 * @param accId
	 *            账号ID
	 * @param isTestTrans
	 *            是否测试转账
	 */
	private void dealWithTransLock(Integer accId, boolean isTestTrans) {
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		if (Objects.isNull(base)) {
			return;
		}
		long expired = System.currentTimeMillis() + TRANS_ACCOUNT_LOCK_EXPIRE;
		String redisKey;
		if (isTestTrans) {
			expired = System.currentTimeMillis() + TEST_TRANS_ACCOUNT_LOCK_EXPIRE;
			redisKey = RedisKeys.TEST_TRANSFER_ACCOUNT_LOCK_TIME;
		} else {
			redisKey = RedisKeys.TRANSFER_ACCOUNT_LOCK_TIME;
			if (base != null && base.getHolder() != null) {
				expired = System.currentTimeMillis() + TRANS_ACCOUNT_LOCK_EXPIRE_MANUL;
			}
		}
		long currTM = System.currentTimeMillis();
		List<String> expiredIds = redisSer.getStringRedisTemplate().boundHashOps(redisKey).entries().entrySet().stream()
				.filter(p -> Long.parseLong(p.getValue().toString()) < currTM).map(p -> p.getKey().toString())
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(expiredIds)) {
			redisSer.getStringRedisTemplate().boundHashOps(redisKey).delete(expiredIds.toArray());
		}
		redisSer.getStringRedisTemplate().boundHashOps(redisKey).put(accId.toString(), String.valueOf(expired));
	}

	/**
	 * 获取未过期的数据
	 */
	private Set<Integer> buildUnExpireTransId() {
		long currTM = System.currentTimeMillis();
		Set<Integer> expiredIds = redisSer.getStringRedisTemplate()
				.boundHashOps(RedisKeys.TEST_TRANSFER_ACCOUNT_LOCK_TIME).entries().entrySet().stream()
				.filter(p -> Objects.nonNull(p.getKey()) && Long.parseLong(p.getValue().toString()) > currTM)
				.map(p -> Integer.parseInt(p.getKey().toString())).collect(Collectors.toSet());
		expiredIds.addAll(redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.TRANSFER_ACCOUNT_LOCK_TIME).entries()
				.entrySet().stream()
				.filter(p -> Objects.nonNull(p.getKey()) && Long.parseLong(p.getValue().toString()) > currTM)
				.map(p -> Integer.parseInt(p.getKey().toString())).collect(Collectors.toSet()));
		return expiredIds;
	}
}