package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.xinbo.fundstransfer.report.SystemAccountManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransLock;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;

@Service
public class AllocateTransferServiceImpl implements AllocateTransferService {
	private static final Logger log = LoggerFactory.getLogger(AllocateTransferServiceImpl.class);
	@Autowired
	private AccountRepository accDao;
	@Autowired
	private OutwardTaskRepository oTaskDao;
	@Autowired
	private OutwardRequestRepository oReqDao;
	@Autowired
	private RedisService redisSer;
	@Autowired
	@Lazy
	private AccountService accSer;
	@Autowired
	private HandicapService handSer;
	@Autowired
	private IncomeRequestService inReqSer;
	@Autowired
	@Lazy
	private AllocateOutwardTaskService allOTackTaskSer;
	@Autowired
	private RedisService redisService;
	@PersistenceContext
	private EntityManager entityManager;
	@Value("${funds.transfer.version}")
	private String CURR_VERSION;
	@Autowired
	private AccountChangeService accChgSer;
	@Autowired
	private SystemAccountManager systemAccountManager;
	private static Timer TIMER = new Timer();

	private static final int EXPR_INBANK = 120000, EXPR_ISSUE = 180000, EXPR_RSV = 360000, EXPR_OUTBANK = 240000;

	/**
	 * 常量：<br/>
	 * ###外层:OUTTER<br/>
	 * ###内层:INNER<br/>
	 * ###中层:MIDDLE</br>
	 */
	private static final int OUTTER = CurrentSystemLevel.Outter.getValue(), INNER = CurrentSystemLevel.Inner.getValue(),
			MIDDLE = CurrentSystemLevel.Middle.getValue();

	/**
	 * 常量:</br>
	 * ###正常:NORMAL</br>
	 * ###可用:ENABLED</br>
	 */
	private static final int NORMAL = AccountStatus.Normal.getStatus(), ENABLED = AccountStatus.Enabled.getStatus();

	private static final int UNDEPOSIT = OutwardTaskStatus.Undeposit.getStatus();

	/**
	 * 四天所对应的毫秒数
	 */
	private static final long FOUR_DAYS_MILIS = 4 * 24 * 60 * 60 * 1000;

	/**
	 * 常量：</br>
	 * ###出款卡:OUTBANK</br>
	 * ###出款第三方:OUTTHIRD</br>
	 * ###入款卡:INBANK</br>
	 * ###入款微信:INWECHAT</br>
	 * ###入款支付宝:INALI</br>
	 * ###支付宝体现卡:BINDALI</br>
	 * ###微信体现卡:BINDWECHAT</br>
	 * ###第三方体现卡:THIRDCOMMON</br>
	 * ###公共体现卡:BINDCOMMON</br>
	 * ###备用卡:RESERVEBANK</br>
	 */
	private static final int OUTBANK = AccountType.OutBank.getTypeId();
	private static final int INBANK = AccountType.InBank.getTypeId();
	private static final int INWECHAT = AccountType.InWechat.getTypeId();
	private static final int INALI = AccountType.InAli.getTypeId();
	private static final int BINDALI = AccountType.BindAli.getTypeId();
	private static final int BINDCOMMON = AccountType.BindCommon.getTypeId();
	private static final int BINDWECHAT = AccountType.BindWechat.getTypeId();
	private static final int THIRDCOMMON = AccountType.ThirdCommon.getTypeId();
	private static final int RESERVEBANK = AccountType.ReserveBank.getTypeId();

	/**
	 * 常量：</br>
	 * 单笔转账最大金额 TRANS_MAX_PER</br>
	 */
	private static final int TRANS_MAX_PER = 49990;

	/**
	 * 常量：锁定时间</br>
	 * 机器锁定时间:LOCK_ROBOT_SECONDS</br>
	 * 人工锁定时间:LOCK_MANUAL_SECONDS</br>
	 */
	private static final int LOCK_ROBOT_SECONDS = 300, LOCK_MANUAL_SECONDS = 1800;

	/**
	 * lua script:转账 解锁</br>
	 * 解锁业务逻辑：<br/>
	 * 机器解锁业务逻辑：<br/>
	 * 1.获取toId锁定记录</br>
	 * 2.条件过滤：操作者，fromId 均相同，则 删除该锁定记录</br>
	 * 人工解锁业务逻辑</br>
	 * 1.获取toId锁定记录</br>
	 * 2.条件过滤：非机器锁定记录，删除该锁定记录</br>
	 */
	private static final String LUA_SCRIPT_ALLOCATING_TRANSFER_UNLOCK = "local fromId = ARGV[1];\n"
			+ "local ptn4to = ARGV[2];\n" + "local opr = ARGV[3];\n" + "local robot = ARGV[4];\n"
			+ "local keys = redis.call('keys',ptn4to);\n" + "if keys == nil or next(keys) == nil then\n"
			+ " return 'ok';\n" + "end\n" + "for i0,v0 in pairs(keys) do\n" + " local inf = {};\n"
			+ " string.gsub(v0, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ " local opr1 = table.concat(inf, ':', 4, 4);\n" + " if opr == robot and opr1 == robot then\n"
			+ "  local fromId1 = table.concat(inf, ':', 2, 2);\n" + "  if fromId1 == fromId then\n"
			+ "   redis.call('del',v0);\n" + "  end\n" + " end\n" + " if opr ~= robot and opr1 ~= robot then"
			+ "  redis.call('del',v0);\n" + " end\n" + "end\n" + "return 'ok';\n";

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
	private static final String LUA_SCRIPT_ALLOCATING_TRANSFER_LOCK = "local fromId = ARGV[1];\n"
			+ "local toId = ARGV[2];\n" + "local opr = ARGV[3];\n" + "local transInt = ARGV[4];\n"
			+ "local robot = ARGV[5];\n" + "local timeRobot = ARGV[6];\n" + "local timeManual = ARGV[7];\n"
			+ "local ptnFromId = ARGV[8];\n" + "local ptnToId = ARGV[9];\n" + "local ptnOpr = ARGV[10];\n"
			+ "local keyRadix = ARGV[11];\n" + "local keyPtn = ARGV[12];\n" + "local dNow = ARGV[13];\n"
			+ "local lockNum = ARGV[14];\n" + "local isRobot = opr ~= nil and opr == robot;\n"
			+ "local keysToId = redis.call('keys',ptnToId);\n" + "if keysToId ~= nil and next(keysToId) ~= nil then\n"
			+ " for i0,v0 in pairs(keysToId) do\n" + "  local inf = {};\n"
			+ "  string.gsub(v0, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "  local opr1 = table.concat(inf, ':', 4, 4);\n" + "  if opr ~= nil and opr ~= opr1 then\n"
			+ "   return 'error';\n" + "  end\n" + " end\n" + "end\n" + "if isRobot then\n"
			+ " local keysFromId = redis.call('keys',ptnFromId);\n"
			+ " if keysFromId ~= nil and next(keysFromId) ~= nil then\n" + "  for i0,v0 in pairs(keysFromId) do\n"
			+ "   redis.call('del',v0);\n" + "  end\n" + " end\n" + "else\n"
			+ " local keysOpr = redis.call('keys',ptnOpr);\n" + " if keysOpr ~= nil and next(keysOpr) ~= nil then\n"
			+ "  for i0,v0 in pairs(keysOpr) do\n" + "   redis.call('del',v0);\n" + "  end\n" + " end\n" + "end\n"
			+ "local radix = nil;" + "local radixStr = redis.call('hget',keyRadix,fromId);\n"
			+ "if radixStr == nil or radixStr == '' or radixStr == false or radixStr == true or tonumber(radixStr) >=0.99 then\n"
			+ " radix = 0.01;\n" + "else\n" + " radix = tonumber(radixStr) + 0.01;\n" + "end\n"
			+ "radix = string.format('%.2f',radix);\n" + "redis.call('hset',keyRadix,fromId,radix);\n"
			+ "local inf = {};\n" + "string.gsub(keyPtn, '[^'..':'..']+', function(w) table.insert(inf, w) end );\n"
			+ "local info = table.concat(inf, ':', 1, 4);\n"
			+ "info = info..':'..radix..':'..transInt..':'..dNow..':'..lockNum;\n" + "local extime = 1800;\n"
			+ "if isRobot then\n" + " extime = tonumber(timeRobot);\n" + "else\n" + " extime = tonumber(timeManual);\n"
			+ "end\n" + "redis.call('set',info,'','EX',extime);\n" + "return 'ok';\n";

