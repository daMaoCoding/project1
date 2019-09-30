package com.xinbo.fundstransfer.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.AccountEntity;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import com.xinbo.fundstransfer.service.HostMonitorService;
import com.xinbo.fundstransfer.service.RedisService;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class HostMonitorServiceImpl implements HostMonitorService {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private RedisService redisService;
	@Autowired @Lazy
	private AccountService accountService;
	@Autowired @Lazy
	private AllocateOutwardTaskService allocateOutwardTaskService;
	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public List<Map<String, Object>> list(String host, String accountLike, Integer[] statusArray) {
		List<Map<String, Object>> result = new ArrayList<>();
		Set<String> keys;
		if (StringUtils.isBlank(host)) {
			keys = redisService.getStringRedisTemplate().keys(RedisKeys.genPattern4HostMonitor());
		} else {
			keys = new HashSet<>();
			keys.add(RedisKeys.gen4HostMonitor(StringUtils.trim(host)));
		}
		if (CollectionUtils.isEmpty(keys)) {
			return result;
		}
		boolean isBlank = StringUtils.isBlank(accountLike);
		String prefix = RedisKeys.genPattern4HostMonitor().replace("*", StringUtils.EMPTY);
		for (String key : keys) {
			if (!redisService.getStringRedisTemplate().hasKey(key)) {
				continue;
			}
			String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
			MessageEntity<List<AccountEntity>> message = new MessageEntity<>();
			if (StringUtils.isNotBlank(val)) {
				message = readValue(val);
			}
			boolean ifNot = !isBlank && !CollectionUtils.isEmpty(message.getData())
					&& (message.getData().stream().filter((p) -> p.getAccount().contains(
							StringUtils.trim(accountLike.length() > 5 ? accountLike.substring(0, 4) : accountLike)))
							.count() > 0)
					&& (message.getData()
							.stream().filter(
									(p) -> p.getAccount()
											.contains(StringUtils.trim(accountLike.length() > 5 ? accountLike.substring(
													accountLike.length() - 4, accountLike.length()) : accountLike)))
							.count() > 0);
			if (isBlank || ifNot) {
				Map<String, Object> item = new HashMap<>();
				item.put("action", message.getAction());
				// item.put("interval", message.getInterval());
				item.put("numOfAccount", message.getData() == null ? 0 : message.getData().size());
				item.put("host", key.replace(prefix, StringUtils.EMPTY));
				item.put("type", message.getType());
				item.put("currSysLevel", message.getCurrSysLevel());
				result.add(item);
			}
		}
		return result;
	}

	@Override
	public void shutdown(String host) {
		if (StringUtils.isBlank(host)) {
			return;
		}
		redisService.getStringRedisTemplate().delete(RedisKeys.gen4HostMonitor(host));
		MessageEntity o = new MessageEntity();
		o.setAction(ActionEventEnum.SHUTDOWN.ordinal());
		o.setIp(host);
		messageBroadCast(o);
	}

	@Override
	public void updateHostType(String host, Integer accountType, Integer currSysLevel) {
		String key = RedisKeys.gen4HostMonitor(StringUtils.trim(host));
		if (null != key && redisService.getStringRedisTemplate().hasKey(key)) {
			String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
			MessageEntity<List<AccountEntity>> message = new MessageEntity<>();
			if (StringUtils.isNotBlank(val)) {
				message = readValue(val);
			}
			message.setCurrSysLevel(currSysLevel);
			message.setType(accountType);
			System.out.println(message.getAction());
			// 存储对象到redis 并持久化处理
			redisService.setString(key, toJSon(message));
			redisService.getStringRedisTemplate().persist(key);
		}
	}

	@Override
	public MessageEntity<List<AccountEntity>> getMessageEntity(String host) {
		String key = RedisKeys.gen4HostMonitor(host);
		if (!redisService.getStringRedisTemplate().hasKey(key)) {
			return null;
		}
		String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
		MessageEntity<List<AccountEntity>> messageEntity = readValue(val);
		return messageEntity;
	}

	@Override
	public void upgradeByCommand() {
		MessageEntity o = new MessageEntity();
		o.setAction(ActionEventEnum.UPGRADE.ordinal());
		messageBroadCast(o);
	}

	@Override
	public void startByCommand(int accountId) {
		executeCommand(accountId, ActionEventEnum.START);
	}

	@Override
	public void stopByCommand(int accountId) {
		executeCommand(accountId, ActionEventEnum.STOP);
	}

	@Override
	public void pauseByCommand(int accountId) {
		executeCommand(accountId, ActionEventEnum.PAUSE);
	}

	@Override
	public void resumeByCommand(int accountId) {
		executeCommand(accountId, ActionEventEnum.RESUME);
	}

	@Override
	public void changeMode(int accountId, ActionEventEnum command) {
		executeCommand(accountId, command);
	}

	@Override
	public void addAccountToHost(String host, int accountId) {
		BizAccount dbAccount = accountService.getById(accountId);
		if (dbAccount == null || StringUtils.isBlank(host)) {
			return;
		}
		String key = RedisKeys.gen4HostMonitor(host);
		if (!redisService.getStringRedisTemplate().hasKey(key)) {
			return;
		}
		// 修改账号表gps字段
		accountService.updateGPS(accountId, host);
		String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
		MessageEntity<List<AccountEntity>> messageEntity = readValue(val);
		if (messageEntity == null) {
			messageEntity = new MessageEntity<List<AccountEntity>>();
		}
		messageEntity.setIp(host);
		List<AccountEntity> accountList = messageEntity.getData();
		if (accountList == null) {
			accountList = new ArrayList<>();
		}
		AccountEntity entity = buildAccountEntity(new AccountEntity(), dbAccount);
		accountList.add(entity);
		messageEntity.setData(accountList);
		redisService.setString(key, toJSon(messageEntity));
		redisService.getStringRedisTemplate().persist(key);
		// 消息发送
		MessageEntity<AccountEntity> sentObj = new MessageEntity<AccountEntity>();
		sentObj.setAction(ActionEventEnum.ADD.ordinal());
		sentObj.setData(entity);
		sentObj.setIp(host);
		messageBroadCast(sentObj);
	}

	@Override
	public void removeAccountFromHost(String host, int accountId) {
		if (StringUtils.isBlank(host)) {
			return;
		}
		String key = RedisKeys.gen4HostMonitor(host);
		if (!redisService.getStringRedisTemplate().hasKey(key)) {
			return;
		}
		// 清空账号的gps信息
		accountService.updateGPS(accountId, null);
		String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
		MessageEntity<List<AccountEntity>> messageEntity = readValue(val);
		if (messageEntity == null) {
			messageEntity = new MessageEntity<>();
		}
		messageEntity.setIp(host);
		List<AccountEntity> accountList = messageEntity.getData();
		if (accountList == null) {
			accountList = new ArrayList<>();
		}
		List<AccountEntity> targetList = accountList.stream().filter((p) -> p.getId() == accountId)
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(targetList)) {
			return;
		}
		accountList = accountList.stream().filter((p) -> p.getId() != accountId).collect(Collectors.toList());
		messageEntity.setData(accountList);
		redisService.setString(key, toJSon(messageEntity));
		redisService.getStringRedisTemplate().persist(key);
		// 消息发送
		MessageEntity<Integer> sentObj = new MessageEntity<Integer>();
		sentObj.setAction(ActionEventEnum.DEL.ordinal());
		sentObj.setData(accountId);
		sentObj.setIp(host);
		messageBroadCast(sentObj);
		// 取消此账号在出款任务排队
		allocateOutwardTaskService.cancelQueue4Robot(accountId);
	}

	@Override
	public void alterSignAndHook(String host, int accountId, String sign, String hook, String hub, String bing,
			Integer interval) throws Exception {
		if (StringUtils.isAnyBlank(host, sign, hook, hub)) {
			throw new Exception("主机,登录账号/密码或支付密码为空.");
		}
		BizAccount account = accountRepository.findById2(accountId);
		if (account == null) {
			throw new Exception("账户不存在.");
		}
		if (StringUtils.isNotEmpty(sign)) {
			account.setSign_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(sign)));
		}
		if (StringUtils.isNotEmpty(hook)) {
			account.setHook_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(hook)));
		}
		if (StringUtils.isNotEmpty(hub)) {
			account.setHub_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(hub)));
		}
		if (StringUtils.isNotEmpty(bing)) {
			account.setBing_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(bing)));
		}
		account.setInterval(interval);
		accountRepository.saveAndFlush(account);
		String pattern4Account = RedisKeys.genPattern4HostMonitor();
		String prefixKey = pattern4Account.replace("*", StringUtils.EMPTY);
		String targetKey = null;
		MessageEntity<List<AccountEntity>> targetMessage = null;
		AccountEntity targetAccount = null;
		Set<String> keys = redisService.getStringRedisTemplate().keys(pattern4Account);
		if (!CollectionUtils.isEmpty(keys)) {
			for (String key : keys) {
				String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
				MessageEntity<List<AccountEntity>> message = readValue(val);
				if (message != null && !CollectionUtils.isEmpty(message.getData())) {
					for (AccountEntity p : message.getData()) {
						if (p.getId() == accountId) {
							targetKey = key;
							targetMessage = message;
							targetAccount = p;
							break;
						}
					}
				}
				if (StringUtils.isNotBlank(targetKey) && targetMessage != null && targetAccount != null) {
					break;
				}
			}
		}
		if (StringUtils.isNotBlank(targetKey) && targetMessage != null && targetAccount != null) {
			targetMessage.setIp(targetKey.replace(prefixKey, StringUtils.EMPTY));
			targetAccount = buildAccountEntity(targetAccount, account);
			redisService.setString(targetKey, toJSon(targetMessage));
			redisService.getStringRedisTemplate().persist(targetKey);
			// 发送消息
			MessageEntity<AccountEntity> sentObj = new MessageEntity<>();
			sentObj.setAction(ActionEventEnum.UPDATE.ordinal());
			sentObj.setData(targetAccount);
			sentObj.setIp(targetMessage.getIp());
			messageBroadCast(sentObj);
			return;
		}
		addAccountToHost(host, accountId);
	}

	@Override
	public void alterSignAndHook(int accountId, String sign, String hook, String hub, String bing) throws Exception {
		BizAccount account = accountRepository.findById2(accountId);
		if (account == null) {
			throw new Exception("账户不存在.");
		}
		String host = findHostByAcc(accountId);
		if (StringUtils.isBlank(host)) {
			if (StringUtils.isNotEmpty(sign)) {
				account.setSign_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(sign)));
			}
			if (StringUtils.isNotEmpty(hook)) {
				account.setHook_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(hook)));
			}
			if (StringUtils.isNotEmpty(hub)) {
				account.setHub_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(hub)));
			}
			if (StringUtils.isNotEmpty(bing)) {
				account.setBing_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(bing)));
			}
			accountRepository.saveAndFlush(account);
		} else {
			alterSignAndHook(host, accountId, sign, hook, hub, bing, null);
		}

	}

	@Override
	public void updateIinterval(String host, int accountId, Integer interval) throws Exception {
		if (StringUtils.isAnyBlank(host)) {
			throw new Exception("主机为空.");
		}
		BizAccount account = accountRepository.findById2(accountId);
		if (account == null) {
			throw new Exception("账户不存在.");
		}
		account.setInterval(interval);
		String pattern4Account = RedisKeys.genPattern4HostMonitor();
		String prefixKey = pattern4Account.replace("*", StringUtils.EMPTY);
		String targetKey = null;
		MessageEntity<List<AccountEntity>> targetMessage = null;
		AccountEntity targetAccount = null;
		Set<String> keys = redisService.getStringRedisTemplate().keys(pattern4Account);
		if (!CollectionUtils.isEmpty(keys)) {
			for (String key : keys) {
				String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
				MessageEntity<List<AccountEntity>> message = readValue(val);
				if (message != null && !CollectionUtils.isEmpty(message.getData())) {
					for (AccountEntity p : message.getData()) {
						if (p.getId() == accountId) {
							targetKey = key;
							targetMessage = message;
							targetAccount = p;
							break;
						}
					}
				}
				if (StringUtils.isNotBlank(targetKey) && targetMessage != null && targetAccount != null) {
					break;
				}
			}
		}
		if (StringUtils.isNotBlank(targetKey) && targetMessage != null && targetAccount != null) {
			targetMessage.setIp(targetKey.replace(prefixKey, StringUtils.EMPTY));
			targetAccount = buildAccountEntity(targetAccount, account);
			redisService.setString(targetKey, toJSon(targetMessage));
			redisService.getStringRedisTemplate().persist(targetKey);
			// 发送消息
			MessageEntity<AccountEntity> sentObj = new MessageEntity<>();
			sentObj.setAction(ActionEventEnum.UPDATE.ordinal());
			sentObj.setData(targetAccount);
			sentObj.setIp(targetMessage.getIp());
			messageBroadCast(sentObj);
			return;
		}
		addAccountToHost(host, accountId);
	}

	@Override
	public void update(BizAccount bizAccount) {
		String targetKey = null;
		MessageEntity<List<AccountEntity>> targetMessage = null;
		AccountEntity targetAccount = null;
		String pattern4Account = RedisKeys.genPattern4HostMonitor();
		String prefixKey = pattern4Account.replace("*", StringUtils.EMPTY);
		// 读取全部主机IP
		Set<String> keys = redisService.getStringRedisTemplate().keys(RedisKeys.genPattern4HostMonitor());
		// 循环每个主机
		for (String key : keys) {
			MessageEntity<List<AccountEntity>> msg;
			// 读取单个主机全部信息
			String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
			// 非空判断同时将val转换成msg的实体类
			if (StringUtils.isBlank(val) || (msg = readValue(val)) == null || CollectionUtils.isEmpty(msg.getData())) {
				continue;
			}
			// 循环判断当前主机是否包含了更新的账号
			List<AccountEntity> filtered = msg.getData().stream()
					.filter(p -> p.getId().intValue() == bizAccount.getId().intValue()).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(filtered)) {
				continue;
			}
			for (AccountEntity p : filtered) {
				if (p.getId().intValue() == bizAccount.getId().intValue()) {
					targetKey = key;
					targetMessage = msg;
					targetAccount = p;
					break;
				}
			}
			// 找到主机信息则无需寻找下一个主机
			if (targetMessage != null && targetAccount != null) {
				break;
			}
		}
		// 账号不存在主机信息中，则无需更新redis
		if (targetMessage == null || targetAccount == null) {
			return;
		}
		// 更新redis账号信息
		String IP = targetKey.replace(prefixKey, StringUtils.EMPTY);
		targetMessage.setIp(IP);
		targetAccount.setBank(bizAccount.getBankType());
		String account = bizAccount.getAccount();
		int type = bizAccount.getType();
		if (type != AccountType.InThird.getTypeId() && type != AccountType.OutThird.getTypeId()) {
			account = CommonUtils.transToStarString(bizAccount.getAccount());
		}
		targetAccount.setAccount(account);
		targetAccount.setOwner(bizAccount.getOwner());
		targetAccount.setHolder(bizAccount.getHolder());
		targetAccount.setType(bizAccount.getType());
		targetAccount.setAlias(bizAccount.getAlias());
		targetAccount.setStatus(bizAccount.getStatus());
		targetAccount.setLimitIn(bizAccount.getLimitIn());
		targetAccount.setLimitOut(bizAccount.getLimitOut());
		targetAccount.setLimitOutOne(bizAccount.getLimitOutOne());
		targetAccount.setLimitBalance(bizAccount.getLimitBalance());
		targetAccount.setLowestOut(bizAccount.getLowestOut());
		targetAccount.setFlag(bizAccount.getFlag());
		targetAccount.setMobile(bizAccount.getMobile());
		// 当日入款上限；出款限额为空时推送系统设置
		Map<String, String> systemSetting = new HashMap<>();
		systemSetting = MemCacheUtils.getInstance().getSystemProfile();
		targetAccount.setLimitIn(bizAccount.getLimitIn() != null ? bizAccount.getLimitIn()
				: Integer.parseInt(systemSetting.get("FINANCE_ACCOUNT_BALANCE_ALARM")));
		targetAccount.setLimitOut(bizAccount.getLimitOut() != null ? bizAccount.getLimitOut()
				: Integer.parseInt(systemSetting.get("INCOME_LIMIT_CHECKIN_TODAY")));
		targetAccount.setLimitOutOne(bizAccount.getLimitOutOne() != null ? bizAccount.getLimitOutOne()
				: Integer.parseInt(systemSetting.get("OUTDRAW_LIMIT_OUT_ONE")));
		targetAccount.setLimitBalance(bizAccount.getLimitBalance() != null ? bizAccount.getLimitBalance()
				: Integer.parseInt(systemSetting.get("OUTDRAW_LIMIT_CHECKOUT_TODAY")));
		targetAccount.setLowestOut(bizAccount.getLowestOut() != null ? bizAccount.getLowestOut()
				: Integer.parseInt(systemSetting.get("OUTDRAW_SYSMONEY_LOWEST")));
		redisService.setString(targetKey, toJSon(targetMessage));
		redisService.getStringRedisTemplate().persist(targetKey);
		// 发送消息
		MessageEntity<AccountEntity> sentObj = new MessageEntity<>();
		sentObj.setAction(ActionEventEnum.UPDATE.ordinal());
		sentObj.setData(targetAccount);
		sentObj.setIp(targetMessage.getIp());
		messageBroadCast(sentObj);
	}

	@Override
	public List<AccountEntity> findAccountEntityList(String host) {
		String key = RedisKeys.gen4HostMonitor(host);
		String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
		MessageEntity<List<AccountEntity>> messageEntity = readValue(val);
		return messageEntity != null ? messageEntity.getData() : new ArrayList<>();
	}

	@Override
	public List<Integer> findAllAccountIdList() {
		List<Integer> result = new ArrayList<>();
		Set<String> keys = redisService.getStringRedisTemplate().keys(RedisKeys.genPattern4HostMonitor());
		if (CollectionUtils.isEmpty(keys)) {
			return result;
		}
		for (String key : keys) {
			String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
			MessageEntity<List<AccountEntity>> message = readValue(val);
			if (message != null && !CollectionUtils.isEmpty(message.getData())) {
				message.getData().forEach((p) -> result.add(Integer.valueOf(p.getId().toString())));
			}
		}
		return result;
	}

	@Override
	public void messageBroadCast(MessageEntity o) {
		if (o == null) {
			return;
		}
		try {
			String msg = mapper.writeValueAsString(o);
			log.debug("主机监控 push 消息：", msg);
			redisService.convertAndSend(RedisTopics.PUSH_MESSAGE_TOOLS, msg);
		} catch (Exception e) {
			log.error("messageBroadCast 错误：" + e);
		}
	}

	private <T> T readValue(String jsonStr) {
		try {
			return mapper.readValue(jsonStr, new TypeReference<MessageEntity<List<AccountEntity>>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String toJSon(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 向客户端发送指令：pause,resume,stop,start
	 */
	private void executeCommand(Integer accountId, ActionEventEnum command) {
		if (accountId == null || command == null) {
			return;
		}
		String pattern4Account = RedisKeys.genPattern4HostMonitor();
		String prefixKey = pattern4Account.replace("*", StringUtils.EMPTY);
		Set<String> keys = redisService.getStringRedisTemplate().keys(pattern4Account);
		if (CollectionUtils.isEmpty(keys)) {
			return;
		}
		for (String key : keys) {
			String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
			if (StringUtils.isBlank(val)) {
				continue;
			}
			MessageEntity<List<AccountEntity>> message = readValue(val);
			if (message == null || CollectionUtils.isEmpty(message.getData())) {
				continue;
			}
			AccountEntity target = null;
			for (AccountEntity accountEntity : message.getData()) {
				if (accountEntity.getId() != accountId.intValue()) {
					continue;
				}
				target = accountEntity;
				break;
			}
			if (target == null) {
				continue;
			}
			redisService.setString(key, toJSon(message));
			redisService.getStringRedisTemplate().persist(key);
			// 发送消息
			MessageEntity<Integer> sentObj = new MessageEntity<>();
			sentObj.setAction(command.ordinal());
			sentObj.setData(accountId);
			String host = key.replace(prefixKey, StringUtils.EMPTY);
			sentObj.setIp(host);
			messageBroadCast(sentObj);
			break;
		}
	}

	private AccountEntity buildAccountEntity(AccountEntity entity, BizAccount dbAccount) {
		if (dbAccount == null) {
			return entity;
		}
		int type = dbAccount.getType();
		String account = dbAccount.getAccount();
		if (type != AccountType.InThird.getTypeId() && type != AccountType.OutThird.getTypeId()) {
			account = CommonUtils.transToStarString(dbAccount.getAccount());
		}
		Map<String, String> systemSetting = new HashMap<>();
		systemSetting = MemCacheUtils.getInstance().getSystemProfile();
		entity = entity != null ? entity : new AccountEntity();
		entity.setId(dbAccount.getId());
		entity.setBank(dbAccount.getBankType());
		entity.setAccount(account);
		entity.setOwner(dbAccount.getOwner());
		entity.setSign(dbAccount.getSign());
		entity.setHook(dbAccount.getHook());
		entity.setHub(dbAccount.getHub());
		entity.setBing(dbAccount.getBing());

		entity.setSign_(dbAccount.getSign_());
		entity.setHook_(dbAccount.getHook_());
		entity.setHub_(dbAccount.getHub_());
		entity.setBing_(dbAccount.getBing_());

		entity.setHolder(dbAccount.getHolder());
		entity.setType(type);
		entity.setInterval(dbAccount.getInterval());
		entity.setStatus(dbAccount.getStatus());
		entity.setAlias(dbAccount.getAlias());
		// 当日入款上限；出款限额为空时推送系统设置
		entity.setLimitIn(dbAccount.getLimitIn() != null ? dbAccount.getLimitIn()
				: Integer.parseInt(systemSetting.get("FINANCE_ACCOUNT_BALANCE_ALARM")));
		entity.setLimitOut(dbAccount.getLimitOut() != null ? dbAccount.getLimitOut()
				: Integer.parseInt(systemSetting.get("INCOME_LIMIT_CHECKIN_TODAY")));
		entity.setLimitOutOne(dbAccount.getLimitOutOne() != null ? dbAccount.getLimitOutOne()
				: Integer.parseInt(systemSetting.get("OUTDRAW_LIMIT_OUT_ONE")));
		entity.setLimitBalance(dbAccount.getLimitBalance() != null ? dbAccount.getLimitBalance()
				: Integer.parseInt(systemSetting.get("OUTDRAW_LIMIT_CHECKOUT_TODAY")));
		entity.setLowestOut(dbAccount.getLowestOut() != null ? dbAccount.getLowestOut()
				: Integer.parseInt(systemSetting.get("OUTDRAW_SYSMONEY_LOWEST")));
		// entity.setLowestOut(dbAccount.getLowestOut());
		return entity;
	}

	@Override
	public String findHostByAcc(Integer accId) {
		if (accId == null) {
			return "";
		}
		Set<String> keys = redisService.getStringRedisTemplate().keys(RedisKeys.genPattern4HostMonitor());
		if (CollectionUtils.isEmpty(keys)) {
			return "";
		}
		String prefix = RedisKeys.genPattern4HostMonitor().replace("*", StringUtils.EMPTY);
		for (String key : keys) {
			if (!redisService.getStringRedisTemplate().hasKey(key)) {
				continue;
			}
			String val = redisService.getStringRedisTemplate().boundValueOps(key).get();
			MessageEntity<List<AccountEntity>> message = new MessageEntity<>();
			if (StringUtils.isNotBlank(val)) {
				message = readValue(val);
			}
			boolean exists = !CollectionUtils.isEmpty(message.getData())
					&& (message.getData().stream().filter((p) -> p.getId().compareTo(accId) == 0).count() > 0);
			if (exists) {
				return key.replace(prefix, StringUtils.EMPTY);
			}
		}
		return "OK";
	}

}
