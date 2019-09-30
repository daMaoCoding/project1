package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.codec.SaltPassword;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.UserCategory;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.domain.pojo.UserCategoryStat;
import com.xinbo.fundstransfer.domain.repository.SysUserRepository;
import com.xinbo.fundstransfer.service.*;
import org.springframework.util.CollectionUtils;

@Service
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Slf4j
public class SysUserServiceImpl implements SysUserService {

	private static String SQL_OUTWARD_TASK_USER_LIST = "select distinct u.id,u.username,u.phone,u.email,u.status,u.uid,u.category,u.avatar,u.handicap from fundsTransfer.sys_user u left join fundsTransfer.sys_user_role ur on u.id=ur.user_id left join fundsTransfer.sys_role_menu_permission rm on ur.role_id=rm.role_id left join fundsTransfer.sys_menu_permission m on rm.menu_permission_id = m.id where  m.permission_key='OutwardTask:*' and category=%d";
	private static String SQL_PERMISSION_KEY_USER_LIST = "select distinct u.id,u.username,u.phone,u.email,u.status,u.uid,u.category,u.avatar,u.handicap from fundsTransfer.sys_user u join fundsTransfer.sys_user_role ur on u.id=ur.user_id join fundsTransfer.sys_role_menu_permission rm on ur.role_id=rm.role_id join fundsTransfer.sys_menu_permission m on rm.menu_permission_id = m.id where  m.permission_key='%s'";
	private static final Cache<Object, SysUser> cahceBuilder = CacheBuilder.newBuilder().maximumSize(10000)
			.expireAfterWrite(2, TimeUnit.DAYS).build();
	@Autowired
	private SysUserRepository sysUserRepository;
	@Autowired
	private SysUserProfileService userProfileService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private AllocateIncomeAccountService incomeAccountAllocateService;
	@PersistenceContext
	private EntityManager entityManager;
	private ObjectMapper mapper = new ObjectMapper();

	private static List<UserCategoryStat> userCategoryStat = null;

	@Override
	public List<SysUser> findUsersForCompanyAuditor(Integer handicap) {
		return sysUserRepository.findUsersForCompanyAuditor(handicap);
	}

	@Override
	public List<SysUser> findByNameLike(String name) {
		return sysUserRepository.findByUsernameLike(name);
	}

	@Override
	public Set<Integer> findOnlineUserId() {
		Set<Integer> result = new HashSet<>();
		String pattern = RedisKeys.genPattern4ShiroSession();
		for (int index = 0; index < 4; index++) {
			if (index > 0) {
				pattern = pattern + "*";
			}
			Set<String> keys = redisService.getRedisTemplate().keys(pattern);
			keys.forEach(p -> result.add(Integer.valueOf(p.split(":")[1])));
		}
		return result;
	}

	@Override
	public boolean online(int userId) {
		return redisService.getRedisTemplate().hasKey(RedisKeys.gen4ShiroSession(userId));
	}

	@Override
	public void broadCastCategoryInfo() {
		List<UserCategoryStat> dataList = findUserCategoryInfo(null, null);
		userCategoryStat = new ArrayList<>(dataList);
		int[] allocatedCountAndAllCount = incomeAccountAllocateService.findAllocatedCountAndAllCount();
		int allocatedAndAllCode = com.xinbo.fundstransfer.domain.pojo.UserCategory.allocatedAndAll;
		UserCategoryStat allocatedAndAll = new UserCategoryStat(allocatedAndAllCode, "入款账号",
				allocatedCountAndAllCount[1], allocatedCountAndAllCount[0]);
		dataList.add(allocatedAndAll);
		broadCastCategoryInfo(dataList);
	}

	@Override
	public void broadCastCategoryInfo(int countOfAllocatedIncomeAuditAccount, int countOfAllIncomeAuditAccount) {
		if (CollectionUtils.isEmpty(userCategoryStat)) {
			userCategoryStat = findUserCategoryInfo(null, null);
		}
		List<UserCategoryStat> dataList = new ArrayList<>(userCategoryStat);
		int allocatedAndAllCode = com.xinbo.fundstransfer.domain.pojo.UserCategory.allocatedAndAll;
		UserCategoryStat allocatedAndAll = new UserCategoryStat(allocatedAndAllCode, "入款账号",
				countOfAllIncomeAuditAccount, countOfAllocatedIncomeAuditAccount);
		dataList.add(allocatedAndAll);
		broadCastCategoryInfo(dataList);
	}

