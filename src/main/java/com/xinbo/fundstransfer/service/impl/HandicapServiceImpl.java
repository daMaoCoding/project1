package com.xinbo.fundstransfer.service.impl;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.UserCategory;
import com.xinbo.fundstransfer.domain.repository.HandicapRepository;
import com.xinbo.fundstransfer.service.*;
import org.springframework.util.ObjectUtils;

@Service
public class HandicapServiceImpl implements HandicapService {
	private static final Cache<Object, BizHandicap> cahceBuilder = CacheBuilder.newBuilder().maximumSize(500)
			.expireAfterWrite(10, TimeUnit.DAYS).build();
	@Autowired
	private HandicapRepository handicapRepository;
	@Autowired @Lazy
	private LevelService levelService;
	@Autowired @Lazy
	private SysDataPermissionService dataPermissionService;
	@Autowired
	private SysUserService userService;
	@Autowired
	private EntityManager entityManager;

	@Override
	public List<BizHandicap> findByStatusEqual(Integer status) {
		return handicapRepository.findByStatusEquals(status);
	}

	/**
	 * 根据当前用户的数据权限获取盘口层级关联关系
	 */
	@Override
	public List<Object[]> handicap2LevelList4User(Integer userId) {
		return handicapRepository.handicap2LevelList4User(userId);
	}

	/**
	 * 查询所有的盘口层级关联信息 list
	 */
	@Override
	public List<Object[]> handicap2LevelListAll() {
		return handicapRepository.handicap2LevelListAll();
	}

	/**
	 * 获取新增的盘口或者没有同步层级的盘口
	 */
	@Override
	public List<BizHandicap> findNewHandciap() {
		return handicapRepository.findNewHandicap();
	}

	@Override
	public List<Integer> findByNameLikeOrCodeLikeOrIdLike(String handicap) {
		return handicapRepository.findByNameLikeOrCodeLikeOrIdLike(handicap);
	}

	@Override
	public List<BizHandicap> findAllToList() {
		// 盘口不能缓存 除非在插入新盘口的时候更新缓存
		List<BizHandicap> handicapList = handicapRepository.findAll();// cahceBuilder.asMap().values().stream().collect(Collectors.toSet()).stream()
		// .collect(Collectors.toList());
		handicapList = handicapList.stream()
				.filter(p -> Objects.equals(1, p.getStatus()) && Objects.nonNull(p.getZone()))
				.collect(Collectors.toList());
		Collections.sort(handicapList,
				(o1, o2) -> Objects.equals(o1.getZone(), o2.getZone()) ? o1.getId().compareTo(o2.getId())
						: o1.getZone().compareTo(o2.getZone()));
		return handicapList;
	}

	@Override
	public List<BizHandicap> findAllZoneToList() {
		return handicapRepository.findAll().stream().filter(p -> Objects.isNull(p.getZone()))
				.collect(Collectors.toList());
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<BizHandicap> findByUserId(Integer userId) {
		if (userId == AppConstants.USER_ID_4_ADMIN) {
			return findAllToList();
		}
		List<Integer> levelIdList = dataPermissionService.findLevelIdList(userId);
		if (CollectionUtils.isEmpty(levelIdList)) {
			return Collections.EMPTY_LIST;
		}
		Set<Integer> handicapIdAll = new HashSet<>();
		levelIdList.forEach(p -> handicapIdAll.add(levelService.findFromCache(p).getHandicapId()));
		List<BizHandicap> result = new ArrayList<>();
		handicapIdAll.forEach(p -> result.add(findFromCacheById(p)));
		return result;
	}

	@Override
	public BizHandicap findFromCacheById(Integer id) {
		if (id == null) {
			return null;
		}
		BizHandicap result = cahceBuilder.getIfPresent(id);
		if (result != null) {
			return result;
		}
		synchronized (id.toString()) {
			result = cahceBuilder.getIfPresent(id);
			if (result != null) {
				return result;
			}
			result = handicapRepository.findById2(id);
			if (result != null) {
				cahceBuilder.put(id, result);
				cahceBuilder.put(result.getCode(), result);
			}
			return result;
		}
	}

	@Override
	public BizHandicap findFromCacheByCode(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}
		BizHandicap result = cahceBuilder.getIfPresent(code);
		if (result != null) {
			return result;
		}
		synchronized (code) {
			result = cahceBuilder.getIfPresent(code);
			if (result != null) {
				return result;
			}
			result = handicapRepository.findByCode(code);
			if (result != null) {
				cahceBuilder.put(code, result);
				cahceBuilder.put(result.getId(), result);
			}
			return result;
		}
	}

	@Override
	public void flushCache() {
		handicapRepository.findAll().forEach(p -> {
			cahceBuilder.put(p.getId(), p);
			cahceBuilder.put(p.getCode(), p);
		});
	}

	@Override
	@Transactional
	public BizHandicap save(BizHandicap o) {
		if (o == null || (o.getId() == null && StringUtils.isBlank(o.getCode()))) {
			return null;
		}
		BizHandicap db;
		if (o.getId() != null) {
			db = handicapRepository.findById2(o.getId());
		} else {
			db = handicapRepository.findByCode(StringUtils.trim(o.getCode()));
		}
		if (db != null) {
			if (StringUtils.isNotBlank(o.getName())) {
				db.setName(o.getName());
			}
			if (StringUtils.isNotBlank(o.getUri())) {
				db.setUri(o.getUri());
			}
		}
		db = db == null ? o : db;
		return handicapRepository.saveAndFlush(db);
	}

