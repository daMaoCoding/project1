/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.SysUserProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * 云闪付本地缓存工具类
 * @author blake
 *
 */
@Slf4j
@Component("YSFLocalCacheUtil")
public class YSFLocalCacheUtil {
	
	@Autowired
	private SysUserProfileService sysUserProfileService;
	
	private ObjectMapper mapper = null;
	private ConcurrentHashMap<String, Long>				deviceLastUsedTime	= new ConcurrentHashMap<String, Long>();
	
	private static final String YSF_COMMON_MONEY_COUNT="YSF_COMMON_MONEY_COUNT";
	/**
	 * 银联云闪付常用金额生成二维码个数<br>
	 * 5分钟更新一次<br>
	 * 结构如下：
	 * <pre>
	 * {
	 * 	0:{
	 * 		"10":5,   金额:随机数数量
	 * 		"100":50
	 * 	}
	 * }
	 * </pre>
	 */
	private LoadingCache<Long, Map<Integer,Integer>> ysfMoneyRandCountCache;
	
	private static final String YSF_COMMON_QR_LOCK_HOUR="YSF_COMMON_QR_LOCK_HOUR";
	/**
	 * 银联云闪付二维码锁定时间，单为：小时<br>
	 *  5分钟更新一次
	 * <pre>
	 * 二维码被取用后，如果对应订单没有取消或者确认，则二维码经过该时间后变为可用 
	 * 取配置表 SysUserProfile 中 YSF_COMMON_QR_LOCK_HOUR
	 * </pre>
	 */
	private LoadingCache<Long, Integer> ysfQrCodeLockTimeCache;
	
	private static final String YSF_COMMON_QR_DECODE_FLAG = "YSF_COMMON_QR_DECODE_FLAG";
	/**
	 * 是否解码app传递过来的二维码内容<br>
	 *  5分钟更新一次
	 * <pre>
	 * 取配置表 SysUserProfile 中 YSF_COMMON_QR_DECODE_FLAG
	 * </pre>
	 */
	private LoadingCache<Long, Boolean> ysfQrCodeDecodeFlagCache;
	
	/**
	 * 平台请求银联云闪付二维码处理超时时间<br>
	 * 取配置表 SysUserProfile 中 YSF_COMMON_PLAT_REQUEST_TIME_OUT
	 */
	private static final String YSF_COMMON_PLAT_REQUEST_TIME_OUT = "YSF_COMMON_PLAT_REQUEST_TIME_OUT";
	/**
	 * 平台请求银联云闪付二维码处理超时时间缓存<br>
	 *  5分钟更新一次
	 * <pre>
	 * 取配置表 SysUserProfile 中 YSF_COMMON_PLAT_REQUEST_TIME_OUT
	 * </pre>
	 */
	private LoadingCache<Long, Long> ysfQrPlatRequestTimeoutCache;
	
	/**
	 * 云闪付二维码金额增长范围<br>
	 * 取配置表 SysUserProfile 中 YSF_COMMON_MONEY_INCREMENT
	 */
	private static final String YSF_COMMON_MONEY_INCREMENT = "YSF_COMMON_MONEY_INCREMENT";
	/**
	 * 云闪付二维码金额增长范围缓存<br>
	 *  5分钟更新一次
	 * <pre>
	 * 取配置表 SysUserProfile 中 YSF_COMMON_MONEY_INCREMENT
	 * </pre>
	 */
	private LoadingCache<Long, Integer> ysfQrMoneyIncrementCache;
	
	/**
	 * 获取生成二维码超时后更换银行卡次数<br>
	 * 取配置表 SysUserProfile 中 TIME_OUT_CHANGE_BANKACCOUNT_COUNT
	 */
	private static final String TIME_OUT_CHANGE_BANKACCOUNT_COUNT = "TIME_OUT_CHANGE_BANKACCOUNT_COUNT";
	/**
	 * 获取生成二维码超时后更换银行卡次数缓存<br>
	 *  5分钟更新一次
	 * <pre>
	 * 取配置表 SysUserProfile 中 TIME_OUT_CHANGE_BANKACCOUNT_COUNT
	 * </pre>
	 */
	private LoadingCache<Long, Integer> timeOutChangeBankAccountCountCache;
	