	private void broadCastCategoryInfo(List<UserCategoryStat> dataList) {
		try {
			String broadCast = mapper.writeValueAsString(dataList);
			String info = CommonUtils.genSysMsg4WS(null, SystemWebSocketCategory.OnlineStat, broadCast);
			redisService.convertAndSend(RedisTopics.BROADCAST, info);
		} catch (Exception e) {
			log.error("处理广播消息失败：" + e);
		}
	}

	@Override
	public Map<Integer, String> findCategory(SysUser opr) {
		Map<Integer, String> result = new TreeMap<>();
		for (com.xinbo.fundstransfer.domain.pojo.UserCategory category : com.xinbo.fundstransfer.domain.pojo.UserCategory
				.values()) {
			result.put(category.getCode(), category.getName());
		}
		List<BizHandicap> handiList = handicapService.findAllToList();
		// 如果是超级管理员则不过滤
		if (Objects.nonNull(opr) && opr.getCategory() != -1) {
			BizHandicap handicap = handicapService.findFromCacheById(opr.getHandicap());
			if (Objects.nonNull(handicap)) {
				Integer zone = Objects.isNull(handicap.getZone()) ? handicap.getId() : handicap.getZone();
				handiList = handiList.stream().filter(p -> {
					if (zone != null) {
						return p.getZone() != null && Objects.equals(p.getZone(), zone);
					}
					return true;
				}).distinct().collect(Collectors.toList());
			}
		}
		for (BizHandicap handicap : handiList) {
			result.put(com.xinbo.fundstransfer.domain.pojo.UserCategory.getCode(handicap),
					com.xinbo.fundstransfer.domain.pojo.UserCategory.getName(handicap));
		}
		return result;
	}

	@Override
	public List<UserCategoryStat> findUserCategoryInfo(SysUser opr, String userNameOruidLike) {
		List<UserCategoryStat> result = new ArrayList<>();
		Set<Integer> onlineSet = findOnlineUserId();
		// CriteriaBuilder cb = entityManager.getCriteriaBuilder();// 总人数查询
		// CriteriaQuery<Tuple> query = cb.createTupleQuery();
		// Root<SysUser> root = query.from(SysUser.class);
		// List<SearchFilter> filterList = new ArrayList<>();
		// if (StringUtils.isNotBlank(userNameLike)) {
		// filterList.add(new SearchFilter("username",
		// SearchFilter.Operator.LIKE, userNameLike));
		// }
		// javax.persistence.criteria.Predicate[] predicateArray =
		// DynamicPredicate.build(cb, query, root, SysUser.class,
		// filterList.toArray(new SearchFilter[filterList.size()]));
		// query.multiselect(root.<Integer>get("id"),
		// root.<Integer>get("category"));
		// query.where(predicateArray);
		// List objList = entityManager.createQuery(query).getResultList();
		List<Object> objList = sysUserRepository.queryUsers(userNameOruidLike);
		Map<Integer, List<Integer>> cagegoryToMap = new HashMap<>();// 用户分类
		int adminCode = UserCategory.ADMIN.getValue();
		int outwardCode = com.xinbo.fundstransfer.domain.pojo.UserCategory.Outward.getCode();
		for (int i = 0; i < objList.size(); i++) {
			Object[] obj = (Object[]) objList.get(i);
			// Object[] valArray = ((Tuple) obj).toArray();
			Integer userId = (Integer) obj[0];
			Integer category = (Integer) obj[1];
			if (adminCode != category || (opr == null ? false : opr.getCategory() == -1)) {
				category = category < outwardCode ? outwardCode : category;
				List<Integer> subIdList = cagegoryToMap.get(category);
				if (subIdList == null) {
					subIdList = new ArrayList<>();
					cagegoryToMap.put(category, subIdList);
				}
				subIdList.add(userId);
			}
		}
		// 返回结果集封装
		Map<Integer, String> category = findCategory(opr);
		// 为提升效率使用entrySet遍历
		for (Map.Entry<Integer, String> entry : category.entrySet()) {
			Integer code = entry.getKey();
			if (StringUtils.isNotBlank(userNameOruidLike) && CollectionUtils.isEmpty(cagegoryToMap.get(code))) {
				continue;
			}
			int total = cagegoryToMap.get(code) == null ? 0 : cagegoryToMap.get(code).size();
			int online = 0;
			if (cagegoryToMap.get(code) != null) {
				for (Integer p : cagegoryToMap.get(code)) {
					online = onlineSet.contains(p) ? (online + 1) : online;
				}
			}
			result.add(new UserCategoryStat(code, entry.getValue(), total, online));
		}
		return result;
	}