	/**
	 * 检测该地区是否区分盘口
	 * <p>
	 * <code>if zone is null</code> 时，<br/>
	 * 以地区MANILA为准
	 * </p>
	 *
	 * @param zone
	 *            区域编码
	 * @return true: 该地区区分盘口</br>
	 *         false:该地区不区分盘口</br>
	 * @see BizHandicap#getId()
	 * @see BizHandicap#getZone()
	 */
	@Override
	public boolean checkDistHandi(Integer zone) {
		return false;
	}

	/**
	 * find the zone ID though the handicap ID
	 * <p>
	 * the handicap doesn't exist, the MANILA zone's ID as result.
	 * </p>
	 *
	 * @param handiId
	 *            the handicap's ID
	 * @return the zone ID
	 */
	@Override
	public int findZoneByHandiId(Integer handiId) {
		if (Objects.isNull(handiId)) {
			return findFromCacheByCode(AppConstants.ZONE_MANILA).getId();
		}
		BizHandicap hd = findFromCacheById(handiId);
		if (Objects.isNull(hd)) {
			return findFromCacheByCode(AppConstants.ZONE_MANILA).getId();
		}
		return Objects.isNull(hd.getZone()) ? hd.getId() : hd.getZone();
	}

	/**
	 * find zone ID though user ID
	 * <p>
	 * if user ID is null, {@code 0} as result
	 * </p>
	 * <p>
	 * if user belongs to Administration category , {@code 0} as result
	 * </p>
	 *
	 * @return zone ID
	 *
	 */
	@Override
	public int findZoneByUserId(Integer userId) {
		if (Objects.isNull(userId))
			return 0;
		SysUser user = userService.findFromCacheById(userId);
		if (Objects.isNull(user) || Objects.equals(user.getCategory(), UserCategory.ADMIN.getValue()))
			return 0;
		return findZoneByHandiId(user.getHandicap());
	}

	/**
	 * find the handicap ID though the handicap ID
	 * <p>
	 * the handicap ID is null, MANILA zone ID as result
	 * </p>
	 *
	 * @param handiId
	 *            the handicap ID
	 * @return the handicap ID
	 *
	 */
	@Override
	public int findHandiByHandiId(Integer handiId) {
		return Objects.isNull(handiId) ? findFromCacheByCode(AppConstants.ZONE_MANILA).getId() : handiId;
	}

	@PostConstruct
	private void init() {
		flushCache();
	}

	@Override
	public List<Integer> findByZone(String zone) {
		return handicapRepository.findByZone(zone);
	}

	@Override
	public void updateZoneByHandicapId(Integer handiId, Integer zone) {
		handicapRepository.updateZoneByHandicapId(handiId, zone);
	}

	// 根据区域id查询盘口编码
	@Override
	public List<Object> findHandicapCodesByZone(Integer zone, int userId) {
		String sql = "select code from biz_handicap where zone=" + zone;
		sql += " and id in( SELECT distinct field_value FROM fundsTransfer.sys_data_permission where user_id=" + userId
				+ " and field_name=\"HANDICAPCODE\" )";
		List<Object> list = entityManager.createNativeQuery(sql).getResultList();
		return list;
	}

	@Override
	public List<Integer> handicapIdList(SysUser operator) {
		List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(operator);
		if(CollectionUtils.isEmpty(dataToList))
			return Collections.EMPTY_LIST;
		return dataToList.stream().filter(p->Objects.nonNull(p)&&Objects.nonNull(p.getId())).map(BizHandicap::getId).collect(Collectors.toList());
		/*
		Objects.requireNonNull(operator, "操作者为空");
		List<Integer> manilaHandiList = Stream.of(StringUtils
				.trimToEmpty(MemCacheUtils.getInstance().getSystemProfile().get("HANDICAP_MANILA_ZONE")).split(";"))
				.filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
		List<Integer> taiwanHandiList = Stream.of(StringUtils
				.trimToEmpty(MemCacheUtils.getInstance().getSystemProfile().get("HANDICAP_TAIWAN_ZONE")).split(";"))
				.filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
		List<Integer> result = new ArrayList<>();
		if (Objects.equals(operator.getCategory(), -1)) {
			result.addAll(manilaHandiList);
			result.addAll(taiwanHandiList);
			return result;
		}
		if (operator.getCategory() > 400) {
			if (manilaHandiList.contains(operator.getCategory() - 400))
				return manilaHandiList;
			if (taiwanHandiList.contains(operator.getCategory() - 400))
				return taiwanHandiList;
		}
		BizHandicap handicap = findFromCacheById(operator.getHandicap());
		int zone = findZoneByHandiId(Objects.isNull(handicap) ? null : handicap.getId());
		if (!CollectionUtils.isEmpty(manilaHandiList) && zone == findZoneByHandiId(manilaHandiList.get(0)))
			return manilaHandiList;
		if (!CollectionUtils.isEmpty(taiwanHandiList) && zone == findZoneByHandiId(taiwanHandiList.get(0)))
			return taiwanHandiList;
		return Collections.EMPTY_LIST;*/
	}
}
