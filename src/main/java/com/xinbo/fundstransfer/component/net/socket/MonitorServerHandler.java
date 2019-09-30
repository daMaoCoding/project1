package com.xinbo.fundstransfer.component.net.socket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.runtime.task.ToolResponseData;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import com.xinbo.fundstransfer.service.RedisService;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class MonitorServerHandler extends IoHandlerAdapter {
	private static final Logger log = LoggerFactory.getLogger(MonitorServerHandler.class);
	public static String SESSION_CLIENTIP_KEY = "KEY_SESSION_CLIENT_IP";
	private RedisService redisService;
	private AllocateOutwardTaskService allocateOutwardTaskService;
	private AccountService accountService;
	private ObjectMapper mapper = new ObjectMapper();
	/**
	 * 缓存帐号对应的主机连接会话，key:帐号ID，value:主机IP所对应SessionID
	 */
	private ConcurrentHashMap<Integer, Long> accountSessionMap = new ConcurrentHashMap<>();
	/**
	 * 连接通知
	 */
	private static final String SOCEKT_CONNECT_EVENT = "SOCEKT_CONNECT_EVENT";
	/**
	 * 银行流水事件通知
	 */
	private static final String SOCEKT_BANKLOG_EVENT = "SOCEKT_BANKLOG_EVENT";
	/**
	 * 第三方流水事件通知
	 */
	private static final String SOCEKT_THIRDLOG_EVENT = "SOCEKT_THIRDLOG_EVENT";
	/**
	 * constant: acknowledge bank flow log
	 */
	private static String CONSTANT_ACK_LOG;

	private static String thisTimeStamp;

	// static {
	// try {
	// ObjectMapper mapper = new ObjectMapper();
	// MessageEntity en = new MessageEntity();
	// en.setAction(ActionEventEnum.ACKLOG.ordinal());
	// en.setData(thisTimeStamp);
	// CONSTANT_ACK_LOG = mapper.writeValueAsString(en);
	// } catch (Exception e) {
	// log.info(" initialize error:", e);
	// }
	// }

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		log.error("Socket server exception.", cause);
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		super.sessionCreated(session);
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		super.sessionOpened(session);
	}

	@Override
	public void sessionClosed(IoSession session) {
		try {
			// 删除缓存
			List<Integer> accountIdList = new ArrayList<>();
			if (accountSessionMap.containsValue(session.getId())) {
				Iterator<Entry<Integer, Long>> iterator = accountSessionMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Integer, Long> entry = iterator.next();
					if (entry.getValue().equals(session.getId())) {
						log.info("{}会话断开，删除缓存 帐号：{}", session.getAttribute(MonitorServerHandler.SESSION_CLIENTIP_KEY),
								entry.getKey());
						accountSessionMap.remove(entry.getKey());
						accountIdList.add(entry.getKey());
					}
				}
			}
			super.sessionClosed(session);
			// 取消该账号在出款任务中的等待排队
			if (!CollectionUtils.isEmpty(accountIdList)) {
				if (allocateOutwardTaskService == null) {
					allocateOutwardTaskService = SpringContextUtils.getBean(AllocateOutwardTaskService.class);
				}
				allocateOutwardTaskService.cancelQueue4Robot(accountIdList);
			}
			log.error("IP {} session closed.", session.getAttribute(SESSION_CLIENTIP_KEY));
			MessageEntity<Integer> messageEntity = new MessageEntity<>();
			messageEntity.setAction(-1);
			messageEntity.setIp(String.valueOf(session.getAttribute(SESSION_CLIENTIP_KEY)));
			if (null == redisService) {
				redisService = SpringContextUtils.getBean(RedisService.class);
			}
			redisService.convertAndSend(RedisTopics.TOOLS_STATUS_REPORT, mapper.writeValueAsString(messageEntity));
		} catch (Exception e) {
			log.error("sessionClosed error:", e);
		}
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		String json = String.valueOf(message);
		log.debug("received tools msg:{}", json);
		if (json.startsWith(SOCEKT_BANKLOG_EVENT)) {
			// boolean add = MemCacheUtils.getInstance().getBanklogs()
			// .offer(json.substring(SOCEKT_BANKLOG_EVENT.length()));
			redisService.rightPush(RedisTopics.BANK_STATEMENT, json.substring(SOCEKT_BANKLOG_EVENT.length()));
			log.debug("流水：{},添加到并发队列中成功:{}", json.substring(SOCEKT_BANKLOG_EVENT.length()), true);
			ToolResponseData data = mapper.readValue(json.substring(SOCEKT_BANKLOG_EVENT.length()),
					ToolResponseData.class);
			ObjectMapper mapper = new ObjectMapper();
			MessageEntity en = new MessageEntity();
			en.setAction(ActionEventEnum.ACKLOG.ordinal());
			if (!"".equals(data.getThisTimeStamp()) && null != data.getThisTimeStamp()) {
				thisTimeStamp = data.getThisTimeStamp();
				en.setData(thisTimeStamp);
			}
			CONSTANT_ACK_LOG = mapper.writeValueAsString(en);
			session.write(CONSTANT_ACK_LOG);
		} else if (json.startsWith(SOCEKT_THIRDLOG_EVENT)) {
			MemCacheUtils.getInstance().getThirdlogs().offer(json.substring(SOCEKT_THIRDLOG_EVENT.length()));
		} else if (json.startsWith(SOCEKT_CONNECT_EVENT)) {
			log.info("事件类型：工具请求获取帐号信息");
			String clientIP = json.substring(SOCEKT_CONNECT_EVENT.length());
			session.setAttribute(SESSION_CLIENTIP_KEY, clientIP);
			if (null == redisService) {
				redisService = SpringContextUtils.getBean(RedisService.class);
			}
			if (null == accountService) {
				accountService = SpringContextUtils.getBean(AccountService.class);
			}
			if (!redisService.getStringRedisTemplate().hasKey("Host:" + clientIP)) {
				MessageEntity<Integer> o = new MessageEntity<>();
				o.setIp(clientIP);
				redisService.setString("Host:" + clientIP, mapper.writeValueAsString(o));
				// 移除 key 的过期时间，key 将持久保持
				redisService.getStringRedisTemplate().persist("Host:" + clientIP);
				log.info("Add new host, ip is {}", clientIP);
			}
			// 服务端返回此IP的配置
			String config = redisService.getString("Host:" + clientIP);
			log.info("IP {} connected, request task.configuration information is [{}]", clientIP, config);
			if (StringUtils.isNotEmpty(config)) {
				MessageEntity<List<AccountEntity>> messageEntity = mapper.readValue(config,
						new TypeReference<MessageEntity<List<AccountEntity>>>() {
						});
				messageEntity.setAction(ActionEventEnum.ACKNOWLEDGED.ordinal());
				if (!CollectionUtils.isEmpty(messageEntity.getData())) {
					for (AccountEntity entity : messageEntity.getData()) {
						String sign_ = StringUtils.trimToNull(entity.getSign_());
						if (sign_ != null) {
							sign_ = FundTransferEncrypter.decryptDb(sign_);
							sign_ = FundTransferEncrypter.encryptPc(sign_);
							entity.setSign_(sign_);
							entity.setSign(StringUtils.EMPTY);
						}
						String bing_ = StringUtils.trimToNull(entity.getBing_());
						if (bing_ != null) {
							bing_ = FundTransferEncrypter.decryptDb(bing_);
							bing_ = FundTransferEncrypter.encryptPc(bing_);
							entity.setBing_(bing_);
							entity.setBing(StringUtils.EMPTY);
						}
						String hook_ = StringUtils.trimToNull(entity.getHook_());
						if (hook_ != null) {
							hook_ = FundTransferEncrypter.decryptDb(hook_);
							hook_ = FundTransferEncrypter.encryptPc(hook_);
							entity.setHook_(hook_);
							entity.setHook(StringUtils.EMPTY);
						}
						String hub_ = StringUtils.trimToNull(entity.getHub_());
						if (hub_ != null) {
							hub_ = FundTransferEncrypter.decryptDb(hub_);
							hub_ = FundTransferEncrypter.encryptPc(hub_);
							entity.setHub_(hub_);
							entity.setHub(StringUtils.EMPTY);
						}
					}
				}
				config = mapper.writeValueAsString(messageEntity);
				session.write(config);
				if (null != messageEntity.getData()) {
					// 添加帐号与session的关联，后续add,delete都同步更新，以快速给指定的帐号发送操作指令
					for (AccountEntity o : messageEntity.getData()) {
						ActionEventEnum event = accountService.getModel4PC(o.getId());
						if (Objects.nonNull(event)) {
							MessageEntity<Integer> sentObj = new MessageEntity<>();
							sentObj.setAction(event.ordinal());
							sentObj.setData(o.getId());
							session.write(mapper.writeValueAsString(sentObj));
							// 缓存在线账号id
							accountService.saveOnlineAccontIds(o.getId(), true);
						}
						accountSessionMap.put(o.getId(), session.getId());
					}
				}
			}
		} else {
			// 状态上报，广播消息，有此IP的连接上报连接状态，约定消息格式MessageEntity实体类json串
			log.debug("事件类型：状态上报，并取消离线账号在出款任务中等待排队");
			// 取消离线账号在出款任务中等待排队
			JavaType inType = mapper.getTypeFactory().constructParametricType(ArrayList.class, AccountEntity.class);
			JavaType type = mapper.getTypeFactory().constructParametricType(MessageEntity.class, inType);
			MessageEntity<List<AccountEntity>> entity = mapper.readValue(json, type);
			if (!CollectionUtils.isEmpty(entity.getData())) {
				List<Integer> accountIdList = new ArrayList<>();
				// filter(p -> p.getRunningStatus() !=
				// RunningStatusEnum.NORMAL.ordinal())
				entity.getData().stream().forEach(p -> {
					if (p.getRunningStatus() != RunningStatusEnum.NORMAL.ordinal()) {
						accountIdList.add(p.getId());
					}
					if (p.getRunningStatus() == RunningStatusEnum.OFFLINE.ordinal()) {
						log.debug("事件类型：状态上报，离线:{}", p.getId());
						if (null != p.getFlag() && p.getFlag().intValue() == 2
								&& p.getSubType() == InBankSubType.IN_BANK_YSF.getSubType()) {
							accountService.savePauseOrResumeOrOnlineForMobile(p.getId(), 999);
						} else {
							// 离线 删除缓存在线账号id
							accountService.saveOnlineAccontIds(p.getId(), false);
							// 删除暂停状态
							accountService.savePauseOrResumeAccountId(p.getId(), 22);
						}

					}
					if (p.getRunningStatus() == RunningStatusEnum.PAUSE.ordinal()) {
						log.debug("事件类型：状态上报，暂停:{}", p.getId());
						if (null != p.getFlag() && p.getFlag().intValue() == 2
								&& p.getSubType() == InBankSubType.IN_BANK_YSF.getSubType().intValue()) {
							accountService.savePauseOrResumeOrOnlineForMobile(p.getId(), 88);
						} else {
							// 缓存暂停
							accountService.savePauseOrResumeAccountId(p.getId(), 88);
							// 在线转暂停 则删除在线
							accountService.saveOnlineAccontIds(p.getId(), false);
						}

					}
					if (p.getRunningStatus() == RunningStatusEnum.NORMAL.ordinal()) {
						log.debug("事件类型：状态上报，暂停恢复:{}", p.getId());
						// 暂停恢复
						if (null != p.getFlag() && p.getFlag().intValue() == 2
								&& p.getSubType() == InBankSubType.IN_BANK_YSF.getSubType().intValue()) {
							accountService.savePauseOrResumeOrOnlineForMobile(p.getId(), 22);
						} else {
							accountService.savePauseOrResumeAccountId(p.getId(), 22);
							// 离线恢复
							accountService.saveOnlineAccontIds(p.getId(), true);
						}
					}

				});
				if (!CollectionUtils.isEmpty(accountIdList)) {
					if (allocateOutwardTaskService == null) {
						allocateOutwardTaskService = SpringContextUtils.getBean(AllocateOutwardTaskService.class);
					}
					allocateOutwardTaskService.cancelQueue4Robot(accountIdList);
				}
			}
			// Redis广播 状态上报信息
			redisService.convertAndSend(RedisTopics.TOOLS_STATUS_REPORT, json);
		}
	}

	public Map<Integer, Long> getAccountSessionMap() {
		return accountSessionMap;
	}

	public void setAccountSessionMap(ConcurrentHashMap<Integer, Long> accountSessionMap) {
		this.accountSessionMap = accountSessionMap;
	}

}