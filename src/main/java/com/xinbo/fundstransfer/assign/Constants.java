package com.xinbo.fundstransfer.assign;

import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;

public class Constants {
	public static final int Outter = CurrentSystemLevel.Outter.getValue();

	public static final int Inner = CurrentSystemLevel.Inner.getValue();

	public static final int NORMAL = AccountStatus.Normal.getStatus();

	public static final int UNDEPOSIT = OutwardTaskStatus.Undeposit.getStatus();
	
	public static final int BINDALI = AccountType.BindAli.getTypeId();
	public static final int BINDCOMMON = AccountType.BindCommon.getTypeId();
	public static final int BINDWECHAT = AccountType.BindWechat.getTypeId();
	public static final int THIRDCOMMON = AccountType.ThirdCommon.getTypeId();
	
	/**
	 * 常量：</br>
	 * ###入款卡:INBANK</br>
	 * ###备用卡:RESERVEBANK</br>
	 */
	public static final int OUTBANK = AccountType.OutBank.getTypeId();
	public static final int INBANK = AccountType.InBank.getTypeId();
	public static final int RESERVEBANK = AccountType.ReserveBank.getTypeId();
	public static final int OUTTHIRD = AccountType.OutThird.getTypeId();
	public static final int BINDCUSTOMER = AccountType.BindCustomer.getTypeId();	
	/**
	 * lua script:批量取出待出款任务</br>
	 * 1.从 分配队列 移动一定数量的任务 到临时队列</br>
	 * 2.返回 从分配队列 移动到 临时队列 中的 出款任务
	 */
	public static final String LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_POP = "local num = ARGV[1];\n"
			+ "local k1 = ARGV[2];\n" + "local k2 = ARGV[3];\n" + "local ret ='';\n"
			+ "local vl = redis.call('zrange',k1,0,tonumber(num)-1);\n" + "for i,v in pairs(vl) do\n"
			+ " if not v then\n" + "  break;\n" + " end\n" + " redis.call('hset',k2,v,'1');\n"
			+ " redis.call('zrem',k1,v);\n" + " if ret == '' and v ~= nil and v ~= ''  then" + "  ret = v;\n"
			+ " elseif ret ~= '' and v ~= '' and v ~= nil then\n" + "  ret = ret..';'..v;\n" + " end\n" + "end\n"
			+ "return ret;";
	/**
	 * lua script:出款卡上次出款任务维护 TODO 注意往AllocatingOutwardTask后面增加信息时，这个脚本要改
	 */
	public static final String LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_ORDER = "local keyTask = ARGV[1];\n"
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
	 * lua script:返回未分配的出款任务</br>
	 * 1.已分配任务 从 临时队列中 删除</br>
	 * 2.未分配任务 从 临时队列中 移动到 待分配队列</br>
	 */
	public static final String LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_BACK = "local key1 = ARGV[1];\n"
			+ "local key2 = ARGV[2];\n" + "local key3 = ARGV[3];\n" + "local ated = ARGV[4];\n"
			+ "local aing = ARGV[5];\n" + "local tar = ARGV[6];\n" + "if tar ~= nil and tar ~= '' then\n"
			+ " redis.call('zrem',key3,tar);\n" + "end\n" + "if ated ~= nil and ated ~= '' then\n"
			+ " local rt0 = {};\n" + " string.gsub(ated, '[^'..';'..']+', function(w) table.insert(rt0, w) end );\n"
			+ " for i0,v0 in pairs(rt0) do\n" + "  if v0 ~= nil and v0 ~= '' then\n"
			+ "   redis.call('hdel',key2,v0);\n" + "  end\n" + " end\n" + "end\n"
			+ "if aing == nil or aing == '' then\n" + " return 'ok';" + "end\n" + "local rt1 = {};\n"
			+ "string.gsub(aing, '[^'..';'..']+', function(w) table.insert(rt1, w) end );\n"
			+ "for i1,v1 in pairs(rt1) do\n" + " if v1 ~= nil and v1 ~= '' then\n" + "  redis.call('hdel',key2,v1);\n"
			+ "  redis.call('lpush',key1,v1);\n" + " end\n" + "end\n" + "return 'ok';";

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
	public static final String LUA_SCRIPT_ALLOC_TRANS_LOCK = "local fromId = ARGV[1];\n" + "local toId = ARGV[2];\n"
			+ "local opr = ARGV[3];\n" + "local transInt = ARGV[4];\n" + "local robot = ARGV[5];\n"
			+ "local timeRobot = ARGV[6];\n" + "local timeManual = ARGV[7];\n" + "local ptnFromId = ARGV[8];\n"
			+ "local ptnToId = ARGV[9];\n" + "local ptnOpr = ARGV[10];\n" + "local keyRadix = ARGV[11];\n"
			+ "local keyPtn = ARGV[12];\n" + "local dNow = ARGV[13];\n" + "local stAlc = ARGV[14];\n"
			+ "local stAck = ARGV[15];\n" + "local stDel = ARGV[16];\n"
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
			+ "    return 'error';\n" + "   end\n" + "  end\n" + " end\n" + "else\n"
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

