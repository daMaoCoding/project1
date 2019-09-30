/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.util;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xinbo.fundstransfer.domain.repository.DaifuSurpportBankTypeRepository;
import com.xinbo.fundstransfer.domain.entity.BizDaifuSurpportBanktypeEntity;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.service.SysUserProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author blake
 *
 */
@Slf4j
@Component("daifuCacheUtil")
public class DaifuCacheUtil {
	
	@Autowired
	private SysUserProfileService sysUserProfileService;
	
	@Autowired
	private DaifuSurpportBankTypeRepository daifuSurpportBankTypeRepository;
	
	private static final String DAIFU_INFO_ORDER_TIME_RANGE="DAIFU_INFO_ORDER_TIME_RANGE";
	
	/**
	 * 时间段范围内的代付订单需要去payCore查询处理状态<br>
	 *  5分钟更新一次
	 * <pre>
	 * 取配置表 SysUserProfile 中 TIME_OUT_CHANGE_BANKACCOUNT_COUNT
	 * 配置格式：40_20 表示从当前时间往前推40小时到当前时间往前推20分钟内创建并且状态不是 取消/完成的订单，需要到payCore获取结果 
	 * </pre>
	 */
	private LoadingCache<Long, DaifuTaskTimeRange> daifuPayCoreSyncTimeRangeCache;
	
	/**
	 * 代付订单转排查时间，单位分钟<br>
	 *  5分钟更新一次
	 * <pre>
	 * 取配置表 SysUserProfile 中 DAIFU_INFO_2_INTERVENE_TIME
	 * 假设设置值为 n ,当代付订单创建，超过n分钟后没有返回确定的结果，此时转排查 
	 * </pre>
	 */
	private static final String DAIFU_INFO_2_INTERVENE_TIME="DAIFU_INFO_2_INTERVENE_TIME";
	
	/**
	 * 代付订单转排查时间缓存，单位分钟<br>
	 *  5分钟更新一次
	 * <pre>
	 * 取配置表 SysUserProfile 中 DAIFU_INFO_2_INTERVENE_TIME
	 * 假设设置值为 n ,当代付订单创建，超过n分钟后没有返回确定的结果，此时转排查 
	 * </pre>
	 */
	private LoadingCache<Long, Integer> daifuInfo2InterveneTimeCache;
	
	/**
	 * 代付提供商支持的银行类型（银行名称）
	 */
	private LoadingCache<Long, Map<String,Set<String>>> channelNameSupportBankTypeCache;
	