	/**
	 * 转账测试
	 */
	@Override
	public TransferEntity applyByTest(int fromId) {
		// 1、查询之前是否分配测试账号，之前有测试账户信息，这次获取时过滤响应的银行
		// 数据存储acc:owner:banktype;acc:owner:banktype
		String transTestInfo = redisSer.getString("TransTestInfo" + fromId);
		String filterBank = "";
		boolean exists = StringUtils.isNotBlank(transTestInfo);
		if (exists) {
			String[] bankinfo = transTestInfo.split(";");
			StringBuilder build = new StringBuilder();
			for (String str : bankinfo) {
				String bankType = StringUtils.substringAfterLast(str, ":");
				if ("中国银行".equals(bankType) || "农业银行".equals(bankType) || "工商银行".equals(bankType)
						|| "建设银行".equals(bankType) || "交通银行".equals(bankType) || "中信银行".equals(bankType)
						|| "平安银行".equals(bankType) || "浦发银行".equals(bankType) || build.toString().contains(bankType)) {
					continue;
				}
				build.append(",'").append(bankType).append("'");
			}
			if (StringUtils.isNotBlank(build.toString())) {
				filterBank = build.toString().substring(1);
			}
		}
		int zone = handSer.findZoneByHandiId(accSer.getFromCacheById(fromId).getHandicapId());
		String time = CommonUtils.getDateStr(new Date(System.currentTimeMillis() - 86400000));
		String querySQL = StringUtils.isNotBlank(filterBank)
				? "select t.account,t.owner,t.bank_type,t.id from biz_account t,biz_handicap t1 where t.id <> %s and t.handicap_id = t1.id and t1.zone = %s and t.type='5' and t.status = '5' and t.create_time <= '%s' and t.bank_type not in (%s) and IFNULL(t.flag,'0') <> '1' order by rand() LIMIT 1"
				: "select t.account,t.owner,t.bank_type,t.id from biz_account t,biz_handicap t1 where t.id <> %s and t.handicap_id = t1.id and t1.zone = %s and t.type='5' and t.status = '5' and t.create_time <= '%s' and IFNULL(t.flag,'0') <> '1' order by rand() LIMIT 1";
		String sql4Data = StringUtils.isNotBlank(filterBank) ? String.format(querySQL, fromId, zone, time, filterBank)
				: String.format(querySQL, fromId, zone, time);
		log.info("query test trans entity sql {}", sql4Data);
		List<Object> transList = entityManager.createNativeQuery(sql4Data).getResultList();
		if (transList.size() == 0) {
			log.info("can't get trans entity {} ", fromId);
			return null;
		}
		Object[] obj = (Object[]) transList.get(0);
		String acc = obj[0].toString().replaceAll(":", "").replaceAll(";", "").trim();
		String owner = obj[1].toString().replaceAll(":", "").replaceAll(";", "").trim();
		String bankType = obj[2].toString().replaceAll(":", "").replaceAll(";", "").trim();
		String toId = obj[3].toString();
		redisSer.setString("TransTestInfo" + fromId,
				exists ? transTestInfo + ";" + acc + ":" + owner + ":" + bankType : acc + ":" + owner + ":" + bankType,
				30, TimeUnit.MINUTES);
		TransferEntity ret = new TransferEntity();
		String amtSQL = "select max(t.property_value) from sys_user_profile t where t.property_key = 'TRANSFER_TEST_AMOUNT'";
		List<Object> amtList = entityManager.createNativeQuery(amtSQL).getResultList();
		String amt;
		if (amtList.size() == 0) {
			amt = CommonUtils.checkProEnv(CURR_VERSION) ? "10" : "1";
		} else {
			amt = (String) amtList.get(0);
		}
		ret.setAmount(new BigDecimal(amt).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
		ret.setFromAccountId(fromId);
		ret.setToAccountId(Integer.parseInt(toId));
		ret.setAccount(acc);
		ret.setOwner(owner);
		ret.setBankType(bankType);
		ret.setAcquireTime(System.currentTimeMillis());
		BigDecimal realBal = accChgSer.buildRelBal(fromId);
		systemAccountManager.regist(ret, realBal);
		return ret;
	}

	/**
	 * 机器转账申请
	 */
	@Override
	public synchronized TransferEntity applyByFrom(int fromId, BigDecimal reBal) {
		AccountBaseInfo fr;
		if (reBal == null || (fr = accSer.getFromCacheById(fromId)) == null || reBal.intValue() < buildTolerance()) {
			log.debug("AskTrans{} relBal:{} 账号为空/余额为空/小于下发阈值", fromId, reBal);
			return null;
		}
		int type = fr.getType();
		if (INBANK != type && INWECHAT != type && INALI != type && BINDALI != type && BINDWECHAT != type
				&& BINDCOMMON != type && THIRDCOMMON != type && RESERVEBANK != type) {
			log.debug("AskTrans{} relBal:{} 账号类型非法 入款卡/入款微信/入款支付宝/下发卡/备用卡/客戶綁定卡  type:{}", fromId, reBal, type);
			return null;
		}
		if (!Objects.equals(fr.getStatus(), AccountStatus.Normal.getStatus())) {
			log.debug("AskTrans{} relBal:{} 账号状态非法 入款卡/入款微信/入款支付宝/下发卡/备用卡/客戶綁定卡  type:{}", fromId, reBal, type);
			return null;
		}
		Boolean frPeer = allOTackTaskSer.checkPeerTrans(fr.getBankType());
		if (frPeer != null && !frPeer) {
			log.debug("AskTrans{} relBal:{} 同行转账{}", fromId, reBal, frPeer);
			return null;
		}
		int netBal = reBal.subtract(buildMinBal(true)).intValue();
		// 调度 转账任务 recrord[0]收款账号；recrord[1]转账金额整数部分； recrord[2]转账金额小数部分
		Object[] record = control(fr, netBal, frPeer);
		if (Objects.isNull(record)) {
			log.debug("AskTrans{} relBal:{} 未找到合适的转账任务", fromId, reBal);
			return null;
		}
		AccountBaseInfo to = (AccountBaseInfo) record[0];
		BigDecimal transInt = (BigDecimal) record[1], transRadix = (BigDecimal) record[2];
		TransferEntity ret = new TransferEntity();
		ret.setFromAccountId(fr.getId());
		ret.setToAccountId(to.getId());
		ret.setAccount(to.getAccount());
		ret.setOwner(to.getOwner());
		ret.setBankType(to.getBankType());
		ret.setBankAddr(to.getBankName());
		ret.setAmount(transInt.add(transRadix).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
		ret.setAcquireTime(System.currentTimeMillis());
		log.info("AskTrans{} >> AllocTransTask{}  frAcc:{} reBal:{} amt:{}", fr.getId(), to.getId(), fr.getAccount(),
				reBal, ret.getAmount());
		return ret;
	}

	/**
	 * 云端转账申请
	 *
	 * @param acc
	 *            账号
	 * @param handi
	 *            盘口编码
	 * @param l
	 *            内外层
	 * @param relBal
	 *            余额
	 * @return 转账实体
	 */
	@Override
	public TransferEntity applyByFrom(String acc, String handi, Integer l, BigDecimal relBal) {
		acc = CommonUtils.formatAccount(acc);
		handi = StringUtils.trimToNull(handi);
		if (acc == null || handi == null || l == null || relBal == null || relBal.compareTo(BigDecimal.TEN) < 0) {
			log.error("AskTransByAcc {} >> relBal:{} check the account|handi|blance|l input. handi:{} l:{}", handi, l);
			return null;
		}
		// 1.汇入账号账金额(基础数据)
		int netBal = relBal.subtract(buildMinBal(true)).intValue(), tolBal = buildTolerance();
		if (netBal < tolBal) {
			log.debug("AskTransByAcc {} >> relBal:{} balance is too low to transfer. netBal:{} tolBal:{}", acc, relBal,
					netBal, tolBal);
			return null;
		}
		// 2.汇入账号盘口(基础数据)
		BizHandicap handicap = handSer.findFromCacheByCode(handi);
		Integer toHand = Objects.nonNull(handicap) ? handicap.getId() : null;
		if (Objects.isNull(toHand)) {
			log.debug("AskTransByAcc {} >> relBal:{} toHand doesn't exist. handi:{}", acc, relBal, handi);
			return null;
		}
		// 3.汇入账号内外层（基础数据）
		CurrentSystemLevel cl = CurrentSystemLevel.valueOf(l);
		Integer toCl = Objects.nonNull(cl) ? cl.getValue() : null;
		if (Objects.isNull(toCl)) {
			log.debug("AskTransByAcc {} >> relBal:{} toCl doesn't exist. l:{}", acc, relBal, l);
			return null;
		}
		// 4.汇入账号黑名单(基础数据)
		Set<String> evading = findBlackList();
		// 5.汇入账号集(基础数据)
		Set<ZSetOperations.TypedTuple<String>> ll = redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.TRANS_AMT)
				.rangeWithScores(0, -1);
		// 6.下发调度
		log.debug("AskTransByAcc {} >> relBal:{} transfer to outAcc regularly, evading：{}", acc, relBal, evading);
		Object[] record = alloc4Regular(acc, netBal, toCl, toHand, OUTBANK, evading, 0, 99999, ll);
		if (Objects.isNull(record)) {
			log.debug("AskTransByAcc {} >> relBal:{} transfer to rsvAcc in force, evading：{}", acc, relBal, evading);
			record = alloc4InForce(acc, netBal, toCl, toHand, RESERVEBANK, evading);// 强制下发备用卡
		}
		if (Objects.isNull(record)) {
			log.debug("AskTransByAcc {} >> relBal:{} transfer to outAcc in force, evading：{}", acc, relBal, evading);
			record = alloc4InForce(acc, netBal, toCl, toHand, OUTBANK, evading);// 强制下发备用卡
		}
		if (Objects.isNull(record)) {
			log.info("AskTransByAcc {} >> relBal:{} no available account at present. evading:{}", acc, relBal, evading);
			return null;
		}
		AccountBaseInfo to = (AccountBaseInfo) record[0];
		BigDecimal transInt = (BigDecimal) record[1], transRadix = (BigDecimal) record[2];
		TransferEntity ret = new TransferEntity();
		ret.setToAccountId(to.getId());
		ret.setAccount(to.getAccount());
		ret.setOwner(to.getOwner());
		ret.setBankType(to.getBankType());
		ret.setBankAddr(to.getBankName());
		ret.setAmount(transInt.add(transRadix).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
		ret.setAcquireTime(System.currentTimeMillis());
		log.info("AskTransByAcc {} >> relBal:{} AllocTransTask{} amt:{}", acc, relBal, to.getId(), ret.getAmount());
		return ret;
	}

	/***
	 * 账号实时余额上报
	 *
	 * @param id
	 *            账号ID
	 * @param relBal
	 *            账号实时余额
	 * @param f
	 *            是否强制更新真实余额
	 */
	@Override
	public void applyRelBal(int id, BigDecimal relBal, boolean f) {
		if (relBal != null && relBal.compareTo(BigDecimal.ZERO) > 0 && (f || checkAvailAmt(id, relBal))) {
			try {
				ACC_AMT.put(id, relBal);
				ACC_ATM_QUENE.add(id);
				accDao.updateBankBalance(relBal, id);
			} catch (Exception e) {
				log.error("applyRelBal:{}", e);
			}
		}
		if (AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_AMT)) {
			try {
				THREAD_ACC_AMT.interrupt();
			} catch (Exception e) {
				log.error("账号实时余额上报线程停止异常" + e);
			} finally {
				THREAD_ACC_AMT = null;
			}
			return;
		} else if (Objects.nonNull(THREAD_ACC_AMT) && THREAD_ACC_AMT.isAlive()) {
			return;
		}
		THREAD_ACC_AMT = new Thread(() -> {
			StringBuilder infArr = null;
			for (;;) {
				try {
					Integer accId = ACC_ATM_QUENE.poll();
					if (Objects.nonNull(accId)) {
						BigDecimal accRBal = ACC_AMT.getIfPresent(accId);
						if (Objects.isNull(accRBal)) {
							continue;
						}
						AccountBaseInfo acc = accSer.getFromCacheById(accId);
						BigDecimal need = applyByTo(acc, accRBal);
						String inf = null;
						int type = acc.getType(), status = acc.getStatus();
						long curr = System.currentTimeMillis();
						long expr = curr + FOUR_DAYS_MILIS;
						if (NORMAL == status && (INBANK == type || BINDALI == type || BINDWECHAT == type
								|| BINDCOMMON == type || THIRDCOMMON == type)) {// 下发卡:3分钟;入款卡:1分钟
							BigDecimal rtoAmt = accRBal.intValue() < MAX_TOLERANCE ? BigDecimal.ZERO : accRBal;
							inf = packAccAmt(expr, acc, accRBal, rtoAmt, BigDecimal.ZERO, BigDecimal.ZERO,
									BigDecimal.ZERO, rtoAmt, rtoAmt, rtoAmt, curr);
						} else if ((type == OUTBANK && (status == NORMAL || status == ENABLED))
								|| (type == RESERVEBANK && status == NORMAL)) {// 出款卡:4分钟;备用卡:8分钟
							BigDecimal low = new BigDecimal(buildLowest(acc));
							BigDecimal high = new BigDecimal(buildHighest(acc));
							BigDecimal peak = new BigDecimal(buildPeak(acc));
							BigDecimal rtoAmt = subtract(accRBal, TRANS_ROBOT_BAL, BigDecimal.ZERO);
							BigDecimal rupToLow = max(need, subtract(low, accRBal, BigDecimal.ZERO));
							BigDecimal rupToLimit = max(need, subtract(high, accRBal, BigDecimal.ZERO));
							BigDecimal rupToPeak = max(need, subtract(peak, accRBal, BigDecimal.ZERO));
							BigDecimal rOverLow = subtract(accRBal, low, BigDecimal.ZERO);
							BigDecimal rOverLimit = subtract(accRBal, high, BigDecimal.ZERO);
							BigDecimal rOverPeak = subtract(accRBal, peak, BigDecimal.ZERO);
							inf = packAccAmt(expr, acc, accRBal, rtoAmt, rupToLow, rupToLimit, rupToPeak, rOverLow,
									rOverLimit, rOverPeak, curr);
						}
						if (StringUtils.isNotBlank(inf)) {
							if (Objects.isNull(infArr)) {
								infArr = new StringBuilder(inf);
							} else {
								infArr.append(";").append(inf);
							}
						}
					} else if (Objects.nonNull(infArr)) {
						initAccAmt(lacc(infArr.toString()));
						infArr = null;
					}
					if (!AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_AMT)) {
						if (Objects.isNull(accId)) {
							try {
								Thread.sleep(2000);
							} catch (Exception e) {
								log.error("账号实时余额上报线程休眠异常");
							}
						}
					} else if (Objects.isNull(infArr)) {
						break;
					}
				} catch (Exception e) {
					log.error("账号实时余额上报线程内部错误 " + e);
				}
			}
		});
		THREAD_ACC_AMT.setName("THREAD_ACC_AMT");
		THREAD_ACC_AMT.start();
	}