	public static final String LUA_SCRIPT_ALLOC_TRANS_LOCK_ = "local fromId = ARGV[1];\n" + "local toId = ARGV[2];\n"
			+ "local opr = ARGV[3];\n" + "local transInt = ARGV[4];\n" + "local robot = ARGV[5];\n"
			+ "local timeRobot = ARGV[6];\n" + "local timeManual = ARGV[7];\n" + "local ptnFromId = ARGV[8];\n"
			+ "local ptnToId = ARGV[9];\n" + "local ptnOpr = ARGV[10];\n" + "local keyRadix = ARGV[11];\n"
			+ "local keyPtn = ARGV[12];\n" + "local dNow = ARGV[13];\n" + "local stAlc = ARGV[14];\n"
			+ "local stAck = ARGV[15];\n" + "local stDel = ARGV[16];\n"
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
			+ "    return 'error';\n" + "   end\n" + "  end\n" + " end\n" + "else\n"
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
	
	/***
	 * 格式：被分配对象数据格式{分类}:{ID} <br/>
	 * ###用户：TARGET_TYPE_USER:UserId <br/>
	 * ###机器：TARGET_TYPE_ROBOT:AccountId<br/>
	 * ###手机：TARGET_TYPE_MOBILE:AccountId<br/>
	 * ###重新分配：TARGET_TYPE_START:0<br/>
	 */
	public static final String QUEUE_FORMAT_USER_OR_ROBOT = "%d:%d";	
	
	/**
	 * 批量取出待出款任务数量
	 */
	public static final String NUM_LPOP = String.valueOf(1);
	
	/**
	 * 分类：被分配对象集合 分类;首次出款标识（FIRT_OUT_YES），人工出款标识（MANUAL_OUT_YES）<br/>
	 * ###用户：TARGET_TYPE_USER<br/>
	 * ###机器：TARGET_TYPE_ROBOT<br/>
	 * ###第三方：TARGET_TYPE_THIRD</br>
	 * ###手机：TARGET_TYPE_MOBILE</br>
	 */
	public static final int TARGET_TYPE_USER = 0, TARGET_TYPE_ROBOT = 1, TARGET_TYPE_START = 2, TARGET_TYPE_THIRD = 3,
			TARGET_TYPE_MOBILE = 4;	
	
	/**
	 * biz_account的out_enable字段 0-专注入款卡，1-非专注入款卡
	 */
	public static final byte IN_ONLY = 0, IN_TURN_OUT = 1;
	
	/**
	 * 云闪付出款模式model: 1-入款模式，2-出款模式
	 */
	public static final int YSF_MODEL_IN = 1, YSF_MODEL_OUT = 2;
	
	/**
	 * 常量：重新分配
	 */
	public static final String DUPLICATE_ALLOCATE = String.format(QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_START, 0);

