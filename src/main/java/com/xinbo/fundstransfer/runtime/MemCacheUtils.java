package com.xinbo.fundstransfer.runtime;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 系统缓存工具类
 * 
 *
 *
 */
public class MemCacheUtils {
	Logger log = LoggerFactory.getLogger(this.getClass());

	SysUserProfileService sysUserProfileService;
	/** 系统设置 */
	Map<String, String> systemProfile = new ConcurrentHashMap<String, String>();
	private static volatile MemCacheUtils instance;
	/** 银行流水抓取队列 */
	// private LinkedList<String> banklogs = new LinkedList<String>();
	private ConcurrentLinkedQueue<String> banklogs = new ConcurrentLinkedQueue<>();
	/** 第三方流水抓取队列 */
	// private LinkedList<String> thirdlogs = new LinkedList<String>();
	private ConcurrentLinkedQueue<String> thirdlogs = new ConcurrentLinkedQueue<>();
	/** 微信流水抓取队列 */
	private ConcurrentLinkedQueue<String> wechatlogs = new ConcurrentLinkedQueue<>();
	/** 支付宝流水抓取队列 */
	private ConcurrentLinkedQueue<String> alipaylogs = new ConcurrentLinkedQueue<>();

	private MemCacheUtils() {
		sysUserProfileService = SpringContextUtils.getBean(SysUserProfileService.class);
		loadingPreferencesData();
	}

	public static MemCacheUtils getInstance() {
		if (instance == null) {
			synchronized (MemCacheUtils.class) {
				if (instance == null) {
					instance = new MemCacheUtils();
				}
			}
		}
		return instance;
	}

	/**
	 * 设置系统编好
	 */
	public Map<String, String> getSystemProfile() {
		return systemProfile;
	}

	/**
	 * 获取系统编好
	 */
	public void setSystemProfile(Map<String, String> systemProfile) {
		this.systemProfile = systemProfile;
	}

	public ConcurrentLinkedQueue<String> getBanklogs() {
		return banklogs;
	}

	public void setBanklogs(ConcurrentLinkedQueue<String> banklogs) {
		this.banklogs = banklogs;
	}

	public ConcurrentLinkedQueue<String> getThirdlogs() {
		return thirdlogs;
	}

	public void setThirdlogs(ConcurrentLinkedQueue<String> thirdlogs) {
		this.thirdlogs = thirdlogs;
	}

	/**
	 * @return the wechatlogs
	 */
	public ConcurrentLinkedQueue<String> getWechatlogs() {
		return wechatlogs;
	}

	/**
	 * @param wechatlogs
	 *            the wechatlogs to set
	 */
	public void setWechatlogs(ConcurrentLinkedQueue<String> wechatlogs) {
		this.wechatlogs = wechatlogs;
	}

	/**
	 * @return the alipaylogs
	 */
	public ConcurrentLinkedQueue<String> getAlipaylogs() {
		return alipaylogs;
	}

	/**
	 * @param alipaylogs
	 *            the alipaylogs to set
	 */
	public void setAlipaylogs(ConcurrentLinkedQueue<String> alipaylogs) {
		this.alipaylogs = alipaylogs;
	}

	/**
	 * 加载系统设置
	 */
	public void loadingPreferencesData() {
		// systemProfile.clear();
		List<SysUserProfile> systemPreferences = sysUserProfileService.get(AppConstants.USER_ID_4_ADMIN);
		for (SysUserProfile o : systemPreferences) {
			try {
				systemProfile.put(o.getPropertyKey(), o.getPropertyValue());
			}catch (Exception e){
				log.error("加载系统配置出错:key:{}, errMsg:{}。",o.getPropertyKey(),e.getMessage(),e);
			}
			log.debug("System preferences : {}, {}[{}]", o.getPropertyName(), o.getPropertyKey(), o.getPropertyValue());
		}
	}

}
