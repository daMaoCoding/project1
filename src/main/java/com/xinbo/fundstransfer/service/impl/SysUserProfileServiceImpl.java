package com.xinbo.fundstransfer.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.repository.HandicapRepository;
import com.xinbo.fundstransfer.domain.repository.SysUserProfileRepository;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import com.xinbo.fundstransfer.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysUserProfileServiceImpl implements SysUserProfileService {
	private static final Cache<Object, SysUserProfile> PropertyKey4User = CacheBuilder.newBuilder().maximumSize(60000)
			.expireAfterWrite(7, TimeUnit.DAYS).build();
	@Autowired
	private SysUserProfileRepository sysUserProfileRepository;
	@Autowired
	private SysUserService userService;
	@Autowired
	private HandicapRepository handicapRepository;

	@Override
	public List<SysUserProfile> get(Integer uid) {
		return sysUserProfileRepository.findByUserId(uid);

	}

	/**
	 * 根据用户id和key查询系统设置的值 ,如果有更新则需要更新缓存
	 */
	@Override
	public SysUserProfile findByUserIdAndPropertyKey(boolean update, Integer uid, String propertyKey) {
		if (update) {
			SysUserProfile sysUserProfile = findByUserIdAndPropertyKey(uid, propertyKey);
			PropertyKey4User.put("PropertyKey4User" + uid + propertyKey, sysUserProfile);
			return sysUserProfile;
		} else {
			SysUserProfile sysUserProfile = PropertyKey4User.getIfPresent("PropertyKey4User" + uid + propertyKey);
			if (sysUserProfile != null) {
				return sysUserProfile;
			} else {
				sysUserProfile = findByUserIdAndPropertyKey(uid, propertyKey);
				if (null != sysUserProfile) {
					PropertyKey4User.put("PropertyKey4User" + uid + propertyKey, sysUserProfile);
					return sysUserProfile;
				} else {
					return null;
				}
			}
		}
	}

	@Override
	public SysUserProfile findByUserIdAndPropertyKey(Integer uid, String propertyKey) {
		return sysUserProfileRepository.findByPropertyKeyAndUserId(propertyKey, uid);
	}

	@Override
	@Transactional
	public SysUserProfile save(SysUserProfile entity) {
		return sysUserProfileRepository.save(entity);
	}

	@Override
	@Transactional
	public SysUserProfile saveAndFlush(SysUserProfile entity) {
		return sysUserProfileRepository.saveAndFlush(entity);
	}

	@Override
	public List<SysUserProfile> findAll(Specification<SysUserProfile> specification) {
		return sysUserProfileRepository.findAll(specification);
	}

	@Override
	@Transactional
	public void deleteById(int id) {
		sysUserProfileRepository.delete(id);
	}

	@Override
	public SysUserProfile findByPropertyKeyAndUserId(String key, Integer userId) {
		return sysUserProfileRepository.findByPropertyKeyAndUserId(key, userId);
	}

	@Override
	public List<SysUserProfile> findQuickLinkList(Integer userId) {
		return sysUserProfileRepository.findQuickLinkList(userId);
	}

	@Override
	public List<SysUserProfile> findByPropertyKey(String propertyKey) {
		return sysUserProfileRepository.findByPropertyKey(propertyKey);
	}

	/**
	 * 此方法适用于:入款账户 支付宝微信账号分配 任务排查分配 以及适用于区域划分盘口，与用户数据盘口权限无关的场景 userProfileZone 是
	 * sys_user_profile 表根据userId 和key=HANDICAP_ZONE_MANILA0_TAIWAN1获取
	 * property_value的值 0 或者1 再根据 property_value 0或者1 查询sys_user_profile 获取 0
	 * HANDICAP_MANILA_ZONE 1 HANDICAP_TAIWAN_ZONE 对应的盘口id
	 *
	 * @param zone
	 *            which is the property_value value from the sys_user_profile
	 *            table
	 * @return int[] which carrying the handicap id
	 */
	public List<Integer> getSysUserProfileHandicapByZone(int zone) {
		String handicapsStr = null;
		if (0 == zone) {
			handicapsStr = MemCacheUtils.getInstance().getSystemProfile()
					.get(UserProfileKey.HANDICAP_MANILA_ZONE.getValue());
		}
		if (1 == zone) {
			handicapsStr = MemCacheUtils.getInstance().getSystemProfile()
					.get(UserProfileKey.HANDICAP_TAIWAN_ZONE.getValue());
		}
		if (StringUtils.isBlank(handicapsStr)) {
			log.info(" 用户所属区域:{},没有划分盘口:{} ", zone, handicapsStr);
			return null;
		}
		// handicapsStr 如: ;1;9;12;18;24;33;
		String[] handicapArr = handicapsStr.split(";");
		List<Integer> list = new ArrayList<>();
		for (int i = 0, len = handicapArr.length; i < len; i++) {
			if (StringUtils.isNotBlank(handicapArr[i])) {
				list.add(Integer.valueOf(handicapArr[i]));
			}
		}
		return list;
	}

	public List<Integer> getHandicapCodeByZone(int zone) {
		String handicapsStr = null;
		if (0 == zone) {
			handicapsStr = MemCacheUtils.getInstance().getSystemProfile()
					.get(UserProfileKey.HANDICAP_MANILA_ZONE.getValue());
		}
		if (1 == zone) {
			handicapsStr = MemCacheUtils.getInstance().getSystemProfile()
					.get(UserProfileKey.HANDICAP_TAIWAN_ZONE.getValue());
		}
		if (StringUtils.isBlank(handicapsStr)) {
			log.info(" 用户所属区域:{},没有划分盘口:{} ", zone, handicapsStr);
			return null;
		}
		// handicapsStr 如: ;1;9;12;18;24;33;
		String[] handicapArr = handicapsStr.split(";");
		List<Integer> list = new ArrayList<>();
		for (int i = 0, len = handicapArr.length; i < len; i++) {
			if (StringUtils.isNotBlank(handicapArr[i])) {
				list.add(Integer.valueOf(handicapArr[i]));
			}
		}
		List<BizHandicap> bizHandicaps = handicapRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicate = new ArrayList<>();
			predicate.add(criteriaBuilder.equal(root.get("status").as(Integer.class), 1));
			CriteriaBuilder.In in = criteriaBuilder.in(root.get("id").as(Integer.class));
			for (int i = 0, size = list.size(); i < size; i++) {
				in.value(list.get(i));
			}
			predicate.add(in);
			Predicate[] pre = new Predicate[predicate.size()];
			Predicate predicate1 = criteriaQuery.where(predicate.toArray(pre)).getRestriction();
			return predicate1;
		});
		List<Integer> handicapCodes = new ArrayList<>();
		if (!CollectionUtils.isEmpty(bizHandicaps)) {
			bizHandicaps.stream().forEach(p -> handicapCodes.add(Integer.valueOf(p.getCode())));
		}
		return handicapCodes;
	}

	/**
	 * 此方法适用于:入款账户 支付宝微信账号分配 任务排查分配 以及适用于区域划分盘口，与用户数据盘口权限无关的场景 userProfileZone 是
	 * sys_user_profile 表根据userId 和key=HANDICAP_ZONE_MANILA0_TAIWAN1获取
	 * property_value的值 0 或者1 再根据 property_value 0或者1 查询sys_user_profile 获取 0
	 * HANDICAP_MANILA_ZONE 1 HANDICAP_TAIWAN_ZONE 对应的盘口id
	 *
	 * @param userId
	 * @return int the value of PropertyValue represents the zone
	 */
	public int getSysUserProfileZoneByUserId(final Integer userId) {
		try {
			log.debug("根据用户id获取用户区域,参数:{}", userId);
			SysUser user = userService.findFromCacheById(userId);
			if (user == null) {
				log.info("userId:{},用户不存在!", userId);
				return -1;
			}
			SysUserProfile userProfile = findByUserIdAndPropertyKey(userId, "HANDICAP_ZONE_MANILA0_TAIWAN1");
			if (userProfile == null) {
				log.info("userId:{},uid:{},用户没有区域划分!", userId, user.getUid());
				return -1;
			}
			if (StringUtils.isBlank(userProfile.getPropertyValue())) {
				log.info("userId:{},用户没有区域划分,PropertyValue is null!", userId);
				return -1;
			}
			return Integer.valueOf(userProfile.getPropertyValue());
		} catch (Exception e) {
			log.error("获取区域划分值异常:", e);
		}
		return -1;
	}

	@Override
	public List<String> getUserIdsByZone(int zone) {
		List<SysUserProfile> sysUserProfiles = findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("propertyValue").as(Integer.class), zone));
			Predicate[] pre = new Predicate[predicates.size()];
			Predicate predicate1 = criteriaQuery.where(predicates.toArray(pre)).getRestriction();
			return predicate1;
		});
		if (CollectionUtils.isEmpty(sysUserProfiles)) {
			return null;
		}
		List<String> userIds = new ArrayList<>();
		sysUserProfiles.stream().forEach(p -> userIds.add(String.valueOf(p.getUserId())));
		return userIds;
	}

	@Override
	public int getZoneByHandicap(int handicap, String key) {
		if (StringUtils.isBlank(key)) {
			key = UserProfileKey.HANDICAP_MANILA_ZONE.getValue();
		}
		int ret = -1;
		SysUserProfile userProfile = sysUserProfileRepository.findByPropertyKeyAndUserId(key,
				AppConstants.USER_ID_4_ADMIN);
		if (userProfile != null) {
			String[] handicaps = userProfile.getPropertyValue().split(";");
			if (handicaps != null && handicaps.length > 0) {
				List<String> handicapsList = Arrays.asList(handicaps).stream().filter(x -> StringUtils.isNotBlank(x))
						.collect(Collectors.toList());
				boolean flag = handicapsList.contains(String.valueOf(handicap));
				if (flag && UserProfileKey.HANDICAP_MANILA_ZONE.getValue().equals(key)) {
					ret = 0;
				}
				if (flag && UserProfileKey.HANDICAP_TAIWAN_ZONE.getValue().equals(key)) {
					ret = 1;
				}
			}
		}
		if (ret == -1) {
			ret = getZoneByHandicap(handicap, UserProfileKey.HANDICAP_TAIWAN_ZONE.getValue());
			// 防止死循环
			return ret;
		}
		return ret;
	}

	@Override
	@Transactional
	public List<SysUserProfile> updateSysUserProfile(JSONArray data) {
		List<SysUserProfile> result = new ArrayList<>();
		try {
			if (data != null) {
				for (int i = 0; i < data.length(); i++) {
					JSONObject obj = data.getJSONObject(i);
					String key = obj.getString("propertyKey");
					String isEnable = obj.getString("isEnable");
					String value = obj.getString("propertyValue");
					SysUserProfile profile = findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN, key);
					if (profile != null) {
						profile.setIsEnable(isEnable);
						profile.setPropertyValue(value);
						save(profile);
						result.add(profile);
					}
				}
			}
		} catch (Exception e) {
			log.info("updateSysUserProfile>>更新配置信息异常");
		}
		return result;
	}
}
