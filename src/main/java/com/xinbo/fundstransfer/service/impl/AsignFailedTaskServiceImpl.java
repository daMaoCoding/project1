package com.xinbo.fundstransfer.service.impl;

import com.google.common.collect.Lists;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageModel;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageProducer;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageType;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.entity.BizTaskReviewEntity;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.TroubleShootDTO;
import com.xinbo.fundstransfer.domain.repository.BizTaskReviewEntityRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2018/9/19.
 */
@Service
public class AsignFailedTaskServiceImpl implements AsignFailedTaskService {
	private static final Logger log = LoggerFactory.getLogger(AsignFailedTaskServiceImpl.class);
	@Autowired
	private BizTaskReviewEntityRepository bizTaskReviewEntityRepository;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private RedisService redisService;
	@Autowired
	private OutwardTaskRepository outwardTaskRepository;

	@Autowired
	private HandicapService handicapService;

	@Autowired
	private SysUserProfileService userProfileService;
	@Autowired
	private MessageProducer messageProducer;
	@Autowired
	private RedisScriptService redisScriptService;
	@Autowired
	@Lazy
	private AccountService accountService;

	// 查询在线接单人员正在处理的单子数量 obj[0] operator obj[1] count 升序
	@Override
	public List<Object[]> getOperatorAndDealingCount(List<String> users) {
		String sql = "select * from (select distinct operator, count(operator) as num from fundsTransfer.biz_task_review where  finish_time is null ";
		List<Object[]> list = getInfo(users, sql);
		return list;
	}

	// 判断是否在线用户
	@Override
	public boolean isOnlineUsers(int userId, int zone) {
		boolean isMember = false;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		boolean exist = template.hasKey(RedisKeys.TASK_REVIEW_ZONE_ONLINEUSER_KEY + zone);
		if (exist) {
			return template.boundSetOps(RedisKeys.TASK_REVIEW_ZONE_ONLINEUSER_KEY + zone)
					.isMember(String.valueOf(userId));
		}
		return isMember;
	}

	private List<Object[]> getInfo(List<String> users, String sql) {
		int size = users.size();
		if (size == 1) {
			sql += " and operator = " + users.get(0);
		} else {
			sql += " and operator in ( ";
			for (int i = 0; i < size; i++) {
				if (i < size - 1) {
					sql += users.get(i) + ",";
				} else {
					sql += users.get(i) + ")";
				}
			}
		}
		sql += " group by operator ) r order by r.num  ;";
		List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
		Set<String> tempSet = new HashSet<>();
		List<String> usersNoCounts;
		if (!CollectionUtils.isEmpty(list)) {
			int size2 = list.size();
			list.stream().forEach(p -> tempSet.add(p[0].toString()));
			if (size > size2) {
				// 在线用户多于处理过单子的用户/正在处理任务的用户
				usersNoCounts = CommonUtils.getDiffrentList(users, new ArrayList<>(tempSet));
				if (!CollectionUtils.isEmpty(usersNoCounts)) {
					usersNoCounts.forEach(p -> {
						Object[] obj = new Object[] { p, 0 };
						list.add(obj);
					});
				}
			}
		} else {
			users.forEach(p -> {
				Object[] obj = new Object[] { p, 0 };
				list.add(obj);
			});
		}
		Collections.sort(list, Comparator.comparingInt(o -> Integer.valueOf(o[1].toString())));
		return list;
	}

