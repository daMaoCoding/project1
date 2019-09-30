package com.xinbo.fundstransfer.service.impl;

import java.util.*;

import javax.persistence.EntityManager;

import com.xinbo.fundstransfer.configuation.RedisConfiguration;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageModel;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageProducer;
import com.xinbo.fundstransfer.component.redis.msgqueue.MessageType;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Administrator on 2018/10/18.
 */
@Slf4j
@Service
class AssignAWInAccountServiceImpl implements AssignAWInAccountService {
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private RedisService redisService;
	@Autowired
	private MessageProducer messageProducer;
	private StringRedisTemplate stringRedisTemplate;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private RedisScriptService redisScriptService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysUserProfileService userProfileService;

	public StringRedisTemplate getTemplate() {
		return stringRedisTemplate;
	}

	@Autowired
	public void setTemplate(StringRedisTemplate stringRedisTemplate) {
		if (stringRedisTemplate == null) {
			stringRedisTemplate = redisService.getStringRedisTemplate();
		}
		this.stringRedisTemplate = stringRedisTemplate;
	}

	// 获取在redis上的接单用户
	@Override
	public Set<String> getUsersOnLine(Integer zone) {
		if (zone == null) {
			Set<String> keys = stringRedisTemplate.keys(RedisKeys.INCOME_APPROVE_USER_ZONE_KEY + "*");
			if (CollectionUtils.isEmpty(keys)) {
				return null;
			}
			Set<String> onlineUsers = new HashSet<>();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				onlineUsers.addAll(stringRedisTemplate.opsForSet().members(it.next().toString()));
			}
			return onlineUsers;
		}
		boolean exists = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_USER_ZONE_KEY + zone);
		if (!exists) {
			return null;
		}
		Set<String> onLineUsers = stringRedisTemplate.boundSetOps(RedisKeys.INCOME_APPROVE_USER_ZONE_KEY + zone)
				.members();
		return onLineUsers;
	}

	// 获取已经被分配的用户
	@Override
	public Set<String> getAssignedUsers(Integer zone) {
		if (zone == null) {
			Set<String> keys = stringRedisTemplate.keys(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + "*");
			if (CollectionUtils.isEmpty(keys)) {
				return null;
			}
			Set<String> assignedUsers = new HashSet<>();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				assignedUsers.addAll(stringRedisTemplate.opsForSet().members(it.next().toString()));
			}
			return assignedUsers;
		}
		boolean exists = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone);
		if (!exists) {
			return null;
		}
		Set<String> assignedUsers = stringRedisTemplate
				.boundSetOps(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone).members();
		return assignedUsers;
	}

	/**
	 * get all of alipay wechat accounts whose status including 1 3 4
	 * {@link AccountStatus}
	 * 
	 * @return
	 */
	@Override
	public Map<Integer, Map<Integer, Set<Object[]>>> getAWAccountsAll(int zone) {
		List<Integer> handicapIds = userProfileService.getSysUserProfileHandicapByZone(zone);
		boolean empty = CollectionUtils.isEmpty(handicapIds);
		if (empty) {
			log.info("AssignAWInAccountServiceImpl.getAWAccountsAll result is empty:{}", empty);
			return null;
		}
		int size = handicapIds.size();
		String sql = "SELECT id,account, status,type FROM fundsTransfer.biz_account WHERE type in (3,4)  and status in(1,3,4)";
		if (size == 1) {
			sql += " and handicap_id=" + handicapIds.get(0);
		} else {
			sql += " and handicap_id in(";
			for (int i = 0; i < size; i++) {
				if (i < size - 1) {
					sql += handicapIds.get(i) + ",";
				} else {
					sql += handicapIds.get(i) + ")";
				}
			}
		}
		List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		// k-v:type-map<status,accountsSet> 默认按type升序 3,4
		Map<Integer, Map<Integer, Set<Object[]>>> ret = new TreeMap<>();
		for (Iterator it = list.iterator(); it.hasNext();) {
			// k-v:status-accountsSet 默认按status升序 1 3 4
			Map<Integer, Set<Object[]>> valueMap = new TreeMap<>();
			Set<Object[]> valueMapValueSet = new HashSet<>();
			Object[] obj = (Object[]) it.next();
			if (CollectionUtils.isEmpty(ret)) {
				valueMapValueSet.add(obj);
				valueMap.put(Integer.valueOf(obj[2].toString()), valueMapValueSet);
				ret.put(Integer.valueOf(obj[3].toString()), valueMap);
			} else {
				if (ret.containsKey(Integer.valueOf(obj[3].toString()))) {
					valueMap = ret.get(Integer.valueOf(obj[3].toString()));
					if (valueMap.containsKey(Integer.valueOf(obj[2].toString()))) {
						valueMapValueSet = valueMap.get(Integer.valueOf(obj[2].toString()));
						valueMapValueSet.add(obj);
						valueMap.put(Integer.valueOf(obj[2].toString()), valueMapValueSet);
					} else {
						valueMapValueSet.add(obj);
						valueMap.put(Integer.valueOf(obj[2].toString()), valueMapValueSet);
					}
				} else {
					valueMapValueSet.add(obj);
					valueMap.put(Integer.valueOf(obj[2].toString()), valueMapValueSet);
				}
				ret.put(Integer.valueOf(obj[3].toString()), valueMap);
			}
		}
		return ret;
	}

	/**
	 * 根据类型查询 微信4 支付宝3 账号信息 以供分配给入款审核 账号状态: 1 3 4
	 * {@link com.xinbo.fundstransfer.domain.enums.AccountStatus}
	 * 
	 * @param type
	 *            {@link com.xinbo.fundstransfer.domain.enums.AccountType}
	 * @return return a map which use account status
	 *         {@link com.xinbo.fundstransfer.domain.enums.AccountStatus} as key and
	 *         account information list as value
	 */
	@Override
	public Map<Integer, Set<Object[]>> getAWAccountsByType(int type) {
		String sql = "SELECT id,account, status FROM fundsTransfer.biz_account WHERE type= " + type
				+ " and status in(1,3,4)";
		List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		Map<Integer, Set<Object[]>> map = new TreeMap<>((o1, o2) -> o2 - o1);
		Set<Object[]> set = new HashSet<>();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Object[] obj = (Object[]) it.next();
			if (CollectionUtils.isEmpty(map)) {
				set.add(obj);
				map.put(Integer.valueOf(obj[2].toString()), set);
			} else {
				if (map.keySet().contains(Integer.valueOf(obj[2].toString()))) {
					set = map.get(Integer.valueOf(obj[2].toString()));
					set.add(obj);
					map.put(Integer.valueOf(obj[2].toString()), set);
				} else {
					set = new HashSet<>();
					set.add(obj);
					map.put(Integer.valueOf(obj[2].toString()), set);
				}
			}
		}
		return map;
	}

	// 点击接单 打开socket 保存用户id
	@Override
	public long saveUserToRedis(int userId, String zone) {
		try {
			// userProfileService.getSysUserProfileZoneByUserId
			// int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			if (Objects.isNull(zone) || !NumberUtils.isDigits(zone)) {
				log.info("用户:{}没有划分区域:{}!", userId, zone);
				return 0;
			}
			Set<String> allUsers = getUsersOnLine(null);
			if (CollectionUtils.isEmpty(allUsers) || allUsers.size() == 1) {
				boolean exist = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY);
				if (exist) {
					stringRedisTemplate.delete(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY);
				}
				boolean exist2 = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
				if (exist2) {
					stringRedisTemplate.delete(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
				}
			}
			long add = stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ZONE_KEY + zone,
					String.valueOf(userId));
			if (add == 1L) {
				popMessageOnStartOrStop(userId, MessageType.INCOME_APPROVE_START.getType());
				MessageModel messageModel = new MessageModel(MessageType.INCOME_APPROVE_START.getType(), userId);
				messageProducer.pushMessage(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY, messageModel);
				stringRedisTemplate.convertAndSend(RedisTopics.ASSIGN_INCOMEAWACCOUNT_TOPIC,
						"ASSIGNAWACCOUNTSTART:" + userId);
			}
			return add;
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.saveUserToRedis error occured:", e);
		}
		return 0L;
	}

	@Override
	public long deleteUserOnRedis(int userId) {
		try {
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			boolean existKey = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_USER_ZONE_KEY + zone);
			if (existKey) {
				boolean isMember = stringRedisTemplate.opsForSet()
						.isMember(RedisKeys.INCOME_APPROVE_USER_ZONE_KEY + zone, String.valueOf(userId));
				if (!isMember) {
					log.info("AssignAWInAccountServiceImpl.deleteUserOnRedis userId :{},is not the cached member . ",
							userId);
					return 0L;
				}
				long delete = isMember
						? stringRedisTemplate.opsForSet().remove(RedisKeys.INCOME_APPROVE_USER_ZONE_KEY + zone,
								String.valueOf(userId))
						: 0;
				log.info("AssignAWInAccountServiceImpl.deleteUserOnRedis userId :{},result:{}", userId, delete);
				if (delete == 1) {
					popMessageOnStartOrStop(userId, MessageType.INCOME_APPROVE_STOP.getType());
					deleteAssignedUser(userId);
					MessageModel messageModel = new MessageModel(MessageType.INCOME_APPROVE_STOP.getType(), userId);
					messageProducer.pushMessage(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY, messageModel);
					stringRedisTemplate.convertAndSend(RedisTopics.ASSIGN_INCOMEAWACCOUNT_TOPIC,
							"ASSIGNAWACCOUNTSTOP:" + userId);
				}
				return delete;
			}
			return 0L;
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.deleteUserOnRedis error occured:", e);
		}
		return 0L;
	}

	// 点击接单的时候分配所有支付宝和微信账号 状态:1,3,4
	@Override
	public boolean assignOnStartByUser(int userId) {
		try {
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			if (zone == -1) {
				log.info("用户:{}没有划分区域!", userId);
				return true;
			}
			deleteCacheUserOffline(zone);
			deleteAssignedRecord(zone);
			Set<String> onlineUsers = getUsersOnLine(zone);
			if (CollectionUtils.isEmpty(onlineUsers)) {
				return true;
			}
			if (onlineUsers.size() == 1) {
				return excute1AssignOnStart(userId, zone);
			} else {
				return executeNAssignOnStart(onlineUsers, zone);
			}
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.assignOnStartByUser error:", e);
			return false;
		}
	}

	/**
	 * <p>
	 * 多于一个人的时候 开始接单 分配<br/>
	 * 分配逻辑:接单人n>1,在用账号总和m(ali:a,wechat:w)<br/>
	 * 1.n>=m,m均分:a,w,多余人数:n-m不分; <br/>
	 * 2.n<m && n>a && n>w :n个人均分a,w，m-n再均分。<br/>
	 * 3.n<m && n>a || n>w :均分
	 * </p>
	 *
	 * @return
	 */
	private boolean executeNAssignOnStart(Set<String> onlineUsers, int zone) {
		try {
			Map<Integer, Map<Integer, Set<Object[]>>> map = getAWAccountsAll(zone);
			if (CollectionUtils.isEmpty(map)) {
				return false;
			} else {
				return assignNormal(zone, onlineUsers) && assignFreezeAndStop(zone, onlineUsers);
			}
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.executeAssignNOnStart error:", e);
			return false;
		}
	}

	// 分配在用的账号 :
	// IncomeApprove:user:status:alipayInAccount:5: 1
	// IncomeApprove:user:status:wechatInAccount:5: 1
	private boolean assignNormal(int zone, Set<String> onlineUsers) {
		try {
			if (CollectionUtils.isEmpty(onlineUsers)) {
				return true;
			}
			int onlineUserSize = onlineUsers.size();
			// 已分配的支付宝在用账号数量
			String aliKey = RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT + ":" + zone + ":"
					+ RedisKeys.INCOME_ACCOUNT_NORMAL_STATUS_KEY;
			long aliNormalAccountsCounts = stringRedisTemplate.opsForSet().size(aliKey);
			// 已分配的微信在用账号数量
			String wechatKey = RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT + ":" + zone + ":"
					+ RedisKeys.INCOME_ACCOUNT_NORMAL_STATUS_KEY;
			long wechatNormalAccountCounts = stringRedisTemplate.opsForSet().size(wechatKey);
			if (aliNormalAccountsCounts == 0L && wechatNormalAccountCounts == 0L) {
				return true;
			}
			// 在线接单人 多于已分配的账号数量
			if (onlineUserSize > (aliNormalAccountsCounts + wechatNormalAccountCounts)) {
				return true;
			}
			if (aliNormalAccountsCounts > 0L) {
				Set<String> aliAccountIds = stringRedisTemplate.opsForSet().members(aliKey);
				assignAliNorMal(zone, onlineUsers, aliAccountIds);
			}
			if (wechatNormalAccountCounts > 0L) {
				Set<String> wechatAccountIds = stringRedisTemplate.opsForSet().members(wechatKey);
				assignWechatNorMal(zone, onlineUsers, wechatAccountIds);
			}
			return true;
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.assignNormal error :", e);
			return false;
		}
	}

	// 支付宝按用户id顺序分配
	private void assignAliNorMal(int zone, Set<String> onlineUsers, Set<String> aliAccountIds) {
		try {
			int aliAccountIdsSize = aliAccountIds.size(), onlineUserSize = onlineUsers.size();
			int avg = aliAccountIdsSize / onlineUserSize;
			int remainder = aliAccountIdsSize % onlineUserSize;
			List<String> onlineUsersList = new ArrayList<>(onlineUsers);
			List<String> aliAccountIdsList = new ArrayList<>(aliAccountIds);
			if (avg > 0) {
				for (int i = 0; i < onlineUserSize; i++) {
					// IncomeApprove:user:status:alipayInAccount:5:1
					String userId = onlineUsersList.get(i);
					StringBuilder key = new StringBuilder(
							RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY + userId + ":1");
					for (int j = i * avg; j < (i + 1) * avg; j++) {
						stringRedisTemplate.opsForSet().add(key.toString(), aliAccountIdsList.get(j));
					}
					stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone, userId);
				}
			}
			if (remainder > 0) {
				for (int i = 0; i < remainder; i++) {
					String userId = onlineUsersList.get(i);
					StringBuilder key = new StringBuilder(
							RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY + userId + ":1");
					stringRedisTemplate.opsForSet().add(key.toString(),
							aliAccountIdsList.get(avg * onlineUserSize + i));
					stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone, userId);
				}
			}
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.assignAliNorMal error:", e);
		}
	}

	// 微信按用户id逆序分配
	private void assignWechatNorMal(int zone, Set<String> onlineUsers, Set<String> wechatAccountIds) {
		try {
			int wechatAccountIdsSize = wechatAccountIds.size(), onlineUserSize = onlineUsers.size();
			int avg = wechatAccountIdsSize / onlineUserSize;
			int remainder = wechatAccountIdsSize % onlineUserSize;
			List<String> onlineUsersList = new ArrayList<>(onlineUsers);
			List<String> wechatAccountIdsList = new ArrayList<>(wechatAccountIds);
			if (avg > 0) {
				for (int i = onlineUserSize - 1; i >= 0; i--) {
					// IncomeApprove:user:status:alipayInAccount:5:1
					String userId = onlineUsersList.get(i);
					StringBuilder key = new StringBuilder(
							RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY + userId + ":1");
					int jLimit = onlineUserSize - 1 - i;
					for (int j = jLimit * avg; j < (jLimit + 1) * avg; j++) {
						stringRedisTemplate.opsForSet().add(key.toString(), wechatAccountIdsList.get(j));
					}
					stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone, userId);
				}
			}
			if (remainder > 0) {
				for (int i = 0; i < remainder; i++) {
					int userIndex = onlineUserSize - 1 - i;
					String userId = onlineUsersList.get(userIndex);
					StringBuilder key = new StringBuilder(
							RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY + userId + ":1");
					stringRedisTemplate.opsForSet().add(key.toString(),
							wechatAccountIdsList.get(avg * onlineUserSize + i));
					stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone, userId);
				}
			}
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.assignWechatNorMal error:", e);
		}
	}

	// 对map按照值大小降序排序
	private static Map<String, Integer> comparator(Map<String, Integer> map, String type) {
		List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
		if ("ali".equals(type)) {
			Collections.sort(entryList, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
		} else {
			Collections.sort(entryList, Comparator.comparing(Map.Entry::getValue));
		}
		Map<String, Integer> map1 = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : entryList) {
			map1.put(entry.getKey(), entry.getValue());
		}
		return map1;
	}

	// 分配冻结账号
	// IncomeApprove:user:status:alipayInAccount:5: 3/4
	// IncomeApprove:user:status:wechatInAccount:5: 3/4
	private boolean assignFreezeAndStop(int zone, Set<String> onlineUsers) {
		try {
			if (CollectionUtils.isEmpty(onlineUsers)) {
				return true;
			}
			int[] status = new int[] { AccountStatus.Freeze.getStatus(), AccountStatus.StopTemp.getStatus() };
			for (int s = 0, slen = status.length; s < slen; s++) {
				String statusKeymid, statusKeysubfix;
				if (status[s] == AccountStatus.Freeze.getStatus()) {
					statusKeymid = RedisKeys.INCOME_ACCOUNT_FREEZE_STATUS_KEY;
					statusKeysubfix = ":3";
				} else {
					statusKeymid = RedisKeys.INCOME_ACCOUNT_STOP_STATUS_KEY;
					statusKeysubfix = ":4";
				}
				String[] type = new String[] { "alipay", "wechat" };
				for (int i = 0, len = type.length; i < len; i++) {
					String key, key2;
					long freezeOrStopAccountsCounts;
					if ("alipay".equals(type[i])) {
						key = RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT + ":" + zone + ":" + statusKeymid;
						key2 = RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY;
						freezeOrStopAccountsCounts = stringRedisTemplate.opsForSet().size(key);
					} else {
						key = RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT + ":" + zone + ":" + statusKeymid;
						key2 = RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY;
						freezeOrStopAccountsCounts = stringRedisTemplate.opsForSet().size(key);
					}
					if (freezeOrStopAccountsCounts == 0) {
						break;
					}
					Set<String> freezeOrStopAccountIds = stringRedisTemplate.opsForSet().members(key);
					if (CollectionUtils.isEmpty(freezeOrStopAccountIds)) {
						break;
					}
					List<String> onlineUsersList = new ArrayList<>(onlineUsers);
					List<String> freezeOrStopAccountIdsList = new ArrayList<>(freezeOrStopAccountIds);
					int onlineUserSize = onlineUsersList.size(),
							freezeOrStopAccountIdsListSize = freezeOrStopAccountIdsList.size();
					int avg = freezeOrStopAccountIdsListSize / onlineUserSize;// 均分
					int remainder = freezeOrStopAccountIdsListSize % onlineUserSize;// 余数
					if (avg > 0) {
						for (int j = 0; j < onlineUserSize; j++) {
							String userId = onlineUsersList.get(j);
							String key3 = key2 + userId + statusKeysubfix;
							for (int k = avg * j; k < avg * (j + 1); k++) {
								stringRedisTemplate.opsForSet().add(key3, freezeOrStopAccountIdsList.get(k));
							}
							stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone,
									userId);
						}
					}
					if (remainder > 0) {
						for (int j = 0; j < remainder; j++) {
							String userId = onlineUsersList.get(j);
							String key3 = key2 + userId + statusKeysubfix;
							stringRedisTemplate.opsForSet().add(key3,
									freezeOrStopAccountIdsList.get(avg * onlineUserSize + j));
							stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone,
									userId);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.assignFreezeAndStop error:", e);
		}
		return true;
	}

	// 一个人接单的时候 分配
	private boolean excute1AssignOnStart(int userId, int zone) {
		try {
			Map<Integer, Map<Integer, Set<Object[]>>> map = getAWAccountsAll(zone);
			if (CollectionUtils.isEmpty(map)) {
				return true;
			} else {
				for (Map.Entry<Integer, Map<Integer, Set<Object[]>>> entry : map.entrySet()) {
					StringBuilder key = new StringBuilder();// 分配的各个类型各个状态的账号id集合key
					StringBuilder keyAccount = new StringBuilder();// 保存各个类型各个状态的账号集合总数
					if (entry.getKey() == AccountType.InAli.getTypeId()) {
						key.append(RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY + userId);
						keyAccount.append(RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT + ":" + zone).append(":");
					} else {
						key.append(RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY + userId);
						keyAccount.append(RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT + ":" + zone).append(":");
					}
					// 支付宝或者微信包括状态为 1 3 4的账号信息
					Map<Integer, Set<Object[]>> map2 = entry.getValue();
					// IncomeApprove:user:status:alipayInAccount:5:1/3/4
					// IncomeApprove:user:status:wechatInAccount:5:1/3/4
					for (Map.Entry<Integer, Set<Object[]>> entry2 : map2.entrySet()) {
						int status = entry2.getKey();// 1 3 4
						StringBuilder key2 = new StringBuilder(key);
						StringBuilder keyAccount2 = new StringBuilder(keyAccount);
						if (status == AccountStatus.Normal.getStatus() || status == AccountStatus.Freeze.getStatus()
								|| status == AccountStatus.StopTemp.getStatus()) {
							Set<Object[]> accountInfos = entry2.getValue();
							key2.append(":" + status);
							if (status == AccountStatus.Normal.getStatus()) {
								keyAccount2.append(RedisKeys.INCOME_ACCOUNT_NORMAL_STATUS_KEY);
							} else if (status == AccountStatus.Freeze.getStatus()) {
								keyAccount2.append(RedisKeys.INCOME_ACCOUNT_FREEZE_STATUS_KEY);
							} else if (status == AccountStatus.StopTemp.getStatus()) {
								keyAccount2.append(RedisKeys.INCOME_ACCOUNT_STOP_STATUS_KEY);
							}
							// id,account, status,type
							accountInfos.stream().forEach(p -> {
								// 缓存 用户分配到 支付宝 微信 1 3 4 三种状态账号的 id
								stringRedisTemplate.opsForSet().add(key2.toString(), p[0].toString());
								// 缓存 支付宝 微信 1 3 4 三个状态的账号 id
								stringRedisTemplate.opsForSet().add(keyAccount2.toString(), p[0].toString());
							});

						}
					}
				}
				stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone,
						String.valueOf(userId));
				return true;
			}
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.excuteAssign1OnStart error :", e);
			return false;
		}
	}

	/**
	 * 结束接单或者关闭socket的时候删除
	 * 
	 * @param userId
	 * @return
	 */
	@Override
	public boolean assignOnStopByUser(int userId) {
		int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
		Set<String> onlineUsers = getUsersOnLine(zone);
		if (CollectionUtils.isEmpty(onlineUsers)) {
			return true;
		}
		if (onlineUsers.size() == 1) {
			excute1AssignOnStart(Integer.valueOf(new ArrayList<>(onlineUsers).get(0)), zone);
		} else {
			executeNAssignOnStart(onlineUsers, zone);
		}
		return true;
	}

	/**
	 * 平台同步支付宝微信账号变更的时候 通知分配
	 * 
	 * @param type
	 *            3 4 {@link AccountType}
	 * @param accountId
	 */
	@Override
	public void sendMessageOnPushAccount(int type, int accountId) {
		try {
			BizAccount account = accountService.getById(accountId);
			if (account == null) {
				return;
			}
			if (account.getHandicapId() == null) {
				return;
			}
			// handicapService.findZoneByHandiId
			int zone = handicapService.findZoneByHandiId(account.getHandicapId());
			if (zone == -1) {
				log.info("盘口:{}没有区域划分!", account.getHandicapId());
				return;
			}
			Set<String> onlineUsers = getUsersOnLine(zone);
			if (CollectionUtils.isEmpty(onlineUsers)) {
				return;
			}
			if (type == AccountType.InAli.getTypeId()) {
				popMessageOnStartOrStop(accountId, MessageType.INCOME_APPROVE_ALIPAYACCUPDATE.getType());
				MessageModel messageModel = new MessageModel(MessageType.INCOME_APPROVE_ALIPAYACCUPDATE.getType(),
						accountId);
				messageProducer.pushMessage(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY, messageModel);
				stringRedisTemplate.convertAndSend(RedisTopics.ASSIGN_INCOMEAWACCOUNT_TOPIC,
						"ASSIGNALIACCOUNT:" + accountId);
			}
			if (type == AccountType.InWechat.getTypeId()) {
				popMessageOnStartOrStop(accountId, MessageType.INCOME_APPROVE_WECHATACCUPDATE.getType());
				MessageModel messageModel = new MessageModel(MessageType.INCOME_APPROVE_WECHATACCUPDATE.getType(),
						accountId);
				messageProducer.pushMessage(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY, messageModel);
				stringRedisTemplate.convertAndSend(RedisTopics.ASSIGN_INCOMEAWACCOUNT_TOPIC,
						"ASSIGNWECHATACCOUNT:" + accountId);

			}
		} catch (Exception e) {
			log.error("AssignAWInAccountServiceImpl.sendMessageOnPushAccount param: type:{},accountId:{}  error :{}",
					type, accountId, e.getLocalizedMessage());
		}
	}

	/**
	 * 平台推送账号的时候处理
	 * 
	 * @param accountId
	 * @param type
	 *            #12 13 {@link MessageType}
	 * @return
	 */
	@Override
	public int dealOnAccountUpdate(int accountId, int type) {
		AccountBaseInfo baseInfo = accountService.getFromCacheById(accountId);
		int status = -99;
		if (baseInfo == null) {
			BizAccount account = accountService.getById(accountId);
			if (account != null) {
				status = account.getStatus();
			}
		} else {
			status = baseInfo.getStatus();
		}
		if (status == -99) {
			return status;
		}
		type = type == MessageType.INCOME_APPROVE_ALIPAYACCUPDATE.getType() ? AccountType.InAli.getTypeId()
				: type == MessageType.INCOME_APPROVE_WECHATACCUPDATE.getType() ? AccountType.InWechat.getTypeId() : -1;
		if (type == -1) {
			return type;
		}
		if (status != AccountStatus.Freeze.getStatus() && status != AccountStatus.Normal.getStatus()
				&& status != AccountStatus.StopTemp.getStatus()) {
			status = deleteAssignedAccountId(accountId, type);
		} else {
			status = assignAccountToLeastUser(accountId, type, status);
		}
		return status;
	}

	/**
	 * 平台同步微信支付宝账号的时候分配给手上账号最少的用户 如果账号状态变更成不是 1 3 4状态,则在10分钟后删除已分配的缓存
	 * 
	 * @param accountId
	 * @param type
	 *            {#3 4 {@link AccountType}}
	 * @param status
	 *            1 3 4 {@link AccountStatus}
	 * @return userId who would have been assigned the accountId finally
	 */
	@Override
	public int assignAccountToLeastUser(int accountId, int type, int status) {
		try {
			String keyPrefix = type == AccountType.InAli.getTypeId() ? RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT
					: RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT;
			if (!Arrays.asList(new int[] { AccountStatus.Normal.getStatus(), AccountStatus.Freeze.getStatus(),
					AccountStatus.StopTemp.getStatus() }).contains(status)) {
				log.info("AssignAWInAccountServiceImpl.assignAccountToLeastUser account status:{},accountId:{}", status,
						accountId);
				return -99;
			}
			BizAccount account = accountService.getById(accountId);
			if (account == null) {
				return -99;
			}
			int zone = userProfileService.getSysUserProfileZoneByUserId(account.getHandicapId());
			if (zone == -1) {
				log.info("该盘口:{}没有划分区域!", account.getHandicapId());
				return -99;
			}
			switch (status) {
			case 1: {
				keyPrefix = keyPrefix + ":" + zone + ":" + RedisKeys.INCOME_ACCOUNT_NORMAL_STATUS_KEY;
				break;
			}
			case 3: {
				keyPrefix = keyPrefix + ":" + zone + ":" + RedisKeys.INCOME_ACCOUNT_FREEZE_STATUS_KEY;
				break;
			}
			case 4: {
				keyPrefix = keyPrefix + ":" + zone + ":" + RedisKeys.INCOME_ACCOUNT_STOP_STATUS_KEY;
				break;
			}
			default:
				break;
			}
			String keyPrefix2 = type == AccountType.InAli.getTypeId() ? RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY
					: RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY;
			int userId = getUserIdWithoutAccount();
			if (userId != -1) {
				stringRedisTemplate.opsForSet().add(keyPrefix, String.valueOf(accountId));
				stringRedisTemplate.opsForSet().add(keyPrefix2 + userId + ":" + status, String.valueOf(accountId));
				stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone,
						String.valueOf(userId));
				return userId;
			}
			// IncomeApprove:user:status:wechatInAccount:5:1
			Set<String> keys = stringRedisTemplate.keys(keyPrefix2 + "*:" + status);
			Map<Integer, String> size2UserIdMap = new TreeMap<>();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				String key = it.next().toString();
				Set<String> accountIds = stringRedisTemplate.opsForSet().members(key);
				size2UserIdMap.put(accountIds.size(), key.split(":")[4]);
			}
			userId = new ArrayList<>(size2UserIdMap.keySet()).get(0);
			int userZone = userProfileService.getSysUserProfileZoneByUserId(userId);
			if (zone != userZone) {
				log.info(
						"AssignAWInAccountServiceImpl.assignAccountToLeastUser userId:{},accountId:{}, accountZone:{}, unequal userZone:{} ",
						userId, accountId, zone, userZone);
				return -99;
			}
			stringRedisTemplate.opsForSet().add(keyPrefix, String.valueOf(accountId));
			stringRedisTemplate.opsForSet().add(keyPrefix2 + userId + ":" + status, String.valueOf(accountId));
			stringRedisTemplate.opsForSet().add(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone,
					String.valueOf(userId));
			return 99;
		} catch (Exception e) {
			log.error("assignAccountToLeastUser error:", e);
		}
		return 99;
	}

	// 获取在线接单但是没有分配有账号的用户
	private int getUserIdWithoutAccount() {
		Set<String> onlineUsers = getUsersOnLine(null);
		if (CollectionUtils.isEmpty(onlineUsers)) {
			return -1;
		}
		Set<String> assignedUsers = getAssignedUsers(null);
		List<String> diff;
		if (CollectionUtils.isEmpty(assignedUsers)) {
			diff = new ArrayList<>(onlineUsers);
			return Integer.valueOf(diff.get(0));
		}
		diff = CommonUtils.getDiffrentList(new ArrayList<>(onlineUsers), new ArrayList<>(assignedUsers));
		if (CollectionUtils.isEmpty(diff)) {
			return -1;
		}
		return Integer.valueOf(diff.get(0));
	}

	// 删除已分配的账号 返回分配到该账号的用户id
	private int deleteAssignedAccountId(int accountId, int type) {
		try {
			int userId = getUserIdByAccountId(accountId);
			int status = checkIsMember(accountId);
			if (userId == -1 || status == -1) {
				return -1;
			}
			if (userId != -1) {
				return deleteAssignedAccountByUser(userId, accountId, type);
			}
			return -1;
		} catch (Exception e) {
			log.error("deleteAssignedAccountId error:", e);
		}
		return -1;
	}

	// 删除某个已经分配的支付宝微信账号 返回分到该账号的用户Id
	@Override
	public int deleteAssignedAccountByUser(int userId, int accountId, int type) {
		try {
			int status = checkIsMember(accountId);
			int status2 = checkAccountIdHeldByUserId(userId, accountId, type);
			if (status == -1 || status2 == -1 || status != status2) {
				return -1;
			}
			int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
			String keyPrefix = type == AccountType.InWechat.getTypeId() ? RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT
					: RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT;
			String keyMidfix = status == AccountStatus.Freeze.getStatus() ? RedisKeys.INCOME_ACCOUNT_FREEZE_STATUS_KEY
					: status == AccountStatus.Normal.getStatus() ? RedisKeys.INCOME_ACCOUNT_NORMAL_STATUS_KEY
							: status == AccountStatus.StopTemp.getStatus() ? RedisKeys.INCOME_ACCOUNT_STOP_STATUS_KEY
									: "";
			if (StringUtils.isEmpty(keyMidfix)) {
				return -1;
			}
			Long rem1 = stringRedisTemplate.opsForSet().remove(keyPrefix + ":" + zone + ":" + keyMidfix,
					String.valueOf(accountId));
			String keyPrefix2 = type == AccountType.InWechat.getTypeId()
					? RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY
					: RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY;
			Long rem2 = stringRedisTemplate.opsForSet().remove(keyPrefix2 + userId + ":" + status2);
			return (rem1.intValue() | rem2.intValue()) > 0 ? userId : -1;
		} catch (Exception e) {
			log.error("deleteAssignedAccountByUser  params:userId:{},accountId:{},type:{}", userId, accountId, type);
			log.error("deleteAssignedAccountByUser  error:", e);
		}
		return -1;
	}

	// 查询账号是否是在已分配的缓存中 返回账号状态值
	private int checkIsMember(int accountId) {
		BizAccount account = accountService.getById(accountId);
		if (account.getType() != AccountType.InWechat.getTypeId()
				&& account.getType() != AccountType.InAli.getTypeId()) {
			return -1;
		}
		int zone = userProfileService.getSysUserProfileZoneByUserId(account.getHandicapId());
		String keyPrefix = account.getType() == AccountType.InWechat.getTypeId() ? RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT
				: RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT;

		Set<String> members = stringRedisTemplate.opsForSet()
				.members(keyPrefix + ":" + zone + ":" + RedisKeys.INCOME_ACCOUNT_NORMAL_STATUS_KEY);
		if (!CollectionUtils.isEmpty(members) && members.contains(String.valueOf(accountId))) {
			return AccountStatus.Normal.getStatus();
		} else {
			Set<String> members2 = stringRedisTemplate.opsForSet()
					.members(keyPrefix + ":" + zone + ":" + RedisKeys.INCOME_ACCOUNT_FREEZE_STATUS_KEY);
			if (!CollectionUtils.isEmpty(members2) && members2.contains(String.valueOf(accountId))) {
				return AccountStatus.Freeze.getStatus();
			} else {
				Set<String> members3 = stringRedisTemplate.opsForSet()
						.members(keyPrefix + ":" + zone + ":" + RedisKeys.INCOME_ACCOUNT_STOP_STATUS_KEY);
				if (!CollectionUtils.isEmpty(members3) && members3.contains(String.valueOf(accountId))) {
					return AccountStatus.StopTemp.getStatus();
				}
			}
		}
		return -1;
	}

	// 判断该账号是否在某个人手中 返回 账号状态值
	private int checkAccountIdHeldByUserId(int userId, int accountId, int type) {
		if (type != AccountType.InWechat.getTypeId() && type != AccountType.InAli.getTypeId()) {
			return -1;
		}
		String keyPrefix = type == AccountType.InWechat.getTypeId() ? RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY
				: RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY;
		String[] status = new String[] { "1", "3", "4" };
		Set<String> accountIds = new HashSet<>();
		for (int i = 0, len = status.length; i < len; i++) {
			// IncomeApprove:user:status:wechatInAccount:5:1
			Set<String> keys = stringRedisTemplate.keys(keyPrefix + userId + ":" + status[i]);
			if (!CollectionUtils.isEmpty(keys)) {
				for (Iterator it = keys.iterator(); it.hasNext();) {
					String key = it.next().toString();
					accountIds.addAll(stringRedisTemplate.opsForSet().members(key));
					if (accountIds.contains(String.valueOf(accountId))) {
						return Integer.valueOf(status[i]);
					}
				}
			}
		}
		return -1;
	}

	// 根据账号id获取分配有该账号的用户id 返回用户id
	private int getUserIdByAccountId(int accountId) {
		BizAccount account = accountService.getById(accountId);
		int userId = -1;
		if (account == null) {
			return userId;
		} else {
			if (account.getType() != AccountType.InWechat.getTypeId()
					&& account.getType() != AccountType.InAli.getTypeId()) {
				return -1;
			}
			Set<String> accountIds = new HashSet<>();
			String keyPrefix = account.getType() == AccountType.InWechat.getTypeId()
					? RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY
					: RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY;
			String[] status = new String[] { "1", "3", "4" };
			for (int i = 0, len = status.length; i < len; i++) {
				// IncomeApprove:user:status:wechatInAccount:5:1
				Set<String> keys = stringRedisTemplate.keys(keyPrefix + "*:" + status[i]);
				if (!CollectionUtils.isEmpty(keys)) {
					for (Iterator it = keys.iterator(); it.hasNext();) {
						String key = it.next().toString();
						accountIds.addAll(stringRedisTemplate.opsForSet().members(key));
						if (accountIds.contains(String.valueOf(accountId))) {
							userId = Integer.valueOf(key.split(":")[4]);
							return userId;
						}
					}
				}
			}
		}
		return -1;
	}

	/**
	 * 通过用户id获取已分配的支付宝微信账号
	 * 
	 * @param userId
	 * @param type
	 *            {#3 4 {@link AccountType}}
	 * @return
	 */
	@Override
	public Map<Integer, Set<Object[]>> getAccountIdsByUser(int userId, int type) {
		String keyPrefix = type == AccountType.InAli.getTypeId() ? RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY
				: RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY;
		Set<String> keys = stringRedisTemplate.keys(keyPrefix + userId + ":*");
		if (CollectionUtils.isEmpty(keys)) {
			return null;
		}
		Map<Integer, Set<Object[]>> retMap = new TreeMap<>();// 1 3 4 key
		Set<String> assignedAWAccountIds = new HashSet<>();
		int status[] = new int[keys.size()];
		List<String> keysList = new ArrayList<>(keys);
		for (int i = 0, size = keysList.size(); i < size; i++) {
			// IncomeApprove:user:status:wechatInAccount:5:1/3/4
			String key = keysList.get(i);
			status[i] = Integer.valueOf(key.split(":")[5]);
			assignedAWAccountIds.addAll(stringRedisTemplate.opsForSet().members(key));
		}
		if (CollectionUtils.isEmpty(assignedAWAccountIds)) {
			return null;
		}
		List<String> accountsList = new ArrayList<>(assignedAWAccountIds);
		int size = accountsList.size(), len = status.length;
		String sql = "select a.id ,a.account,a.owner,a.status,a.bank_name,h.code from biz_account a join biz_handicap h on a.handicap_id=h.id where a.type="
				+ type;
		if (len == 1) {
			sql += "  and a.status =" + status[0];
		} else {
			sql += " and a.status in(";
			for (int i = 0; i < len; i++) {
				if (i < len - 1) {
					sql += status[i] + ",";
				} else {
					sql += status[i] + ")";
				}
			}
		}
		if (size == 1) {
			sql += " and a.id=" + new ArrayList<>(assignedAWAccountIds).get(0);
		} else {
			sql += " and a.id in (";
			for (int i = 0; i < size; i++) {
				if (i < size - 1) {
					sql += accountsList.get(i) + ",";
				} else {
					sql += accountsList.get(i) + ")";
				}
			}
		}
		List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		for (Iterator it = list.iterator(); it.hasNext();) {
			// a.id ,a.account,a.owner,a.status,a.bank_name,h.code
			Object[] obj = (Object[]) it.next();
			Set<Object[]> accounts = new HashSet<>();
			int key = Integer.valueOf(obj[3].toString());
			if (CollectionUtils.isEmpty(retMap)) {
				accounts.add(obj);
				retMap.put(key, accounts);
			} else {
				if (retMap.containsKey(key)) {
					accounts = retMap.get(key);
					accounts.add(obj);
					retMap.put(key, accounts);
				} else {
					accounts.add(obj);
					retMap.put(key, accounts);
				}
			}
		}
		return retMap;
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
		try {
			boolean exist = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY);
			if (exist) {
				List<String> msg = stringRedisTemplate.opsForList().range(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY, 0,
						-1);
				if (CollectionUtils.isEmpty(msg)) {
					return;
				}
				String mess = "{\"operatorId\":" + userId + ",\"type\":" + type + "}";
				for (Iterator it = msg.iterator(); it.hasNext();) {
					String mes = it.next().toString();
					if (mes.equals(mess)) {
						stringRedisTemplate.opsForList().remove(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_KEY, 1, mes);
					}
				}
			}
			boolean exist2 = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY);
			if (exist2) {
				List<String> msg = stringRedisTemplate.opsForList()
						.range(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY, 0, -1);
				if (CollectionUtils.isEmpty(msg)) {
					return;
				}
				String mess = "{\"operatorId\":" + userId + ",\"type\":" + type + "}";
				for (Iterator it = msg.iterator(); it.hasNext();) {
					String mes = it.next().toString();
					if (mes.equals(mess)) {
						stringRedisTemplate.opsForList().remove(RedisKeys.INCOME_APPROVE_MESSAGEQUEUE_BACKUP_KEY, 1,
								mes);
					}
				}
			}
		} catch (Exception e) {
			log.error("popMessageOnStartOrStop error:", e);
		}
	}

	// 删除已分配的记录
	@Override
	public void deleteAssignedRecord(int zone) {
		try {
			Set<String> assignedUsers = getAssignedUsers(zone);
			if (CollectionUtils.isEmpty(assignedUsers)) {
				return;
			}
			for (Iterator it = assignedUsers.iterator(); it.hasNext();) {
				String userIdStr = it.next().toString();
				// IncomeApprove:user:status:alipayInAccount:175:1/3/4
				// IncomeApprove:user:status:wechatInAccount:175:1/3/4
				Set<String> assignedAliKeys = stringRedisTemplate
						.keys(RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY + userIdStr + ":*");
				if (!CollectionUtils.isEmpty(assignedAliKeys)) {
					for (Iterator it2 = assignedAliKeys.iterator(); it2.hasNext();) {
						stringRedisTemplate.delete(it2.next().toString());
					}
				}
				Set<String> assignedWechatKeys = stringRedisTemplate
						.keys(RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY + userIdStr + ":*");
				if (!CollectionUtils.isEmpty(assignedWechatKeys)) {
					for (Iterator it2 = assignedWechatKeys.iterator(); it2.hasNext();) {
						stringRedisTemplate.delete(it2.next().toString());
					}
				}
			}
		} catch (Exception e) {
			log.error("deleteAssignedRecord error:", e);
		}
	}

	// 用户结束接单的时候,删除该用户的缓存
	@Override
	public void deleteAssignedUser(int userId) {
		int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
		stringRedisTemplate.opsForSet().remove(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone,
				String.valueOf(userId));
		Set<String> aliKeys = stringRedisTemplate
				.keys(RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY + userId + ":*");
		if (!CollectionUtils.isEmpty(aliKeys)) {
			stringRedisTemplate.delete(aliKeys);
		}
		Set<String> wechatKeys = stringRedisTemplate
				.keys(RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY + userId + ":*");
		if (!CollectionUtils.isEmpty(wechatKeys)) {
			stringRedisTemplate.delete(wechatKeys);
		}
		if (CollectionUtils.isEmpty(getUsersOnLine(zone))) {
			String ali1Normal = RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT + ":" + zone + ":"
					+ RedisKeys.INCOME_ACCOUNT_NORMAL_STATUS_KEY;
			String ali3Freeze = RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT + ":" + zone + ":"
					+ RedisKeys.INCOME_ACCOUNT_FREEZE_STATUS_KEY;
			String ali4Stop = RedisKeys.INCOME_ZONE_ALIPAY_ACCOUNT + ":" + zone + ":"
					+ RedisKeys.INCOME_ACCOUNT_STOP_STATUS_KEY;

			String wechat1Normal = RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT + ":" + zone + ":"
					+ RedisKeys.INCOME_ACCOUNT_NORMAL_STATUS_KEY;
			String wechat3Freeze = RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT + ":" + zone + ":"
					+ RedisKeys.INCOME_ACCOUNT_FREEZE_STATUS_KEY;
			String wechat4Stop = RedisKeys.INCOME_ZONE_WECHAT_ACCOUNT + ":" + zone + ":"
					+ RedisKeys.INCOME_ACCOUNT_STOP_STATUS_KEY;

			boolean ali1NoramlExist = stringRedisTemplate.hasKey(ali1Normal);
			boolean ali3FreezeExist = stringRedisTemplate.hasKey(ali3Freeze);
			boolean ali4StopExist = stringRedisTemplate.hasKey(ali4Stop);

			boolean wechat1NormalExist = stringRedisTemplate.hasKey(wechat1Normal);
			boolean wechat3FreezeExist = stringRedisTemplate.hasKey(wechat3Freeze);
			boolean wechat4StopExist = stringRedisTemplate.hasKey(wechat4Stop);

			if (ali1NoramlExist) {
				stringRedisTemplate.delete(ali1Normal);
			}
			if (ali3FreezeExist) {
				stringRedisTemplate.delete(ali3Freeze);
			}
			if (ali4StopExist) {
				stringRedisTemplate.delete(ali4Stop);
			}

			if (wechat1NormalExist) {
				stringRedisTemplate.delete(wechat1Normal);
			}
			if (wechat3FreezeExist) {
				stringRedisTemplate.delete(wechat3Freeze);
			}
			if (wechat4StopExist) {
				stringRedisTemplate.delete(wechat4Stop);
			}
		}
	}

	// 开始接单的时候 异常退出的情况下 删除不在线接单的用户缓存
	@Override
	public void deleteCacheUserOffline(int zone) {
		try {
			Set<String> onlineUser = getUsersOnLine(zone);
			if (CollectionUtils.isEmpty(onlineUser)) {
				return;
			}
			Set<String> assignedUsers = getAssignedUsers(zone);
			List<String> diff = new ArrayList<>(onlineUser);
			if (!CollectionUtils.isEmpty(assignedUsers)) {
				diff = CommonUtils.getDiffrentList(new ArrayList<>(onlineUser), new ArrayList<>(assignedUsers));
			}
			if (CollectionUtils.isEmpty(diff)) {
				return;
			}
			for (Iterator it = diff.iterator(); it.hasNext();) {
				String userId = it.next().toString();
				if (!onlineUser.contains(userId) && assignedUsers.contains(userId)) {
					stringRedisTemplate.opsForSet().remove(RedisKeys.INCOME_APPROVE_USER_ASSIGNED_ZONE_KEY + zone,
							userId);
					Set<String> aliKeys = stringRedisTemplate
							.keys(RedisKeys.INCOME_APPROVE_USER_ALIPAY_ACCOUNT_KEY + userId + ":*");
					if (!CollectionUtils.isEmpty(aliKeys)) {
						stringRedisTemplate.delete(aliKeys);
					}
					Set<String> wechatKeys = stringRedisTemplate
							.keys(RedisKeys.INCOME_APPROVE_USER_WECHAT_ACCOUNT_KEY + userId + ":*");
					if (!CollectionUtils.isEmpty(wechatKeys)) {
						stringRedisTemplate.delete(wechatKeys);
					}
				}
			}
		} catch (Exception e) {
			log.error("deleteCacheUserOffline error:", e);
		}
	}

	private static final String INCOME_APPROVE_ASSIGN_LOCK_SCRIPT = "local k = KEYS[1];\n local v = ARGV[1];\n"
			+ "  local ex = redis.call('SETNX',k,v); \nlocal ret='0';  \nif ex ~=nil and ex >0 then\n"
			+ "  ret = redis.call('EXPIRE',k,15) \n end  \n return ret..''";

	// 分配支付宝 微信账号时候 获取锁
	@Override
	public int lock4AssignAW() {
		RedisScript script = redisScriptService.getRedisScriptInstance(String.class,
				"INCOME_APPROVE_ASSIGN_LOCK_SCRIPT", INCOME_APPROVE_ASSIGN_LOCK_SCRIPT);
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		Object ret = template.execute(script, Collections.singletonList(RedisKeys.INCOME_APPROVE_ASSIGN_LOCK), "1");
		log.info("返回结果 :{}", ret);
		int res = 0;
		if (ret != null) {
			res = Integer.valueOf(ret.toString());
		}
		return res;
	}

	// 释放分配锁
	@Override
	public void unlock4AssignAW() {
		boolean exist = stringRedisTemplate.hasKey(RedisKeys.INCOME_APPROVE_ASSIGN_LOCK);
		if (exist) {
			stringRedisTemplate.delete(RedisKeys.INCOME_APPROVE_ASSIGN_LOCK);
		}
	}
}
