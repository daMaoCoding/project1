package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.websocket.IncomeRequestWebSocketEndpoint;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 分配算法：</br>
 * 已知：</br>
 * 每人分配最大账号数:countOfMax</br>
 * 正常账号总数:countOfNormal</br>
 * 停用账号总数:countOfStop</br>
 * 冻结账号总数:countOfFreeze</br>
 * 分配条件：</br>
 * 1.正常账号，停用账号，冻结账号平均分配</br>
 * 2.每人分配正常账号数+ 每人分配停用账号数 + 每人分配冻结账号数据 <= 每人分配的最大账号数 </br>
 * 计算：</br>
 * 每人分配正常账号数:countOfNormalPer;</br>
 * 每人分配停用账号数 countOfStopPer</br>
 * 每人分配冻结账号数 countOfFreezePer</br>
 * 算法说明：
 * 1.首先每人从正常账号集中领取一个账号，如正常账号集不足被每人领取，则从停用账号集中借用不足的数目为人员分配，如停用账号集不足，则从冻结账号集中借用不足的数目为人员分配</br>
 * 2.再次每人从停用账号集中领取一个账号，如停用账号集不足被每人领取，则从冻结账号集中借用不足的数目为人员分配，如冻结账号集不足，则从正常账号集中借用不足的数目为人员分配</br>
 * 3.最后每人从冻结账号集中领取一个账号，如冻结账号集不足被每人领取，则从正常账号结中借用不足的数目为人员分配，如正常账号集不足，则从停用账号集中借用不足的数目为人员分配</br>
 * 4.递归：1，2，3 步，until:账号被分配完 or 每人都已分配 countOfMax</br>
 * 方法实现：buildAllocCount(int countOfMax, int countOfNormal, int countOfStop, int
 * countOfFreeze, List<AuditAlloc> allocList)
 */
@Service
public class AllocateIncomeAccountServiceImpl implements AllocateIncomeAccountService {
	private static final Logger log = LoggerFactory.getLogger(AllocateIncomeAccountServiceImpl.class);
	@Autowired
	private SysUserService userService;
	@Autowired
	RedisService redisService;
	@Autowired
	@Lazy
	AccountService accountService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysUserProfileService userProfileService;
	public static boolean CAL_HOST = false;

	/**
	 * 当应用关闭时：置APP_STOP为TRUE
	 */
	public volatile static boolean APP_STOP = false;

	/**
	 * 分配线程:THREAD_ALLOC</br>
	 * 更新线程:THREAD_UPDATE
	 */
	private volatile static Thread THREAD_ALLOC = null, THREAD_UPDATE = null;
	/**
	 * 重新分配标识
	 */
	private volatile static boolean[] duplicateAllocate = new boolean[] { false, false, false, false, false, false,
			false, false, false, false };
	/**
	 * 分配最大账号数 </br>
	 * 注意：</br>
	 * 1.如果数据库配置>MAX_ALLOCATE_NUM，则以MAX_ALLOCATE_NUM为准</br>
	 * 2.如果数据库配置<1，则以MAX_ALLOCATE_NUM为准</br>
	 */
	private final static int MAX_ALLOCATE_NUM = 100;
	/**
	 * 为保证这些Redis值的 </br>
	 * RedisKeys.INCOME_AUDIT_ACCOUNT_NORMAL</br>
	 * RedisKeys.INCOME_AUDIT_ACCOUNT_STOP</br>
	 * RedisKeys.INCOME_AUDIT_ACCOUNT_FREEZE 一致性，准确性</br>
	 * 这些值的修改，应满足</br>
	 * 1.只允许发生在一台主机上checkHostRunRight()</br>
	 * 2.且串行执行（执行代码段加锁:lockUpdate:读写锁）
	 */
	private static final ReadWriteLock LOCK_UPDATE = new ReentrantReadWriteLock();

	/**
	 * lua script:批量取出待更新账号</br>
	 */
	private static final String LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD_POP = "local k1 = ARGV[1];\n"
			+ "local ret ='';\n" + "local hks = redis.call('hkeys',k1);\n" + "if hks == nil or next(hks) == nil then\n"
			+ " return ret;\n" + "end\n" + "for i,hk in pairs(hks) do\n" + " if hk ~= nil and hk ~= '' then\n"
			+ "  local hv = redis.call('hget',k1,hk);\n" + "  if hv ~= nil and hv ~= '' then\n"
			+ "   ret = ret..hk..':'..hv..';';\n" + "  end\n" + " end\n" + " redis.call('hdel',k1,hk);\n" + "end\n"
			+ "return ret;";

	/**
	 * lua script:系统初始化（入款审核账号：更新）</br>
	 */
	private static final String LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD_LOAD = "local k1 = ARGV[1];\n"
			+ "local k2 = ARGV[2];\n" + "local k3 = ARGV[3];\n" + "local v1 = ARGV[4];\n" + "local v2 = ARGV[5];\n"
			+ "local v3 = ARGV[6];\n" + "redis.call('set',k1,v1);\n" + "redis.call('set',k2,v2);\n"
			+ "redis.call('set',k3,v3);\n" + "redis.call('persist',k1);\n" + "redis.call('persist',k2);\n"
			+ "redis.call('persist',k3);\n" + "return 'ok';";

	/**
	 * lua script:已分配入款账号 推送
	 */
	private static final String LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_ALLOC = "local arg0 = ARGV[1];\n"
			+ "local arg1 = ARGV[2];\n" + "local ret = 'ok';\n" + "if arg0 == nil or arg0 == '' then\n"
			+ " return ret;\n" + "end\n" + "local rt0 = {};\n"
			+ "string.gsub(arg0, '[^'..';'..']+', function(w) table.insert(rt0, w) end );\n"
			+ "for i0,v0 in pairs(rt0) do\n" + " if v0 ~= nil and v0 ~= '' then\n" + "  local rt1 = {};\n"
			+ "  string.gsub(v0, '[^'..':'..']+', function(w) table.insert(rt1, w) end );\n"
			+ "  local ptn = table.concat(rt1, ':', 1, 2)..':*';\n" + "  local keys = redis.call('keys',ptn);\n"
			+ "  if keys ~= nil and next(keys) ~= nil and next(keys) ~= '' then" + "   for i1,v1 in pairs(keys) do\n"
			+ "    if v1 ~= nil and v1 ~= '' then\n" + "     redis.call('del',v1);\n" + "    end\n" + "   end\n"
			+ "  end" + "  redis.call('set',v0,'');\n" + "  redis.call('persist',v0);\n"
			+ "  redis.call('publish',arg1,v0);\n" + " end\n" + "end\n" + "return ret;";

	private static String ONLINE_CLUSTER_HOST_BLACK = StringUtils.EMPTY;