	@Override
	public SysUser findFromCacheById(Integer id) {
		if (id == null) {
			return null;
		}
		SysUser result = cahceBuilder.getIfPresent(id);
		if (result != null) {
			return result;
		}
		result = sysUserRepository.findById2(id);
		if (result != null) {
			cahceBuilder.put(id, result);
		}
		return result;
	}

	@Override
	public SysUser findByUid(String uid) {
		if (StringUtils.isBlank(uid)) {
			return null;
		}
		SysUser result = sysUserRepository.getByUid(uid);
		if (result != null) {
			cahceBuilder.put(uid, result);
		}
		return result;
	}

	@Override
	public List<SysUser> findOutWardTaskUser() {
		Query outQative = entityManager.createNativeQuery(String.format(SQL_OUTWARD_TASK_USER_LIST,
				com.xinbo.fundstransfer.domain.pojo.UserCategory.Outward.getCode()));
		outQative.unwrap(SQLQuery.class).setResultTransformer(Transformers.aliasToBean(SysUser.class));
		return outQative.getResultList();
	}

	@Override
	public List<SysUser> findByUidLike(String likeOperator) {
		if (StringUtils.isBlank(likeOperator)) {
			return new ArrayList<>();
		}
		Specification<SysUser> specification = DynamicSpecifications.build(SysUser.class,
				new SearchFilter("category", SearchFilter.Operator.NOTEQ, UserCategory.Robot.getValue()),
				new SearchFilter("uid", SearchFilter.Operator.LIKE, StringUtils.trimToEmpty(likeOperator)));
		return sysUserRepository.findAll(specification);
	}

	@Override
	public List<SysUser> findByNameAndCategory(String nameLike, Integer[] category) {
		if (StringUtils.isBlank(nameLike) && (category == null || category.length == 0)) {
			return null;
		}
		List<SearchFilter> filterList = new ArrayList<>();
		if (StringUtils.isNotBlank(nameLike)) {
			filterList.add(new SearchFilter("username", SearchFilter.Operator.LIKE, StringUtils.trim(nameLike)));
		}
		if (category != null && category.length > 0) {
			filterList.add(new SearchFilter("category", SearchFilter.Operator.IN, category));
		}
		Specification<SysUser> speci = DynamicSpecifications.build(SysUser.class,
				filterList.toArray(new SearchFilter[filterList.size()]));
		return sysUserRepository.findAll(speci);
	}

	@Override
	public Page<SysUser> findPage(Specification<SysUser> specification, Pageable pageable) {
		return sysUserRepository.findAll(specification, pageable);
	}