	// 根据ip和账号，以及账号下的待分配排查任务数量，组装成 k-v:ip-任务数量 升序
	private Map<String, Integer> wrapIpMapTasksCounts(Map<String, Set<Integer>> ip2AccountMap,
			Map<Integer, List<Object[]>> accountMapReviewTasks) {
		Map<String, Integer> map = new LinkedHashMap<>();
		ip2AccountMap.entrySet().forEach(ip -> {
			AtomicInteger tasksCount = new AtomicInteger(0);
			ip.getValue().stream().forEach(p -> tasksCount.getAndAdd(accountMapReviewTasks.get(p).size()));
			map.put(ip.getKey(), tasksCount.get());
			tasksCount.set(0);
		});
		List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, Comparator.comparingInt(Map.Entry::getValue));
		map.clear();
		list.stream().forEach(p -> map.put(p.getKey(), p.getValue()));
		log.debug("根据ip和账号，以及账号下的待分配排查任务数量，组装成 k-v:ip-任务数量 升序 结果:{}", map.toString());
		return map;
	}

	// 根据查询出来的待分配排查任务，组装以accountId为key，以包含taskid handicap 的数组为元素的list做value
	private Map<Integer, List<Object[]>> wrapAccountIdMapReviewTask(List<Object[]> list) {
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		log.debug("根据查询出来的待分配排查任务，组装以accountId为key，以包含taskid handicap 的数组为元素的list做value :{}", list.toString());
		Map<Integer, List<Object[]>> map = new LinkedHashMap<>();
		list.stream().forEach(p -> {
			List<Object[]> list1 = new ArrayList<>();
			list1.add(new Object[] { p[0], p[1] });
			if (!CollectionUtils.isEmpty(map)) {
				int key = Integer.valueOf(p[2].toString());
				if (map.containsKey(key)) {
					map.get(key).add(new Object[] { p[0], p[1] });
				} else {
					map.put(key, list1);
				}
			} else {
				map.put(Integer.valueOf(p[2].toString()), list1);
			}
		});
		List<Map.Entry<Integer, List<Object[]>>> list1 = new ArrayList<>(map.entrySet());
		Collections.sort(list1, Comparator.comparingInt(o -> o.getValue().size()));
		map.clear();
		list1.stream().forEach(p -> map.put(p.getKey(), p.getValue()));
		log.debug("封装 结果:{}", map.toString());
		return map;
	}

	// 查询ip主机以及该ip下挂的账号，按账号数量升序返回
	@Override
	public Map<String, Set<Integer>> getRealIpAndAccountIds(Integer[] accountIds) {
		if (accountIds == null || accountIds.length == 0) {
			return null;
		}
		String sql = "SELECT DISTINCT a.id,h.ip  FROM  biz_account a, biz_host h  where a.gps is not null ";
		if (accountIds.length == 1) {
			sql += "and a.id =" + accountIds[0];
		} else {
			sql += "and a.id in (";
			for (int i = 0, len = accountIds.length; i < len; i++) {
				if (i < len - 1) {
					sql += accountIds[i] + ",";
				} else {
					sql += accountIds[i] + ")";
				}
			}
		}
		sql += "  and (a.gps = h.ip or h.host_info like concat('%',a.gps,',%')) ";
		if (accountIds.length == 1) {
			sql += " limit 1 ";
		}
		List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		Map<String, Set<Integer>> ret = new LinkedHashMap<>(), map = new HashMap<>();
		for (int i = 0, size = list.size(); i < size; i++) {
			Object[] obj = list.get(i);// accountId ip
			String ip = obj[1].toString();
			Integer accountId = Integer.valueOf(obj[0].toString());
			Set<Integer> accountIdSet = new HashSet<>();
			if (map.size() > 0) {
				if (map.containsKey(ip)) {
					accountIdSet = map.get(ip);
					accountIdSet.add(accountId);
					map.put(ip, accountIdSet);
				} else {
					accountIdSet.add(accountId);
					map.put(ip, accountIdSet);
				}
			} else {
				accountIdSet.add(accountId);
				map.put(ip, accountIdSet);
			}
		}
		List<Map.Entry<String, Set<Integer>>> list1 = new ArrayList<>(map.entrySet());
		Collections.sort(list1, Comparator.comparingInt(o -> o.getValue().size()));
		list1.stream().forEach(p -> ret.put(p.getKey(), p.getValue()));
		return ret;
	}

	// 获取用户锁定的在排查的任务,如果该任务id对应的出款账号有挂载物理机则该ip下的所有任务都算是锁定
	// userId为空则返回全部锁定的
	// type : query 表示查询用 assign 表示分配用
	@Override
	public List<String> getLockedTaskIdByUserId(Integer userId, String type) {
		Set<String> locked = null;
		Set<String> lockedSet, keys;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		if (userId == null) {
			// ReviewTask:UserId:Ip:TaskId:5:10001-->taskIds
			// ReviewTask:UserId:Ip:TaskId:5:ip-->taskIds
			keys = template.keys(RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + "*:*");
		} else {
			keys = template.keys(RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + userId + ":*");
		}
		if (!CollectionUtils.isEmpty(keys)) {
			locked = new HashSet<>();
			for (String key : keys) {
				lockedSet = template.boundSetOps(key).members();
				locked.addAll(lockedSet);
				// 如果是分配,则要包含锁定任务对应ip下的所有任务
				if ("assign".equals(type)) {
					String[] keyArray = key.split(":");
					String ip = keyArray[5];// IP 或者是10001 表示该任务对应的出款账号没有挂载物理机上
					if (checkStrIsIp(ip)) {
						// ReviewTask:User:Ip:Account:TaskId:5:192.168.100.1:100 --> TaskId Set
						// 该任务id对应的出款账号关联的物理ip
						Set<String> ipAccountTasksKeys = template
								.keys(RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + "*:" + ip + ":*");
						if (!CollectionUtils.isEmpty(ipAccountTasksKeys)) {
							for (Iterator it = ipAccountTasksKeys.iterator(); it.hasNext();) {
								// ReviewTask:User:Ip:Account:TaskId:5:192.168.100.1:100
								String key3 = it.next().toString();
								Set<String> ipAccountTasks = template.boundSetOps(key3).members();
								locked.addAll(ipAccountTasks);
							}
						}
					}
				}
			}
		}
		if (!CollectionUtils.isEmpty(locked)) {
			return new ArrayList<>(locked);
		} else {
			return new ArrayList<>();
		}
	}

	// 判断是否是IP
	private boolean checkStrIsIp(String ipStr) {
		if (ipStr.length() < 7 || ipStr.length() > 15 || StringUtils.isBlank(ipStr)) {
			return false;
		}
		/**
		 * 判断IP格式和范围
		 */
		String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
		Pattern pat = Pattern.compile(rexp);
		Matcher mat = pat.matcher(ipStr);
		boolean ipAddress = mat.find();
		return ipAddress;

	}

	// 排查锁定的时候 把锁定的任务出款账号对应的ip下的所有出款账号的任务也锁定
	@Override
	public int lockTaskForCheck(int taskId, int userId) {
		BizOutwardTask task = outwardTaskRepository.findById2((long) taskId);
		Long add = 0L;
		if (task != null && (task.getStatus().equals(OutwardTaskStatus.Failure.getStatus())
				|| task.getStatus().equals(OutwardTaskStatus.ManagerDeal.getStatus()))) {
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			// 10001表示该任务的出款账号没有挂载ip或者该任务没有出款账号
			String key = RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + userId + ":10001";
			if (task.getAccountId() != null) {
				Map<String, Set<Integer>> map = getRealIpAndAccountIds(new Integer[] { task.getAccountId() });
				if (!CollectionUtils.isEmpty(map)) {
					for (Map.Entry<String, Set<Integer>> entry : map.entrySet()) {
						// entry.getKey() -->ip
						// 分配给用户的有Ip的所有任务
						String ip = entry.getKey();
						String key1 = RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + userId + ":" + ip + ":*";
						Set<String> keys2 = template.keys(key1);
						key = RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + userId + ":" + ip;
						if (!CollectionUtils.isEmpty(keys2)) {
							for (Iterator it = keys2.iterator(); it.hasNext();) {
								Set<String> taskIds = template.boundSetOps(it.next().toString()).members();
								if (!CollectionUtils.isEmpty(taskIds)) {
									for (Iterator it2 = taskIds.iterator(); it2.hasNext();) {
										add = template.boundSetOps(key).add(it2.next().toString());
									}
								}
							}
						}
					}
				} else {
					// 没有挂载ip
					add = template.boundSetOps(key).add(String.valueOf(taskId));
				}
			} else {
				// 没有出款账号的任务排查
				add = template.boundSetOps(key).add(String.valueOf(taskId));
			}
		}
		return add.intValue();
	}

	// 查询待排查人员手中或已处理的任务信息
	@Override
	public List<Object[]> troubleShootList(TroubleShootDTO troubleShootDTO, Integer[] operator, Integer fromAccount[],
			List<Integer> shooterList, String[] handicapCodes, List<String> lockedTaskIds) {
		String sql;
		if ("1".equals(troubleShootDTO.getQueryType()) || "3".equals(troubleShootDTO.getQueryType())) {
			sql = "select t.id ,t.outward_request_id,t.amount,t.asign_time,t.time_consuming,t.operator,t.account_id,t.remark,t.screenshot,t.to_account,t.to_account_owner,t.handicap,t.level,t.member,t.order_no,t.status ,r.operator as shooter ";
		} else {
			sql = "select t.id ,t.outward_request_id,t.amount,r.finish_time,t.time_consuming,t.operator,t.account_id,r.remark,t.screenshot,t.to_account,t.to_account_owner,t.handicap,t.level,t.member,t.order_no,t.status ,r.operator as shooter ,r.finish_time - r.asign_time ";
		}
		sql = wrapSql(sql, troubleShootDTO, operator, fromAccount, shooterList, handicapCodes, lockedTaskIds);
		List<Object[]> list = entityManager.createNativeQuery(sql)
				.setFirstResult(troubleShootDTO.getPageNo() * troubleShootDTO.getPageSize())
				.setMaxResults(troubleShootDTO.getPageSize()).getResultList();
		return list;
	}

	@Override
	public double troubleShootSum(TroubleShootDTO troubleShootDTO, Integer[] operator, Integer fromAccount[],
			List<Integer> shooterList, String[] handicapCodes, List<String> lockedTaskIds) {
		String sql = " select sum(t.amount)  ";
		sql = wrapSql(sql, troubleShootDTO, operator, fromAccount, shooterList, handicapCodes, lockedTaskIds);
		BigDecimal sum = (BigDecimal) entityManager.createNativeQuery(sql).getSingleResult();
		return Objects.isNull(sum) ? 0 : sum.doubleValue();
	}

	@Override
	public long troubleShootCount(TroubleShootDTO troubleShootDTO, Integer[] operator, Integer fromAccount[],
			List<Integer> shooterList, String[] handicapCodes, List<String> lockedTaskIds) {
		String sql = " select count(t.amount)  ";
		sql = wrapSql(sql, troubleShootDTO, operator, fromAccount, shooterList, handicapCodes, lockedTaskIds);
		BigInteger count = (BigInteger) entityManager.createNativeQuery(sql).getSingleResult();
		return Objects.isNull(count) ? 0 : count.longValue();
	}

	@Override
	public Object getTaskReviewByTaskId(int taskId) {
		Object obj = bizTaskReviewEntityRepository.findReviewTaskByTaskId(taskId);
		return obj;
	}

	// socket打开连接,用户保存在redis,分配排查任务
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean asignTaskOnSocketOpen(int userId) {
		try {
			log.info(
					"user {} open socket  , {} execute  AsignFailedTaskServiceImpl.asignTaskOnSocketOpen method after socket open ",
					userId, CommonUtils.getInternalIp());
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			log.debug("获取用户 区域: userId:{},zone:{}", userId, zone);
			if (zone == -1) {
				log.info("用户:{},没有划分区域!", userId);
				return true;
			}
			log.debug("用户开始接单 删除 未处理的任务:{}", userId);
			deleteIpAccountTasksOnStart(userId);
			asignReviewTaskOnStart(userId);
			return true;
		} catch (Exception e) {
			log.error("  execute asignTaskOnSocketOpen error :", e);
			return false;
		}
	}

	private static final String TASK_REVIEW_ASSIGN_LOCK_SCRIPT = "local k = KEYS[1];\n local v = ARGV[1];\n"
			+ "  local ex = redis.call('SETNX',k,v); \nlocal ret=0;  \nif ex ~=nil and ex >0 then\n"
			+ "  ret = redis.call('EXPIRE',k,15) \n end  \n return ret";

	@Override
	public int getLock4AsignTask() {
		Long ret = redisService.getStringRedisTemplate().execute(
				redisScriptService.getRedisScriptInstance(Long.class, "TASK_REVIEW_ASSIGN_LOCK_SCRIPT",
						TASK_REVIEW_ASSIGN_LOCK_SCRIPT),
				Collections.singletonList(RedisKeys.TASK_REVIEW_ASSIGN_LOCK), "1");
		log.info("订单排查 获取redis锁 结果 :{}", ret);
		return ret.intValue();
	}

	@Override
	public void releaseLock4AsignTask() {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		boolean exist = template.hasKey(RedisKeys.TASK_REVIEW_ASSIGN_LOCK);
		if (exist) {
			log.debug("订单排查 释放redis锁");
			template.delete(RedisKeys.TASK_REVIEW_ASSIGN_LOCK);
		}
	}

	// 分区域保存在线接单用户id
	@Override
	public void startTaskReview(int userId) {
		try {
			int zoneId = userProfileService.getSysUserProfileZoneByUserId(userId);
			log.debug("问题排查接单 根据用户id:{},获取区域结果:{}", userId, zoneId);
			if (zoneId == -1) {
				log.info("该账号没有划分区域!");
				return;
			}
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			template.boundSetOps(RedisKeys.TASK_REVIEW_ZONE_ONLINEUSER_KEY + zoneId).add(String.valueOf(userId));
			// 获取所有在线接单人
			List<String> users = getReviewingUserInRedis(999999);
			log.debug("在线接单人数:{}", users.toString());
			if (CollectionUtils.isEmpty(users) || users.size() == 1) {
				// 第一个人接单的时候把异常操作的遗留缓存事件删除
				boolean exists = template.hasKey(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
				if (exists) {
					template.delete(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
				}
				boolean exists2 = template.hasKey(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY);
				if (exists2) {
					template.delete(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY);
				}
			}
			popMessageOnStartOrStop(userId, MessageType.TASK_REVIEW_START.getType());
			// 生成处理消息
			MessageModel messageModel = new MessageModel(MessageType.TASK_REVIEW_START.getType(), userId);
			messageProducer.pushMessage(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY, messageModel);
			template.convertAndSend(RedisTopics.ASIGN_REVIEWTASK_TOPIC, "ASIGNFAILEDTASK:START:" + userId);
		} catch (Exception e) {
			log.error("问题排查接单异常:", e);
			return;
		}
	}

	// 暂停接单 type=2 删除已分配给该用户 userId 的未锁定的任务,锁定的如有ip对应关系也删除
	// 结束接单 type=3 删除所有已分配给该用户任务
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeUserAndTasksInRedis(int userId, int type) {
		log.debug("问题排查 暂停/结束接单 删除缓存 参数 :", userId, type);
		try {
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			boolean isMember = isOnlineUsers(userId, zone);
			if (isMember) {
				template.boundSetOps(RedisKeys.TASK_REVIEW_ZONE_ONLINEUSER_KEY + zone).remove(String.valueOf(userId));
			}
			// 用户锁定排查订单 的 key
			// ReviewTask:LockUserId:Ip:TaskId:5:192.168.12.6/10001
			String key = RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + userId + ":*";
			Set<String> lockedKeys = template.keys(key);
			// 锁定的但是出款账号没有ip的任务
			List<String> lockedTaskIdsNoIp = new ArrayList<>();
			if (!CollectionUtils.isEmpty(lockedKeys)) {
				for (Iterator it = lockedKeys.iterator(); it.hasNext();) {
					String key2 = it.next().toString();
					String[] key2Array = key2.split(":");
					String ip = key2Array[5];
					if (checkStrIsIp(ip)) {
						// 暂停 结束 要把有ip的单子删除
						template.delete(key2);
					} else {
						if (type == 3) {
							// 结束都删除
							template.delete(key2);
						}
						if (type == 2) {
							// 暂停 不删除已锁定没有ip关系账号出款任务id
							// ReviewTask:LockUserId:Ip:TaskId:5:10001
							lockedTaskIdsNoIp.addAll(template.opsForSet().members(key2));
						}
					}
				}
			}
			// 删除redis缓存 :用户分配到的所有有ip单子
			// ReviewTask:User:Ip:Account:TaskId:5:192.168.10.12:101
			String key4 = RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + userId + ":*";
			Set<String> keys3 = template.keys(key4);
			// Set<String> taskIdsWithIp = new HashSet<>();// 所有有Ip的任务
			if (!CollectionUtils.isEmpty(keys3)) {
				for (Iterator it = keys3.iterator(); it.hasNext();) {
					String key3 = it.next().toString();
					// taskIdsWithIp.addAll(template.boundSetOps(key3).members());
					template.delete(key3);
				}
			}
			// 该用户分配到的所有单子
			List<String> users = new ArrayList<>();
			users.add(String.valueOf(userId));
			if (type == 3) {
				deleteByTaskIdsAndUsers(null, users);
			}
			if (type == 2) {
				// 暂停
				deleteByTaskIdsAndUsers(lockedTaskIdsNoIp, users);
			}
			if (type == 3) {
				// 结束接单
				if (isMember) {
					// 防止结束接单的时候不正常操作导致有订单和缓存
					// 本区域在线接单用户
					List<String> onlineUsers = getReviewingUserInRedis(zone);
					if (!CollectionUtils.isEmpty(onlineUsers)) {
						// 本区域所有用户
						List<String> zoneUsers = userProfileService.getUserIdsByZone(zone);
						// 离线不接单的用户
						List<String> offlineUsers = CommonUtils.getDiffrentList(onlineUsers, zoneUsers);
						if (CollectionUtils.isEmpty(offlineUsers)) {
							return;
						}
						// 删除本区域下不在线但是有接单缓存的任务
						deleteByTaskIdsAndUsers(null, offlineUsers);
					}
				}
			}
		} catch (Exception e) {
			log.error("问题排查 暂停/结束接单 删除缓存 异常 :", e);
		}
	}

	// 根据zone获取在线处理排查的人员 zone==999999 表示查询所有区域接单人
	@Override
	public List<String> getReviewingUserInRedis(int zone) {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		List<String> list = new ArrayList<>();
		if (zone == 999999) {
			Set<String> keys = template.keys(RedisKeys.TASK_REVIEW_ZONE_ONLINEUSER_KEY + "*");
			if (!CollectionUtils.isEmpty(keys)) {
				for (Iterator it = keys.iterator(); it.hasNext();) {
					list.addAll(template.boundSetOps(it.next().toString()).members());
				}
			}
			return list;
		}
		boolean hasKey = template.hasKey(RedisKeys.TASK_REVIEW_ZONE_ONLINEUSER_KEY + zone);
		if (!hasKey) {
			return null;
		}
		Set<String> userSet = template.boundSetOps(RedisKeys.TASK_REVIEW_ZONE_ONLINEUSER_KEY + zone).members();
		if (!CollectionUtils.isEmpty(userSet)) {
			list.addAll(userSet);
		}
		log.debug("根据区域:{},获取接单人:{}", zone, list.toString());
		return list;
	}

	// 获取暂停排查的人和手中的排查任务 但是已经暂停的
	@Override
	public List<Object[]> getUserAndReviewTaskNotInRedis(List<String> users) {
		String sqlOut = " select   a.operator, a.operatorCount , a.status,a.statusCount  from (";
		String sql = "select r.operator as operator , t.status as status, count(r.operator) as operatorCount , count(t.status) as statusCount from fundsTransfer.biz_task_review r join fundsTransfer.biz_outward_task  t on r.taskid = t.id   where  r.asign_time is not null and r.finish_time is null  ";
		if (!CollectionUtils.isEmpty(users)) {
			sql += "  and ";
			if (users.size() == 1) {
				sql += " r.operator !=" + users.get(0);
			} else {
				sql += " r.operator not in ( ";
				for (int i = 0, L = users.size(); i < L; i++) {
					if (i < users.size() - 1) {
						sql += users.get(i) + ",";
					} else {
						sql += users.get(i) + ")";
					}
				}
			}
		}
		sql += " group by  r.operator,t.status ";
		sqlOut += sql;
		sqlOut += ") a  order by a.statusCount DESC ";
		List<Object[]> list = entityManager.createNativeQuery(sqlOut).getResultList();
		return list;
	}

	// 查询正在排查的人员以及手中的记录,按排查数量降序返回
	@Override
	public List<Object[]> getReviewingUserAndReviewTask(List<String> users, int status) {
		String sqlOut = " select   a.operator, a.operatorCount , a.status,a.statusCount  from (";
		String sql;
		if (status == 0) {
			sql = "select r.operator as operator , t.status as status, count(r.operator) as operatorCount , count(t.status) as statusCount from fundsTransfer.biz_task_review r join fundsTransfer.biz_outward_task  t on r.taskid = t.id   where r.asign_time  is not null and r.finish_time is null  ";
		} else {
			sql = "select r.operator as operator , t.status as status, count(r.operator) as operatorCount , count(t.status) as statusCount from fundsTransfer.biz_task_review r join fundsTransfer.biz_outward_task  t on r.taskid = t.id   where t.status="
					+ status + " and  r.asign_time  is not null and r.finish_time is null  ";
		}

		if (!CollectionUtils.isEmpty(users)) {
			sql += "  and ";
			if (users.size() == 1) {
				sql += " r.operator=" + users.get(0);
			} else {
				sql += " r.operator in ( ";
				for (int i = 0, L = users.size(); i < L; i++) {
					if (i < users.size() - 1) {
						sql += users.get(i) + ",";
					} else {
						sql += users.get(i) + ")";
					}
				}
			}
		}
		sql += " group by  r.operator,t.status  ";
		sqlOut += sql;
		sqlOut += ") a  order by a.statusCount DESC";
		List<Object[]> list = entityManager.createNativeQuery(sqlOut).getResultList();
		List<String> usersWithoutTask, tempList = new ArrayList<>();
		if (!CollectionUtils.isEmpty(list)) {
			int t = tempList.size(), u = users.size();
			list.stream().forEach(p -> tempList.add(p[0].toString()));
			if (t < u) {
				usersWithoutTask = CommonUtils.getDiffrentList(users, tempList);
				usersWithoutTask.forEach(p -> {
					Object[] obj = new Object[] { p, 0, 0, 0 };
					list.add(obj);
				});
			}
		} else {
			users.forEach(p -> {
				Object[] obj = new Object[] { p, 0, 0, 0 };
				list.add(obj);
			});
		}
		return list;
	}

	@Override
	public void asignOnTurnToFail(Long taskId) {
		try {
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			popMessageOnStartOrStop(taskId.intValue(), MessageType.TASK_REVIEW_UPDATE.getType());
			// 生成处理消息
			MessageModel messageModel = new MessageModel(MessageType.TASK_REVIEW_UPDATE.getType(), taskId.intValue());
			messageProducer.pushMessage(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY, messageModel);
			template.convertAndSend(RedisTopics.ASIGN_REVIEWTASK_TOPIC, "ASIGNFAILEDTASK:UPDATE:" + taskId);
		} catch (Exception e) {
			log.error("asignOnTurnToFail error:", e);
		}
	}

	// 转待排查的时候分配待排查任务,总是分配给手中数量最少的用户
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean asignReviewTaskOnTurnToFail(long taskId) {
		try {
			log.info(" 转待排查的时候分配待排查任务,总是分配给手中数量最少的用户 taskId:{} ", taskId);
			BizOutwardTask bizOutwardTask = outwardTaskRepository.findById2(taskId);
			if (bizOutwardTask != null
					&& ((bizOutwardTask.getStatus().intValue() == OutwardTaskStatus.Failure.getStatus().intValue()
							&& bizOutwardTask.getAccountId() != null && bizOutwardTask.getOperator() == null)
							|| bizOutwardTask.getStatus().intValue() == OutwardTaskStatus.ManagerDeal.getStatus()
									.intValue())) {
				log.debug("分配 任务 :{}", ObjectMapperUtils.serialize(bizOutwardTask));
				asignReviewTaskToUserWithLeastTasks(bizOutwardTask);
			}
			return true;
		} catch (Exception e) {
			log.error("  execute AsignFailedTaskServiceImpl.asignReviewTaskOnTurnToFail error :", e);
			return false;
		}
	}

	// 把待排查任务分配给手中任务最少的用户 优先考虑分配给拥有该出款账号ip的接单人
	@Transactional(rollbackFor = Exception.class)
	void asignReviewTaskToUserWithLeastTasks(BizOutwardTask bizOutwardTask) {
		BizHandicap handicap = handicapService.findFromCacheByCode(bizOutwardTask.getHandicap());
		if (handicap == null) {
			log.info(" 盘口为空 :{} ", bizOutwardTask.getHandicap());
		}
		if (bizOutwardTask.getAccountId() != null) {
			// 根据任务的盘口获取所属区域
			int zone = userProfileService.getZoneByHandicap(handicap.getId(), "");
			if (zone == -1) {
				log.info("该任务:{} 所属盘口:{} 没有划分区域:{},无法分配! ", bizOutwardTask.getId(), handicap.getCode(), zone);
				return;
			}
			// 在线人数
			List<String> onlineUsers = getReviewingUserInRedis(zone);
			boolean empty = CollectionUtils.isEmpty(onlineUsers);
			if (empty) {
				log.info("zone :{}, onlineUsers is empty :{},can not asign review task:{}", zone, empty,
						bizOutwardTask.getId());
				return;
			}
			log.debug("在线人数:{}", onlineUsers.toString());
			// 根据账号查询该账号挂载在哪个主机ip下
			Map<String, Set<Integer>> map = getRealIpAndAccountIds(new Integer[] { bizOutwardTask.getAccountId() });
			if (!CollectionUtils.isEmpty(map)) {
				// 该任务的出款账号有ip
				if (map.keySet().size() > 1) {
					log.info("该任务:{}的出款账号:{},有多个挂载Ip:{},无法分配!", bizOutwardTask.getId(), bizOutwardTask.getAccountId(),
							map.keySet().toArray());
					// 分配给任务最少的人手中
					asignToUserWithLeastTasks(onlineUsers, bizOutwardTask);
					return;
				}
				StringRedisTemplate template = redisService.getStringRedisTemplate();
				// 根据在线人数获取正在处理的单子 按处理单子数量升序
				List<Object[]> onlineUsesList = getOperatorAndDealingCount(onlineUsers);
				// 该出款账号下挂的ip
				StringBuilder ip = new StringBuilder();
				// 即将分配该任务的用户Id
				String userId = null;
				// 保存有ip的单子到缓存
				String key = null;
				map.entrySet().stream().forEach(p -> ip.append(p.getKey()));
				// ReviewTask:User:Ip:Account:TaskId:5:192.168.10.12:10000
				Set<String> keys = template
						.keys(RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + "*:" + ip.toString() + ":*");
				if (CollectionUtils.isEmpty(keys)) {
					// 没有按ip分配过
					Object[] obj = new Object[] { bizOutwardTask.getId(), bizOutwardTask.getHandicap(),
							bizOutwardTask.getAccountId() };
					List<Object[]> list = new ArrayList<>();
					list.add(obj);
					// onlineUsesList 按照正在接单的数量升序
					log.debug("分配单子:onlineUsesList {}, 任务id和盘口 {}, 任务出款账号和ip组装 {}", onlineUsesList.toString(),
							list.toString(), map.toString());
					asignReviewTaskByIp(onlineUsesList, list, map);
					return;
				} else {
					// 该ip分配过 ,且一个Ip 只能分给一个人
					// ReviewTask:User:Ip:Account:TaskId:5:192.168.10.12:10000
					keys = template.keys(RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + "*:" + ip.toString() + ":"
							+ bizOutwardTask.getAccountId());
					if (CollectionUtils.isEmpty(keys)) {
						// 该账号下的任务没有分配过
						keys = template
								.keys(RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + "*:" + ip.toString() + ":*");
						// 默认按key升序
						Map<Integer, String> map2 = new TreeMap<>();
						for (Iterator it = keys.iterator(); it.hasNext();) {
							String key1 = it.next().toString();
							String[] keyArray = key1.split(":");
							userId = keyArray[5];
							map2.put(template.boundSetOps(key1).members().size(), userId);
						}
						if (!CollectionUtils.isEmpty(map2)) {
							// 用户id
							userId = new ArrayList<>(map2.values()).get(0);
							key = RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + userId + ":" + ip.toString() + ":"
									+ bizOutwardTask.getAccountId();
						}

					} else {
						key = new ArrayList<>(keys).get(0);
						userId = key.split(":")[5];
					}
					BizTaskReviewEntity bizTaskReviewEntity = new BizTaskReviewEntity();
					bizTaskReviewEntity.setOperator(userId);
					bizTaskReviewEntity.setHandicap(bizOutwardTask.getHandicap());
					bizTaskReviewEntity.setAsignTime(new Date());
					bizTaskReviewEntity.setTaskid(bizOutwardTask.getId().intValue());
					bizTaskReviewEntityRepository.saveAndFlush(bizTaskReviewEntity);
					template.boundSetOps(key).add(bizOutwardTask.getId().toString());
				}
				// 如有之前的锁定则锁定
				if (StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(ip.toString())
						&& checkStrIsIp(ip.toString())) {
					addTaskIdTolockedTasksWithIP(userId, ip.toString(), bizOutwardTask.getId().toString());
				}

			} else {
				// 如果该任务的出款账号没有挂载ip,则分配给该任务所属盘口的区域之下手中排查任务最少的用户
				asignToUserWithLeastTasks(onlineUsers, bizOutwardTask);
			}
		} else {// 该任务没有出款账号
			int zone = userProfileService.getZoneByHandicap(handicap.getId(), "");
			if (zone == -1) {
				log.info("该任务:{} 所属盘口:{} 没有划分区域:{},无法分配! ", bizOutwardTask.getId(), handicap.getCode(), zone);
				return;
			}
			// 在线人数 // 任务所属盘口区域
			List<String> onlineUsers = getReviewingUserInRedis(zone);
			boolean empty = CollectionUtils.isEmpty(onlineUsers);
			if (empty) {
				log.info(" 在线用户为空 :{},任务 task:{}", empty, bizOutwardTask.getId());
				return;
			}
			// 该任务没有出款账号id
			asignToUserWithLeastTasks(onlineUsers, bizOutwardTask);
		}
	}

	// 该ip下的出款账号的任务是否锁定 ReviewTask:LockUserId:Ip:TaskId:
	// 如果有锁定则分配后直接锁定
	private void addTaskIdTolockedTasksWithIP(String userId, String ip, String taskId) {
		if (StringUtils.isBlank(userId) || StringUtils.isBlank(ip) || StringUtils.isBlank(taskId)) {
			return;
		}
		log.debug("该ip下的出款账号的任务是否锁定 如果有锁定则分配后直接锁定 :{},{},{}", userId, ip, taskId);
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		String lockedKey = RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + userId + ":" + ip;
		if (template.hasKey(lockedKey)) {
			template.boundSetOps(lockedKey).add(taskId);
		}
	}

	// 该 出款账号的任务是否锁定 ReviewTask:LockUserId:Ip:TaskId:
	// 如果有锁定则分配后直接锁定
	private void addTaskIdTolockedTasksNoIp(String userId, String accountId, String taskId) {
		if (StringUtils.isBlank(userId) || StringUtils.isBlank(taskId)) {
			return;
		}
		if (StringUtils.isBlank(accountId)) {
			BizOutwardTask task = outwardTaskRepository.findById2(Long.valueOf(taskId));
			if (task == null) {
				return;
			}
			if (task.getAccountId() == null) {
				accountId = userId;
			} else {
				BizAccount account = accountService.getById(task.getAccountId());
				if (account == null) {
					accountId = userId;
				} else {
					accountId = account.getId().toString();
				}
			}
		}
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		String key = RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + userId + ":" + accountId;
		if (template.hasKey(key)) {
			template.boundSetOps(key).add(taskId);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	void asignToUserWithLeastTasks(List<String> onlineUsers, BizOutwardTask bizOutwardTask) {
		// 在线人数 list.size(),list每条记录：Object[0]-userid,Object[1]-countTask数量最少的
		List<Object[]> list = getOperatorAndDealingCount(onlineUsers);
		log.debug("分配 在线人数 ：{} 任务：{},当前在线人数和任务数量:{}", onlineUsers.toString(),
				ObjectMapperUtils.serialize(bizOutwardTask), list.toString());
		if (!CollectionUtils.isEmpty(list)) {
			// countTask数量最少的
			Object[] obj = list.get(list.size() - 1);
			String userId = obj[0].toString();
			BizTaskReviewEntity bizTaskReviewEntity = new BizTaskReviewEntity();
			bizTaskReviewEntity.setAsignTime(new Date());
			bizTaskReviewEntity.setOperator(userId);
			bizTaskReviewEntity.setTaskid(bizOutwardTask.getId().intValue());
			bizTaskReviewEntity.setHandicap(bizOutwardTask.getHandicap());
			bizTaskReviewEntityRepository.saveAndFlush(bizTaskReviewEntity);
			// addTaskIdTolockedTasksNoIp(userId, bizOutwardTask.getAccountId().toString(),
			// bizOutwardTask.getId().toString());
			updateDuplicate();
		}
	}

	/**
	 * 重新均分
	 *
	 * @param userId
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void asignReviewTask(int userId) {
		try {
			log.debug("执行 分配 排查单子:{}", userId);
			executeAsign(userId);
			updateDuplicate();
		} catch (Exception e) {
			log.error("分配 error  :", e);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	void executeAsign(int userId) {
		try {
			// 待分配的单子 Object[0]:taskId,Object[1]:handicap盘口编码 object[2] 账号id
			List<Object[]> list = findFailureStatusTask(userId);
			boolean empty = CollectionUtils.isEmpty(list);
			if (empty) {
				log.info("没有需要分配的任务 :{},userId:{}", empty, userId);
				return;
			}
			log.debug("待排查的单子:{},userId:{}", list.toString(), userId);
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			log.debug("用户:{},区域:{}", userId, zone);
			if (zone == -1) {
				log.info(" 用户:{}没有划分区域!", userId);
				return;
			}
			// 本区域接单人
			List<String> users = getReviewingUserInRedis(zone);
			log.debug("本区域接单人:{}", users.toString());
			// 本区域接单人以及手中接单任务 obj[0] operator obj[1] count 升序
			List<Object[]> onlineUses = getOperatorAndDealingCount(users);
			log.debug("本区域接单人以及手中接单任务:{}", onlineUses.toString());
			// 待分配的排查任务出款账号
			Set<Integer> accountIdsSet = new HashSet<>();
			// 没有出款账号的出款任务
			List<Object[]> noAccountIdTaskList = new ArrayList<>();
			// 有出款账号的出款任务
			List<Object[]> withAccountIdTaskList = new ArrayList<>();
			for (int i = 0, size = list.size(); i < size; i++) {
				Object[] objArr = list.get(i);
				Object obj = objArr[2];
				if (obj != null) {
					// 出款账号id
					accountIdsSet.add(Integer.valueOf(obj.toString()));
					withAccountIdTaskList.add(objArr);
				} else {
					// 第三方出款的时候没有出款账号id
					noAccountIdTaskList.add(objArr);
				}
			}
			List<Integer> accountIdsList = new ArrayList<>(accountIdsSet);
			Integer[] accountIdsArray = new Integer[accountIdsList.size()];
			for (int i = 0, size = accountIdsSet.size(); i < size; i++) {
				accountIdsArray[i] = accountIdsList.get(i);
			}
			// 根据待排查的这些任务出款账号id 查询 物理主机 和 该机器下挂的账号id 按账号数量升序返回
			Map<String, Set<Integer>> ip2AccountMapList = getRealIpAndAccountIds(accountIdsArray);
			boolean emptyMap = CollectionUtils.isEmpty(ip2AccountMapList);
			if (emptyMap && CollectionUtils.isEmpty(noAccountIdTaskList)) {
				log.info("所有待排查任务出款卡没有物理主机挂载,ip2AccountMapList is empty :{}", emptyMap);
				asignReviewTaskWithoutIp(onlineUses, list);
			} else {
				AtomicInteger valuesSize = new AtomicInteger(0);
				boolean isEmpty = CollectionUtils.isEmpty(ip2AccountMapList.values());
				if (isEmpty) {
					log.info("ip2AccountMapList.values() is empty :{}", isEmpty);
					return;
				}
				ip2AccountMapList.values().stream().forEach(p -> valuesSize.getAndAdd(p.size()));
				if (valuesSize.get() < accountIdsArray.length) {
					log.info("只有部分待排查任务出款卡挂载在物理主机, partial accountIds running on PC :{},{},{}",
							valuesSize.get() < accountIdsArray.length, valuesSize.get(), accountIdsArray.length);
					asignPartialReviewTaskByIp(onlineUses, users, withAccountIdTaskList, ip2AccountMapList);
				} else {
					log.info("所有待排查任务出款卡都挂载在物理主机, all accountIds running on PC :{}",
							valuesSize.get() >= accountIdsArray.length);
					asignReviewTaskByIp(onlineUses, withAccountIdTaskList, ip2AccountMapList);
				}
			}
			if (!CollectionUtils.isEmpty(noAccountIdTaskList)) {
				log.debug("分配没有出款账号的任务:{}", noAccountIdTaskList.toString());
				asignReviewTaskWithoutIp(onlineUses, noAccountIdTaskList);
			}
		} catch (Exception e) {
			log.error("AsignFailedTaskServiceImpl.executeAsign error :{}", e.getLocalizedMessage());
		}
	}

	// 全部根据ip来分配待排查任务
	@Transactional(rollbackFor = Exception.class)
	void asignReviewTaskByIp(List<Object[]> onlineUses, List<Object[]> list,
			Map<String, Set<Integer>> ip2AccountMapList) {
		try {
			log.debug("根据IP分配:onlineUses {},list:{},ip2AccountMapList:{}", onlineUses.toString(), list.toString(),
					ip2AccountMapList.toString());
			// k-v : accountId- list:objec[]:taskid,handicap 以list大小升序 任务数量升序
			Map<Integer, List<Object[]>> accountMapReviewTasks = wrapAccountIdMapReviewTask(list);
			if (CollectionUtils.isEmpty(accountMapReviewTasks)) {
				return;
			}
			// k-v:ip-该ip下所有账号的任务数量之和 升序
			Map<String, Integer> ipMapTasksCounts = wrapIpMapTasksCounts(ip2AccountMapList, accountMapReviewTasks);
			log.info("根据IP分配  ip->accountId:{}", ip2AccountMapList);
			// ip总数
			List<String> keys = new LinkedList<>(ipMapTasksCounts.keySet());
			int userSize = onlineUses.size(), ipsSize = keys.size();
			// ip均分
			int avg = ipsSize / userSize;
			// 余数
			int remainder = ipsSize % userSize;
			if (avg > 0) {
				for (int i = 0; i < userSize; i++) {
					// 在线用户 onlineUses 按照最近处理单子升序/或者正在处理的单子数量升序
					Object[] user = onlineUses.get(i);
					int start = ipsSize - avg * i - 1;
					int limit = ipsSize - avg * (i + 1) - 1;
					String key0 = RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + user[0];
					for (int j = start; j > limit; j--) {
						// 任务数量降序获取
						// key ： ip
						String ip = keys.get(j);
						StringBuilder key1 = new StringBuilder(key0).append(":").append(ip);
						// 挂在该ip下的待分配的出款账号
						Set<Integer> accountIds = ip2AccountMapList.get(ip);
						accountIds.stream().forEach(accountId -> {
							// 挂在该ip下的待分配的出款账号获取待分配的排查任务
							StringBuilder key2 = new StringBuilder(key1).append(":").append(accountId);
							log.info("redis 保存 key2: {}", key2);
							List<Object[]> reviewTasks = accountMapReviewTasks.get(accountId);
							if (!CollectionUtils.isEmpty(reviewTasks)) {
								List<BizTaskReviewEntity> saveList = new ArrayList<>();
								reviewTasks.stream().forEach(p -> {
									BizTaskReviewEntity bizTaskReviewEntity = new BizTaskReviewEntity();
									bizTaskReviewEntity.setTaskid(Integer.valueOf(p[0].toString()));
									bizTaskReviewEntity.setOperator(user[0].toString());
									bizTaskReviewEntity.setAsignTime(new Date());
									bizTaskReviewEntity.setHandicap(p[1].toString());
									redisService.getStringRedisTemplate().boundSetOps(key2.toString())
											.add(p[0].toString());
									// 如有之前的锁定则锁定
									if (StringUtils.isNotBlank(user[0].toString()) && StringUtils.isNotBlank(ip)
											&& checkStrIsIp(ip)) {
										log.info("userId--ip--accountId--taskId:{}--{}--{}--{}", user[0], ip, accountId,
												p[0]);
										addTaskIdTolockedTasksWithIP(user[0].toString(), ip, p[0].toString());
									}
									saveList.add(bizTaskReviewEntity);
								});
								bizTaskReviewEntityRepository.save(saveList);

							}
						});
					}
				}
			}
			if (remainder > 0) {
				for (int j = 0; j < remainder; j++) {
					// 在线用户 onlineUses 按照最近处理单子升序/或者正在处理的单子数量升序
					Object[] user;
					if (avg > 0) {
						// 上面已分配过 则倒序获取在线用户
						user = onlineUses.get(userSize - j - 1);
					} else {
						// 没分配过 则顺序获取 在线用户
						user = onlineUses.get(j);
					}
					// ip
					String ip = keys.get(ipsSize - avg * userSize - j - 1);
					StringBuilder key0 = new StringBuilder(RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY)
							.append(user[0]).append(":").append(ip);
					// 挂在该ip下的待分配的出款账号
					Set<Integer> accountIds = ip2AccountMapList.get(ip);
					accountIds.stream().forEach(accountId -> {
						// 根据挂在该ip下的待分配的出款账号获取待分配的排查任务
						List<Object[]> reviewTasks = accountMapReviewTasks.get(accountId);
						if (!CollectionUtils.isEmpty(reviewTasks)) {
							List<BizTaskReviewEntity> saveList = new ArrayList<>();
							reviewTasks.stream().forEach(task -> {
								StringBuilder redisKey = new StringBuilder(key0).append(":").append(accountId);
								BizTaskReviewEntity bizTaskReviewEntity = new BizTaskReviewEntity();
								bizTaskReviewEntity.setTaskid(Integer.valueOf(task[0].toString()));
								bizTaskReviewEntity.setOperator(user[0].toString());
								bizTaskReviewEntity.setAsignTime(new Date());
								bizTaskReviewEntity.setHandicap(task[1].toString());
								saveList.add(bizTaskReviewEntity);
								redisService.getStringRedisTemplate().boundSetOps(redisKey.toString())
										.add(task[0].toString());
								// 如有之前锁定则锁定
								if (StringUtils.isNotBlank(user[0].toString()) && StringUtils.isNotBlank(ip)
										&& checkStrIsIp(ip)) {
									addTaskIdTolockedTasksWithIP(user[0].toString(), ip, task[0].toString());
								}
							});
							bizTaskReviewEntityRepository.save(saveList);
						}
					});
				}
			}
		} catch (Exception e) {
			log.error("分配 error :", e);
		}
	}

	// 部分根据ip分配待排查任务
	@Transactional(rollbackFor = Exception.class)
	void asignPartialReviewTaskByIp(List<Object[]> users, List<String> onlieUsers, List<Object[]> tasks,
			Map<String, Set<Integer>> ip2AccountMapList) {
		try {
			log.debug("根据ip 分配任务：users {},onlieUsers {},tasks {} ,ip2AccountMapList:{}", users.toString(),
					onlieUsers.toString(), tasks.toString(), ip2AccountMapList.toString());
			Set<String> tasksAccuntIds = new HashSet<>();
			tasks.stream().forEach(p -> tasksAccuntIds.add(p[2].toString()));
			List<String> list1 = new ArrayList<>(tasksAccuntIds), list2 = new ArrayList<>();
			Set<Integer> set = new HashSet<>();
			ip2AccountMapList.values().stream().forEach(p -> set.addAll(p));
			set.stream().forEach(p -> list2.add(p.toString()));
			// 所有任务的出款账号 与这些账号中有挂载ip的差集
			List<String> diffrentList = CommonUtils.getDiffrentList(list1, list2);
			List<Object[]> asignWithoutIpTasksList = new ArrayList<>(), asignByIpTasksList = new ArrayList<>();
			for (int i = 0, size = tasks.size(); i < size; i++) {
				Object[] obj = tasks.get(i);
				if (diffrentList.contains(obj[2].toString())) {
					// 不根据ip分配的任务
					asignWithoutIpTasksList.add(obj);
				} else {
					// 根据ip分配的任务
					asignByIpTasksList.add(obj);
				}
			}
			if (!CollectionUtils.isEmpty(asignByIpTasksList)) {
				asignReviewTaskByIp(users, asignByIpTasksList, ip2AccountMapList);
			}
			asignRemainingTasks(users, asignWithoutIpTasksList);
		} catch (Exception e) {
			log.error("asignPartialReviewTaskByIp error:", e);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	void asignRemainingTasks(List<Object[]> users, List<Object[]> tasks) {
		try {
			log.debug("分配剩余的任务 tasks:{},users:{}", tasks.toString(), users.toString());
			int userSize = users.size(), tasksSize = tasks.size();
			int avg = tasksSize / userSize;
			int remainder = tasksSize % userSize;
			if (avg > 0) {
				asignInAverage(users, avg, tasks);
			}
			if (remainder > 0) {
				for (int i = 0; i < remainder; i++) {
					Object[] user = users.get(userSize - 1 - i);
					Object[] task = tasks.get(i + avg * userSize);
					BizTaskReviewEntity bizTaskReviewEntity = new BizTaskReviewEntity();
					bizTaskReviewEntity.setTaskid(Integer.valueOf(task[0].toString()));
					bizTaskReviewEntity.setOperator(user[0].toString());
					bizTaskReviewEntity.setAsignTime(new Date());
					bizTaskReviewEntity.setHandicap(task[1].toString());
					bizTaskReviewEntityRepository.saveAndFlush(bizTaskReviewEntity);
					// addTaskIdTolockedTasksNoIp(user[0].toString(), null, task[0].toString());
				}
			}
		} catch (Exception e) {
			log.error("asignPartialReviewTaskByIp error:", e);
		}
	}

	// 不根据ip分配待排查任务 均分
	@Transactional(rollbackFor = Exception.class)
	void asignReviewTaskWithoutIp(List<Object[]> users, List<Object[]> tasks) {
		log.debug("分配 不根据ip:用户:{},任务:{}", users.toString(), tasks.toString());
		int userSize = users.size(), taskSize = tasks.size();
		int avg = taskSize / userSize;
		int remainder = taskSize % userSize;
		if (avg > 0) {
			asignInAverage(users, avg, tasks);
		}
		if (remainder > 0) {
			for (int i = 0; i < remainder; i++) {
				Object[] user = users.get(i);
				Object[] task = tasks.get(i + avg * userSize);
				BizTaskReviewEntity bizTaskReviewEntity = new BizTaskReviewEntity();
				bizTaskReviewEntity.setTaskid(Integer.valueOf(task[0].toString()));
				bizTaskReviewEntity.setOperator(user[0].toString());
				bizTaskReviewEntity.setAsignTime(new Date());
				bizTaskReviewEntity.setHandicap(task[1].toString());
				bizTaskReviewEntityRepository.saveAndFlush(bizTaskReviewEntity);
				// addTaskIdTolockedTasksNoIp(user[0].toString(), null, task[0].toString());
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	void asignInAverage(List<Object[]> users, int avg, List<Object[]> tasks) {
		log.debug("均分:users {},tasks {},avg {}", users.toString(), tasks.toString(), avg);
		int userSize = users.size();
		for (int i = 0; i < userSize; i++) {
			List<BizTaskReviewEntity> saveList = new ArrayList<>();
			Object[] user = users.get(i);
			for (int j = i * avg; j < (i + 1) * avg; j++) {
				Object[] task = tasks.get(j);
				BizTaskReviewEntity bizTaskReviewEntity = new BizTaskReviewEntity();
				bizTaskReviewEntity.setTaskid(Integer.valueOf(task[0].toString()));
				bizTaskReviewEntity.setOperator(user[0].toString());
				bizTaskReviewEntity.setAsignTime(new Date());
				bizTaskReviewEntity.setHandicap(task[1].toString());
				saveList.add(bizTaskReviewEntity);
				// addTaskIdTolockedTasksNoIp(user[0].toString(), null, task[0].toString());
			}
			if (saveList.size() > avg) {
				log.info("AsignFailedTaskServiceImpl.asignInAverage saveList is larger than avg:{}",
						saveList.size() > avg);
				return;
			}
			bizTaskReviewEntityRepository.save(saveList);
			saveList.clear();
		}
	}

	// 根据taskid 用户 operator 删除分配的排查单子
	@Transactional(rollbackFor = Exception.class)
	void deleteByTaskIdAndOperator(List<String> list, int operator) {
		if (!CollectionUtils.isEmpty(list)) {
			String sql = "delete from biz_task_review  where operator= " + operator;
			// 删除所有已分配但未锁定排查的
			int size = list.size();
			if (size == 1) {
				sql += " and taskid = " + list.get(0);
			} else {
				sql += " and taskid  in ( ";
				for (int i = 0; i < size; i++) {
					if (i < size - 1) {
						sql += list.get(i) + ",";
					} else {
						sql += list.get(i) + ")";
					}
				}
			}
			sql += " and  finish_time is null ";
			int delete = entityManager.createNativeQuery(sql).executeUpdate();
			log.info(" AsignFailedTaskServiceImpl.deleteByTaskIdAndOperator result :{}", delete);
		}
	}

	/**
	 * 删除--接单人区域下的未锁定的所有单子
	 * 
	 * @param userId
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteIpAccountTasksOnStart(int userId) {
		try {
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			// 本区域在线接单人
			List<String> users = getReviewingUserInRedis(zone);
			log.debug("接单删除用户未处理的单子 : userId:{},zone:{},在线人数:{}", userId, zone, users.toString());
			if (CollectionUtils.isEmpty(users)) {
				return;
			}
			// ReviewTask:LockUserId:Ip:TaskId:5:192.168.100.1 有ip的锁定记录key
			// ReviewTask:LockUserId:Ip:TaskId:5:10001 没有ip的锁定记录key
			// 本区域所有用户锁定的key
			Set<String> allLockedKeys = new HashSet<>();
			users.stream().forEach(p -> {
				Set<String> lockeKeys = template.keys(RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + p + ":*");
				log.debug("本区域下 所有锁定的用户:{}", lockeKeys.toString());
				if (!CollectionUtils.isEmpty(lockeKeys)) {
					allLockedKeys.addAll(lockeKeys);
				}
			});
			Set<String> lockedTaskIds = new HashSet<>();
			if (!CollectionUtils.isEmpty(allLockedKeys)) {
				for (Iterator it = allLockedKeys.iterator(); it.hasNext();) {
					String key = it.next().toString();
					// 同区域下的人锁的单子 包含了ip和非ip的单子
					lockedTaskIds.addAll(template.boundSetOps(key).members());
				}
			}
			log.debug("本区域下 所有锁定的用户:{}", lockedTaskIds.toString());
			// 删除未锁定的
			deleteByTaskIdsAndUsers(new ArrayList<>(lockedTaskIds), users);
		} catch (Exception e) {
			log.error("deleteIpAccountTasksOnStart error occured :", e);
		}
	}

	/**
	 * * 删除本区域下所有用户已分配未锁定的单子
	 * 
	 * @param lockedTaskIds
	 * @param users
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByTaskIdsAndUsers(List<String> lockedTaskIds, List<String> users) {
		if (CollectionUtils.isEmpty(users)) {
			return;
		}
		try {
			String sql = "select id from biz_task_review where ";
			int userSize = users.size();
			if (userSize == 1) {
				sql += "  operator =" + users.get(0);
			} else {
				sql += "  operator in (";
				for (int i = 0; i < userSize; i++) {
					if (i < userSize - 1) {
						sql += users.get(i) + ",";
					} else {
						sql += users.get(i) + ")";
					}
				}
			}
			if (!CollectionUtils.isEmpty(lockedTaskIds)) {
				int size = lockedTaskIds.size();
				if (size == 1) {
					sql += " and  taskid !=" + lockedTaskIds.get(0);
				} else {
					sql += " and  taskid not in (";
					for (int i = 0; i < size; i++) {
						if (i < size - 1) {
							sql += lockedTaskIds.get(i) + ",";
						} else {
							sql += lockedTaskIds.get(i) + ")";
						}
					}
				}
			}
			sql += " and  asign_time is not null and finish_time is null  ";
			List<Integer> ids = entityManager.createNativeQuery(sql).getResultList();
			if (CollectionUtils.isEmpty(ids)) {
				// 如果为空则不会锁
				return;
			}
			sql = " delete from biz_task_review where ";
			if (ids.size() == 1) {
				sql += " id =" + ids.get(0);
			} else {
				sql += " id in (";
				for (int i = 0, size = ids.size(); i < size; i++) {
					if (i < size - 1) {
						sql += ids.get(i) + ",";
					} else {
						sql += ids.get(i) + ")";
					}
				}
			}
			int del = entityManager.createNativeQuery(sql).executeUpdate();
			log.info(" 删除本区域下所有用户已分配未锁定的单子 sql :{}, result:{}", sql, del);
		} catch (Exception e) {
			log.error(" 删除本区域下所有用户已分配未锁定的单子 error:", e);
		}
	}

	// 获取锁定的单子 对应的出款账号对应的ip
	private List<String> getIpsInRedisByLockTaskIds() {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		Set<String> lockedTaskIdsKeys = template.keys(RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + "*:*");
		List<String> ipsList = new ArrayList<>();
		if (!CollectionUtils.isEmpty(lockedTaskIdsKeys)) {
			for (Iterator it = lockedTaskIdsKeys.iterator(); it.hasNext();) {
				// ReviewTask:UserId:Ip:TaskId:5:192.168.10.1
				// ReviewTask:UserId:Ip:TaskId:5:10001
				String key = it.next().toString();// ...userId:ip-->taskId SET
				String[] keyArray = key.split(":");
				if (checkStrIsIp(keyArray[5])) {
					ipsList.add(keyArray[5]);
				}
			}
		}
		return ipsList;
	}

	/**
	 * 删除分配的排查单子
	 * 
	 * @param list
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	int deleteReviewTasks(List<Object[]> list) {
		log.debug("删除任务:{}", list.toString());
		try {
			if (!CollectionUtils.isEmpty(list)) {
				String sql = "delete from biz_task_review  where ";
				int size = list.size();
				if (size == 1) {
					sql += " id = " + list.get(0)[0];
				} else {
					sql += " id in ( ";
					for (int i = 0; i < size; i++) {
						if (i < size - 1) {
							sql += list.get(i)[0] + ",";
						} else {
							sql += list.get(i)[0] + ")";
						}
					}
				}
				int delete = entityManager.createNativeQuery(sql).executeUpdate();
				log.info(" AsignFailedTaskServiceImpl.deleteReviewTasks result :{}", delete);
				return delete;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	private String wrapSql(String sql, List<String> list) {
		if (!CollectionUtils.isEmpty(list)) {
			int size = list.size();
			if (size == 1) {
				sql += " and taskid !=" + list.get(0);
			} else {
				sql += " and taskid not in(";
				for (int i = 0; i < size; i++) {
					if (i < size - 1) {
						sql += list.get(i) + ",";
					} else {
						sql += list.get(i) + ")";
					}
				}
			}
		}
		return sql;
	}

	// 查询所有已分配的任务
	private List<Object[]> findAsignedReviewTasks() {
		String sql = "select id, taskid from biz_task_review where finish_time is null ";
		List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
		return list;
	}

	// socket 打开-点击接单的时候分配
	@Transactional(rollbackFor = Exception.class)
	void asignReviewTaskOnStart(int userId) {
		// zone 默认值 -1
		int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
		log.debug("开始接单 获取用户区域: userId:{},zone:{}", userId, zone);
		if (zone == -1) {
			log.info("开始接单--用户:{}没有划分区域!", zone);
			return;
		}
		// 该区域下的在线接单的排查人员
		List<String> users = getReviewingUserInRedis(zone);
		log.info("区域{},在线接单的排查人员人数:{}", zone, users.toString());
		if (!CollectionUtils.isEmpty(users)) {
			log.info("区域{},在线接单的排查人员人数:{}", zone, users.size());
			if (users.size() > 1) {
				asignReviewTask(userId);
			} else {
				// 第一个接单的时候
				log.debug("第一个人接单 :userId  {}", userId);
				asignFailureTasksFirstTime(userId, zone);
			}
		}
	}

	// 查询待排查任务 userId不为空 获取该用户区域下的所有盘口的排查任务
	// 返回: List<Object[]> obj[0] taskId obj[1] handicapId obj[2] accountId
	@Override
	public List<Object[]> findFailureStatusTask(Integer userId) {
		if (userId == null) {
			log.info("AsignFailedTaskServiceImpl.findFailureStatusTask userId:{}", userId);
			return null;
		}
		int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
		if (zone == -1) {
			log.info("用户:{}没有划分区域!", userId);
			return null;
		}
		// handicapService.findHandicapCodesByZone
		List<Integer> handicapCodesForUser = userProfileService.getHandicapCodeByZone(zone);
		if (CollectionUtils.isEmpty(handicapCodesForUser)) {
			log.info("AsignFailedTaskServiceImpl.findFailureStatusTask 无数据权限:{}",
					CollectionUtils.isEmpty(handicapCodesForUser));
			return null;
		}
		// 不包含人工出款单子 包含主管处理
		String sql = "SELECT DISTINCT t.id ,t.handicap,t.account_id FROM biz_outward_task t WHERE  t.status=6 and t.account_id is not null and t.operator is null  ";
		sql = commonConcatSql(sql, handicapCodesForUser);
		sql += "  UNION ALL ";
		sql += "  SELECT DISTINCT t.id ,t.handicap,t.account_id FROM biz_outward_task t WHERE status=2 ";
		sql = commonConcatSql(sql, handicapCodesForUser);
		List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
		return list;
	}

	private String commonConcatSql(String sql, List<Integer> handicapCodesForUser) {
		sql += "and t.id not in  (SELECT taskid  FROM  biz_task_review  where  asign_time is not null and finish_time is null ) ";
		if (!CollectionUtils.isEmpty(handicapCodesForUser)) {
			int size = handicapCodesForUser.size();
			if (size == 1) {
				sql += " and t.handicap= \'" + handicapCodesForUser.get(0) + "\'";
			} else {
				sql += " and t.handicap in (";
				for (int i = 0; i < size; i++) {
					if (i < size - 1) {
						sql += "\"" + handicapCodesForUser.get(i) + "\",";
					} else {
						sql += "\"" + handicapCodesForUser.get(i) + "\")";
					}
				}
			}
		}
		return sql;
	}

	// 第一次接单,第一个人接单的时候
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void asignFailureTasksFirstTime(int userId, int zone) {
		try {
			List<String> users = getReviewingUserInRedis(zone);
			log.debug("第一个接单 userId:{}, zone:{}", userId, zone);
			if (CollectionUtils.isEmpty(users)) {
				return;
			}
			log.debug("在线接单用户：{}", users.toString());
			List<Object[]> list2 = getAssignedReviwTaskByUserId(users);
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			if (!CollectionUtils.isEmpty(list2)) {
				// 接单的时候,防止不正当操作而不结束接单导致还有单子已分配,先删除这些单子
				log.debug("接单的时候,防止不正当操作而不结束接单导致还有单子已分配,先删除这些单子 ：{}", list2.toString());
				deleteReviewTasks(list2);
			}
			// 第一人接单 所以删除redis缓存中所有的缓存的任务
			// ReviewTask:LockUserId:Ip:TaskId
			Set<String> allKeys = new HashSet<>();
			users.stream().forEach(p -> {
				Set<String> keys = template.keys(RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + p + ":*");
				if (!CollectionUtils.isEmpty(keys)) {
					allKeys.addAll(keys);
				}
			});
			log.debug("第一人接单 所以删除redis缓存中所有的缓存的任务 :{}", allKeys.toString());
			if (!CollectionUtils.isEmpty(allKeys)) {
				for (Iterator it = allKeys.iterator(); it.hasNext();) {
					String key = it.next().toString();
					template.delete(key);
				}
			}
			// ReviewTask:User:Ip:Account:TaskId:
			Set<String> allKeys2 = new HashSet<>();
			users.stream().forEach(p -> {
				Set<String> keys2 = template.keys(RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + p + ":*");
				if (!CollectionUtils.isEmpty(keys2)) {
					allKeys2.addAll(keys2);
				}
			});

			if (!CollectionUtils.isEmpty(allKeys2)) {
				log.debug("第一人接单 所以删除redis缓存中所有的缓存的任务2 ：{}", allKeys2.toString());
				for (Iterator it = allKeys2.iterator(); it.hasNext();) {
					String key2 = it.next().toString();
					template.delete(key2);
				}
			}
			log.debug("执行 分配:{}", userId);
			executeAsign(userId);
		} catch (Exception e) {
			log.error("AsignFailedTaskServiceImpl.asignFailureTasksFirstTime execute error :", e);
		}
	}

	@Override
	public void stopReviewTask(int userId) {
		log.debug("问题排查 结束接单 参数:{}", userId);
		try {
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			log.debug("问题排查 结束接单 通过用户id:{},获取区域结果:{}", userId, zone);
			if (zone == -1) {
				log.info("用户:{}没有分配区域!", userId);
				return;
			}
			popMessageOnStartOrStop(userId, MessageType.TASK_REVIEW_STOP.getType());
			// 生成处理消息
			MessageModel messageModel = new MessageModel(MessageType.TASK_REVIEW_STOP.getType(), userId);
			messageProducer.pushMessage(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY, messageModel);
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			template.convertAndSend(RedisTopics.ASIGN_REVIEWTASK_TOPIC, "ASIGNFAILEDTASK:STOP:" + userId);
		} catch (Exception e) {
			log.error(" execute stopReviewTask error:", e);
		}
	}

	/**
	 * 停止接单
	 * 
	 * @param userId
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean dealOnstopReviewTask(int userId) {
		try {
			log.info("停止接单: host :{},stopReviewTask userId:{}", CommonUtils.getInternalIp(), userId);
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			removeUserAndTasksInRedis(userId, 3);
			// 在线人数
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			List<String> onlineUsers = getReviewingUserInRedis(zone);
			if (CollectionUtils.isEmpty(onlineUsers)) {
				log.info("停止接单: onlineUsers is empty ! ");
				return true;
			}
			log.info("停止接单: onlineUsers :{}, ", onlineUsers.toString());
			asignReviewTask(userId);
			template.convertAndSend(RedisTopics.ASIGN_REVIEWTASK_TOPIC, "FRESH_PAGE");
			return true;
		} catch (Exception e) {
			log.error(" 停止接单 error:", e);
			return false;
		}
	}

	@Override
	public boolean dealOnpauseReviewTask(int userId) {
		try {
			log.debug("暂停接单:{}", userId);
			removeUserAndTasksInRedis(userId, 2);
			// 在线人数 list.size(),list中的数据Object[0]：userid,Object[1]：countTask 降序
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			log.debug("暂停接单:userId:{},区域:{}", userId, zone);
			if (zone == -1) {
				log.info("用户:{},没有分配区域:{}", userId, zone);
				return true;
			}
			// 在线接单的排查人员
			List<String> users = getReviewingUserInRedis(zone);
			if (!CollectionUtils.isEmpty(users)) {
				log.debug("在线接单人:{}", users.toString());
				asignReviewTask(userId);
			}
			return true;
		} catch (Exception e) {
			log.error("问题排查暂停接单 处理 error:", e);
			return false;
		}
	}

	// 暂停接单
	@Override
	@Transactional
	public void pauseReviewTask(int userId) {
		log.debug("问题排查暂停接,参数:{}", userId);
		try {
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			log.debug("问题排查暂停接,根据用户id 获取区域结果:{}", zone);
			if (zone == -1) {
				log.info("用户:{}没有分配区域!", userId);
				return;
			}
			popMessageOnStartOrStop(userId, MessageType.TASK_REVIEW_PAUSE.getType());
			// 生成处理消息
			MessageModel messageModel = new MessageModel(MessageType.TASK_REVIEW_PAUSE.getType(), userId);
			messageProducer.pushMessage(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY, messageModel);
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			template.convertAndSend(RedisTopics.ASIGN_REVIEWTASK_TOPIC, "ASIGNFAILEDTASK:PAUSE:" + userId);
		} catch (Exception e) {
			log.error(" 问题排查暂停接单异常:", e);
		}
	}

	// 处理排查
	@Override
	@Transactional
	public int updateReviewTask(int taskId, String remark, int userId) {
		try {
			List<Object[]> obj = findOneReviewTask(taskId, userId);
			if (CollectionUtils.isEmpty(obj)) {
				// 在出款汇总操作的时候 查不到
				log.info("非本人操作调用 updateReviewTask reviewTask taskId:{},userId:{}", taskId, userId);
				List<BizTaskReviewEntity> list = bizTaskReviewEntityRepository
						.getByTaskidAndAndFinishTimeIsNull(taskId);
				if (!CollectionUtils.isEmpty(list)) {
					log.info("非本人操作调用 getByTaskidAndAndFinishTimeIsNull  reviewTask  size:{},taskId:{} ", list.size(),
							taskId);
					bizTaskReviewEntityRepository.delete(list);
				}
				return 0;
			}
			// ReviewTask:LockUserId:Ip:TaskId:
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			Set<String> lockKeys = template.keys(RedisKeys.TASK_REVIEW_USERLOCK_TASK_KEY + userId + ":*");
			if (!CollectionUtils.isEmpty(lockKeys)) {
				for (Iterator it = lockKeys.iterator(); it.hasNext();) {
					String key = it.next().toString();
					String member = String.valueOf(taskId);
					boolean isMember = template.boundSetOps(key).isMember(member);
					if (isMember) {
						template.boundSetOps(key).remove(String.valueOf(member));
					}
				}
			}
			Set<String> keys = redisService.getStringRedisTemplate()
					.keys(RedisKeys.TASK_REVIEW_USER_IP_ACCOUNT_TASKS_KEY + userId + ":*");
			if (!CollectionUtils.isEmpty(keys)) {
				// ...userId:ip:accountId
				for (Iterator it = keys.iterator(); it.hasNext();) {
					String key = it.next().toString();// ...userId:ip:accountId
					// ..:ip:accountId -->taskIds set
					boolean isMember = template.boundSetOps(key).isMember(String.valueOf(taskId));
					if (isMember) {
						template.boundSetOps(key).remove(String.valueOf(taskId));
					}
				}
			}
			int update = bizTaskReviewEntityRepository.updateByTaskId(taskId, remark, userId);
			return update;
		} catch (Exception e) {
			log.error("更新任务排查表异常 :", e);
			return 0;
		}
	}

	private List<Object[]> findOneReviewTask(int taskId, int userId) {
		String sql = " select * from biz_task_review where taskid=" + taskId + " and operator=" + userId
				+ " and finish_time is null ";
		List<Object[]> obj = entityManager.createNativeQuery(sql).getResultList();
		return obj;
	}

	// 添加备注
	@Override
	@Transactional
	public int updateRemark(int userId, int taskId, String remark) {
		int update = bizTaskReviewEntityRepository.updateByRemark(userId, taskId, remark);
		return update;
	}

	// 查询重复记录
	@Override
	public List<Object[]> findDuplicaRecords() {
		String sql = "SELECT taskid, operator  FROM fundsTransfer.biz_task_review WHERE \n"
				+ "EXISTS (SELECT taskid  FROM fundsTransfer.biz_task_review where asign_time is not null and finish_time is NULL and operator is not null  group by taskid HAVING(COUNT(taskid)>1))\n"
				+ "ORDER BY  asign_time DESC ";
		List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
		return list;
	}

	// 处理重复分配 Object[0] :taskId ,Object[1]:operator
	@Override
	@Transactional
	public void updateDuplicate() {
		List<Object[]> list = findDuplicaRecords();
		Map<Integer, List<Integer>> map = null;// key:taskId value:用户id
		List<Integer> operatorList;// 用户id
		if (!CollectionUtils.isEmpty(list)) {
			log.info("updateDuplicate execute findDuplicaRecords is not empty :{}", !CollectionUtils.isEmpty(list));
			return;
		}
		if (!CollectionUtils.isEmpty(list)) {
			map = new LinkedHashMap<>();
			for (int i = 0, llen = list.size(); i < llen; i++) {
				Object[] obj = list.get(i);
				if (map.size() > 0 && map.containsKey(obj[0])) {
					operatorList = map.get(obj[0]);
					operatorList.add(Integer.valueOf(obj[1].toString()));
				} else {
					operatorList = new LinkedList<>();
					operatorList.add(Integer.valueOf(obj[1].toString()));
					map.put(Integer.valueOf(obj[0].toString()), operatorList);

				}
			}
		}
		if (map != null && map.size() > 0) {
			Iterator iterator = map.keySet().iterator();
			while (iterator.hasNext()) {
				Integer key = Integer.valueOf(iterator.next().toString());
				List<Integer> list1 = map.get(key);
				if (list1.size() == 2) {
					bizTaskReviewEntityRepository.updateDuplicate(key, list1.get(0));
				} else {
					for (int llen = list1.size(), i = llen - 1; i > 0; i--) {
						bizTaskReviewEntityRepository.updateDuplicate(key, list1.get(i));
					}
				}
			}
		}
	}

	/**
	 * 用户接单或者结束接单的时候 删除事件队列里多余事件
	 *
	 * @param userId
	 *            the operator id who execute start or stop action
	 * @param type
	 *            the action type including : start stop {@link MessageType}
	 */
	@Override
	public void popMessageOnStartOrStop(int userId, int type) {
		log.debug("问题排查接单/结束 处理遗留消息队列 参数,userid:{},type:{}", userId, type);
		try {
			StringRedisTemplate template = redisService.getStringRedisTemplate();
			boolean exist = template.hasKey(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY);
			log.debug("是否存在ReviewTask:MessageKey :{}", exist);
			if (exist) {
				List<String> msg = template.opsForList().range(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY, 0, -1);
				log.debug("动作消息:{}", msg);
				if (CollectionUtils.isEmpty(msg)) {
					return;
				}
				String mess = "{\"operatorId\":" + userId + ",\"type\":" + type + "}";
				for (Iterator it = msg.iterator(); it.hasNext();) {
					String mes = it.next().toString();
					log.debug("消息：{}", mes);
					if (mes.equals(mess)) {
						log.debug("删除消息:{}", mes);
						template.opsForList().remove(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_KEY, 1, mes);
					}
				}
			}
			boolean exist2 = template.hasKey(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY);
			log.debug("是否存在 ReviewTask:MessageBackUpKey ：{}", exist2);
			if (exist2) {
				List<String> msg = template.opsForList().range(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY, 0, -1);
				log.debug("备份消息:{}", msg);
				if (CollectionUtils.isEmpty(msg)) {
					return;
				}
				String mess = "{\"operatorId\":" + userId + ",\"type\":" + type + "}";
				for (Iterator it = msg.iterator(); it.hasNext();) {
					String mes = it.next().toString();
					log.debug("遍历的备份消息:{}", mes);
					if (mes.equals(mess)) {
						log.debug("删除 遍历的备份消息:{}", mes);
						template.opsForList().remove(RedisKeys.TASK_REVIEW_MESSAGEQUEUE_BACKUP_KEY, 1, mes);
					}
				}
			}
		} catch (Exception e) {
			log.error("问题排查接单/结束 处理遗留消息队列异常:", e);
		}
	}

	// 获取用户已分配的排查任务
	@Override
	public List<Object[]> getAssignedReviwTaskByUserId(List<String> users) {
		if (CollectionUtils.isEmpty(users)) {
			return null;
		}
		try {
			String sql = " select id ,taskid from biz_task_review where asign_time is not null and finish_time is null and operator ";
			if (users.size() == 1) {
				sql += " = " + users.get(0);
			} else {
				sql += "in (";
				for (int i = 0, size = users.size(); i < size; i++) {
					if (i < size - 1) {
						sql += users.get(i) + ",";
					} else {
						sql += users.get(i) + ")";
					}
				}
			}
			List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
			return list;
		} catch (Exception e) {
			log.error("获取用户已分配的排查任务 异常：", e);
			return Lists.newArrayList();
		}
	}

	private String wrapSql(String sql, TroubleShootDTO troubleShootDTO, Integer[] operator, Integer fromAccount[],
			List<Integer> shooterList, String[] handicapCodes, List<String> lockedTaskIds) {
		sql += "  from biz_outward_task t join biz_task_review r on t.id=r.taskid and t.handicap=r.handicap where 1=1  ";
		if ("1".equals(troubleShootDTO.getQueryType()) || "3".equals(troubleShootDTO.getQueryType())) {
			// 在排查
			sql += " and (t.status=6 or t.status=2 ) and  r.asign_time is not null  and r.finish_time is null ";
		}
		if ("2".equals(troubleShootDTO.getQueryType())) {
			// 已排查 t.status !=6
			sql += " and r.finish_time is not null ";
		}
		if (!CollectionUtils.isEmpty(shooterList)) {
			sql += "  and r.operator";
			Integer[] shooter = new Integer[shooterList.size()];
			// 排查人
			sql = concatSql(sql, shooterList.toArray(shooter));
		}
		if (null != handicapCodes && handicapCodes.length > 0) {
			// 盘口编码
			sql += "  and t.handicap  ";
			sql = concatSql(sql, handicapCodes);
		}
		if (StringUtils.isNotBlank(troubleShootDTO.getLevel())) {
			// 层级名称
			sql += "  and t.level = \"" + troubleShootDTO.getLevel() + "\"";
		}
		if (StringUtils.isNotBlank(troubleShootDTO.getOrderNo())) {
			sql += " and t.order_no = \"" + StringUtils.trim(troubleShootDTO.getOrderNo()) + "\"";
		}
		if (StringUtils.isNotBlank(troubleShootDTO.getMember())) {
			sql += "  and t.member =\"" + StringUtils.trim(troubleShootDTO.getMember()) + "\"";
		}
		if (StringUtils.isNotBlank(troubleShootDTO.getAmountStart())) {
			sql += " and  t.amount >=" + StringUtils.trim(troubleShootDTO.getAmountStart());
		}
		if (StringUtils.isNotBlank(troubleShootDTO.getAmountEnd())) {
			sql += "  and t.amount <=" + StringUtils.trim(troubleShootDTO.getAmountEnd());
		}
		if ("1".equals(troubleShootDTO.getQueryType())) {
			if (StringUtils.isNotBlank(troubleShootDTO.getStartTime())) {
				sql += " and  r.asign_time >=\"" + troubleShootDTO.getStartTime() + "\"";
			}
			if (StringUtils.isNotBlank(troubleShootDTO.getEndTime())) {
				sql += "  and r.asign_time <=\"" + troubleShootDTO.getEndTime() + "\"";
			}
		} else {
			if (StringUtils.isNotBlank(troubleShootDTO.getStartTime())) {
				sql += " and  r.finish_time >=\"" + troubleShootDTO.getStartTime() + "\"";
			}
			if (StringUtils.isNotBlank(troubleShootDTO.getEndTime())) {
				sql += "  and r.finish_time <=\"" + troubleShootDTO.getEndTime() + "\"";
			}
		}
		if (fromAccount != null && fromAccount.length > 0) {
			sql += "  and t.account_id ";
			// 出款账号
			sql = concatSql(sql, fromAccount);
		}
		if (operator != null && operator.length > 0) {
			sql += "  and t.operator ";
			// 出款人
			sql = concatSql(sql, operator);
		}

		if (StringUtils.isNotBlank(troubleShootDTO.getType())) {
			if ("0".equals(troubleShootDTO.getType())) {
				sql += "  and t.operator is not null and t.account_id is not null ";
			}
			if ("1".equals(troubleShootDTO.getType())) {
				sql += "  and t.operator is  null and t.account_id is not null ";
			}
		}
		if ("3".equals(troubleShootDTO.getQueryType())) {
			if (!CollectionUtils.isEmpty(lockedTaskIds)) {
				if (lockedTaskIds.size() == 1) {
					sql += "  and t.id = " + lockedTaskIds.get(0);
				} else {
					sql += "  and t.id in (";
					for (int i = 0, lockedTaskIdslen = lockedTaskIds.size(); i < lockedTaskIdslen; i++) {
						if (i < lockedTaskIdslen - 1) {
							sql += lockedTaskIds.get(i) + ",";
						} else {
							sql += lockedTaskIds.get(i) + ")";
						}
					}
				}

			}
		}
		if ("1".equals(troubleShootDTO.getQueryType())) {
			if (!CollectionUtils.isEmpty(lockedTaskIds)) {
				if (lockedTaskIds.size() == 1) {
					sql += "  and t.id != " + lockedTaskIds.get(0);
				} else {
					sql += "  and t.id  not in (";
					for (int i = 0, lockedTaskIdslen = lockedTaskIds.size(); i < lockedTaskIdslen; i++) {
						if (i < lockedTaskIdslen - 1) {
							sql += lockedTaskIds.get(i) + ",";
						} else {
							sql += lockedTaskIds.get(i) + ")";
						}
					}
				}

			}
		}
		if ("2".equals(troubleShootDTO.getQueryType())) {
			sql += "  order by r.finish_time desc";
		} else {
			sql += "  order by t.asign_time ";
		}
		return sql;
	}

	private String concatSql(String sql, Object[] param) {
		if (param.length == 1) {
			sql += " = \"" + param[0] + "\"";
		} else {
			sql += " in ( ";
			for (int i = 0; i < param.length; i++) {
				if (i < param.length - 1) {
					sql += "\"" + param[i] + "\",";
				} else {
					sql += "\"" + param[i] + "\")";
				}
			}
		}
		return sql;
	}

	private static final Integer[] getDiffArray(Integer[] old) {
		Integer[] diff = new Integer[0], produce = new Integer[0];
		int sum = 0, avg = 0, remainder = 0;
		for (int i = 0; i < old.length; i++) {
			sum += old[i];
		}
		if (sum > 0) {
			avg = sum / old.length;
			remainder = sum % old.length;
		}
		if (avg > 0) {
			// 均值大于0才均分 均值为0就不均分
			produce = new Integer[old.length];
			for (int i = 0, olen = old.length; i < olen; i++) {
				produce[i] = avg;
			}
			System.out.println(" remainder >>>> " + remainder);
			if (remainder > 0) {
				for (int i = 0; i < remainder; i++) {
					produce[i] += 1;
				}
			}
		}
		if (produce.length > 0) {
			diff = new Integer[produce.length];
			Arrays.sort(old, Collections.reverseOrder());
			Arrays.sort(produce, Collections.reverseOrder());
			for (int i = 0; i < old.length; i++) {
				diff[i] = old[i] - produce[i];
			}
		}
		if (produce.length > 0) {
			for (int i = 0, olen = old.length; i < olen; i++) {
				System.out.println("old array sorted :[" + i + "]-->" + old[i]);
			}
			for (int i = 0; i < produce.length; i++) {
				System.out.println("produce array sorted :[" + i + "]-->" + produce[i]);
			}
		}
		return diff;
	}
}
