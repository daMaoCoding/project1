package com.xinbo.fundstransfer.service.impl;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.SysDataPermission;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.SysDataPermissionENUM;
import com.xinbo.fundstransfer.domain.repository.SysDataPermissionRepository;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;

@Service
@Slf4j
public class SysDataPermissionImpl implements SysDataPermissionService {


	@Autowired
	private SysDataPermissionRepository sysDataPermissionRepository;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private LevelService levelService;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private RedisService redisService;
	private static ObjectMapper mapper = new ObjectMapper();

	/**
	 * 用户的盘口数据权限</br>
	 * key#userId 用户ID</br>
	 * value#handicapId1;handicapId2 用户拥有权限的盘口Id以分号分割</br>
	 */
	private static final Cache<Integer, String> UserHandicapCacheBuilder = CacheBuilder.newBuilder().maximumSize(20000)
			.expireAfterWrite(4, TimeUnit.DAYS).build();

	/**
	 * 通过用户id查询用户数据权限
	 * 
	 * @param userId
	 * @return
	 */
	@Override
	public List<SysDataPermission> findSysDataPermission(Integer userId) {
		List<SysDataPermission> list = null;
		try {
			SysDataPermission sysDataPermission = new SysDataPermission();
			sysDataPermission.setUserId(userId);
			// sysDataPermission.setFieldName(SysDataPermissionENUM.LEVELCODE.getValue());
			Specification<SysDataPermission> specification;
			String[] list1 = new String[2];
			list1[0] = SysDataPermissionENUM.LEVELCODE.getValue();
			list1[1] = SysDataPermissionENUM.HANDICAPCODE.getValue();
			if (userId != 1) {
				specification = DynamicSpecifications.build(SysDataPermission.class,
						new SearchFilter("fieldName", SearchFilter.Operator.IN, list1),
						new SearchFilter("userId", SearchFilter.Operator.EQ, sysDataPermission.getUserId()));
			} else {
				specification = DynamicSpecifications.build(SysDataPermission.class,
						new SearchFilter("fieldName", SearchFilter.Operator.IN, list1));
			}
			if (userId == 1) {
				list = distinct(sysDataPermissionRepository.findAll(specification));
			} else {
				list = sysDataPermissionRepository.findAll(specification);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 去重
	 * 
	 * @param sysDataPermissionList
	 * @return
	 */
	public List<SysDataPermission> distinct(List<SysDataPermission> sysDataPermissionList) {
		if (sysDataPermissionList == null || sysDataPermissionList.size() == 0) {
			return Collections.emptyList();
		}
		Set<SysDataPermission> set = new TreeSet<>((o1, o2) -> {
			// 字符串,则按照asicc码升序排列
			return Integer.valueOf(o2.getFieldValue()).compareTo(Integer.valueOf(o1.getFieldValue()));
		});
		set.addAll(sysDataPermissionList);
		return new ArrayList<>(set);
	}

	/**
	 * 先删除用户数据权限 根据用户id，层级id ,盘口id保存数据
	 * 
	 * @param levelIds
	 * @param handicapIds
	 * @param id
	 * @return
	 */
	@Override
	@Transactional
	public List<SysDataPermission> savePermission(String levelIds, String handicapIds, Integer id) {
		List<SysDataPermission> resplist = new ArrayList<>();
		SysDataPermission sysDataPermissionResp;
		try {

			deleteByUserId(id);// 删除

			if (StringUtils.isNotBlank(levelIds)) {
				String[] levelIdsArr = levelIds.split(",");
				for (String val : levelIdsArr) {
					SysDataPermission sysDataPermission = new SysDataPermission();
					sysDataPermission.setClassName("层级");
					sysDataPermission.setDescription("层级");
					sysDataPermission.setFieldName(SysDataPermissionENUM.LEVELCODE.getValue());
					sysDataPermission.setFieldValue(val);
					sysDataPermission.setUserId(id);
					sysDataPermission.setOperator("");
					sysDataPermission.setNativeSql("");
					sysDataPermissionResp = sysDataPermissionRepository.save(sysDataPermission);
					resplist.add(sysDataPermissionResp);
				}
			}
			if (StringUtils.isNotBlank(handicapIds)) {
				String[] handicapIdsArr = handicapIds.split(",");
				for (String val : handicapIdsArr) {
					SysDataPermission sysDataPermission = new SysDataPermission();
					sysDataPermission.setClassName("盘口");
					sysDataPermission.setDescription("盘口");
					sysDataPermission.setFieldName(SysDataPermissionENUM.HANDICAPCODE.getValue());
					sysDataPermission.setFieldValue(val);
					sysDataPermission.setUserId(id);
					sysDataPermission.setOperator("");
					sysDataPermission.setNativeSql("");
					sysDataPermissionResp = sysDataPermissionRepository.save(sysDataPermission);
					resplist.add(sysDataPermissionResp);
				}
			}
			// 发送刷新用户盘口权限广播
			redisService.convertAndSend(RedisTopics.REFRESH_USER_HANDICAP_PERMISSION_TOPIC,
					mapper.writeValueAsString(id));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resplist;
	}

	@Override
	@Transactional
	public void deleteByUserId(Integer userId) {
		try {
			log.debug("删除开始");
			SysDataPermission temp = new SysDataPermission();
			temp.setUserId(userId);
			Specification<SysDataPermission> specification = DynamicSpecifications.build(SysDataPermission.class,
					new SearchFilter("userId", SearchFilter.Operator.EQ, temp.getUserId()));
			List<SysDataPermission> oldList = sysDataPermissionRepository.findAll(specification);
			if (oldList != null && oldList.size() > 0) {
				// 删除
				sysDataPermissionRepository.deleteByUserId(userId);
			}
		} catch (Exception e) {
			log.error("删除失败");
		}

	}

	/**
	 * 根据当前用户查询盘口信息数据,如果查不到则返回没有绑定权限的盘口,如新盘口
	 */
	@Override
	public List<BizHandicap> findByPermFirstThenNoPerm(SysUser sysUser) {
		List<BizHandicap> list = getHandicapByUserId(sysUser);
		if (list == null || list.size() == 0) {
			list = handicapService.findNewHandciap();
		}
		return list;
	}

	/**
	 * 根据用户ID 获取盘口信息
	 *
	 * @return
	 */
	@Override
	public List<BizHandicap> getHandicapByUserId(SysUser sysUser) {
		log.trace("获取用户盘口信息");
		List<BizHandicap> bizHandicapList = new ArrayList<>();
		List<SysDataPermission> list = new ArrayList<>();
		if (sysUser != null && sysUser.getId() != null) {
			if (!"admin".equals(sysUser.getUid())) {
				try {
					list = findSysDataPermission(sysUser.getId());
				} catch (Exception e) {
					log.error("获取盘口信息失败：" + e);
				}
			} else {
				try {
					bizHandicapList = handicapService.findAllToList();
				} catch (Exception e) {
					log.error("获取盘口信息失败：" + e);
				}
			}

			if (null != list && list.size() > 0) {
				for (int i = 0, L = list.size(); i < L; i++) {
					BizHandicap bizHandicap;
					if (SysDataPermissionENUM.LEVELCODE.getValue().equals(list.get(i).getFieldName())) {
						BizLevel bizLevel = levelService.findFromCache(Integer.valueOf(list.get(i).getFieldValue()));
						if (bizLevel != null && bizLevel.getHandicapId() != null) {
							bizHandicap = handicapService.findFromCacheById(bizLevel.getHandicapId());
							bizHandicapList.add(bizHandicap);
						}
					}
					if (SysDataPermissionENUM.HANDICAPCODE.getValue().equals(list.get(i).getFieldName())) {
						bizHandicap = handicapService.findFromCacheById(Integer.valueOf(list.get(i).getFieldValue()));
						if (bizHandicap != null)
							bizHandicapList.add(bizHandicap);
					}
				}
			}
		}
		if (bizHandicapList != null && bizHandicapList.size() > 0) {
			HashSet hs = new HashSet(bizHandicapList);
			bizHandicapList.clear();
			bizHandicapList.addAll(hs);
		}
		return bizHandicapList;
	}

	/**
	 * 根据用户只读取盘口信息，不读取类型为层级的权限。
	 */
	@Override
	public List<BizHandicap> getOnlyHandicapByUserId(SysUser sysUser) {
		log.trace("获取用户盘口信息");
		List<BizHandicap> bizHandicapList = new ArrayList<>();
		List<SysDataPermission> list = new ArrayList<>();
		if (sysUser != null && sysUser.getId() != null) {
			if (!"admin".equals(sysUser.getUid())) {
				try {
					list = findSysDataPermission(sysUser.getId());
				} catch (Exception e) {
					log.error("获取盘口信息失败：" + e);
				}
			} else {
				try {
					bizHandicapList = handicapService.findAllToList();
				} catch (Exception e) {
					log.error("获取盘口信息失败：" + e);
				}
			}

			if (null != list && list.size() > 0) {
				for (int i = 0, L = list.size(); i < L; i++) {
					BizHandicap bizHandicap;
					if (SysDataPermissionENUM.HANDICAPCODE.getValue().equals(list.get(i).getFieldName())) {
						bizHandicap = handicapService.findFromCacheById(Integer.valueOf(list.get(i).getFieldValue()));
						if (bizHandicap != null)
							bizHandicapList.add(bizHandicap);
					}
				}
			}
		}
		if (bizHandicapList != null && bizHandicapList.size() > 0) {
			HashSet hs = new HashSet(bizHandicapList);
			bizHandicapList.clear();
			bizHandicapList.addAll(hs);
		}
		return bizHandicapList;
	}

	/**
	 * 通过用户id 获取用户拥有的层级权限
	 * 
	 * @param userId
	 * @return
	 */
	@Override
	public List<Integer> findLevelIdList(Integer userId) {
		List<SysDataPermission> list = findSysDataPermission(userId);
		List<Integer> levelList = new ArrayList<>();
		if (list != null && list.size() > 0) {
			for (int i = 0, L = list.size(); i < L
					&& SysDataPermissionENUM.LEVELCODE.getValue().equals(list.get(i).getFieldName()); i++) {
				levelList.add(Integer.parseInt(list.get(i).getFieldValue()));
			}
		}
		return levelList;
	}

	@Override
	public List<Integer> findPermLevelIdBylevelIdsAndUserId(Integer[] levelIds, Integer userId) {
		String sql = " select field_value  from sys_data_permission where 1=1 and field_name='LEVELCODE' and user_id =  "
				+ userId + " and ";
		if (levelIds.length > 1) {
			sql += " field_value in ( ";
			for (int i = 0; i < levelIds.length; i++) {
				if (i < levelIds.length - 1) {
					sql += levelIds[i] + ",";
				} else {
					sql += levelIds[i] + ")";
				}
			}
		} else {
			sql += " field_value =" + levelIds[0];
		}
		List<Integer> list = entityManager.createNativeQuery(sql).getResultList();
		return list;
	}

	@Override
	public List<String> handicapIdsList(String handicap, SysUser sysUser1) {
		Set<String> handicapIdsList = new HashSet<>();
		if (StringUtils.isBlank(handicap)) {
			List<BizHandicap> list = getHandicapByUserId(sysUser1);
			if (list != null && list.size() > 0) {
				list.stream().forEach(p -> handicapIdsList.add(p.getId().toString()));
			}
		} else {
			handicapIdsList.add(handicap);
		}
		return new ArrayList<>(handicapIdsList);
	}

	// 各个页面查询条件使用 :用户层级权限
	@Override
	public List<String> levelIdsList(String level, SysUser sysUser1) {
		List<SysDataPermission> list = findSysDataPermission(sysUser1.getId());
		Set<String> levelList = new HashSet<>();
		if (list != null && list.size() > 0) {
			for (int i = 0, L = list.size(); i < L
					&& SysDataPermissionENUM.LEVELCODE.getValue().equals(list.get(i).getFieldName()); i++) {
				levelList.add(list.get(i).getFieldValue());
			}
		}
		return new ArrayList<>(levelList);
	}

	// 各个页面查询条件使用 :用户盘口权限
	@Override
	public List<String> handicapCodeList(String handicap, SysUser sysUser1) {
		Set<String> handicapCodeList = new HashSet<>();
		if (StringUtils.isBlank(handicap)) {
			List<BizHandicap> list = getHandicapByUserId(sysUser1);
			if (list != null && list.size() > 0) {
				list.stream().forEach(p -> handicapCodeList.add(p.getCode()));
			}
		} else {
			handicapCodeList.add(handicap);
		}
		return new ArrayList<>(handicapCodeList);
	}

	// 从缓存中获取用户拥有的盘口权限，盘口之间用分号分割
	@Override
	public String findUserHandicapFromCache(Integer userId) {
		if (Objects.isNull(userId)) {
			return ";;";
		}
		String result = UserHandicapCacheBuilder.getIfPresent(userId);
		if (StringUtils.isBlank(result)) {
			result = findPermHandicapByUserId(userId);
			if (StringUtils.isNotBlank(result)) {
				result = ";" + result + ";";
				UserHandicapCacheBuilder.put(userId, result);
				return result;
			}
			return ";;";
		}
		return result;
	}

	private String findPermHandicapByUserId(Integer userId) {
		String sql = "select GROUP_CONCAT(field_value SEPARATOR ';') from sys_data_permission where field_name='HANDICAPCODE' and user_id = "
				+ userId;
		Object result = entityManager.createNativeQuery(sql).getSingleResult();
		if (Objects.nonNull(result)) {
			return result.toString();
		}
		return null;
	}

	public void flushUserHandicapCache(Integer userId) {
		UserHandicapCacheBuilder.put(userId, findPermHandicapByUserId(userId));
	}
}