	@Override
	@Transactional
	public SysUser saveOrUpdate(SysUser opr, Integer resetPassword, BigDecimal moneyLimit, BigDecimal auditLimit,
			SysUser userVo, SysUser userInDb) throws Exception {
		if (userVo != null && userVo.getId() != null && userInDb != null) {
			if (StringUtils.isNotBlank(userVo.getUsername())) {
				userInDb.setUsername(StringUtils.trim(userVo.getUsername()));
			}
			if (StringUtils.isNotBlank(userVo.getPhone())) {
				userInDb.setPhone(StringUtils.trim(userVo.getPhone()));
			}
			if (StringUtils.isNotBlank(userVo.getEmail())) {
				userInDb.setEmail(StringUtils.trim(userVo.getEmail()));
			}
			if (userVo.getStatus() != null) {
				userInDb.setStatus(userVo.getStatus());
			}
			if (StringUtils.isNotBlank(userVo.getUid())) {
				userInDb.setUid(StringUtils.trim(userVo.getUid()));
			}
			if (StringUtils.isNotBlank(userVo.getAvatar())) {
				userInDb.setAvatar(StringUtils.trim(userVo.getAvatar()));
			}
			if (userVo.getCategory() != null) {
				userInDb.setCategory(userVo.getCategory());
			}
			if (resetPassword == 1 && StringUtils.isNotBlank(userVo.getPassword())) {
				SaltPassword saltPassword = SaltPassword.encryptPassword(userVo.getPassword());
				userInDb.setPassword(saltPassword.password);
				userInDb.setSalt(saltPassword.salt);
			}
			saveAndFlush(userInDb);
		} else {
			Integer zone = null;
			BizHandicap handicap = handicapService.findFromCacheById(opr.getHandicap());
			if (Objects.nonNull(handicap)) {
				zone = Objects.isNull(handicap.getZone()) ? handicap.getId() : handicap.getZone();
				userVo.setHandicap(zone);
			}
			userVo.setCreateTime(new Date());
			SaltPassword saltPassword = SaltPassword.encryptPassword(userVo.getPassword());
			userVo.setPassword(saltPassword.password);
			userVo.setSalt(saltPassword.salt);
			userVo.setHandicap(zone);
			userVo = saveAndFlush(userVo);
		}
		userVo = sysUserRepository.findById2(userVo.getId());
		if (moneyLimit != null) {
			List<SearchFilter> profilterList = new ArrayList<>();
			profilterList.add(new SearchFilter("userId", SearchFilter.Operator.EQ, userVo.getId()));
			profilterList.add(new SearchFilter("propertyKey", SearchFilter.Operator.EQ,
					UserProfileKey.OUTDRAW_MONEYLIMIT.getValue()));
			Specification<SysUserProfile> proSpecif = DynamicSpecifications.build(SysUserProfile.class,
					profilterList.toArray(new SearchFilter[profilterList.size()]));
			userProfileService.findAll(proSpecif).forEach((p) -> userProfileService.deleteById(p.getId()));
			SysUserProfile moneyLimitProfile = new SysUserProfile();
			moneyLimitProfile.setPropertyKey(UserProfileKey.OUTDRAW_MONEYLIMIT.getValue());
			moneyLimitProfile.setPropertyValue(String.valueOf(moneyLimit));
			moneyLimitProfile.setUserId(userVo.getId());
			userProfileService.save(moneyLimitProfile);
		}
		if (auditLimit != null) {
			List<SearchFilter> profilterList = new ArrayList<>();
			profilterList.add(new SearchFilter("userId", SearchFilter.Operator.EQ, userVo.getId()));
			profilterList.add(new SearchFilter("propertyKey", SearchFilter.Operator.EQ,
					UserProfileKey.INCOME_AUDITLIMIT.getValue()));
			Specification<SysUserProfile> proSpecif = DynamicSpecifications.build(SysUserProfile.class,
					profilterList.toArray(new SearchFilter[profilterList.size()]));
			userProfileService.findAll(proSpecif).forEach((p) -> userProfileService.deleteById(p.getId()));
			SysUserProfile auditLimitProfile = new SysUserProfile();
			auditLimitProfile.setPropertyKey(UserProfileKey.INCOME_AUDITLIMIT.getValue());
			auditLimitProfile.setPropertyValue(String.valueOf(auditLimit));
			auditLimitProfile.setUserId(userVo.getId());
			userProfileService.save(auditLimitProfile);
		}
		redisService.convertAndSend(RedisTopics.REFRESH_USER, userVo.getId().toString());
		log.info("更新用户信息：sendMessage，id：" + userVo.getId());
		return userVo;
	}

	@Override
	public void invalidateInCache(Integer userId) {
		SysUser user = sysUserRepository.findById2(userId);
		if (user == null) {
			return;
		}
		if (user.getId() != null) {
			cahceBuilder.invalidate(user.getId());
		}
		if (StringUtils.isNotBlank(user.getUid())) {
			cahceBuilder.invalidate(StringUtils.trim(user.getUid()));
		}
	}

	@Override
	@Transactional
	public SysUser saveAndFlush(SysUser sysUser) {
		return sysUserRepository.saveAndFlush(sysUser);
	}

	@Override
	public int findHolders(Integer id) {
		return sysUserRepository.findHolders(id);
	}

	@Transactional
	@Override
	public void deleteUser(Integer id) {
		sysUserRepository.deleteDataPermission(id);
		sysUserRepository.deleteUserRole(id);
		sysUserRepository.deleteUserProfile(id);
		sysUserRepository.deleteSysUser(id);
	}

	@Override
	public Map<String, Object> findPerplesByRoleId(Integer type, String account, Integer roleId,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage = sysUserRepository.findPerplesByRoleId(account, roleId, pageRequest);
		if (type.equals(2)) {
			dataToPage = sysUserRepository.findUsers(account, roleId, pageRequest);
		}
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		return map;
	}

	@Transactional
	@Override
	public void deleteUserRole(Integer roleId, List userIdsList) {
		sysUserRepository.deleteUserRoleByid(roleId, userIdsList);
	}

	@Transactional
	@Override
	public void addUserToRole(Integer roleId, List<String> userIdsList) {
		for (int i = 0; i < userIdsList.size(); i++) {
			sysUserRepository.addUserToRole(roleId, userIdsList.get(i));
		}
	}

	@Override
	public List<SysUser> findUserByPermissionKey(String permissionKey) {
		Query outQative = entityManager.createNativeQuery(String.format(SQL_PERMISSION_KEY_USER_LIST,
				permissionKey));
		outQative.unwrap(SQLQuery.class).setResultTransformer(Transformers.aliasToBean(SysUser.class));
		return outQative.getResultList();
	}
}