	/**
	 * 转账确认
	 */
	@Override
	public void ackByRobot(TransferEntity entity) {
		if (Objects.isNull(entity.getToAccountId())) {
			return;
		}
		long consume = 0;// 耗时(秒)
		if (Objects.nonNull(entity.getAcquireTime())) {
			consume = (System.currentTimeMillis() - entity.getAcquireTime()) / 1000;
		}
		boolean result = Objects.nonNull(entity.getResult()) && entity.getResult() == 1;// True:成功；False:失败；
		log.info("AckTransTask{}>>result:{} consume:{}秒 frId:{} reBal:{} amt:{}", entity.getToAccountId(), result,
				consume, entity.getFromAccountId(), entity.getBalance(), entity.getAmount());
		try {
			AccountBaseInfo to = accSer.getFromCacheById(entity.getToAccountId());
			int status = result ? IncomeRequestStatus.Matching.getStatus() : IncomeRequestStatus.Canceled.getStatus();
			String remark = result ? "下发成功" : "下发失败";
			if (!result) {
				// 转账失败：90秒内 汇款账号 不能向此账户转钱
				addToBlackListNew(entity.getFromAccountId(), entity.getToAccountId(), 90, TimeUnit.SECONDS, false);
			}
			Date time = entity.getTime() == null ? new Date() : entity.getTime();
			AccountBaseInfo from = accSer.getFromCacheById(entity.getFromAccountId());
			BizIncomeRequest o = new BizIncomeRequest();
			o.setFromId(entity.getFromAccountId());
			o.setToId(entity.getToAccountId());
			o.setHandicap(0);
			o.setLevel(0);
			o.setToAccount(to.getAccount());
			o.setOperator(null);
			o.setAmount(new BigDecimal(entity.getAmount()));
			o.setCreateTime(time);
			o.setOrderNo(String.valueOf(time.getTime()));
			o.setRemark(CommonUtils.genRemark(null, remark, new Date(), "机器"));
			o.setType(transToReqType(from.getType()));
			o.setFromAccount(from.getAccount());
			o.setMemberUserName(StringUtils.EMPTY);
			o.setMemberRealName(to.getOwner());
			o.setStatus(status);
			o.setToAccountBank(to.getBankName());
			inReqSer.save(o, true);
			// 解除 汇款账号 与 收款账号 之间转账锁定关系
			unlockTrans(entity.getFromAccountId(), entity.getToAccountId(), AppConstants.USER_ID_4_ADMIN);
		} catch (Exception e) {
			log.error("机器下发确认失败 ", e);
		}
	}

	/**
	 * 转账确认
	 */
	@Override
	public void ackByCloud(String frAcc, int toId) {
		TIMER.schedule(new TimerTask() {
			public void run() {
				try {
					cancelByCloud(frAcc, toId);
				} catch (Exception e) {
					log.error("", e.getMessage());
				}
			}
		}, 15000L);
	}

	/**
	 * 取消转账
	 */
	@Override
	public void cancelByCloud(String frAcc, int toId) throws Exception {
		frAcc = CommonUtils.formatAccount(frAcc);
		if (Objects.isNull(frAcc)) {
			return;
		}
		lunlock(frAcc, toId, AppConstants.USER_ID_4_ADMIN);
	}

	/**
	 * 获取转账金额
	 *
	 * @param from
	 *            汇款账号
	 * @param to
	 *            汇入账号
	 * @param operator
	 *            操作者 operator== ppConstants.USER_ID_4_ADMIN 代表机器
	 * @return BigDecimal[0] 汇款金额整数部分 BigDecimal[1] 汇款金额小数部分
	 */
	@Override
	public BigDecimal[] findTrans(int from, int to, int operator) {
		Set<String> keys = redisSer.getStringRedisTemplate()
				.keys(RedisKeys.genPattern4TransferAccountLock(from, to, operator));
		for (String p : keys) {
			BigDecimal[] trans = buildTrans(p);
			if (trans != null) {
				return trans;
			}
		}
		return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
	}

	private BigDecimal[] findTrans(String from, int to, int operator) {
		Set<String> keys = redisSer.getStringRedisTemplate()
				.keys(RedisKeys.genPattern4TransferAccountLock(from, to, operator));
		for (String p : keys) {
			BigDecimal[] trans = buildTrans(p);
			if (trans != null) {
				return trans;
			}
		}
		return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
	}

	/**
	 * 转账解锁
	 *
	 * @param toId
	 *            转入账号
	 */
	@Override
	public void unlockTrans(Integer fromId, Integer toId, Integer operator) throws Exception {
		if (toId == null) {
			log.error("转账解锁 信息不完整.");
			throw new Exception("转账解锁 信息不完整.");
		}
		lunlock(fromId, toId, operator != null ? operator : AppConstants.USER_ID_4_ADMIN);
	}