	/**
	 * 应用启动时，初始过程</br>
	 * 1.清理无效的入款审核用户 </br>
	 * #系统未启动成功时，不可能有长链接连接到该应用所在主机，此时，凡是注册到该应用所在主机的用户，均视为无效用户
	 * 2.把该Web应用所在Host的Internal Ip 写入Redis </br>
	 * 3.系统广播：WEB-应用启动
	 */
	@PostConstruct
	public void init() {
		Set<String> allHost = new HashSet<>();
		List<RedisClientInfo> clientList = redisService.getStringRedisTemplate().getClientList();
		clientList.forEach((p -> allHost.add(p.get(RedisClientInfo.INFO.ADDRESS_PORT).split(":")[0])));
		// 1.清理无效的入款审核用户
		// 1.1，该应用所在主机IP
		String internalIp = CommonUtils.getInternalIp(allHost);
		try {
			// 1.2，所有用户
			List<Object> allUserIdList = new ArrayList<>();
			redisService.getStringRedisTemplate().keys(RedisKeys.genOrigin4IncomeAuditAccountAllocate())
					.forEach((p) -> allUserIdList.add(p.split(":")[1]));
			// 1.3，用户所对应的长链接服务端IP
			List<Object> valList = redisService.getStringRedisTemplate()
					.boundHashOps(RedisKeys.INCOME_AUDIT_ACCOUNT_HOST).multiGet(allUserIdList);
			// 1.4，计算无效用户（统未启动成功时，不可能有长链接连接到该应用所在主机，此时，凡是注册到该应用所在主机的用户，均视为无效用户）
			List<String> unvalidUserId = new ArrayList<>();
			Set<String> unvalidKeys = new HashSet<>();
			for (int index = 0; index < valList.size(); index++) {
				String realIp = (String) valList.get(index);
				if (realIp == null || StringUtils.equals(realIp, internalIp)) {
					String userId = allUserIdList.get(index).toString();
					unvalidUserId.add(userId);
					unvalidKeys
							.addAll(redisService.keys(RedisKeys.genPattern4IncomeAuditAccountAllocateByUserId(userId)));
				}
			}
			// 1.5，清理无效用户相关信息
			if (unvalidUserId.size() > 0) {
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INCOME_AUDIT_ACCOUNT_HOST)
						.delete(unvalidUserId.toArray());
			}
			if (unvalidKeys.size() > 0) {
				redisService.getStringRedisTemplate().delete(unvalidKeys);
			}
		} finally {
			// 2.把该Web应用所在Host的Internal Ip 写入Redis
			String hosts = redisService.getStringRedisTemplate().boundValueOps(RedisKeys.ONLINE_CLUSTER_HOST).get();
			String hostBlacks = redisService.getStringRedisTemplate()
					.boundValueOps(RedisKeys.ONLINE_CLUSTER_HOST_BLACKLIST).get();
			ONLINE_CLUSTER_HOST_BLACK = StringUtils.trimToEmpty(hostBlacks);
			hosts = StringUtils.trimToEmpty(hosts);
			if (!hosts.contains(CommonUtils.getInternalIp())) {
				hosts = StringUtils.isBlank(hosts) ? CommonUtils.getInternalIp()
						: hosts + "," + CommonUtils.getInternalIp();
				log.debug("load internal IP:{},Hosts:{}", CommonUtils.getInternalIp(), hosts);
				if (StringUtils.isBlank(hostBlacks) || !hostBlacks.contains(CommonUtils.getInternalIp())) {
					redisService.getStringRedisTemplate().boundValueOps(RedisKeys.ONLINE_CLUSTER_HOST).set(hosts);
					redisService.getStringRedisTemplate().persist(RedisKeys.ONLINE_CLUSTER_HOST);
				}
			}
			if (StringUtils.isBlank(ONLINE_CLUSTER_HOST_BLACK)
					|| !ONLINE_CLUSTER_HOST_BLACK.contains(CommonUtils.getInternalIp())) {
				AllocateIncomeAccountServiceImpl.CAL_HOST = hosts.startsWith(CommonUtils.getInternalIp());
			} else {
				AllocateIncomeAccountServiceImpl.CAL_HOST = false;
			}
		}
	}

	@Scheduled(cron = "0 0/4 *  * * ?")
	public void scheduleTask() {
		reboot();
	}

	@Override
	public synchronized void reboot() {
		// 检测：主机有无分配权限：无：释放更新线程；释放分配线程
		if (!checkHostRunRight()) {
			// 释放更新线程 ,分配线程
			if (THREAD_UPDATE != null || THREAD_ALLOC != null) {
				log.trace("IncomeAccountAllocate 释放 更新线程 或  分配线程>>localIp:{}", CommonUtils.getInternalIp());
				releaseThread();
			}
			return;
		}
		// 主机有分配权限：创建更新线程，分配线程
		if (THREAD_UPDATE == null || THREAD_ALLOC == null) {
			log.trace("IncomeAccountAllocate 创建 更新线程 或  分配线程>>localIp:{}", CommonUtils.getInternalIp());
			createThead();
		}
		// 主机有分配权限：入款分配账号 全量加载
		Set<String> normalIdSet = new HashSet<>(), stopIdSet = new HashSet<>(), freezeIdSet = new HashSet<>();
		SearchFilter tf = new SearchFilter("type", SearchFilter.Operator.EQ, AccountType.InBank.getTypeId());
		SearchFilter snf = new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Normal.getStatus());
		SearchFilter ssf = new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.StopTemp.getStatus());
		SearchFilter ff = new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Freeze.getStatus());
		SearchFilter uf = new SearchFilter("updateTime", SearchFilter.Operator.GTE,
				new Date(System.currentTimeMillis() - 86400000 * buildValidDaysOfFreezeAccount()));
		for (BizHandicap handicap : handicapService.findAllToList()) {
			if (!checkHandicap(handicap.getCode())) {
				log.debug("unload income account  handicap:{}.", handicap.getCode());
				continue;
			}
			log.debug("loading income account  handicap:{}", handicap.getCode());
			SearchFilter hf = new SearchFilter("handicapId", SearchFilter.Operator.EQ, handicap.getId());
			accountService.findAccountIdList(hf, tf, snf).forEach(p -> normalIdSet.add(p.toString()));
			accountService.findAccountIdList(hf, tf, ssf).forEach(p -> stopIdSet.add(p.toString()));
			accountService.findAccountIdList(hf, tf, ff, uf).forEach(p -> freezeIdSet.add(p.toString()));
		}
		try {
			LOCK_UPDATE.writeLock().lock();
			log.debug("入款账号分配 重新加载 全部账号数据 host:{}", CommonUtils.getInternalIp());
			lload2reboot(normalIdSet, stopIdSet, freezeIdSet);
		} finally {
			LOCK_UPDATE.writeLock().unlock();
		}
	}

	@Override
	public boolean registOrCancel(int auditor, boolean regist) {
		String keyPattern = RedisKeys.genPattern4IncomeAuditAccountAllocateByUserId(auditor);
		Set<String> keys = redisService.getStringRedisTemplate().keys(keyPattern);
		if (regist) {
			if (CollectionUtils.isEmpty(keys)) {
				String keyOrigin = RedisKeys.genOrigin4IncomeAuditAccountAllocate(auditor);
				redisService.getStringRedisTemplate().boundValueOps(keyOrigin).set(StringUtils.EMPTY);
				redisService.getStringRedisTemplate().boundValueOps(keyOrigin).persist();
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INCOME_AUDIT_ACCOUNT_HOST)
						.put(String.valueOf(auditor), CommonUtils.getInternalIp());
			} else {
				keys.forEach((p) -> redisService.getStringRedisTemplate().boundValueOps(p).persist());
			}
		} else {
			redisService.getStringRedisTemplate().delete(keys);
			redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INCOME_AUDIT_ACCOUNT_HOST)
					.delete(String.valueOf(auditor));
		}
		return !CollectionUtils.isEmpty(keys);
	}

	@Override
	public int[] findAllocatedCountAndAllCount() {
		Set<Integer> all = new HashSet<>();
		buildAllAccount(null).forEach((k, v) -> all.addAll(v.keySet()));
		List<Integer> allocatedList = new ArrayList<>();
		Set<String> keys = redisService.getStringRedisTemplate().keys(RedisKeys.genOrigin4IncomeAuditAccountAllocate());
		if (!CollectionUtils.isEmpty(keys)) {
			buildAllocatedAccount(all, keys, allocatedList, null);
		}
		return new int[] { allocatedList.size(), all.size() };
	}

	/**
	 * 分配入款审核账号
	 */
	@Override
	public void allocate(int inzone, boolean ifUnavailableBroadCast) {
		if (checkHostRunRight()) {// 主机有分配权限
			// 创建 更新线程 或 分配线程
			if (THREAD_UPDATE == null || THREAD_ALLOC == null) {
				createThead();
			}
			// 打开重新分配标识符
			duplicateAllocate[inzone] = true;
		} else {// 主机无分配权限
			log.trace("IncomeAccountAllocate 不具备分配权限>>localIp:{}", CommonUtils.getInternalIp());
			// 分配信息 广播到 有分配权限的主机
			if (ifUnavailableBroadCast) {
				redisService.convertAndSend(RedisTopics.ACCOUNT_ALLOCATING, String.valueOf(inzone));
			}
			// 释放 更新线程，分配线程
			if (THREAD_UPDATE != null || THREAD_ALLOC != null) {
				releaseThread();
			}
			// 关闭重新分配标识符
			for (int index = 0; index < duplicateAllocate.length; index++) {
				duplicateAllocate[index] = false;
			}
		}

	}

	/**
	 * 当账号分类或状态改变时，更新Redis存储值
	 */
	@Override
	public void update(int id, int type, int status) {
		// 将变更的 入款审核账号 放入 Redis缓存
		redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD)
				.put(String.valueOf(id), (type + ":" + status));
	}

	@Deprecated
	@Scheduled(fixedDelay = 5000)
	public void calHostRunRight() {
		if (StringUtils.isNotBlank(ONLINE_CLUSTER_HOST_BLACK)
				&& ONLINE_CLUSTER_HOST_BLACK.contains(CommonUtils.getInternalIp())) {
			AllocateIncomeAccountServiceImpl.CAL_HOST = false;
			return;
		}
		Set<String> allHost = new HashSet<>();
		String originHosts = redisService.getStringRedisTemplate().boundValueOps(RedisKeys.ONLINE_CLUSTER_HOST).get();
		String hosts = null;
		try {
			List<RedisClientInfo> clientList = redisService.getStringRedisTemplate().getClientList();
			clientList.forEach((p -> allHost.add(p.get(RedisClientInfo.INFO.ADDRESS_PORT).split(":")[0])));
			if (StringUtils.isBlank(originHosts)) {
				AllocateIncomeAccountServiceImpl.CAL_HOST = false;
			}
			for (String host : originHosts.split(",")) {
				hosts = allHost.contains(host) ? (hosts == null ? host : hosts + "," + host) : hosts;
			}
			hosts = Objects.isNull(hosts) ? StringUtils.EMPTY : hosts;
			if (!StringUtils.equals(originHosts, hosts)) {
				redisService.getStringRedisTemplate().boundValueOps(RedisKeys.ONLINE_CLUSTER_HOST).set(hosts);
			}
			boolean ret = hosts.startsWith(CommonUtils.getInternalIp());
			log.trace("IncomeAccountAllocate 判断该应用是否具有分配权限>>localIp:{},ret:{},hosts:{}", CommonUtils.getInternalIp(),
					ret, hosts);
			AllocateIncomeAccountServiceImpl.CAL_HOST = ret;
		} catch (Exception e) {
			AllocateIncomeAccountServiceImpl.CAL_HOST = false;
			log.error("CheckHostRunRight >> allHost: {} originHosts：{} hosts： {}", String.join(",", allHost),
					originHosts, hosts);
		}
	}

	/**
	 * 判断该应用主机是否具有运行权限(update,allocate)
	 */
	@Override
	public boolean checkHostRunRight() {
		return AllocateIncomeAccountServiceImpl.CAL_HOST;
	}

	/**
	 * 核对该用户是否可以停止接单（入款审核）
	 */
	@Override
	public String checkLogout(Integer userId) {
		if (userId == null) {
			log.debug("用户id为空");
			return "用户id为空";
		}
		Integer inzone = buildInzone(userId);
		if (Objects.isNull(inzone)) {
			log.debug("区域为空");
			return "YES";
		}
		Set<String> keys = redisService.getStringRedisTemplate().keys(RedisKeys.genOrigin4IncomeAuditAccountAllocate());
		if (CollectionUtils.isEmpty(keys)) {
			log.debug("分配入款账号缓存 key-IncomeAuditAccountAllocate:为空");
			return "YES";
		}
		// 该区域下 所有分配的账号分组 在用 停用 冻结
		Map<AccountStatus, Map<Integer, AccountStatus>> all = buildAllAccount(inzone);
		Map<Integer, AccountStatus> allAccountId = new HashMap<>();
		int countOfAccount = 0;
		for (Map.Entry<AccountStatus, Map<Integer, AccountStatus>> entry : all.entrySet()) {
			countOfAccount = countOfAccount + entry.getValue().size();
			allAccountId.putAll(entry.getValue());
		}
		// 该区域下的所有入款审核人员 数据下标表示区域值,[下标]里的值是用户id字符串:如 ;1;2;3;
		String[] auditor = buildInZoneAuditor();
		Map<Integer, List<Integer>> allocatedMap = buildAllocatedAccount(allAccountId.keySet(), keys, null,
				auditor[inzone]);
		int countPerson = allocatedMap.containsKey(userId) ? allocatedMap.size() - 1 : allocatedMap.size();
		boolean flag = buildCountOfMax() * countPerson >= countOfAccount;
		if (flag) {
			return "YES";
		} else {
			log.debug("该区域下入款审核人:{},配置的审核入款账号数量乘积:{},小于所有可以分配的审核入款账号数量:{}!", countPerson,
					(buildCountOfMax() * countPerson), countOfAccount);
			return "该区域下入款审核人" + countPerson + "与配置的审核入款账号数量乘积" + (buildCountOfMax() * countPerson)
					+ "小于所有可以分配的审核入款账号数量:" + countOfAccount + "!";
		}
		// return buildCountOfMax() * countPerson >= countOfAccount;
	}

	@Override
	public boolean checkHandicap(String handicapCode) {
		BizHandicap handicap = handicapService.findFromCacheByCode(handicapCode);
		return Objects.nonNull(handicap) && Objects.equals(handicap.getStatus(), 1);
	}

	@Override
	public List<Integer> findAccountIdList(List<Integer> auditorList) {
		List<Integer> result = new ArrayList<>();
		if (CollectionUtils.isEmpty(auditorList)) {
			return result;
		}
		Set<String> keys = redisService.getStringRedisTemplate().keys(RedisKeys.genOrigin4IncomeAuditAccountAllocate());
		buildAllocatedAccount(null, keys, null, null).forEach((k, v) -> {
			if (auditorList.contains(k))
				result.addAll(v);
		});
		return result;
	}

	/**
	 * {@code 0} MANILA {@code 1} TAIWAN
	 */
	private Integer transToInZone(int accId) {
		AccountBaseInfo base = accountService.getFromCacheById(accId);
		if (Objects.isNull(base) || Objects.isNull(base.getHandicapId()))
			return null;
		String tmp = String.format(";%d;", base.getHandicapId());
		String MANILA = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.HANDICAP_MANILA_ZONE.getValue());
		if (Objects.nonNull(MANILA) && MANILA.contains(tmp))
			return 0;
		String TAIWAN = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.HANDICAP_TAIWAN_ZONE.getValue());
		if (Objects.nonNull(TAIWAN) && TAIWAN.contains(tmp))
			return 1;
		return null;
	}

	private String[] buildInZoneAuditor() {
		List<SysUserProfile> dataList = userProfileService
				.findByPropertyKey(IncomeRequestWebSocketEndpoint.HANDICAP_ZONE_MANILA0_TAIWAN1);
		String[] ret = new String[] { ";", ";", ";", ";", ";", ";", ";", ";", ";", ";" };
		if (!CollectionUtils.isEmpty(dataList)) {
			for (SysUserProfile pro : dataList) {
				Integer inzone = Integer.valueOf(pro.getPropertyValue());
				ret[inzone] = ret[inzone] + pro.getUserId() + ";";
			}
		}
		return ret;
	}

	private Integer buildInzone(Integer auditor) {
		SysUserProfile profile = userProfileService.findByUserIdAndPropertyKey(auditor,
				IncomeRequestWebSocketEndpoint.HANDICAP_ZONE_MANILA0_TAIWAN1);
		if (Objects.nonNull(profile) && Objects.nonNull(profile.getPropertyValue())) {
			return Integer.valueOf(profile.getPropertyValue().trim());
		}
		return null;
	}

	/**
	 * 创建更新线程,分配线程
	 */
	private synchronized void createThead() {
		// 创建更新线程
		if (THREAD_UPDATE == null || !THREAD_UPDATE.isAlive()) {
			THREAD_UPDATE = new Thread(() -> {
				for (;;) {
					try {
						if (APP_STOP || THREAD_UPDATE == null || THREAD_UPDATE.isInterrupted()) {
							throw new InterruptedException();
						}
						Thread.sleep(120000L);
						boolean change = false;
						boolean[] CHANGE = new boolean[] { false, false, false, false, false, false, false, false,
								false, false };
						try {
							LOCK_UPDATE.writeLock().lock();
							String pops = lpop4upd();
							if (StringUtils.isBlank(pops)) {
								continue;
							}
							HashSet<String> normalIdSet = new HashSet<>(), stopIdSet = new HashSet<>(),
									freezeIdSet = new HashSet<>();
							// 获取所有待更新账号信息
							fetch4upd(normalIdSet, stopIdSet, freezeIdSet);
							// 检测是否需要重新分配
							for (String infs : pops.split(";")) {
								String[] inf = infs.split(":");
								Integer t = Integer.valueOf(inf[1]), s = Integer.valueOf(inf[2]);
								boolean d = check4upd(inf[0], t, s, normalIdSet, stopIdSet, freezeIdSet);
								change = change || d;
								Integer inzone = transToInZone(Integer.valueOf(inf[0]));
								if (Objects.nonNull(inzone))
									CHANGE[inzone] = CHANGE[inzone] || d;
							}
							if (change) {
								// 回写账号信息到Redis
								lload2reboot(normalIdSet, stopIdSet, freezeIdSet);
							}
						} finally {
							LOCK_UPDATE.writeLock().unlock();
						}
						int length = CHANGE.length;
						for (int inzone = 0; inzone < length; inzone++) {
							if (CHANGE[inzone]) {
								allocate(inzone, true);// 重新分配
							}
						}
					} catch (InterruptedException e) {
						log.error("创建更新线程异常:", e);
						break;
					}
				}
			});
			THREAD_UPDATE.start();
		}
		// 创建分配线程
		if (THREAD_ALLOC == null || !THREAD_ALLOC.isAlive()) {
			THREAD_ALLOC = new Thread(() -> {
				for (;;) {
					try {
						TimeUnit.MILLISECONDS.sleep(300L);
						if (APP_STOP || THREAD_ALLOC == null || THREAD_ALLOC.isInterrupted()) {
							throw new InterruptedException();
						}
						String[] inZoneAuditor = null;
						int len = duplicateAllocate.length;
						for (int inzone = 0; inzone < len; inzone++) {
							if (duplicateAllocate[inzone]) {
								duplicateAllocate[inzone] = false;
								try {
									if (Objects.isNull(inZoneAuditor)) {
										inZoneAuditor = buildInZoneAuditor();
									}
									alloc4IncomeAudit(inzone,
											Objects.nonNull(inZoneAuditor) ? inZoneAuditor[inzone] : null);
								} catch (Exception e) {
									log.error("IncomeAccountAllocate 分配入款审核账号 " + e);
								}
							}
						}
					} catch (InterruptedException e) {
						log.error("创建分配线程异常:", e);
						break;
					}
				}
			});
			THREAD_ALLOC.start();
		}
	}

	/**
	 * 释放线程
	 */
	private void releaseThread() {
		try {
			if (THREAD_UPDATE != null) {
				log.trace("IncomeAccountAllocate 释放更新线程>>localIp:{}", CommonUtils.getInternalIp());
				THREAD_UPDATE.interrupt();// 释放更新线程

			}
			if (THREAD_ALLOC != null) {
				log.trace("IncomeAccountAllocate 释放分配线程>>localIp:{}", CommonUtils.getInternalIp());
				THREAD_ALLOC.interrupt();// 释放分配线程
			}
		} finally {
			THREAD_UPDATE = null;
			THREAD_ALLOC = null;
		}
	}

	private boolean check4upd(String id, int type, int status, Set<String> normalIdSet, Set<String> stopIdSet,
			Set<String> freezeIdSet) {
		boolean result = false, normalContain = normalIdSet.contains(id), stopContain = stopIdSet.contains(id),
				freezeContain = freezeIdSet.contains(id);
		if (type == AccountType.InBank.getTypeId()) {
			if (normalContain || stopContain || freezeContain) {
				if (status == AccountStatus.StopTemp.getStatus() && (normalContain || freezeContain)) {
					normalIdSet.remove(id);
					freezeIdSet.remove(id);
					stopIdSet.add(id);
					result = true;
				}
				if (status == AccountStatus.Normal.getStatus() && (stopContain || freezeContain)) {
					stopIdSet.remove(id);
					freezeIdSet.remove(id);
					normalIdSet.add(id);
					result = true;
				}
				if (status == AccountStatus.Freeze.getStatus() && (stopContain || normalContain)) {
					stopIdSet.remove(id);
					normalIdSet.remove(id);
					freezeIdSet.add(id);
					result = true;
				}
				if (status != AccountStatus.StopTemp.getStatus() && status != AccountStatus.Normal.getStatus()
						&& status != AccountStatus.Freeze.getStatus()) {
					normalIdSet.remove(id);
					stopIdSet.remove(id);
					freezeIdSet.remove(id);
					result = true;
				}
			} else if (status == AccountStatus.StopTemp.getStatus()) {
				stopIdSet.add(id);
				result = true;
			} else if (status == AccountStatus.Normal.getStatus()) {
				normalIdSet.add(id);
				result = true;
			} else if (status == AccountStatus.Freeze.getStatus()) {
				freezeIdSet.add(id);
				result = true;
			}
		} else if (normalContain || stopContain || freezeContain) {
			normalIdSet.remove(id);
			stopIdSet.remove(id);
			freezeIdSet.remove(id);
			result = true;
		}
		return result;
	}

	/**
	 * 入款审核账号分配给审核人员,并把分配信息推送到前端</br>
	 * 1.获取全部账号信息 </br>
	 * 2.获取已分配信息 </br>
	 * 3.待分配账号，根据1，2 结果计算 </br>
	 * 4.计算每人分配 正常账号数 停用账号数 冻结账号数 </br>
	 * 5.分配正常账号，停用账号，冻结账号</br>
	 * 6.保存分配信息,并推送到前端 7.已分配数，总账号数推送到前端
	 */
	private void alloc4IncomeAudit(int inzone, String auditor) {
		Set<String> keys = redisService.getStringRedisTemplate().keys(RedisKeys.genOrigin4IncomeAuditAccountAllocate());
		if (CollectionUtils.isEmpty(keys)) {
			return;
		}
		// 1.全部账号
		Map<AccountStatus, Map<Integer, AccountStatus>> all = buildAllAccount(inzone);
		Map<Integer, AccountStatus> allNormal = all.get(AccountStatus.Normal),
				allStop = all.get(AccountStatus.StopTemp), allFreeze = all.get(AccountStatus.Freeze);
		int countOfNormal = allNormal.size(), countOfStop = allStop.size(), countOfFreeze = allFreeze.size();
		Map<Integer, AccountStatus> allAccountId = new HashMap<>();
		allAccountId.putAll(allNormal);
		allAccountId.putAll(allStop);
		allAccountId.putAll(allFreeze);
		int countOfAll = allAccountId.size();
		// 2.已分配账号
		List<Integer> allocatedList = new ArrayList<>();
		Map<Integer, List<Integer>> allocatedMap = buildAllocatedAccount(allAccountId.keySet(), keys, allocatedList,
				auditor);
		List<AuditAlloc> allocList = initAuditAllocList(allNormal, allStop, allFreeze, allocatedMap);
		// 3.待分配账号
		Map<AccountStatus, List<Integer>> allocating = buildAllocatingAccount(allNormal, allStop, allFreeze,
				allocatedList);
		// 4.计算每人分配 正常账号数 停用账号数 冻结账号数
		allocList = buildAllocCount(buildCountOfMax(), countOfNormal, countOfStop, countOfFreeze, allocList);
		// 5.1分配正常账号
		List<Integer> normalAllocating = allocating.get(AccountStatus.Normal);
		allocList = orderAuditAllocList(allocList, AccountStatus.Normal.getStatus());
		for (AuditAlloc item : allocList) {
			normalAllocating = allocate(item.getCountOfNormal(), item.getNormal(), normalAllocating);
		}
		// 5.2分配停用账号
		List<Integer> stopAllocating = allocating.get(AccountStatus.StopTemp);
		allocList = orderAuditAllocList(allocList, AccountStatus.StopTemp.getStatus());
		for (AuditAlloc item : allocList) {
			stopAllocating = allocate(item.getCountOfStop(), item.getStop(), stopAllocating);
		}
		// 5.3分配冻结账号
		List<Integer> freezeAllocating = allocating.get(AccountStatus.Freeze);
		allocList = orderAuditAllocList(allocList, AccountStatus.Freeze.getStatus());
		for (AuditAlloc item : allocList) {
			freezeAllocating = allocate(item.getCountOfFreeze(), item.getFreeze(), freezeAllocating);
		}
		// 6.保存分配信息,并推送到前端
		int countOfAllocated = 0;
		StringBuilder pubmsg = new StringBuilder();
		for (AuditAlloc item : allocList) {
			item.getNormal().addAll(item.getStop());
			item.getNormal().addAll(item.getFreeze());
			countOfAllocated = countOfAllocated + item.getNormal().size();
			pubmsg.append(RedisKeys.gen4IncomeAuditAccountAllocate(item.userId, item.getNormal())).append(";");
		}
		// 推送到前端
		lpush2alloc(pubmsg.toString());
		// 7.已分配数/总账号数 推送到前端
		userService.broadCastCategoryInfo(countOfAllocated, countOfAll);
	}

	/**
	 * 分配账号 </br>
	 * 1.counting == relValList.size():不需要分配</br>
	 * 2.counting>relValList.size()</br>
	 * #需要从 allocating 分配(counting-elValList.size())个给到 relValList
	 * 3.counting<relValList.size() </br>
	 * #需要从 relValList 返还elValList.size()-counting)个到 allocating集合中
	 *
	 * @param counting
	 *            需要分配账号数目
	 * @param relValList
	 *            已分配账号列表
	 * @param allocating
	 *            待分配列表（队列）
	 */
	private List<Integer> allocate(int counting, List<Integer> relValList, List<Integer> allocating) {
		int relCount = relValList.size();
		if (relCount < counting) {
			relValList.addAll(allocating.subList(0, (counting - relCount)));
			allocating = allocating.subList((counting - relCount), allocating.size());
		} else if (relCount > counting) {
			int count = relCount - counting;
			while (count > 0) {
				allocating.add(relValList.remove(0));
				count = count - 1;
			}
		}
		return allocating;
	}

	/**
	 * 组装用户的有效性
	 * #key格式：IncomeAuditAccountAllocate:userId:allocatedAccountId1:allocatedAccountId2:allocatedAccountId3:
	 *
	 * @param keys
	 *            所有账号
	 * @return Map<Integer, Boolean>=>userId->Boolean
	 */
	private Map<Integer, Boolean> buildValidAuditor(Set<String> keys) {
		Map<Integer, Boolean> result = new HashMap<>();
		Set<String> allHost = new HashSet<>();
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		template.getClientList().forEach((p -> allHost.add(p.get(RedisClientInfo.INFO.ADDRESS_PORT).split(":")[0])));
		List<Object> keyList = new ArrayList<>();
		keys.forEach((p) -> keyList.add(p.split(":")[1]));
		List<Object> valList = template.boundHashOps(RedisKeys.INCOME_AUDIT_ACCOUNT_HOST).multiGet(keyList);
		for (int index = 0; index < keyList.size(); index++) {
			String val = (String) valList.get(index);
			result.put(Integer.valueOf(keyList.get(index).toString()), allHost.contains(val));
		}
		return result;
	}

	/**
	 * 对审核分配列表（List<AuditAlloc>）排序，排列顺序：饱和AuditAlloc，未饱和AuditAlloc </br>
	 * 饱和AuditAlloc：分配列表的元素个数 > 分配数目；未饱和AuditAlloc：分配列表的元素个数 <= 分配数目</br>
	 * 正常分配数目：AuditAlloc.countOfNormal ；正常分配列表个数：AuditAlloc.normal.size()</br>
	 *
	 * @param allocList
	 *            审核分配列表
	 * @param status
	 *            状态：@see com.xinbo.fundstransfer.domain.enums.AccountStatus
	 *            (正常，停用，冻结)
	 */
	private List<AuditAlloc> orderAuditAllocList(List<AuditAlloc> allocList, int status) {
		List<AuditAlloc> result = allocList;
		if (AccountStatus.Normal.getStatus().equals(status)) {
			result = allocList.stream().filter((p) -> p.getNormal().size() > p.getCountOfNormal())
					.collect(Collectors.toList());
			result.addAll(allocList.stream().filter((p) -> p.getNormal().size() <= p.getCountOfNormal())
					.collect(Collectors.toList()));
		} else if (AccountStatus.StopTemp.getStatus().equals(status)) {
			result = allocList.stream().filter((p) -> p.getStop().size() > p.getCountOfStop())
					.collect(Collectors.toList());
			result.addAll(allocList.stream().filter((p) -> p.getStop().size() <= p.getCountOfStop())
					.collect(Collectors.toList()));
		} else if (AccountStatus.Freeze.getStatus().equals(status)) {
			result = allocList.stream().filter((p) -> p.getStop().size() > p.getCountOfStop())
					.collect(Collectors.toList());
			result.addAll(allocList.stream().filter((p) -> p.getStop().size() <= p.getCountOfStop())
					.collect(Collectors.toList()));
		}
		return result;
	}

	/**
	 * 计算每人分配 正常账号数 停用账号数 冻结账号数
	 *
	 * @param countOfMax
	 *            每人分配最大数目
	 * @param countOfNormal
	 *            正常账号总数
	 * @param countOfStop
	 *            停用账号总数
	 * @param countOfFreeze
	 *            冻结账号总数
	 * @param allocList
	 *            分配人信息
	 */
	private List<AuditAlloc> buildAllocCount(int countOfMax, int countOfNormal, int countOfStop, int countOfFreeze,
			List<AuditAlloc> allocList) {
		int countOfUser = CollectionUtils.isEmpty(allocList) ? 0 : allocList.size();
		if (countOfUser == 0 || countOfMax <= 0 || (countOfNormal == 0 && countOfStop == 0 && countOfFreeze == 0)) {
			return allocList;
		}
		if (countOfNormal > 0) {// 分配正常账号
			// 待分配账号取值：未分配账号 大于等于 审核人数时，未分配账号使用审核人总数。否则使用正常账号总数
			int allocNormal = countOfNormal >= countOfUser ? countOfUser : countOfNormal;
			// 未分配账号总数：未分配账号-待分配账号
			countOfNormal = countOfNormal - allocNormal;
			// 无账号 审核人总数：审核人总数-待分配账号总数
			int borrowToNormal = countOfUser - allocNormal;

			int allocStop = countOfStop >= borrowToNormal ? borrowToNormal : countOfStop;
			countOfStop = countOfStop - allocStop;
			borrowToNormal = borrowToNormal - allocStop;
			int allocFreeze = countOfFreeze >= borrowToNormal ? borrowToNormal : countOfFreeze;
			countOfFreeze = countOfFreeze - allocFreeze;
			allocCount(allocNormal, allocStop, allocFreeze, allocList);
			if ((countOfMax = countOfMax - 1) <= 0 || borrowToNormal > allocFreeze) {
				return allocList;
			}
			return buildAllocCount(countOfMax, countOfNormal, countOfStop, countOfFreeze, allocList);
		}
		if (countOfStop > 0) {// 分配暂停账号
			int allocStop = countOfStop >= countOfUser ? countOfUser : countOfStop;
			countOfStop = countOfStop - allocStop;
			int borrowToStop = countOfUser - allocStop;
			int allocFreeze = countOfFreeze >= borrowToStop ? borrowToStop : countOfFreeze;
			countOfFreeze = countOfFreeze - allocFreeze;
			borrowToStop = borrowToStop - allocFreeze;
			int allocNormal = countOfNormal >= borrowToStop ? borrowToStop : countOfNormal;
			countOfNormal = countOfNormal - allocNormal;
			allocCount(allocNormal, allocStop, allocFreeze, allocList);
			if (borrowToStop > allocNormal || (countOfMax = countOfMax - 1) <= 0) {
				return allocList;
			}
		}
		if (countOfFreeze > 0) {// 分配冻结账号
			int allocFreeze = countOfFreeze >= countOfUser ? countOfUser : countOfFreeze;
			countOfFreeze = countOfFreeze - allocFreeze;
			int borrowToFreeze = countOfUser - allocFreeze;
			int allocNormal = countOfNormal >= borrowToFreeze ? borrowToFreeze : countOfNormal;
			countOfNormal = countOfNormal - allocNormal;
			borrowToFreeze = borrowToFreeze - allocNormal;
			int allocStop = countOfStop >= borrowToFreeze ? borrowToFreeze : countOfStop;
			countOfStop = countOfStop - allocStop;
			borrowToFreeze = borrowToFreeze - allocStop;
			allocCount(allocNormal, allocStop, allocFreeze, allocList);
			if (borrowToFreeze > 0 || (countOfMax = countOfMax - 1) <= 0) {
				return allocList;
			}
		}
		return buildAllocCount(countOfMax, countOfNormal, countOfStop, countOfFreeze, allocList);
	}

	/**
	 * 把正常账号，停用账号，冻结账号依次分配给入款审核人员
	 * 备注：allocNormal+allocStop+allocFreeze<=allocList.size();
	 *
	 * @param allocNormal
	 *            正常账号数
	 * @param allocStop
	 *            停用账号数
	 * @param allocFreeze
	 *            冻结账号数
	 * @param allocList
	 *            分配信息
	 */
	private void allocCount(int allocNormal, int allocStop, int allocFreeze, final List<AuditAlloc> allocList) {
		for (AuditAlloc alloc : allocList) {
			if (allocNormal <= 0 && allocStop <= 0 && allocFreeze <= 0) {
				return;
			}
			if (allocNormal > 0) {
				alloc.setCountOfNormal(alloc.getCountOfNormal() + 1);
				allocNormal = allocNormal - 1;
			} else if (allocStop > 0) {
				alloc.setCountOfStop(alloc.getCountOfStop() + 1);
				allocStop = allocStop - 1;
			} else if (allocFreeze > 0) {
				alloc.setCountOfFreeze(alloc.getCountOfFreeze() + 1);
				allocFreeze = allocFreeze - 1;
			}
		}
	}

	/**
	 * 全部账号按照状态进行分组
	 */
	private Map<AccountStatus, Map<Integer, AccountStatus>> buildAllAccount(Integer inzone) {
		String normalHis = null, stopHis = null, freezeHis = null;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		// 获取全部 normal,stop,freeze 账号信息（需要加读锁）
		try {
			LOCK_UPDATE.readLock().lock();
			normalHis = template.boundValueOps(RedisKeys.INCOME_AUDIT_ACCOUNT_NORMAL).get();
			stopHis = template.boundValueOps(RedisKeys.INCOME_AUDIT_ACCOUNT_STOP).get();
			freezeHis = template.boundValueOps(RedisKeys.INCOME_AUDIT_ACCOUNT_FREEZE).get();
		} finally {
			LOCK_UPDATE.readLock().unlock();
		}
		normalHis = StringUtils.trimToNull(normalHis);
		stopHis = StringUtils.trimToNull(stopHis);
		freezeHis = StringUtils.trimToNull(freezeHis);
		// 封装全部 normal,stop,freeze 账号信息
		Map<AccountStatus, Map<Integer, AccountStatus>> result = new HashMap<>();
		Map<Integer, AccountStatus> normal = new HashMap<>(), stop = new HashMap<>(), freeze = new HashMap<>();
		if (normalHis != null) {// 正常账号
			Stream.of(normalHis.split(",")).mapToInt((x) -> Integer.valueOf(x)).distinct().filter(p -> {
				if (Objects.isNull(inzone))
					return true;
				Integer t = transToInZone(p);
				return Objects.nonNull(t) && Objects.nonNull(inzone) && t == inzone;
			}).forEach((x) -> normal.put(x, AccountStatus.Normal));
		}
		if (stopHis != null) {// 停用账号
			Stream.of(stopHis.split(",")).mapToInt((x) -> Integer.valueOf(x)).distinct().filter(p -> {
				if (Objects.isNull(inzone))
					return true;
				Integer t = transToInZone(p);
				return Objects.nonNull(t) && t == inzone;
			}).filter((x) -> !normal.containsKey(x)).forEach((x) -> stop.put(x, AccountStatus.StopTemp));
		}
		if (freezeHis != null) {// 冻结账号
			Stream.of(freezeHis.split(",")).mapToInt((x) -> Integer.valueOf(x)).distinct().filter(p -> {
				if (Objects.isNull(inzone))
					return true;
				Integer t = transToInZone(p);
				return Objects.nonNull(t) && t == inzone;
			}).filter((x) -> !normal.containsKey(x) && !stop.containsKey(x))
					.forEach((x) -> freeze.put(x, AccountStatus.Freeze));
		}
		result.put(AccountStatus.Normal, normal);
		result.put(AccountStatus.StopTemp, stop);
		result.put(AccountStatus.Freeze, freeze);
		return result;
	}

	/**
	 * 组装已分配账号
	 * #key格式：IncomeAuditAccountAllocate:userId:allocatedAccountId1:allocatedAccountId2:allocatedAccountId3:
	 *
	 * @param allAccount
	 *            所有账号
	 * @param keys
	 *            入款审核用户在Redis中储存Key
	 * @param allocatedList
	 *            已分配账号（当返回值使用）
	 * @return Map<userId, List<allocatedAccountId>>
	 */
	private Map<Integer, List<Integer>> buildAllocatedAccount(Set<Integer> allAccount, Set<String> keys,
			List<Integer> allocatedList, String auditor) {
		Map<Integer, List<Integer>> result = new HashMap<>();
		Set<Integer> allocatedSet = new HashSet<>();
		Map<Integer, Boolean> validAuditor = buildValidAuditor(keys);
		Set<String> unvalidKeys = new HashSet<>();
		for (String key : keys) {
			String[] arr = key.split(":");
			// redis 中缓存的数据格式是字符串
			// IncomeAuditAccountAllocate:5:249570:250242:261441:2914:2884:248614:249573:2521:260511:5292:2398:
			Integer userId = Integer.valueOf(arr[1]);
			if (!validAuditor.get(userId)) {
				unvalidKeys.add(key);
				continue;
			}
			if (Objects.nonNull(auditor) && !auditor.contains(String.format(";%d;", userId))) {
				continue;
			}
			List<Integer> accountIdList = new ArrayList<>();
			if (arr.length > 2) {
				// 该用户 入款审核接单的入款账号
				for (String p : Arrays.copyOfRange(arr, 2, arr.length)) {
					int t = Integer.valueOf(p);
					if (allocatedSet.contains(t) || (allAccount != null && !allAccount.contains(t))) {
						continue;
					}
					accountIdList.add(t);
				}
				allocatedSet.addAll(accountIdList);
			}
			result.put(userId, accountIdList);
		}
		redisService.getStringRedisTemplate().delete(unvalidKeys);
		if (allocatedList != null) {
			allocatedList.addAll(allocatedSet);
		}
		return result;
	}

	/**
	 * 组装待分配账号，并按照状态分组
	 *
	 * @param allNormal
	 *            所有正常账号
	 * @param allStop
	 *            所有停用账号
	 * @param allFreeze
	 *            所有冻结账号
	 * @param allocatedList
	 *            已分配账号
	 */
	private Map<AccountStatus, List<Integer>> buildAllocatingAccount(Map<Integer, AccountStatus> allNormal,
			Map<Integer, AccountStatus> allStop, Map<Integer, AccountStatus> allFreeze, List<Integer> allocatedList) {
		Map<AccountStatus, List<Integer>> result = new HashMap<>();
		List<Integer> normal = new ArrayList<>(allNormal.keySet()), stop = new ArrayList<>(allStop.keySet()),
				freeze = new ArrayList<>(allFreeze.keySet());
		normal.removeAll(allocatedList);
		stop.removeAll(allocatedList);
		freeze.removeAll(allocatedList);
		result.put(AccountStatus.Normal, normal);
		result.put(AccountStatus.StopTemp, stop);
		result.put(AccountStatus.Freeze, freeze);
		return result;
	}

	/**
	 * 初始化审核分配信息
	 *
	 * @param allNormal
	 *            所有正常账号
	 * @param allStop
	 *            所有停用账号
	 * @param allFreeze
	 *            所有冻结账号
	 * @param allAllocatedMap
	 *            所有已分配账号 Map<userId, List<allAllocatedAccouontId>>
	 */
	private List<AuditAlloc> initAuditAllocList(Map<Integer, AccountStatus> allNormal,
			Map<Integer, AccountStatus> allStop, Map<Integer, AccountStatus> allFreeze,
			Map<Integer, List<Integer>> allAllocatedMap) {
		List<AuditAlloc> result = new ArrayList<>();
		for (Map.Entry<Integer, List<Integer>> entry : allAllocatedMap.entrySet()) {
			List<Integer> valList = entry.getValue();
			List<Integer> normal = valList.stream().filter((p) -> AccountStatus.Normal.equals(allNormal.get(p)))
					.collect(Collectors.toList());
			List<Integer> stop = valList.stream().filter((p) -> AccountStatus.StopTemp.equals(allStop.get(p)))
					.collect(Collectors.toList());
			List<Integer> freeze = valList.stream().filter((p) -> AccountStatus.Freeze.equals(allFreeze.get(p)))
					.collect(Collectors.toList());
			result.add(new AuditAlloc(entry.getKey(), normal, stop, freeze));
		}
		return result;
	}

	private int buildCountOfMax() {
		SysUserProfile profile = userProfileService.findByPropertyKeyAndUserId(
				UserProfileKey.INCOME_ACCOUNTS_PERUSER.getValue(), AppConstants.USER_ID_4_ADMIN);
		int result = profile == null || StringUtils.isBlank(profile.getPropertyValue()) ? MAX_ALLOCATE_NUM
				: Integer.valueOf(profile.getPropertyValue());
		return result > MAX_ALLOCATE_NUM || result < 1 ? MAX_ALLOCATE_NUM : result;
	}

	private int buildValidDaysOfFreezeAccount() {
		SysUserProfile profile = userProfileService.findByPropertyKeyAndUserId(
				UserProfileKey.FINANCE_FROZEN_ACCOUNTS_FLOWS_EXPIRY.getValue(), AppConstants.USER_ID_4_ADMIN);
		int result = profile == null || StringUtils.isBlank(profile.getPropertyValue()) ? 7
				: Integer.valueOf(profile.getPropertyValue());
		return result <= 0 ? 7 : result;
	}

	/**
	 * 全量获取 入款审核分配账号 （normalIdSet：正常，stopIdSet：停用；freezeIdSet：冻结）
	 */
	private void fetch4upd(Set<String> normalIdSet, Set<String> stopIdSet, Set<String> freezeIdSet) {
		String normalHis = redisService.getStringRedisTemplate().boundValueOps(RedisKeys.INCOME_AUDIT_ACCOUNT_NORMAL)
				.get();
		String stopHis = redisService.getStringRedisTemplate().boundValueOps(RedisKeys.INCOME_AUDIT_ACCOUNT_STOP).get();
		String freezeHis = redisService.getStringRedisTemplate().boundValueOps(RedisKeys.INCOME_AUDIT_ACCOUNT_FREEZE)
				.get();
		for (String t : StringUtils.trimToEmpty(normalHis).split(",")) {
			if (StringUtils.isNotBlank(t)) {
				normalIdSet.add(t);
			}
		}
		for (String t : StringUtils.trimToEmpty(stopHis).split(",")) {
			if (StringUtils.isNotBlank(t)) {
				stopIdSet.add(t);
			}
		}
		for (String t : StringUtils.trimToEmpty(freezeHis).split(",")) {
			if (StringUtils.isNotBlank(t)) {
				freezeIdSet.add(t);
			}
		}
	}

	/**
	 * 执行lua script:批量取出待更新账号</br>
	 */
	private String lpop4upd() {
		return redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD_POP";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD_POP;
			}
		}, null, RedisKeys.INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD);
	}

	/**
	 * 执行 lua script : 全量加载 入款审核账号（正常，停用，冻结）
	 */
	private void lload2reboot(Set<String> normalSet, Set<String> stopSet, Set<String> freezeSet) {
		StringBuilder normal = new StringBuilder(), stop = new StringBuilder(), freeze = new StringBuilder();
		normalSet.forEach(p -> normal.append(p).append(","));
		stopSet.forEach(p -> stop.append(p).append(","));
		freezeSet.forEach(p -> freeze.append(p).append(","));
		redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD_LOAD";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_UPD_LOAD;
			}
		}, null, RedisKeys.INCOME_AUDIT_ACCOUNT_NORMAL, RedisKeys.INCOME_AUDIT_ACCOUNT_STOP,
				RedisKeys.INCOME_AUDIT_ACCOUNT_FREEZE, normal.toString(), stop.toString(), freeze.toString());
	}

	/**
	 * 执行 lua script :推送入款分配账号
	 */
	private void lpush2alloc(String pubmsg) {
		redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_ALLOC";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return LUA_SCRIPT_INCOME_AUDIT_ACCOUNT_ALLOCATE_ALLOC;
			}
		}, null, pubmsg, RedisTopics.ACCOUNT_ALLOCATED);
	}

	/**
	 * 用户审核分配信息
	 */
	private class AuditAlloc {
		private int userId;
		/**
		 * 分配正常账号数
		 */
		private int countOfNormal = 0;
		/**
		 * 分配停用账号数
		 */
		private int countOfStop = 0;
		/**
		 * 分配停用账号数
		 */
		private int countOfFreeze = 0;
		/**
		 * 正常账号列表
		 */
		private List<Integer> normal = null;
		/**
		 * 停用账号列表
		 */
		private List<Integer> stop = null;
		/**
		 * 冻结账号列表
		 */
		private List<Integer> freeze = null;

		private AuditAlloc(int userId, List<Integer> normal, List<Integer> stop, List<Integer> freeze) {
			this.userId = userId;
			this.normal = normal == null ? new ArrayList<>() : normal;
			this.stop = stop == null ? new ArrayList<>() : stop;
			this.freeze = freeze == null ? new ArrayList<>() : freeze;
		}

		private int getUserId() {
			return userId;
		}

		private int getCountOfNormal() {
			return countOfNormal;
		}

		private void setCountOfNormal(int countOfNormal) {
			this.countOfNormal = countOfNormal;
		}

		private int getCountOfStop() {
			return countOfStop;
		}

		private void setCountOfStop(int countOfStop) {
			this.countOfStop = countOfStop;
		}

		private int getCountOfFreeze() {
			return countOfFreeze;
		}

		private void setCountOfFreeze(int countOfFreeze) {
			this.countOfFreeze = countOfFreeze;
		}

		private List<Integer> getNormal() {
			return normal;
		}

		private List<Integer> getStop() {
			return stop;
		}

		private List<Integer> getFreeze() {
			return freeze;
		}
	}
}