	@PostConstruct
	public void initialize() {
		daifuPayCoreSyncTimeRangeCache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<Long, DaifuTaskTimeRange>() {
			@Override
			public DaifuTaskTimeRange load(Long key) {
				DaifuTaskTimeRange result = new DaifuTaskTimeRange();
				List<SysUserProfile> sysUserProfile = sysUserProfileService.findByPropertyKey(DAIFU_INFO_ORDER_TIME_RANGE);
				if(!CollectionUtils.isEmpty(sysUserProfile)) {
					String incrementStr = sysUserProfile.iterator().next().getPropertyValue();
					if(!StringUtils.isEmpty(incrementStr) && incrementStr.contains("_")) {
						String[] tmp = incrementStr.split("_");
						if(tmp.length>1) {
							result.setHour(Integer.parseInt(tmp[0]));
							result.setMinute(Integer.parseInt(tmp[1]));
						}
					}
				}
				if(ObjectUtils.isEmpty(result.getHour())) {
					result.setHour(48);
				}
				if(ObjectUtils.isEmpty(result.getMinute())) {
					result.setMinute(20);
				}
				return result;
			}
		});	
		daifuInfo2InterveneTimeCache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<Long, Integer>() {
			@Override
			public Integer load(Long key) {
				List<SysUserProfile> sysUserProfile = sysUserProfileService.findByPropertyKey(DAIFU_INFO_2_INTERVENE_TIME);
				Integer result = 0;
				if(!CollectionUtils.isEmpty(sysUserProfile)) {
					String interveneTime = sysUserProfile.iterator().next().getPropertyValue();
					result = Integer.parseInt(interveneTime);
				}
				return result;
			}
		});
		
		channelNameSupportBankTypeCache = CacheBuilder.newBuilder().refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<Long, Map<String,Set<String>>>() {
			@Override
			public Map<String,Set<String>> load(Long key) {
				Map<String,Set<String>> result = new HashMap<String, Set<String>>();
				List<BizDaifuSurpportBanktypeEntity> allChannelSupportBankTypeList = daifuSurpportBankTypeRepository.findAll();
				if(!CollectionUtils.isEmpty(allChannelSupportBankTypeList)) {
					for(BizDaifuSurpportBanktypeEntity c:allChannelSupportBankTypeList) {
						Set<String> t = null;
						if(!StringUtils.isEmpty(c.getSupportBankType())) {
							t = new HashSet<String>(Arrays.asList(c.getSupportBankType().split(",")));
						}
						result.put(c.getProvider(), t);
					}
				}
				return result;
			}
		});	
	}
	
	/**
	 * 获取代付订单转排查时间，单位分钟
	 * @return
	 */
	public int getDaifu2InterveneTime() {
		int result = 0;
		try {
			result = daifuInfo2InterveneTimeCache.get(0L);
		}catch (Exception e) {
			log.error("从本地缓存中获取代付订单转排查时间时异常",e);
			result = 5;
		}
		return result;
	}
	
	/**
	 * 获取生成二维码超时后更换银行卡次数
	 * @return
	 */
	public Timestamp getPayCoreTimeStart() {
		Calendar now = Calendar.getInstance();
		DaifuTaskTimeRange range = null;
		try {
			if(daifuPayCoreSyncTimeRangeCache.get(0L)!=null) {
				range = daifuPayCoreSyncTimeRangeCache.get(0L);
			}
		} catch (Exception e) {
			log.error("获取PayCoreTimeStart时异常",e);
		}
		if(ObjectUtils.isEmpty(range)) {
			range = new DaifuTaskTimeRange();
		}
		if(ObjectUtils.isEmpty(range.getHour())) {
			range.setHour(48);
		}
		now.add(Calendar.HOUR_OF_DAY, -range.getHour());
		return new Timestamp(now.getTimeInMillis());
	}
	
	public Timestamp getPayCoreTimeEnd() {
		Calendar now = Calendar.getInstance();
		DaifuTaskTimeRange range = null;
		try {
			if(daifuPayCoreSyncTimeRangeCache.get(0L)!=null) {
				range = daifuPayCoreSyncTimeRangeCache.get(0L);
			}
		} catch (Exception e) {
			log.error("获取getPayCoreTimeEnd时异常",e);
		}
		if(ObjectUtils.isEmpty(range)) {
			range = new DaifuTaskTimeRange();
		}
		if(ObjectUtils.isEmpty(range.getMinute())) {
			range.setMinute(20);
		}
		now.add(Calendar.MINUTE, -range.getMinute());
		return new Timestamp(now.getTimeInMillis());
	}

	class DaifuTaskTimeRange{
		private Integer minute;
		private Integer hour;
		
		/**
		 * @return the minute
		 */
		public Integer getMinute() {
			return minute;
		}
		/**
		 * @param minute the minute to set
		 */
		public void setMinute(Integer minute) {
			this.minute = minute;
		}
		/**
		 * @return the hour
		 */
		public Integer getHour() {
			return hour;
		}
		/**
		 * @param hour the hour to set
		 */
		public void setHour(Integer hour) {
			this.hour = hour;
		}
		
	}

	/**
	 * 从缓存中获取通道支持的银行
	 * @param channelName
	 * @return
	 */
	public Set<String> getChannelSupportBankFromCache(String channelName) {
		Set<String> result = null;
		try {
			if(!ObjectUtils.isEmpty(channelNameSupportBankTypeCache.get(0L))){
				result = channelNameSupportBankTypeCache.get(0L).get(channelName);
			}
		} catch (ExecutionException e) {
			log.error("从本地缓存获取代付提供商{}支持的银行类型时异常",e);
		}
		if(result==null) {
			result = new HashSet<String>();
		}
		return result;
	}
	
	
}