	/**
	 * 把账号添加到下发黑名单
	 *
	 * @param accId
	 *            账号ID
	 * @param canDel
	 *            是否能被自动删除
	 */
	@Override
	public void addToBlackList(int accId, boolean canDel) {
		log.info("把账号添加到下发黑名单 accId:{},是否可以被程序删除 {}", accId, canDel ? "是" : "否");
		addToBlackList(WILD_CARD_ACCOUNT, accId, 1, TimeUnit.DAYS, canDel);// 一天之内：任何账号都不能向此账号下发金额
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI).remove(accId + "");
	}

	/**
	 * fr 账号给指定任务出款加入黑名单
	 */
	@Override
	public void addToBlackList(int fr, long taskId, long expirSeconds) {
		redisSer.getStringRedisTemplate().boundValueOps(RedisKeys.gen4TransBlack(fr, taskId, 0)).set(StringUtils.EMPTY,
				expirSeconds, TimeUnit.SECONDS);
	}

	/**
	 * 把账号从下发黑名单中移除
	 *
	 * @param accId
	 *            账号ID
	 */
	@Override
	public void rmFrBlackList(int accId) {
		log.info("把账号从下发黑名单中移除 accId:{}", accId);
		Set<String> keys = redisSer.getStringRedisTemplate().keys(RedisKeys.genPattern4TransBlack_toId(accId));
		redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.OUTWARD_HIGH_PRIORITY).delete(accId + "");
		redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ISSUED_HIGH_PRIORITY).delete(accId + "");
		if (!CollectionUtils.isEmpty(keys)) {
			redisSer.getStringRedisTemplate().delete(keys);
		}
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
		return !redisSer.getStringRedisTemplate().hasKey(RedisKeys.gen4TransBlack(WILD_CARD_ACCOUNT, toId, 0) + ":0")
				&& !redisSer.getStringRedisTemplate()
						.hasKey(RedisKeys.gen4TransBlack(WILD_CARD_ACCOUNT, toId, 0) + ":1");
	}

	/**
	 * 下发黑名单中数据是否可以自动删除
	 *
	 * @param toId
	 *            账号ID
	 * @return true:可以自动删除</br>
	 *         false:不存在或不能被自动删除</br>
	 */
	@Override
	public boolean blackCanDel(int toId) {
		return redisSer.getStringRedisTemplate().hasKey(RedisKeys.gen4TransBlackCanDel(WILD_CARD_ACCOUNT, toId, 0));
	}

	/**
	 * 查询所有黑名单集
	 */
	@Override
	public Set<String> findBlackList() {
		return redisSer.getStringRedisTemplate().keys(RedisKeys.genPattern4TransBlack());
	}

	/**
	 * 检测:余额的有效性
	 */
	private boolean checkAvailAmt(int accId, BigDecimal amt) {
		AccountBaseInfo acc = accSer.getFromCacheById(accId);
		Long rptTime = ACC_TIME.getIfPresent(accId);
		if (acc != null && OUTBANK == acc.getType() && NORMAL == acc.getStatus()) {
			if (rptTime != null && System.currentTimeMillis() - rptTime >= 5000) {
				return true;
			}
		}
		if (rptTime == null || System.currentTimeMillis() - rptTime >= 40000) {
			return true;
		}
		BigDecimal his = ACC_AMT.getIfPresent(accId);
		return amt != null && amt.floatValue() >= 0 && (his == null || his.intValue() != amt.intValue());
	}

	private void addToBlackList(int frId, int toId, long expir, TimeUnit timeUnit, boolean canDel) {
		String canDel1Expire0 = canDel ? "1" : "0";
		redisSer.getStringRedisTemplate().boundValueOps(RedisKeys.gen4TransBlack(frId, toId, 0) + ":" + canDel1Expire0)
				.set(StringUtils.EMPTY, expir, timeUnit);
		AccountBaseInfo base = accSer.getFromCacheById(toId);
		if (base != null && base.getType() != null) {
			BigDecimal balance = accChgSer.buildRelBal(toId);
			if (balance != null && ((Objects.equals(base.getType(), 8)
					&& balance.intValue() > CommonUtils.getTriggerClearBalance4BindCommon())
					|| (!Objects.equals(base.getType(), 8)
							&& balance.intValue() > CommonUtils.getTriggerClearBalance4OtherCard()))) {
				if (Objects.equals(base.getType(), 5) || Objects.equals(base.getType(), 1)) {
					redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.OUTWARD_HIGH_PRIORITY).put(toId + "",
							balance.intValue() + "");
				} else {
					redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ISSUED_HIGH_PRIORITY).put(toId + "",
							balance.intValue() + "");
				}
			}
		}
	}
	
	private void addToBlackListNew(int frId, int toId, long expir, TimeUnit timeUnit, boolean canDel) {
		String canDel1Expire0 = canDel ? "1" : "0";
		redisSer.getStringRedisTemplate().boundValueOps(RedisKeys.gen4TransBlack(frId, toId, 0) + ":" + canDel1Expire0)
				.set(StringUtils.EMPTY, expir, timeUnit);
	}

	/**
	 * 转账调度控制器
	 * <p>
	 * 测试环境：frRbal 最低值为10;</br>
	 * 生产环境：frRbal 最低值为 MAX_TOLERANCE
	 * </p>
	 *
	 * @param fr
	 *            汇款账号基本信息
	 * @param netBal
	 *            汇款账号当前可转出金额
	 * @return object[0] 收款账号真实基本信息</br>
	 *         object[1] 汇款金额整数部分</br>
	 *         object[2] 汇款金额小数部分</br>
	 */
	private Object[] control(AccountBaseInfo fr, int netBal, Boolean frPeer) {
		// 1.汇入账号盘口(基础数据)
		int toHand = buildHandi(fr);
		// 2. 汇入账号内外层(基础数据)
		int toCl = Objects.isNull(fr.getCurrSysLevel()) ? OUTTER : fr.getCurrSysLevel();
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		// 3.汇入账号黑名单(基础数据)
		Set<String> evading = findBlackList();
		// 4.汇入账号集(基础数据)
		StringBuilder llSb = new StringBuilder();
		Set<ZSetOperations.TypedTuple<String>> ll = template.boundZSetOps(RedisKeys.TRANS_AMT).rangeWithScores(0, -1);
		if (ll != null) {
			ll.forEach(p -> llSb.append(";").append(p.getValue()).append(":").append(p.getScore()));
		}
		String llStr = llSb.toString();
		// 5.大额汇款(基础数据)
		int shareSize = buildShare(), shareNo = netBal / shareSize;
		int max = 99999, min = (shareNo >= 8 ? 7 : shareNo - 1) * shareSize;
		log.debug("AskTrans{} netBal:{} toHand:{} toCl:{} shareSize:{} shareNo:{} 汇入账号黑名单 {} llStr:{}", fr.getId(),
				netBal, toHand, toCl, shareSize, shareNo, evading, llStr);
		// 7.转账任务调度
		if (INBANK == fr.getType()) {// 入款银行卡转账调度
			if (shareNo >= 2) {// 生产：10000；测试：100
				log.debug(
						"AskTrans{} 任务调度(入款卡-大额->出款在用卡) frId:{} netBal:{} toCl:{} toHand:{} evading:{} min:{} max:{} ll:{} peer:{}",
						fr.getId(), fr.getId(), netBal, toCl, toHand, evading, min, max, llStr, frPeer);
				Object[] ret = alloc4Regular(fr, netBal, toCl, toHand, OUTBANK, NORMAL, evading, min, max, ll, frPeer);
				if (Objects.nonNull(ret)) {
					return ret;
				}
				if (shareNo == 2) {
					int max2 = (shareNo + 1) * shareSize;
					log.debug(
							"AskTrans{} 任务调度(入款卡-小额->出款在用卡) frId:{} netBal:{} toCl:{} toHand:{} evading:{} min:{} max:{} ll:{} peer:{}",
							fr.getId(), fr.getId(), netBal, toCl, toHand, evading, 0, max2, llStr, frPeer);
					ret = alloc4Regular(fr, netBal, toCl, toHand, OUTBANK, NORMAL, evading, 0, max2, ll, frPeer);
					if (Objects.nonNull(ret)) {
						return ret;
					}
				}
			} else {
				log.debug(
						"AskTrans{} 任务调度(入款卡-小额->出款在用卡) frId:{} netBal:{} toCl:{} toHand:{} evading:{} min:{} max:{} ll:{} peer:{}",
						fr.getId(), fr.getId(), netBal, toCl, toHand, evading, 0, max, llStr, frPeer);
				Object[] ret = alloc4Regular(fr, netBal, toCl, toHand, OUTBANK, NORMAL, evading, 0, max, ll, frPeer);
				if (Objects.nonNull(ret)) {
					return ret;
				}
			}
			log.debug("AskTrans{} 任务调度(入款卡-大额强制->备用在用卡) frId:{} netBal:{} toCl:{} toHand:{} evading:{} peer:{}",
					fr.getId(), fr.getId(), netBal, toCl, toHand, evading, frPeer);
			Object[] ret = alloc4InForce(fr, netBal, toCl, toHand, RESERVEBANK, NORMAL, evading, frPeer);// 强制下发备用卡
			if (Objects.nonNull(ret)) {
				return ret;
			}
			log.debug(
					"AskTrans{} 任务调度(入款卡-强制->出款在用卡) frId:{} netBal:{} toCl:{} toHand:{} evading:{} min:{} max:{} ll:{} peer:{}",
					fr.getId(), fr.getId(), netBal, toCl, toHand, evading, 0, max, llStr, frPeer);
			ret = alloc4InForce(fr, netBal, toCl, toHand, OUTBANK, NORMAL, evading, frPeer);// 强制下发备用卡
			if (Objects.nonNull(ret)) {
				return ret;
			}
		} else if (RESERVEBANK == fr.getType()) {// 备用卡转账调度
			if (shareNo >= 2) {
				log.debug(
						"AskTrans{} 任务调度(备用卡-大额->出款在用卡) frId:{} netBal:{} toCl:{} toHand:{} evading:{} min:{} max:{} ll:{} peer:{}",
						fr.getId(), fr.getId(), netBal, toCl, toHand, evading, min, max, llStr, frPeer);
				Object[] ret = alloc4Regular(fr, netBal, toCl, toHand, OUTBANK, NORMAL, evading, min, max, ll, frPeer);
				if (Objects.nonNull(ret)) {
					return ret;
				}
			}
			log.debug(
					"AskTrans{} 任务调度(备用卡-小额->出款在用卡) frId:{} netBal:{} toCl:{} toHand:{} evading:{} min:{} max:{} ll:{} peer:{}",
					fr.getId(), fr.getId(), netBal, toCl, toHand, evading, 0, max, llStr, frPeer);
			Object[] ret = alloc4Regular(fr, netBal, toCl, toHand, OUTBANK, NORMAL, evading, 0, max, ll, frPeer);
			if (Objects.nonNull(ret)) {
				return ret;
			}
		} else {// 下发卡：转账调度
			log.debug(
					"AskTrans{} 任务调度(下发卡-小额->出款可用卡) frId:{} netBal:{} toCl:{} toHand:{} evading:{} min:{} max:{} ll:{} peer:{}",
					fr.getId(), fr.getId(), netBal, toCl, toHand, evading, 0, max, llStr, frPeer);
			return alloc4Regular(fr, netBal, toCl, toHand, OUTBANK, NORMAL, evading, 0, max, ll, frPeer);
		}
		return null;
	}

	/**
	 * 转账任务分配
	 *
	 * @param fr
	 *            汇款账号基本信息
	 * @param netBal
	 *            汇款账号可以指出金额
	 * @param toCl
	 *            汇入账号所属层级
	 * @param toHad
	 *            汇入账号所属盘口
	 * @param toTyp
	 *            汇入账号分类
	 * @param toSts
	 *            汇入账号状态
	 * @param evading
	 *            转账需避开的关系(fr->to);
	 * @param min
	 *            转账金额最小值
	 * @param max
	 *            转账金额最大值
	 * @param ll
	 *            汇入账号集
	 * @param frPeer
	 *            汇款账号同行转账开关(null:)
	 * @return Object[0] toAccount</br>
	 *         Object[1] 转账金额 transInt</br>
	 *         Object[2] 转账小数 transRadix</br>
	 * @see com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo
	 * @see com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType
	 * @see com.xinbo.fundstransfer.domain.enums.AccountStatus
	 * @see com.xinbo.fundstransfer.domain.entity.BizHandicap
	 */
	private Object[] alloc4Regular(AccountBaseInfo fr, int netBal, int toCl, int toHad, int toTyp, int toSts,
			Set<String> evading, int min, int max, Set<ZSetOperations.TypedTuple<String>> ll, Boolean frPeer) {
		if (CollectionUtils.isEmpty(ll)) {
			log.debug("AskTrans{} Regular ll is null", fr.getId());
			return null;
		}
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		Double[] frToScore = enscores(toCl, toTyp, toSts, min, max);
		log.debug("AskTrans{} Regular toCl:{} toTyp:{} toSts:{} min:{} max:{} frToScore0:{} frToScore1:{}", fr.getId(),
				toCl, toTyp, toSts, min, max, frToScore[0], frToScore[1]);
		List<int[]> toFil = filterScore(ll, frToScore[0], frToScore[1]);
		if (CollectionUtils.isEmpty(toFil)) {
			log.debug("AskTrans{} Regular toFil size:0", fr.getId());
			return null;
		}
		for (int[] toInf : toFil) {
			// int[0]id;int[1]内外层;int[2]分类;int[3]状态;int[4]金额;int[5]盘口;int[6]时间min;int[7]urgent
			int toId = toInf[0];
			log.debug("AskTrans{} Regular toId:{}", fr.getId(), toId);
			// 检测 盘口
			if (AllocateOutwardTaskService.WILD_CARD_HANDICAP != toHad && toHad != toInf[5]) {
				log.debug("AskTrans{} Regular toHad:{} toInf5:{}", fr.getId(), toHad, toInf[5]);
				continue;
			}
			if (AllocateOutwardTaskService.WILD_CARD_HANDICAP == toHad && toHad != toInf[5]) {
				BizHandicap handicap = handSer.findFromCacheById(toInf[5]);
				if (Objects.nonNull(handicap) && AppConstants.EXCLUSIVE_HANDICAP.contains(handicap.getCode())) {
					log.debug("AskTrans{} Regular toHad:{} toInf5:{}", fr.getId(), toHad, toInf[5]);
					continue;
				}
			}
			// 余额 检测
			if (!checkBal(fr, toCl)) {
				log.debug("AskTrans{} Regular ( checkBal ) toHad:{} toInf5:{}", fr.getId(), toHad, toInf[5]);
				continue;
			}
			// 检测 黑名单
			if (!checkBlack(evading, fr.getId(), toId)) {
				log.debug("AskTrans{} TransBlack{} Regular", fr.getId(), toId);
				continue;
			}
			// 检测 类型/状态
			AccountBaseInfo to = accSer.getFromCacheById(toId);
			if (toTyp != to.getType() || toSts != to.getStatus()) {
				log.debug("AskTrans{} toId:{} toTyp:{} getType:{} toSts:{} getStatus:{}", fr.getId(), toId, toTyp,
						to.getType(), toSts, to.getStatus());
				continue;
			}
			// 检测 同行转账
			if (!allOTackTaskSer.checkPeer(frPeer, fr, to)) {
				log.debug("AskTrans{} TransPeer{} Regular", fr.getId(), toId);
				continue;
			}
			try {
				Integer transInt = Math.min(netBal, toInf[4]);// 转账金额整数部分
				log.debug("AskTrans{} TransLockIng{} Regular transInt:{}", fr.getId(), toId, transInt);
				lockTrans(fr.getId(), toId, AppConstants.USER_ID_4_ADMIN, transInt, toInf[7]);
				// 回写 toId所需金额
				if ((netBal + MAX_TOLERANCE) >= toInf[4]) {
					template.boundZSetOps(RedisKeys.TRANS_AMT).remove(String.valueOf(toId));
				} else {
					toInf[4] = toInf[4] - transInt;
					double score = enscore(toInf[1], toInf[2], toInf[3], toInf[4], toInf[5], toInf[6], toInf[7]);
					template.boundZSetOps(RedisKeys.TRANS_AMT).add(String.valueOf(toId), score);
				}
				BigDecimal[] trans = findTrans(fr.getId(), toId, AppConstants.USER_ID_4_ADMIN);// 获取转账整数与小数部分
				return trans[0].intValue() > 0 ? new Object[] { to, trans[0], trans[1] } : null;
			} catch (Exception e) {
				log.debug("AskTrans{} TransLockError{} Regular", fr.getId(), toId);// 锁定失败：抛出异常
			}
		}
		return null;
	}

	/**
	 * 转账任务分配
	 *
	 * @param frAcc
	 *            汇款账号
	 * @param netBal
	 *            汇款账号可以指出金额
	 * @param toCl
	 *            汇入账号所属层级
	 * @param toHad
	 *            汇入账号所属盘口
	 * @param toTyp
	 *            汇入账号分类
	 * @param evading
	 *            转账需避开的关系(fr->to);
	 * @param min
	 *            转账金额最小值
	 * @param max
	 *            转账金额最大值
	 * @param ll
	 *            汇入账号集
	 * @return Object[0] toAccount</br>
	 *         Object[1] 转账金额 transInt</br>
	 *         Object[2] 转账小数 transRadix</br>
	 * @see com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo
	 * @see com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType
	 * @see com.xinbo.fundstransfer.domain.enums.AccountStatus
	 * @see com.xinbo.fundstransfer.domain.entity.BizHandicap
	 */
	private Object[] alloc4Regular(String frAcc, int netBal, int toCl, int toHad, int toTyp, Set<String> evading,
			int min, int max, Set<ZSetOperations.TypedTuple<String>> ll) {
		if (CollectionUtils.isEmpty(ll)) {
			log.debug("AskTransByAcc {} Regular ll is empty", frAcc);
			return null;
		}
		Double[] frToScore = enscores(toCl, toTyp, NORMAL, min, max);
		log.debug("AskTransByAcc {} Regular toCl:{} toTyp:{} min:{} max:{} frToScore0:{} frToScore1:{}", frAcc, toCl,
				toTyp, min, max, frToScore[0], frToScore[1]);
		List<int[]> toFil = filterScore(ll, frToScore[0], frToScore[1]);
		if (CollectionUtils.isEmpty(toFil)) {
			log.debug("AskTransByAcc {} Regular toFil size:0", frAcc);
			return null;
		}
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		for (int[] toInf : toFil) {
			// int[0]id;int[1]内外层;int[2]分类;int[3]状态;int[4]金额;int[5]盘口;int[6]时间min;int[7]urgent
			int toId = toInf[0];
			log.debug("AskTransByAcc {} Regular toId:{}", frAcc, toId);
			// 检测 盘口
			if (AllocateOutwardTaskService.WILD_CARD_HANDICAP != toHad && toHad != toInf[5]) {
				log.debug("AskTransByAcc {} Regular toHad:{} toInf5:{}", frAcc, toHad, toInf[5]);
				continue;
			}
			// 检测 类型/状态
			AccountBaseInfo to = accSer.getFromCacheById(toId);
			if (toTyp != to.getType() || NORMAL != to.getStatus()) {
				log.debug("AskTransByAcc {} Regular toId:{} toTyp:{} getType:{} getStatus:{}", frAcc, toId, toTyp,
						to.getType(), to.getStatus());
				continue;
			}
			// 检测 黑名单
			if (!checkBlack(evading, frAcc, toId)) {
				log.debug("AskTransByAcc {} Regular Black {}", frAcc, toId);
				continue;
			}
			// 检测 同行转账
			if (Objects.nonNull(allOTackTaskSer.checkPeerTrans(to.getBankType()))) {
				log.debug("AskTransByAcc {} Regular Peer {}", frAcc, toId);
				continue;
			}
			try {
				Integer transInt = Math.min(netBal, toInf[4]);// 转账金额整数部分
				log.debug("AskTransByAcc {} Regular TransLockIng{} transInt:{}", frAcc, toId, transInt);
				lockTrans(frAcc, toId, AppConstants.USER_ID_4_ADMIN, transInt, toInf[7]);
				// 回写 toId所需金额
				if ((netBal + MAX_TOLERANCE) > toInf[4]) {
					template.boundZSetOps(RedisKeys.TRANS_AMT).remove(String.valueOf(toId));
				} else {
					toInf[4] = toInf[4] - transInt;
					double score = enscore(toInf[1], toInf[2], toInf[3], toInf[4], toInf[5], toInf[6], toInf[7]);
					template.boundZSetOps(RedisKeys.TRANS_AMT).add(String.valueOf(toId), score);
				}
				BigDecimal[] trans = findTrans(frAcc, toId, AppConstants.USER_ID_4_ADMIN);// 获取转账整数与小数部分
				return trans[0].intValue() > 0 ? new Object[] { to, trans[0], trans[1] } : null;
			} catch (Exception e) {
				log.debug("AskTransByAcc {} Regular TransLockError{}", frAcc, toId);// 锁定失败：抛出异常
			}
		}
		return null;
	}

	/**
	 * 强制下发
	 * 
	 * @param fr
	 *            汇出账号基本信息
	 * @param netBal
	 *            汇出账号可转金额
	 * @param toCl
	 *            汇入账号内外层
	 * @param toHad
	 *            汇入账号盘口
	 * @param toType
	 *            汇入账号分类
	 * @param toStatus
	 *            汇入账号状态
	 * @param evading
	 *            汇入账号黑名单
	 * @param frPeer
	 *            汇出账号同行转账状态
	 */
	private Object[] alloc4InForce(AccountBaseInfo fr, int netBal, int toCl, int toHad, int toType, int toStatus,
			Set<String> evading, Boolean frPeer) {
		if (!(OUTBANK == toType && NORMAL == toStatus || RESERVEBANK == toType && NORMAL == toStatus)) {
			log.debug("AskTrans{} InForce 参数非法 toHad:{} toType:{} toStatus:{}", fr.getId(), toHad, toType, toStatus);
			return null;
		}
		List<BizAccount> dataList = accSer.getAccountList(DynamicSpecifications.build(BizAccount.class,
				new SearchFilter("type", SearchFilter.Operator.EQ, toType),
				new SearchFilter("status", SearchFilter.Operator.EQ, toStatus)), null);
		Map<Integer, BigDecimal> accMap = new HashMap<>();
		int tolerance = buildTolerance();
		Predicate<BizAccount> fil = (p) -> (p.getCurrSysLevel() == null && toCl == OUTTER)
				|| (p.getCurrSysLevel() != null && p.getCurrSysLevel() == toCl);
		dataList.stream().filter(fil).forEach(p -> {
			AccountBaseInfo base = accSer.getFromCacheById(p.getId());
			int top = p.getType() == OUTBANK && p.getStatus() == NORMAL ? buildHighest(base) : buildPeak(base);
			BigDecimal bal = max(p.getBankBalance(), ACC_AMT.getIfPresent(p.getId()));
			bal = Objects.isNull(bal) ? BigDecimal.ZERO : bal;
			BigDecimal come = (BigDecimal) buildStat(base)[0];
			BigDecimal trans = buildTransing(p.getId());
			int need = top - bal.add(come).add(trans).intValue();
			if (need >= tolerance) {
				accMap.put(p.getId(), new BigDecimal(Math.min(Math.min(need, TRANS_MAX_PER), netBal)));
			}
		});
		if (CollectionUtils.isEmpty(accMap)) {
			log.debug("AskTrans{} InForce 没有卡需要钱 toHad:{} toType:{} toStatus:{}", fr.getId(), toHad, toType, toStatus);
			return null;
		}
		List<Map.Entry<Integer, BigDecimal>> accList = new ArrayList<>(accMap.entrySet());
		accList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		for (Map.Entry<Integer, BigDecimal> entry : accList) {
			log.debug("AskTrans{} InForce toId:{}", fr.getId(), entry.getKey());
			AccountBaseInfo to = accSer.getFromCacheById(entry.getKey());
			try {
				String toIdStr = String.valueOf(to.getId());
				// 校验黑名单
				if (!checkBlack(evading, fr.getId(), to.getId())) {
					log.info("TransBlack{} InForce frId:{}", to.getId(), fr.getId());
					continue;
				}
				// 检测 同行转账
				if (!allOTackTaskSer.checkPeer(frPeer, fr, to)) {
					log.info("TransPeer{} InForce", to.getId());
					continue;
				}
				if (AllocateOutwardTaskService.WILD_CARD_HANDICAP != toHad
						&& !Objects.equals(toHad, to.getHandicapId())) {
					log.debug("AskTrans{} Force toHad:{} toInf5:{}", fr.getId(), toHad, to.getHandicapId());
					continue;
				}
				if (AllocateOutwardTaskService.WILD_CARD_HANDICAP == toHad
						&& !Objects.equals(toHad, to.getHandicapId())) {
					BizHandicap handicap = handSer.findFromCacheById(to.getHandicapId());
					if (Objects.nonNull(handicap) && AppConstants.EXCLUSIVE_HANDICAP.contains(handicap.getCode())) {
						log.debug("AskTrans{} Force toHad:{} toInf5:{}", fr.getId(), toHad, to.getHandicapId());
						continue;
					}
				}
				// 计算 转账金额整数部分
				Integer transInt = entry.getValue().intValue();
				// 锁定 转账记录(锁定失败：抛出异常)
				log.info("TransLockIng{} InForce frId:{} amt:{}", to.getId(), fr.getId(), transInt);
				lockTrans(fr.getId(), to.getId(), AppConstants.USER_ID_4_ADMIN, transInt, TransLock.STATUS_REGULAR_);
				BigDecimal[] trans = findTrans(fr.getId(), to.getId(), AppConstants.USER_ID_4_ADMIN);
				Double score = template.boundZSetOps(RedisKeys.TRANS_AMT).score(toIdStr);
				if (Objects.nonNull(score)) {
					int[] toInf = descore(score);// [0]内/外层[1]账号分类[2]账号状态[3]金额[4]盘口[5]时间(min)[6]是否紧急0否1是
					if (toInf[6] == TransLock.STATUS_NEED_ && toInf[3] > transInt) {
						toInf[3] = toInf[3] - transInt < tolerance ? tolerance : (toInf[3] - transInt);
						score = enscore(toInf[0], toInf[1], toInf[2], toInf[3], toInf[4], toInf[5], toInf[6]);
						template.boundZSetOps(RedisKeys.TRANS_AMT).add(toIdStr, score);
					} else {
						template.boundZSetOps(RedisKeys.TRANS_AMT).remove(toIdStr);
					}
				}
				return trans[0].intValue() > 0 ? new Object[] { to, trans[0], trans[1] } : null;
			} catch (Exception e) {
				log.error("TransLockError{} InForce frId:{} ", to.getId(), fr.getId());
			}
		}
		return null;
	}

	/**
	 * 强制下发
	 *
	 * @param frAcc
	 *            汇出账号
	 * @param netBal
	 *            汇出账号可转金额
	 * @param toCl
	 *            汇入账号内外层
	 * @param toHad
	 *            汇入账号盘口
	 * @param toType
	 *            汇入账号分类
	 * @param evading
	 *            汇入账号黑名单
	 */
	private Object[] alloc4InForce(String frAcc, int netBal, int toCl, int toHad, int toType, Set<String> evading) {
		if (OUTBANK != toType && RESERVEBANK != toType) {
			log.debug("AskTransByAcc {} InForce 参数非法 toHad:{} toType:{}", frAcc, toHad, toType);
			return null;
		}
		List<BizAccount> dataList = accSer.getAccountList(DynamicSpecifications.build(BizAccount.class,
				new SearchFilter("handicapId", SearchFilter.Operator.EQ, toHad),
				new SearchFilter("type", SearchFilter.Operator.EQ, toType),
				new SearchFilter("status", SearchFilter.Operator.EQ, NORMAL)), null);
		if (CollectionUtils.isEmpty(dataList)) {
			log.debug("AskTransByAcc {} InForce doesn't exist available account. toHad:{} toType:{} toStat:{}", frAcc,
					toHad, toType, NORMAL);
			return null;
		}
		Map<Integer, BigDecimal> accMap = new HashMap<>();
		int tolBal = buildTolerance();
		Predicate<BizAccount> fil = (p) -> p.getCurrSysLevel() == null && toCl == OUTTER || p.getCurrSysLevel() == toCl;
		dataList.stream().filter(fil).forEach(p -> {
			AccountBaseInfo base = accSer.getFromCacheById(p.getId());
			int top = p.getType() == OUTBANK ? buildHighest(base) : buildPeak(base);
			BigDecimal bal = max(p.getBankBalance(), ACC_AMT.getIfPresent(p.getId()));
			BigDecimal trans = buildTransing(p.getId());
			BigDecimal come = (BigDecimal) buildStat(base)[0];
			int need = top - bal.add(come).add(trans).intValue();
			if (need >= tolBal) {
				accMap.put(p.getId(), new BigDecimal(Math.min(Math.min(need, TRANS_MAX_PER), netBal)));
			}
		});
		if (CollectionUtils.isEmpty(accMap)) {
			log.debug("AskTransByAcc {} InForce 没有卡需要钱 toHad:{} toType:{}", frAcc, toHad, toType);
			return null;
		}
		List<Map.Entry<Integer, BigDecimal>> accList = new ArrayList<>(accMap.entrySet());
		accList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		for (Map.Entry<Integer, BigDecimal> entry : accList) {
			log.debug("AskTransByAcc {} InForce toId:{}", frAcc, entry.getKey());
			AccountBaseInfo to = accSer.getFromCacheById(entry.getKey());
			try {
				String toIdStr = String.valueOf(to.getId());
				// 校验黑名单
				if (!checkBlack(evading, frAcc, to.getId())) {
					log.info("AskTransByAcc {} InForce Black toId:{}", frAcc, to.getId());
					continue;
				}
				// 检测 同行转账
				if (Objects.nonNull(allOTackTaskSer.checkPeerTrans(to.getBankType()))) {
					log.debug("AskTransByAcc {} InForce Peer{}", frAcc, to.getId());
					continue;
				}
				// 计算 转账金额整数部分
				Integer transInt = entry.getValue().intValue();
				// 锁定 转账记录(锁定失败：抛出异常)
				log.info("AskTransByAcc {} InForce TransLockIng{} amt:{}", frAcc, to.getId(), transInt);
				lockTrans(frAcc, to.getId(), AppConstants.USER_ID_4_ADMIN, transInt, TransLock.STATUS_REGULAR_);
				BigDecimal[] trans = findTrans(frAcc, to.getId(), AppConstants.USER_ID_4_ADMIN);
				Double score = template.boundZSetOps(RedisKeys.TRANS_AMT).score(toIdStr);
				if (Objects.nonNull(score)) {
					int[] toInf = descore(score);// [0]内/外层[1]账号分类[2]账号状态[3]金额[4]盘口[5]时间(min)[6]是否紧急0否1是
					if (toInf[6] == TransLock.STATUS_NEED_ && transInt < toInf[3]) {
						toInf[3] = toInf[3] - transInt < tolBal ? tolBal : (toInf[3] - transInt);
						score = enscore(toInf[0], toInf[1], toInf[2], toInf[3], toInf[4], toInf[5], toInf[6]);
						template.boundZSetOps(RedisKeys.TRANS_AMT).add(toIdStr, score);
					} else {
						template.boundZSetOps(RedisKeys.TRANS_AMT).remove(toIdStr);
					}
				}
				return trans[0].intValue() > 0 ? new Object[] { to, trans[0], trans[1] } : null;
			} catch (Exception e) {
				log.error("AskTransByAcc {} InForce TransLockError{}", frAcc, to.getId());
			}
		}
		return null;
	}

	private BigDecimal applyByTo(AccountBaseInfo base, BigDecimal bankBal) {
		try {
			int type = base.getType(), status = base.getStatus();
			if (OUTBANK != type && RESERVEBANK != type) {
				return null;
			}
			String kv = String.valueOf(base.getId());
			if ((OUTBANK == type && NORMAL != status && ENABLED != status)
					|| (RESERVEBANK == type && NORMAL != status)) {
				redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.TRANS_AMT).remove(kv);
				return null;
			}
			int maxBal, accountId = base.getId(), tolerance = buildTolerance();
			BigDecimal transing = buildTransing(accountId);
			if (OUTBANK == type && NORMAL == status) {
				maxBal = buildHighest(base);
				BizOutwardTask task = oTaskDao.applyTask(base.getId(), UNDEPOSIT);
				if (Objects.nonNull(task)) {
					int taskAmt = task.getAmount().intValue();
					int inMapping = ((BigDecimal) (buildStat4OAccInTask(base)[0])).intValue();
					int virBal = bankBal.add(transing).subtract(buildOverBal()).intValue() + inMapping;
					if (virBal < taskAmt) {
						int need = maxBal > taskAmt ? (maxBal - virBal) : taskAmt - virBal;
						if (need < tolerance) {
							need = tolerance;
						}
						log.info("ApplyTransTask{} >> taskAmt:{} reBal:{} need:{}", base.getId(), taskAmt, bankBal,
								need);
						BizOutwardRequest oReq = oReqDao.findOne(task.getOutwardRequestId());
						Date d = Objects.nonNull(oReq) ? oReq.getCreateTime() : task.getAsignTime();
						int l = Objects.isNull(base.getCurrSysLevel()) ? OUTTER : base.getCurrSysLevel();
						double score = enscore(l, base.getType(), base.getStatus(), need, base.getHandicapId(), d,
								TransLock.STATUS_NEED_);
						redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.TRANS_AMT).add(kv, score);
						return new BigDecimal(need);
					}
					return null;
				} else if (bankBal.intValue() >= buildLowest(base)) {
					redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.TRANS_AMT).remove(kv);
					return null;
				}
			} else {
				maxBal = buildPeak(base);
			}
			// 校验:汇入，汇出统计数据
			int inMapping = ((BigDecimal) (buildStat(base)[0])).intValue();
			// 校验：所需金额
			int need = maxBal - bankBal.intValue() - inMapping - transing.intValue();
			if (need <= 0) {
				redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.TRANS_AMT).remove(kv);
				return null;
			} else if (tolerance > need) {
				need = tolerance;
			}
			log.info("ApplyTransNormal{} >> reBal:{} need:{}", base.getId(), bankBal, need);
			int l = Objects.isNull(base.getCurrSysLevel()) ? OUTTER : base.getCurrSysLevel();
			int hand = Objects.isNull(base.getHandicapId()) ? AllocateOutwardTaskService.WILD_CARD_HANDICAP
					: base.getHandicapId();
			double score = enscore(l, base.getType(), base.getStatus(), need, hand, null, TransLock.STATUS_REGULAR_);
			redisSer.getStringRedisTemplate().boundZSetOps(RedisKeys.TRANS_AMT).add(kv, score);
		} catch (Exception e) {
			log.error("出款卡/备用卡 计算所需金额出错 " + e);
		}
		return null;
	}

	/**
	 * 获取:账号预留余额设置</br>
	 * 生产环境：isRobot ? TRANS_ROBOT_BAL : TRANS_MANUAL_BAL</br>
	 * 测试环境：BigDecimal.ONE
	 */
	private BigDecimal buildMinBal(boolean isRobot) {
		if (CommonUtils.checkProEnv(CURR_VERSION)) {
			return isRobot ? TRANS_ROBOT_BAL : TRANS_MANUAL_BAL;
		} else {
			return BigDecimal.ONE;
		}
	}

	private BigDecimal buildOverBal() {
		return CommonUtils.checkProEnv(CURR_VERSION) ? new BigDecimal(MAX_TOLERANCE * 2) : BigDecimal.TEN;
	}

	private int buildShare() {
		return CommonUtils.checkProEnv(CURR_VERSION) ? 5000 : 50;
	}

	/**
	 * 获取：下发最小金额
	 */
	private int buildTolerance() {
		return CommonUtils.checkProEnv(CURR_VERSION) ? MAX_TOLERANCE : BigDecimal.TEN.intValue();
	}

	/**
	 * 获取：出款卡最大余额
	 */
	private Integer buildHighest(AccountBaseInfo base) {
		if (base.getLimitBalance() != null && base.getLimitBalance() > 0) {
			return base.getLimitBalance();
		}
		return TRANS_MAX_PER;
	}

	/**
	 * 获取：余额峰值
	 * <p>
	 * 当账号基本信息的余额峰值为空 或余额峰值小于出款卡最大额度时，则取出款卡最大余额 </br>
	 * 参考 @see this#buildHighest(AccountBaseInfo base)
	 * </p>
	 *
	 * @param base
	 *            账号基本信息
	 */
	private Integer buildPeak(AccountBaseInfo base) {
		Integer limitBal = buildHighest(base);
		if (base.getPeakBalance() == null || base.getPeakBalance() < limitBal) {
			return limitBal;
		}
		return base.getPeakBalance();
	}

	/**
	 * 获取：出款卡最低余额，低于此设置通知下发人员
	 */
	private Integer buildLowest(AccountBaseInfo base) {
		if (base.getLowestOut() != null) {
			return base.getLowestOut();
		}
		return Integer.parseInt(
				MemCacheUtils.getInstance().getSystemProfile().get(UserProfileKey.OUTDRAW_SYSMONEY_LOWEST.getValue()));
	}

	/**
	 * 获取：出款卡（汇入，汇出统计信息）
	 *
	 * @return BigDecimal[0] inMapping, igDecimal[1] outMapping
	 */
	private Object[] buildStat(AccountBaseInfo base) {
		String time;
		Long crawlTime = ACC_TIME.getIfPresent(base.getId());
		if (Objects.isNull(crawlTime)) {
			time = CommonUtils.getStartTimeOfCurrDay();
		} else {
			int type = base.getType(), status = base.getStatus();
			if (NORMAL == status && INBANK == type) {
				time = CommonUtils.getDateFormat2Str(new Date(crawlTime - EXPR_INBANK));
			} else if (NORMAL == status
					&& (BINDALI == type || BINDCOMMON == type || BINDWECHAT == type || THIRDCOMMON == type)) {
				time = CommonUtils.getDateFormat2Str(new Date(crawlTime - EXPR_ISSUE));
			} else if (NORMAL == status && OUTBANK == type) {
				time = CommonUtils.getDateFormat2Str(new Date(crawlTime - EXPR_OUTBANK));
			} else if (NORMAL == status && RESERVEBANK == type) {
				time = CommonUtils.getDateFormat2Str(new Date(crawlTime - EXPR_RSV));
			} else {
				time = CommonUtils.getStartTimeOfCurrDay();
			}
		}
		String FMT_OUT_STAT = "select IFNULL((select sum(amount)inMapping from biz_income_request where status=%d and to_id=%d and create_time>='%s'),0)inMapping,IFNULL((select sum(amount) outMapping from biz_outward_task where status=%d and account_id=%d),0)outMapping from dual";
		String SQL = String.format(FMT_OUT_STAT, IncomeRequestStatus.Matching.getStatus(), base.getId(), time,
				OutwardTaskStatus.Deposited.getStatus(), base.getId());
		return (Object[]) entityManager.createNativeQuery(SQL).getSingleResult();
	}

	private Object[] buildStat4OAccInTask(AccountBaseInfo base) {
		String time = CommonUtils.getDateFormat2Str(new Date(System.currentTimeMillis() - 20000));
		String FMT_OUT_STAT = "select IFNULL((select sum(amount)inMapping from biz_income_request where status=%d and to_id=%d and create_time>='%s'),0)inMapping,IFNULL((select sum(amount) outMapping from biz_outward_task where status=%d and account_id=%d),0)outMapping from dual";
		String SQL = String.format(FMT_OUT_STAT, IncomeRequestStatus.Matching.getStatus(), base.getId(), time,
				OutwardTaskStatus.Deposited.getStatus(), base.getId());
		return (Object[]) entityManager.createNativeQuery(SQL).getSingleResult();
	}

	/**
	 * 获取：正在下发金额（已被人或机器领取，但未确认给系统）
	 */
	private BigDecimal buildTransing(int accountId) {
		BigDecimal result = BigDecimal.ZERO;
		String ptn = RedisKeys.genPattern4TransferAccountLock_to(accountId);
		for (String p : redisSer.getStringRedisTemplate().keys(ptn)) {
			BigDecimal[] trans = buildTrans(p);
			if (trans != null) {
				result = result.add(trans[0]);
			}
		}
		return result;
	}

	/**
	 * 获取：下发金额整数部分；下发金额小数部分
	 *
	 * @return BigDecimal[0] 下发金额整数部分，BigDecimal[1] 下发金额小数部分
	 */
	private BigDecimal[] buildTrans(String p) {
		String[] inf = p == null ? null : p.split(":");
		if (inf != null && inf.length >= 6 && inf[4] != null && inf[5] != null) {
			return new BigDecimal[] { new BigDecimal(inf[5]), new BigDecimal(inf[4]) };
		}
		return null;
	}

	/**
	 * 判断frId与toId 之间是否存在转账关系
	 *
	 * @param black
	 *            所有黑名单集
	 * @param frId
	 *            汇出账号
	 * @param toId
	 *            汇入账号
	 * @return true:存在转账关系；false:不存在转账关系
	 */
	@Override
	public boolean checkBlack(Set<String> black, int frId, int toId) {
		boolean ret = CollectionUtils.isEmpty(black)
				|| (black.stream().filter(p -> p.startsWith(RedisKeys.gen4TransBlack(WILD_CARD_ACCOUNT, toId, 0))
						|| p.startsWith(RedisKeys.gen4TransBlack(frId, toId, 0))).count() == 0);
		return ret;
	}

	@Override
	public boolean checkBlack(Set<String> black, String frAcc, int toId) {
		boolean ret = CollectionUtils.isEmpty(black)
				|| (black.stream().filter(p -> p.startsWith(RedisKeys.gen4TransBlack(WILD_CARD_ACCOUNT, toId, 0))
						|| p.startsWith(RedisKeys.gen4TransBlack(frAcc, toId, 0))).count() == 0);
		return ret;
	}

	@Override
	public boolean checkBlack(Set<String> black, int frAcc, long taskId) {
		boolean ret = CollectionUtils.isEmpty(black)
				|| (black.stream().filter(p -> p.startsWith(RedisKeys.gen4TransBlack(0, taskId, 0))
						|| p.startsWith(RedisKeys.gen4TransBlack(frAcc, taskId, 0))).count() == 0);
		return ret;
	}

	private boolean checkBal(AccountBaseInfo fr, int toCl) {
		int type = fr.getType();
		if (BINDALI == type || BINDWECHAT == type || BINDCOMMON == type || THIRDCOMMON == type) {
			int handi = buildHandi(fr);
			CurrentSystemLevel csl = CurrentSystemLevel.valueOf(toCl);
			csl = Objects.isNull(csl) ? CurrentSystemLevel.Outter : csl;
			BigDecimal rsv = findAccAtm(ACC_AMT_RSV, handi, csl);// 备用卡
			BigDecimal inbak = findAccAtm(ACC_AMT_IN, handi, csl);// 入款卡
			BigDecimal need = findAccAtm(ACC_AMT_OUT_RUPTOLIMIT, handi, csl);// 出款卡
			return inbak.add(rsv).compareTo(need) < 0;
		}
		return true;
	}

	/**
	 * 获取汇出账号ID
	 *
	 * @param toId
	 *            汇入账号
	 */
	@Override
	public List<Integer> findFrIdList(Integer toId) {
		List<Integer> result = new ArrayList<>();
		String ptn = RedisKeys.genPattern4TransferAccountLock_to(toId);
		for (String p : redisSer.getStringRedisTemplate().keys(ptn)) {
			String[] inf = p == null ? null : p.split(":");
			if (inf != null && inf.length >= 6 && inf[1] != null) {
				if (inf[1].length() > 13) {
					result.add(Integer.valueOf(inf[1].substring(0, inf[1].length() - 13)));
				} else {
					result.add(Integer.valueOf(inf[1]));
				}

			}
		}
		return result;
	}

	public Integer transToReqType(int fromAccountType) {
		if (fromAccountType == AccountType.InBank.getTypeId()) {
			return IncomeRequestType.IssueCompBank.getType();
		} else if (fromAccountType == AccountType.InThird.getTypeId()) {
			return IncomeRequestType.WithdrawThird.getType();
		} else if (fromAccountType == AccountType.InAli.getTypeId()) {
			return IncomeRequestType.WithdrawAli.getType();
		} else if (fromAccountType == AccountType.InWechat.getTypeId()) {
			return IncomeRequestType.WithdrawWechat.getType();
		} else if (fromAccountType == AccountType.BindWechat.getTypeId()) {
			return IncomeRequestType.IssueWechat.getType();
		} else if (fromAccountType == AccountType.BindAli.getTypeId()) {
			return IncomeRequestType.IssueAli.getType();
		} else if (fromAccountType == AccountType.ThirdCommon.getTypeId()) {
			return IncomeRequestType.IssueComnBank.getType();
		} else if (fromAccountType == AccountType.BindCommon.getTypeId()) {
			return IncomeRequestType.IssueComnBank.getType();
		} else if (fromAccountType == AccountType.ReserveBank.getTypeId()) {
			return IncomeRequestType.ReserveToOutBank.getType();
		} else if (fromAccountType == AccountType.OutBank.getTypeId()) {
			return IncomeRequestType.TransferOutBank.getType();
		} else if (fromAccountType == AccountType.OutThird.getTypeId()) {
			return IncomeRequestType.TransferOutThird.getType();
		} else if (fromAccountType == AccountType.BindCustomer.getTypeId()) {
			return IncomeRequestType.IssueCompBank.getType();
		}
		return null;
	}

	/**
	 * 执行 lua script: 解锁
	 */
	private String lunlock(Object fromId, int toId, int operator) {
		String ret = redisSer.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_TRANSFER_UNLOCK";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOCATING_TRANSFER_UNLOCK;
			}
		}, null, String.valueOf(fromId), RedisKeys.genPattern4TransferAccountLock_to(toId), String.valueOf(operator),
				String.valueOf(AppConstants.USER_ID_4_ADMIN));
		return StringUtils.trimToNull(ret);
	}

	private void lockTrans(Object fromId, Integer toId, Integer operator, Integer transInt, Integer status)
			throws Exception {
		if (fromId == null || toId == null || operator == null || transInt == null) {
			log.error("转账锁定 信息不完整.");
			throw new Exception("转账锁定 信息不完整.");
		}
		boolean ret = llock(fromId, toId, operator, transInt, status);
		if (!ret) {
			log.error("修改账号锁定状态 该账号已被锁住  toId:{};isLock:true;operator:{}", toId, operator);
			throw new Exception("该账号已被锁住.");
		}
	}

	/**
	 * 执行 lua script: 锁定
	 */
	private boolean llock(Object fromId, Integer toId, Integer operator, Integer transInt, int status) {
		String ret = redisSer.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_TRANSFER_LOCK";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOCATING_TRANSFER_LOCK;
			}
		}, null, String.valueOf(fromId), String.valueOf(toId), String.valueOf(operator), String.valueOf(transInt),
				String.valueOf(AppConstants.USER_ID_4_ADMIN), String.valueOf(LOCK_ROBOT_SECONDS),
				String.valueOf(LOCK_MANUAL_SECONDS), RedisKeys.genPattern4TransferAccountLock_from(fromId),
				RedisKeys.genPattern4TransferAccountLock_to(toId),
				RedisKeys.genPattern4TransferAccountLock_operator(operator), RedisKeys.FROM_ACCOUNT_TRANS_RADIX_AMOUNT,
				RedisKeys.genPattern4TransferAccountLock(fromId, toId, operator),
				String.valueOf(System.currentTimeMillis()), String.valueOf(status));
		return StringUtils.equals("ok", ret);
	}

	/**
	 * 生成分值
	 * <p>
	 * 根据内层/外层；账号类型；账号状态；金额；盘口信息生成一个分值</br>
	 * 分值格式：1051675.12001001</br>
	 * 层级：1 外层; 类型：05 出款卡; 状态: 1 可用/正常; 金额: 67512; 盘口:1
	 * </p>
	 * <p>
	 * 整数部分:共七位，从左往右</br>
	 * 第一位:内层/外层</br>
	 * 第二，三位:卡类型</br>
	 * 第四位:卡状态</br>
	 * 第五，六，七位:分别表示金额的万位，千位，百位</br>
	 * 小数部分：共8位,从左往右</br>
	 * 第一，二位：金额的十位，个位</br>
	 * 第三,四，五位:盘口</br>
	 * 第六,七位:当前时间(minute)</br>
	 * 第八位:是否有任务在身</br>
	 * 第九位预留位</br>
	 * </p>
	 *
	 * @param l
	 *            内/外层；
	 * @param type
	 *            账号类型
	 * @param status
	 *            账号状态
	 * @param amount
	 *            金额
	 * @param handicapId
	 *            盘口
	 * @param d
	 *            生成时间
	 * @param urgent
	 *            紧急情况 默认 0 ；1:出款任务
	 * @return 分值
	 * @see com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType
	 * @see com.xinbo.fundstransfer.domain.enums.AccountStatus
	 * @see com.xinbo.fundstransfer.domain.entity.BizHandicap
	 */
	private Double enscore(int l, int type, int status, int amount, int handicapId, Date d, int urgent) {
		String min = String.format("%tM", Objects.isNull(d) ? new Date() : d);
		return enscore(l, type, status, amount, handicapId, Integer.valueOf(min), urgent);
	}

	private Double enscore(int l, int type, int status, int amount, int handicapId, int min, int urgent) {
		urgent = urgent < 0 || urgent > 10 ? 0 : urgent;
		return Double.valueOf(String.format("%d%02d%d%03d.%02d%03d%02d%01d1", l, type, status, (amount / 100),
				(amount % 100), handicapId, min, urgent));
	}

	/**
	 * 生成分值区间段
	 *
	 * @see this#enscore(int l, int type, int status, int amount, int handicapId,
	 *      Date d, int urgent)
	 */
	private Double[] enscores(int l, int type, int status, int min, int max) {
		Date d = new Date();
		return new Double[] { enscore(l, type, status, min, AllocateOutwardTaskService.WILD_CARD_HANDICAP, d, 0),
				enscore(l, type, status, max, AllocateOutwardTaskService.WILD_CARD_HANDICAP, d, 0) };
	}

	/**
	 * @return int[0] 内/外层</br>
	 *         int[0] ID</br>
	 *         int[1] 内/外层</br>
	 *         int[2] 账号分类</br>
	 *         int[3] 账号状态</br>
	 *         int[4] 金额</br>
	 *         int[5] 盘口</br>
	 *         int[6] 时间(minute)</br>
	 *         int[7] 是否紧急 0：否：1 是</br>
	 */
	private List<int[]> filterScore(Set<ZSetOperations.TypedTuple<String>> toAll, double fromScore, double toScore) {
		List<int[]> ret = new ArrayList<>();
		List<ZSetOperations.TypedTuple<String>> fil = toAll.stream()
				.filter(p -> p.getScore() >= fromScore && p.getScore() <= toScore).collect(Collectors.toList());// Set<ZSetOperations.TypedTuple<String>>是无序的
		fil.forEach(p -> {
			int[] infs = descore(p.getScore());
			int[] des = Arrays.copyOf(new int[] { Integer.parseInt(p.getValue()) }, infs.length + 1);
			System.arraycopy(infs, 0, des, 1, infs.length);
			ret.add(des);
		});
		ret.sort((o1, o2) -> {
			if (o1[2] == OUTBANK && o2[2] == OUTBANK && o1[3] == NORMAL && o2[3] == NORMAL) {// 出款在用卡
				if (o1[7] != o2[7]) {
					return o2[7] - o1[7];
				}
				if (o1[7] == TransLock.STATUS_NEED_ && o2[7] == TransLock.STATUS_NEED_) {
					if (o1[6] == o2[6]) {
						return o2[4] - o1[4];
					} else {
						if (o1[6] >= 0 && o1[6] < 5 && o2[6] >= 55 && o2[6] < 60
								|| o2[6] >= 0 && o2[6] < 5 && o1[6] >= 55 && o1[6] < 60) {
							if (60 - Math.abs(o1[6] - o2[6]) <= 2) {
								return o2[4] - o1[4];
							} else {
								return o2[6] - o1[6];
							}
						}
						if (Math.abs(o1[6] - o2[6]) <= 2) {
							return o2[4] - o1[4];
						} else {
							return o1[6] - o2[6];
						}
					}
				}
			}
			return o2[4] - o1[4];
		});
		return ret;
	}

	/**
	 * 解析分值
	 * <p>
	 * 参数<code>score</code>必须由函数</br>
	 * <code>this#enscore(int l, int type, int status, int amount, int handicapId)</code>生成
	 * </p>
	 *
	 * @param score
	 *            分值
	 * @return int[0] 内/外层</br>
	 *         int[1] 账号分类</br>
	 *         int[2] 账号状态</br>
	 *         int[3] 金额</br>
	 *         int[4] 盘口</br>
	 *         int[5] 时间(minute)</br>
	 *         int[6] 是否紧急 0：否：1 是</br>
	 */
	private int[] descore(double score) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(10);
		nf.setMinimumFractionDigits(10);
		nf.setGroupingUsed(false);
		String sc = nf.format(score);
		Integer time = 0, need = 0;
		if (sc.length() >= 15) {
			time = Integer.parseInt(sc.substring(13, 15));
		}
		if (sc.length() >= 16) {
			need = Integer.parseInt(sc.substring(15, 16));
		}
		return new int[] { Integer.parseInt(sc.substring(0, 1)), Integer.parseInt(sc.substring(1, 3)),
				Integer.parseInt(sc.substring(3, 4)), Integer.parseInt(sc.substring(4, 7) + sc.substring(8, 10)),
				Integer.parseInt(sc.substring(10, 13)), time, need };
	}

	/**
	 * 封装账号金额信息
	 *
	 * @param expr
	 *            该账号金额信息过期时间
	 * @param acc
	 *            账号基本信息
	 * @param relAmt
	 *            账号真实余额
	 * @param rtoAmt
	 *            账号实际可转出金额
	 * @param rupToLow
	 *            真实值距离最低告警值金额
	 * @param rupToLimit
	 *            真实余额距离最高告警值金额
	 * @param rupToPeak
	 *            真实余额距离峰值金额
	 * @param rOverLow
	 *            真实余额超离最低告警值金额
	 * @param rOverLimit
	 *            真实余额超离最高告警值金额
	 * @param rOverPeak
	 *            真实余额超离峰值金额
	 * @param rptTime
	 *            余额上报时间
	 */
	private String packAccAmt(long expr, AccountBaseInfo acc, BigDecimal relAmt, BigDecimal rtoAmt, BigDecimal rupToLow,
			BigDecimal rupToLimit, BigDecimal rupToPeak, BigDecimal rOverLow, BigDecimal rOverLimit,
			BigDecimal rOverPeak, long rptTime) {
		String fmt = "%d:%d:%d:%d:%d:%d:%d:%d:%d:%d:%d:%d:%d:%d";
		int hand = buildHandi(acc);
		int l = Objects.nonNull(acc.getCurrSysLevel()) ? acc.getCurrSysLevel() : OUTTER;
		int relBalInt = relAmt.setScale(0, BigDecimal.ROUND_DOWN).intValue();
		int rtoAmtInt = rtoAmt.setScale(0, BigDecimal.ROUND_DOWN).intValue();
		int rupToLowInt = rupToLow.setScale(0, BigDecimal.ROUND_DOWN).intValue();
		int rupToLimitInt = rupToLimit.setScale(0, BigDecimal.ROUND_DOWN).intValue();
		int rupToPeakInt = rupToPeak.setScale(0, BigDecimal.ROUND_DOWN).intValue();
		int rOverLowInt = rOverLow.setScale(0, BigDecimal.ROUND_DOWN).intValue();
		int rOverLimitInt = rOverLimit.setScale(0, BigDecimal.ROUND_DOWN).intValue();
		int rOverPeakInt = rOverPeak.setScale(0, BigDecimal.ROUND_DOWN).intValue();
		return String.format(fmt, expr, acc.getId(), acc.getType(), hand, l, relBalInt, rtoAmtInt, rupToLowInt,
				rupToLimitInt, rupToPeakInt, rOverLowInt, rOverLimitInt, rOverPeakInt, rptTime);
	}

	/**
	 * 封装账号金额信息
	 * <p>
	 * 消息格式</br>
	 * plh0:plh1:plh2:plh3:plh4:plh5:plh6:plh7:plh8:plh9:plh10:plh11:plh12:plh13</br>
	 * plh0:(expr)过期时间</br>
	 * plh1:(id)账号ID</br>
	 * plh2:(type)账号类型</br>
	 * plh3:(hand)账号所属盘口;如果账号没有盘口则:盘口标识为<code>AllocateOutwardTaskService.WILD_CARD_HANDICAP</code></br>
	 * plh4:(level)内外层,内层为<code>CurrentSystemLevel.Outter.getValue()</code>;外层为<code>CurrentSystemLevel.Inner.getValue()</code></br>
	 * plh5:(relAmt)账号真实余额</br>
	 * plh6:(rtoAmt)账号实际可转出金额</br>
	 * plh7:(rupToLow)真实值距离最低告警值金额</br>
	 * plh8:(rupToLimit)真实余额距离最高告警值金额</br>
	 * plh9:(rupToPeak)真实余额距离峰值金额</br>
	 * plh10:(rOverLow)真实余额超离最低告警值金额</br>
	 * plh11:(rOverLimit)真实余额超离最高告警值金额</br>
	 * plh12:(rOverPeak)真实余额超离峰值金额</br>
	 * plh13:(rptTime)余额上报时间
	 * </p>
	 */
	private static final String LUA_SCRIPT_ALLOCATING_TRANS_ACC = "local curr = tonumber(ARGV[1]);\n"
			+ "local aKey = ARGV[2];\n" + "local infs = ARGV[3];\n" + "local ret ='';\n"
			+ "if infs ~= nil and infs ~= '' then\n" + "  local accs = {};\n"
			+ "  string.gsub(infs, '[^'..';'..']+', function(w) table.insert(accs, w) end );\n"
			+ "  for i0,v0 in pairs(accs) do\n" + "   local acc = {};\n"
			+ "   string.gsub(v0, '[^'..':'..']+', function(w) table.insert(acc, w) end );\n"
			+ "   redis.call('hset',aKey,acc[2],v0);\n" + "  end\n" + "end\n"
			+ "local kvs = redis.call('hgetall',aKey);\n" + "for i0,v0 in pairs(kvs) do\n" + " if i0 % 2 == 0 then\n"
			+ "  local acc = {};\n" + "  string.gsub(v0, '[^'..':'..']+', function(w) table.insert(acc, w) end );"
			+ "  if tonumber(acc[1]) <= curr then\n" + "   redis.call('hdel',aKey,acc[2]);\n" + "  else\n"
			+ "   if ret == '' then\n" + "    ret = v0;\n" + "   else\n" + "    ret = ret..';'..v0;\n" + "   end\n"
			+ "  end\n" + " end\n" + "end\n" + "return ret;\n";

	/**
	 * 执行 lua script:获取账号统计信息/添加账号统计信息
	 *
	 * @param infs
	 *            最新金额信息 可以为null
	 */
	private String lacc(String infs) {
		return redisSer.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_TRANS_ACC";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_ALLOCATING_TRANS_ACC;
			}
		}, null, String.valueOf(System.currentTimeMillis()), RedisKeys.TRANS_ACC, StringUtils.trimToEmpty(infs));
	}

	/**
	 * 初始化 账号实时金额
	 * <p>
	 * key</br>
	 * : ; :outter ; :inner ; handicapId: ; handicapId:outter ; handicapId:inner
	 * </p>
	 *
	 * @see this.ACC_AMT_IN
	 * @see this.ACC_AMT_RSV
	 * @see this.ACC_AMT_OUT_RUPTOLIMIT
	 */
	private void initAccAmt(String inf) {
		if (StringUtils.isBlank(inf)) {
			ACC_AMT_IN = null;
			ACC_AMT_RSV = null;
			ACC_AMT_OUT_RUPTOLIMIT = null;
		}
		String[] accList = inf.split(";");
		Map<String, BigDecimal> IN = new HashMap<>(), RSV = new HashMap<>(), OUT_RUPTOLIMIT = new HashMap<>();
		long curr = System.currentTimeMillis();
		for (String accInf : accList) {
			String[] infs = accInf.split(":");
			int accId = Integer.valueOf(infs[1]), type = Integer.parseInt(infs[2]), hand = Integer.parseInt(infs[3]),
					l = Integer.parseInt(infs[4]);
			BigDecimal relAmt = new BigDecimal(infs[5]), rtoAmt = new BigDecimal(infs[6]);
			Long rptTime = Long.valueOf(infs[13]);
			long div = curr - rptTime;
			if (INBANK == type && div < EXPR_INBANK) {
				buildAccAmt(IN, hand, l, rtoAmt);
			}
			if (RESERVEBANK == type && div < EXPR_RSV) {
				buildAccAmt(RSV, hand, l, rtoAmt);
			}
			if (OUTBANK == type && div < EXPR_OUTBANK) {
				buildAccAmt(OUT_RUPTOLIMIT, hand, l, new BigDecimal(infs[8]));// [8]rupToLimit
			}
			ACC_AMT.put(accId, relAmt);
			ACC_TIME.put(accId, rptTime);
		}
		ACC_AMT_IN = IN;
		ACC_AMT_RSV = RSV;
		ACC_AMT_OUT_RUPTOLIMIT = OUT_RUPTOLIMIT;
	}

	/**
	 * 求o1,o2中最大值
	 */
	private BigDecimal max(BigDecimal o1, BigDecimal o2) {
		if (Objects.isNull(o1)) {
			return o2;
		}
		if (Objects.isNull(o2)) {
			return o1;
		}
		return o1.compareTo(o2) > 0 ? o1 : o2;
	}

	/**
	 * 求o1,o2差值，如果:如果差值<=0,则取def
	 *
	 * @param o1
	 *            not null
	 * @param o2
	 *            not null
	 * @param def
	 *            not null
	 */
	private BigDecimal subtract(BigDecimal o1, BigDecimal o2, BigDecimal def) {
		return o1.compareTo(o2) > 0 ? o1.subtract(o2) : def;
	}

	/**
	 * 获取账号实时金额信息
	 *
	 * @param STORAGE
	 *            (ACC_AMT_IN,ACC_AMT_ISU,ACC_AMT_RSV,ACC_AMT_OUT_RELATM,ACC_AMT_OUT_RTOAMT,ACC_AMT_OUT_RUPTOLOW
	 *            ,ACC_AMT_OUT_RUPTOLIMIT,ACC_AMT_OUT_RUPTOPEAK,ACC_AMT_OUT_ROVERLOW,ACC_AMT_OUT_ROVERLIMIT,ACC_AMT_OUT_ROVERPEAK)
	 * @param handicap
	 *            盘口</br>
	 *            可以为null
	 * @param l
	 *            内层/外层</br>
	 *            可以为null
	 */
	private BigDecimal findAccAtm(Map<String, BigDecimal> STORAGE, Integer handicap, CurrentSystemLevel l) {
		if (Objects.isNull(STORAGE)) {
			return BigDecimal.ZERO;
		}
		String key = (Objects.isNull(handicap)
				|| Objects.equals(handicap, AllocateOutwardTaskService.WILD_CARD_HANDICAP) ? StringUtils.EMPTY
						: handicap)
				+ ":" + (Objects.isNull(l) ? StringUtils.EMPTY : l.getValue());
		BigDecimal ret = STORAGE.get(key);
		return Objects.isNull(ret) ? BigDecimal.ZERO : ret;
	}

	/**
	 * <p>
	 * key</br>
	 * : ; :outter ; :inner ; handicapId: ; handicapId:outter ; handicapId:inner
	 * </p>
	 */
	private void buildAccAmt(Map<String, BigDecimal> storage, Integer hand, Integer l, BigDecimal atm) {
		// 所有 ：
		String keyAll = ":";
		BigDecimal valAll = storage.get(keyAll);
		valAll = Objects.isNull(valAll) ? BigDecimal.ZERO : valAll;
		storage.put(keyAll, valAll.add(atm));
		BizHandicap handicap = Objects.isNull(hand)
				|| Objects.equals(AllocateOutwardTaskService.WILD_CARD_HANDICAP, hand) ? null
						: handSer.findFromCacheById(hand);
		boolean excHandi = Objects.isNull(handicap) ? false
				: AppConstants.EXCLUSIVE_HANDICAP.contains(handicap.getCode());
		if (Objects.equals(l, OUTTER)) {
			// :外层
			if (!excHandi) {
				String keyOut = ":" + CurrentSystemLevel.Outter.getValue();
				BigDecimal valOut = storage.get(keyOut);
				valOut = Objects.isNull(valOut) ? BigDecimal.ZERO : valOut;
				storage.put(keyOut, valOut.add(atm));
			}
			if (Objects.nonNull(hand) && !Objects.equals(hand, AllocateOutwardTaskService.WILD_CARD_HANDICAP)) {
				// 盘口:外层
				String keyHandOut = hand + ":" + CurrentSystemLevel.Outter.getValue();
				BigDecimal valHandOut = storage.get(keyHandOut);
				valHandOut = Objects.isNull(valHandOut) ? BigDecimal.ZERO : valHandOut;
				storage.put(keyHandOut, valHandOut.add(atm));
			}
		} else if (Objects.equals(l, INNER)) {
			// :内层
			if (!excHandi) {
				String keyIn = ":" + CurrentSystemLevel.Inner.getValue();
				BigDecimal valIn = storage.get(keyIn);
				valIn = Objects.isNull(valIn) ? BigDecimal.ZERO : valIn;
				storage.put(keyIn, valIn.add(atm));
			}
			if (Objects.nonNull(hand) && !Objects.equals(hand, AllocateOutwardTaskService.WILD_CARD_HANDICAP)) {
				// 盘口:内层
				String keyHandIn = hand + ":" + CurrentSystemLevel.Inner.getValue();
				BigDecimal valHandIn = storage.get(keyHandIn);
				valHandIn = Objects.isNull(valHandIn) ? BigDecimal.ZERO : valHandIn;
				storage.put(keyHandIn, valHandIn.add(atm));
			}
		} else if (Objects.equals(l, MIDDLE)) {
			// ：中层
			if (!excHandi) {
				String keyMd = ":" + CurrentSystemLevel.Middle.getValue();
				BigDecimal valMd = storage.get(keyMd);
				valMd = Objects.isNull(valMd) ? BigDecimal.ZERO : valMd;
				storage.put(keyMd, valMd.add(atm));
			}
			if (Objects.nonNull(hand) && !Objects.equals(hand, AllocateOutwardTaskService.WILD_CARD_HANDICAP)) {
				// 盘口:中层
				String keyHandMd = hand + ":" + CurrentSystemLevel.Middle.getValue();
				BigDecimal valHandMd = storage.get(keyHandMd);
				valHandMd = Objects.isNull(valHandMd) ? BigDecimal.ZERO : valHandMd;
				storage.put(keyHandMd, valHandMd.add(atm));
			}
		}
	}

	private int buildHandi(AccountBaseInfo acc) {
		int hand = Objects.nonNull(acc.getHandicapId()) ? acc.getHandicapId()
				: AllocateOutwardTaskService.WILD_CARD_HANDICAP;
		int type = acc.getType();
		if ((INBANK == type || RESERVEBANK == type || BINDALI == type || BINDWECHAT == type || BINDCOMMON == type
				|| THIRDCOMMON == type) && !Objects.equals(hand, AllocateOutwardTaskService.WILD_CARD_HANDICAP)) {
			BizHandicap handicap = handSer.findFromCacheById(acc.getHandicapId());
			hand = Objects.nonNull(handicap) && AppConstants.EXCLUSIVE_HANDICAP.contains(handicap.getCode())
					? handicap.getId()
					: AllocateOutwardTaskService.WILD_CARD_HANDICAP;
		}
		return hand;
	}

	private static final Cache<Integer, BigDecimal> ACC_AMT = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(90, TimeUnit.SECONDS).build();
	private static final Cache<Integer, Long> ACC_TIME = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(2, TimeUnit.DAYS).build();
	private static volatile Map<String, BigDecimal> ACC_AMT_IN = new HashMap<>();
	private static volatile Map<String, BigDecimal> ACC_AMT_RSV = new HashMap<>();
	private static volatile Map<String, BigDecimal> ACC_AMT_OUT_RUPTOLIMIT = new HashMap<>();
	private volatile static Thread THREAD_ACC_AMT = null;
	private static final ConcurrentLinkedQueue<Integer> ACC_ATM_QUENE = new ConcurrentLinkedQueue<>();

	@Override
	public boolean isLockThirdTrans(String toId, String operatorUid) throws Exception {
		return !CollectionUtils.isEmpty(redisService.getStringRedisTemplate().keys("thirdTrans:" + toId + ":" + "*"));
	}

	@Override
	public boolean isLockThirdTransByoperator(String toId, String operatorUid) throws Exception {
		return redisService.getStringRedisTemplate().hasKey("thirdTrans:" + toId + ":" + operatorUid);
	}
}