	/**
	 * 任务需要等待的最大金额</br>
	 * （任务金额大于等于此值，直接分配，不需要等待）
	 */
	public static final int WAITING_AMT_MAX = 5000;
	/**
	 * 任务最大可以等待时间（2.5分钟）
	 */
	public static final int WAITING_MILIS_MAX = 150000;	
	/**
	 * 常量：<br/>
	 * ###外层:OUTTER<br/>
	 * ###内层:INNER<br/>
	 * ###中层:Middle<br/>
	 */
	public static final int OUTTER = CurrentSystemLevel.Outter.getValue(), INNER = CurrentSystemLevel.Inner.getValue(),
			MIDDLE = CurrentSystemLevel.Middle.getValue(), DESIGNATED = CurrentSystemLevel.Designated.getValue();	
	/**
	 * 标识：</br>
	 * 银行维护 BANK_MAINTAIN，</br>
	 * 银行维护恢复 BANK_MAINTAIN_RESORE
	 */
	public static final String BANK_MAINTAIN = "银行维护", BANK_MAINTAIN_RESORE = "银行维护恢复";
	/**
	 * 机器标识 ROBOT</br>
	 * 系统标识 SYS</br>
	 */
	public static final String ROBOT = "机器", SYS = "系统", THIRD = "第三方", MOBILE = "手机";	
	public static final String ALLOC_MERGE_LEVEL_LAST_TIME = "LastTime", ALLOC_MERGE_LEVEL_DEADLINE = "DeadLine";
	/**
     * 常量标识：出款任务分类</br>
     * 首次出款：FIRT_OUT_YES <br/>
     * 机器出款:ROBOT_OUT_YES</br>
     * 人工出款：MANUAL_OUT_YES<br/>
     * 第三方出款:THIRD_OUT_YES</br>
     */
	public static final int FIRT_OUT_YES = 1, ROBOT_OUT_YES = 0, MANUAL_OUT_YES = 1, THIRD_OUT_YES = 3;	
	/**
	 * 常量：<br/>
	 * ###锁定成功：LOCK_REQ_SUCCESS;<br/>
	 * ###锁定失败：LOCK_REQ_FAIL;<br/>
	 * ###重新分配：RE_ALLOCATE（请不要改动）<br/>
	 * ###财务确认:FIN_ACKED</br>
	 */
	public static final String LOCK_REQ_SUCCESS = "锁定成功", LOCK_REQ_FAIL = "锁定失败", RE_ALLOCATE = "重新分配";	
	
	/**
     * 常量：所有盘口标识；任意盘口
     */
	public static final int WILD_CARD_HANDICAP = 0;	
	
	/**
	 * 常量：所有账号
	 */
	public static final int WILD_CARD_ACCOUNT = 0;
	/**
	 * 最大误差 MAX_TOLERANCE(生产环境，测试环境：BigDecimal.TEN)</br>
	 */
	public static final int MAX_TOLERANCE = 500;
	
	/**
	 * 常量：锁定时间</br>
	 * 分配锁定时间:LOCK_ROBOT_ALLOC_SECONDS</br>
	 * 认领锁定时间:LOCK_ROBOT_CLAIM_SECONDS</br>
	 * 人工锁定时间:LOCK_MANUAL_SECONDS</br>
	 */
	public static final int LOCK_ROBOT_ALLOC_SECONDS = 65, LOCK_ROBOT_CLAIM_SECONDS = 300,
			LOCK_ROBOT_ACK_SECONDS = 1200, LOCK_ROBOT_DEL_SECONDS = 1800, LOCK_MANUAL_SECONDS = 1800;

	public static final int WILD_CARD_HANDI = 0;	
	
	public static final int NOT_OK = -1;
	public static final int OK = 1;
	public static final int INBANK_OK = 0;
	public static final int OK_BUT_BALANCE = 2;
	
	public static final long CLEAN_INVLD_DATA_INTR = 30000;	
	public static final String ALLOC_NEW_TRANS_CNST = "AllocNewTransCnst";
	/**
	 * 24小时：毫秒数
	 */
	public final static Long ONE_DAY_TIMESTAMP = 86400000L;
	
	/**
	 * Redis中出款卡的排序的键值
	 */
	public final static String OUT_ACCOUNT_ORDERED = "out_account_ordered";
	
	/**
	 * 出款卡当前轮次的出款累计金额
	 */
	public final static String OUT_ACCOUNT_MONEY = "out_account_money";
	
	/**
	 * 缺钱 && 有出款任务 && 余额小于任务金额
	 */
	public static final int O_SCR_N_MONY_Y_TASK_N_BAL = 2;
	/**
	 * 人工出款卡，缺钱
	 */
	public static final int O_SCR_MANUAL_N_MONY = 3;
	/**
	 * 缺钱 && 有出款任务
	 */
	public static final int O_SCR_N_MONY_Y_TASK = 4;
	/**
	 * 缺钱 && 无出款任务
	 */
	public static final int O_SCR_N_MONY_N_TASK = 5;
}