	@PostConstruct
	public void initialize() {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		ysfMoneyRandCountCache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<Long, Map<Integer,Integer>>() {
			@Override
			public Map<Integer,Integer> load(Long key) throws Exception{
				try {
					log.debug("从redis中更新 业主未确认出款金额开关");
					Map<Integer,Integer> result = new HashMap<>();
					//查询系统配置的银联云闪付常用金额二维码个数配置
					List<SysUserProfile> sysUserProfile=	sysUserProfileService.findByPropertyKey(YSF_COMMON_MONEY_COUNT);
					if(!CollectionUtils.isEmpty(sysUserProfile)) {
						SysUserProfile config = sysUserProfile.iterator().next();
						if(config!=null && !StringUtils.isEmpty(config.getPropertyValue())) {
							ObjectMapper mapper = new ObjectMapper();
							mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
							try {
								Map<Integer,Integer> tmp = mapper.readValue(config.getPropertyValue(), mapper.getTypeFactory().constructMapType(HashMap.class, Integer.class, Integer.class));
								if(!ObjectUtils.isEmpty(tmp)) {
									result.putAll(tmp);
								}
							} catch (IOException e) {
								log.error("读取数据库中银联云闪付常用金额二维码个数配置时，产生异常",e);
							}
						}
					}
					return result;
				} finally {
					
				}
			}
		});	
		ysfQrCodeLockTimeCache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<Long, Integer>() {
			@Override
			public Integer load(Long key) throws Exception{
				try {
					Integer result = null;
					List<SysUserProfile> sysUserProfile = sysUserProfileService.findByPropertyKey(YSF_COMMON_QR_LOCK_HOUR);
					if(!CollectionUtils.isEmpty(sysUserProfile)) {
						String lockHourStr = sysUserProfile.iterator().next().getPropertyValue();
						if(!StringUtils.isEmpty(lockHourStr) && StringUtils.isNumeric(lockHourStr)) {
							result = Integer.parseInt(lockHourStr);
						}
					}
					//如果未配置该参数，默认2小时
					if(result == null) {
						result = 2;
					}
					return result;
				} finally {
					
				}
			}
		});	
		
		ysfQrCodeDecodeFlagCache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<Long, Boolean>() {
			@Override
			public Boolean load(Long key) {
				//因为最开始约定是服务的要解码，所以默认是 需要解码
				Boolean result = true;
				List<SysUserProfile> sysUserProfile = sysUserProfileService.findByPropertyKey(YSF_COMMON_QR_DECODE_FLAG);
				if(!CollectionUtils.isEmpty(sysUserProfile)) {
					String decodeFlagStr = sysUserProfile.iterator().next().getPropertyValue();
					//当没有配置时，默认不需要解码
					result = "1".equals(decodeFlagStr);
				}
				return result;
			}
		});	
		ysfQrPlatRequestTimeoutCache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<Long, Long>() {
			@Override
			public Long load(Long key) {
				Long result = null;
				List<SysUserProfile> sysUserProfile = sysUserProfileService.findByPropertyKey(YSF_COMMON_PLAT_REQUEST_TIME_OUT);
				if(!CollectionUtils.isEmpty(sysUserProfile)) {
					String timeoutStr = sysUserProfile.iterator().next().getPropertyValue();
					result = StringUtils.isEmpty(timeoutStr)||!StringUtils.isNumeric(timeoutStr)?3L:Long.parseLong(timeoutStr);
				}
				if(ObjectUtils.isEmpty(result)) {
					result = 3L;
				}
				return result;
			}
		});	
		
		ysfQrMoneyIncrementCache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<Long, Integer>() {
			@Override
			public Integer load(Long key) {
				Integer result = null;
				List<SysUserProfile> sysUserProfile = sysUserProfileService.findByPropertyKey(YSF_COMMON_MONEY_INCREMENT);
				if(!CollectionUtils.isEmpty(sysUserProfile)) {
					String incrementStr = sysUserProfile.iterator().next().getPropertyValue();
					result = StringUtils.isEmpty(incrementStr)||!StringUtils.isNumeric(incrementStr)?0:Integer.parseInt(incrementStr);
				}
				if(ObjectUtils.isEmpty(result)) {
					result = 0;
				}
				return result;
			}
		});	
		
		timeOutChangeBankAccountCountCache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<Long, Integer>() {
			@Override
			public Integer load(Long key) {
				Integer result = null;
				List<SysUserProfile> sysUserProfile = sysUserProfileService.findByPropertyKey(TIME_OUT_CHANGE_BANKACCOUNT_COUNT);
				if(!CollectionUtils.isEmpty(sysUserProfile)) {
					String incrementStr = sysUserProfile.iterator().next().getPropertyValue();
					result = StringUtils.isEmpty(incrementStr)||!StringUtils.isNumeric(incrementStr)?0:Integer.parseInt(incrementStr);
				}
				if(ObjectUtils.isEmpty(result)) {
					result = 0;
				}
				return result;
			}
		});	
		
		long millisUntilMidnight = Duration.between(LocalDateTime.now(), LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(6, 0, 0))).toMillis();
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
			try {
				// 每天清里一次 deviceLastUsedTime
				// ，防止deviceLastUsedTime数据过多并过滤长时间不使用的设备
				deviceLastUsedTime.clear();
			} catch (Exception e) {
				log.error(e.getMessage());
			} finally {
				
			}
		}, millisUntilMidnight, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
	}

	/**
	 * 获取银行账号最近一次使用时间
	 * @param account
	 * @return 如果今日使用过，则返回使用时间，否则返回 -1
	 */
	public Long getAccountLastUseTime(String account) {
		Long result = deviceLastUsedTime.get(account);
		return result == null ? -1L : result;
	}
	
	/**
	 * 设置账号使用时间 <br>
	 * 用于接收redis消息时记录设备使用时间 <br>
	 * 其他地方请不要调用该接口
	 * @param redisMsg
	 */
	public void setAccountUseTime(String redisMsg) {
		// redisMsg 格式：{"bankAccount":"银行卡号","time":1550626414008}
		try {
			Map<String,String> msg = mapper.readValue(redisMsg, mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
			if(msg.get("bankAccount")!=null) {
				Long time = null;
				if(msg.get("time")!=null) {
					time = Long.parseLong(msg.get("time").toString());
				}else {
					time =System.currentTimeMillis();
				}
				deviceLastUsedTime.put(msg.get("bankAccount"), time);
			}
		} catch (IOException e) {
			log.error("设置云闪付账号最近使用时间时发生异常",e);
		}
	}
	
	public Map<Integer,Integer> getMoneyMaxCount() {
		Map<Integer, Integer> result = null;
		try {
			result = ysfMoneyRandCountCache.get(0L);
		} catch (ExecutionException e) {
			log.error("从本地缓存中读取云闪付常用金额生成二维码个数时发生异常",e);
		}
		if(result == null) {
			result = new HashMap<>();
		}
		return result;
	}

	/**
	 * 获取二维码锁定时间，单为 毫秒
	 * @return
	 */
	public long getYSFQrCodeLockTime() {
		Integer lockHour = null;
		try {
			lockHour = ysfQrCodeLockTimeCache.get(0L);
		} catch (Exception e) {
			log.error("本地缓存获取二维码锁定小时数时异常",e);
		}
		log.debug("本地缓存获取到的二维码锁定小时数为：{}",lockHour);
		if(lockHour==null) {
			log.debug("本地缓存获取到的二维码锁定小时数为空，取3小时");
			lockHour = 2;
		}
		return lockHour * 60 * 60 * 1000L;
	}

	/**
	 * 是否解码app传递过来的二维码内容
	 * @return true 解码 false 不解码
	 */
	public boolean decodeQrContent() {
		try {
			if(ysfQrCodeDecodeFlagCache.get(0L)!=null) {
				return ysfQrCodeDecodeFlagCache.get(0L);
			}
		} catch (ExecutionException e) {
			log.error("获取是否解码app传递过来二维码内容开关时异常",e);
		}
		return false;
	}
	
	/**
	 * 获取 “请求云闪付二维码”超时时间<br>
	 * @return 超时时间，单为秒。如果SysUserProfile中没有配置YSF_COMMON_PLAT_REQUEST_TIME_OUT，默认返回 3
	 */
	public Long getQrProcessTimeOutSeconds() {
		Long result = null;
		try {
			if(ysfQrPlatRequestTimeoutCache.get(0L)!=null) {
				result = ysfQrPlatRequestTimeoutCache.get(0L);
			}
		} catch (Exception e) {
			log.error("获取是否解码app传递过来二维码内容开关时异常",e);
			//如果异常，默认3秒
			result = 3L;
		}
		return result;
	}

	/**
	 * 云闪付二维码金额增长范围
	 * @return
	 */
	public Integer getYsfQrCodeMoneyIncrement() {
		Integer result = null;
		try {
			if(ysfQrMoneyIncrementCache.get(0L)!=null) {
				result = ysfQrMoneyIncrementCache.get(0L);
			}
		} catch (Exception e) {
			log.error("获取云闪付入库增长返回时异常",e);
			result = 0;
		}
		return result;
	}
	
	/**
	 * 获取生成二维码超时后更换银行卡次数
	 * @return
	 */
	public Integer getTimeOutChangeBankAccountCount() {
		Integer result = null;
		try {
			if(timeOutChangeBankAccountCountCache.get(0L)!=null) {
				result = timeOutChangeBankAccountCountCache.get(0L);
			}
		} catch (Exception e) {
			log.error("获取生成二维码超时后更换银行卡次数时异常",e);
			result = 0;
		}
		return result;
	}
	
	/**
	 * 需求 7876 
	 * 获取云闪付每日入款限额
	 * @return
	 */
	public BigDecimal getSysYsfDayInLimit() {
		//默认3W
		BigDecimal defaultDayInTotal = new BigDecimal(30000); 
		String configYsfDayInLimit = MemCacheUtils.getInstance().getSystemProfile().get(UserProfileKey.Income_YSF_OneDay_Limit.getValue());
		if(!StringUtils.isEmpty(configYsfDayInLimit)) {
			defaultDayInTotal = new BigDecimal(configYsfDayInLimit);
		}
		return defaultDayInTotal;
	}
	
}
