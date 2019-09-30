package com.xinbo.fundstransfer.service.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.mina.util.ConcurrentHashSet;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.SysMenuPermission;
import com.xinbo.fundstransfer.domain.repository.SysMenuPermissionRepository;
import com.xinbo.fundstransfer.service.MenuInitialService;
import org.springframework.util.CollectionUtils;

@Service
public class MenuInitialServiceImpl implements MenuInitialService {
	private static final Logger logger = LoggerFactory.getLogger(MenuInitialServiceImpl.class);
	@Autowired
	private SysMenuPermissionRepository menuPermissionDao;
	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * 用户权限缓存</br>
	 * key:userId</br>
	 * value:用户权限信息</br>
	 */
	private static final Cache<Integer, List<SysMenuPermission>> cacheBuilder = CacheBuilder.newBuilder()
			.maximumSize(2000).expireAfterWrite(4, TimeUnit.DAYS).build();

	/**
	 * 权限用户缓存</br>
	 * key:perm 权限标识</br>
	 * value:权限所授权的用户集合</br>
	 */
	private static final Map<String, ConcurrentHashSet<Integer>> PermissionToUserIdSet = new ConcurrentHashMap<>();

	/**
	 * Home Meun ID
	 */
	private static final int HOME_MENU_ID = 1;

	/**
	 * 获取用户菜单信息
	 *
	 * @param userId
	 *            用户ID
	 */
	@Override
	public List<Map<String, Object>> findMenuList(Integer userId) {
		List<Map<String, Object>> result = new ArrayList<>();
		if (userId == null) {
			return result;
		}
		result.add(packHomeMenu());
		if (userId == AppConstants.USER_ID_4_ADMIN) {
			result.addAll(packMainMeun(null, 1));
			return result;
		}
		List<SysMenuPermission> allocMenuList = menuPermissionDao.findByUserId(userId);
		if (CollectionUtils.isEmpty(allocMenuList)) {
			return result;
		}
		Map<Integer, SysMenuPermission> allPerm = new HashMap<>();
		menuPermissionDao.findAll().forEach((p) -> allPerm.put(p.getId(), p));
		Set<SysMenuPermission> authorized = new HashSet<>();
		for (SysMenuPermission current : allocMenuList) {
			if (current.getId() == HOME_MENU_ID) {
				continue;
			}
			buildAuthMenu(allPerm, authorized, current.getId());
		}
		result.addAll(packMainMeun(null, 1, new ArrayList<>(authorized)));
		return result;
	}

	/**
	 * 从内存中获取用户的权限信息
	 *
	 * @param userId
	 *            用户ID
	 * @return 用户权限信息
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<SysMenuPermission> findFromCahce(int userId) {
		List<SysMenuPermission> result = cacheBuilder.getIfPresent(userId);
		if (result != null) {
			return result;
		}
		synchronized (String.valueOf(userId)) {
			result = cacheBuilder.getIfPresent(userId);
			if (result != null) {
				return result;
			}
			if (userId != AppConstants.USER_ID_4_ADMIN) {
				String sqlFmt = "select distinct p.id,p.permission_key permissionKey,p.name,p.uri,p.parent_id parentId,p.icon from sys_menu_permission p,sys_role_menu_permission rp,sys_user_role ur,sys_user u where p.id=rp.menu_permission_id and rp.role_id=ur.role_id and ur.user_id=u.id and u.id=%d union select p.id, p.permission_key permissionKey, p.name, p.uri, p.parent_id parentId,p.icon from sys_menu_permission p where  p.permission_key='Home:*'";
				Query outQative = entityManager.createNativeQuery(String.format(sqlFmt, userId));
				outQative.unwrap(SQLQuery.class)
						.setResultTransformer(Transformers.aliasToBean(SysMenuPermission.class));
				result = outQative.getResultList();
			} else {
				result = menuPermissionDao.findAll();
			}
			cacheBuilder.put(userId, result);
			Set<String> tmp = new HashSet<>();
			result.forEach((p) -> {
				String key = p.getPermissionKey().split(":")[0];
				tmp.add(key);
				ConcurrentHashSet<Integer> userIdSet = PermissionToUserIdSet.get(key);
				userIdSet = userIdSet == null ? new ConcurrentHashSet<>() : userIdSet;
				userIdSet.add(userId);
				PermissionToUserIdSet.put(key, userIdSet);
			});
			PermissionToUserIdSet.forEach((k, v) -> {
				if (!tmp.contains(k) && v != null) {
					v.remove(userId);
				}
			});
			return result;
		}
	}

	/**
	 * 清空某用户的缓存权限数据
	 *
	 * @param userId
	 *            用户ID
	 */
	@Override
	public void invalidCache(Integer userId) {
		logger.debug("清空userId:{} 的 用户权限缓存数据", userId);
		cacheBuilder.invalidate(userId);
		findFromCahce(userId);
	}

	/**
	 * 根据权限获取授权用户
	 *
	 * @param perms
	 *            权限集
	 * @return 授权用户集
	 */
	@Override
	public Set<Integer> findUserIdByPerm(Set<String> perms) {
		if (CollectionUtils.isEmpty(perms)) {
			return Collections.emptySet();
		}
		Set<Integer> result = new HashSet<>();
		perms.forEach((p) -> {
			String shortPermission = p.split(":")[0];
			ConcurrentHashSet<Integer> userIdSet = PermissionToUserIdSet.get(shortPermission);
			if (userIdSet == null) {
				ConcurrentHashSet<Integer> tmp = new ConcurrentHashSet<>();
				menuPermissionDao.findUserIdListByShortPermission(shortPermission)
						.forEach((t) -> tmp.add(t.intValue()));
				PermissionToUserIdSet.put(shortPermission, tmp);
				userIdSet = tmp;
			}
			result.addAll(userIdSet);
		});
		return result;
	}

	/**
	 * 封装主页菜单信息
	 * 
	 * @param parentId
	 *            父节点ID (可以为null)
	 * @param level
	 *            父节点所属层级
	 * @param authorized
	 *            授权Menu集合
	 * @return key#action 权值</br>
	 *         key#name 菜单名</br>
	 *         key#level 菜单所在层级(首层为1,以后一次加1)</br>
	 *         key#page</br>
	 *         key#class 菜单样式</br>
	 *         key#children 子节点</br>
	 */
	private List<Map<String, Object>> packMainMeun(Integer parentId, int level, List<SysMenuPermission> authorized) {
		List<Map<String, Object>> result = new ArrayList<>();
		List<SysMenuPermission> childList = findChildList(parentId);
		if (CollectionUtils.isEmpty(childList)) {
			return result;
		}
		for (SysMenuPermission child : childList) {
			authorized.stream().filter(p -> child.getId().equals(p.getId())).forEach(p -> {
				int relLevel = child.getParentId() == null ? level : level + 1;
				Map<String, Object> sub = new HashMap<>();
				sub.put("action", child.getPermissionKey());
				sub.put("name", child.getName());
				sub.put("page", child.getUri());
				sub.put("class", child.getIcon());
				sub.put("level", relLevel);
				List<Map<String, Object>> nextList = packMainMeun(child.getId(), relLevel, authorized);
				nextList = CollectionUtils.isEmpty(nextList) ? null : nextList;
				sub.put("children", nextList);
				result.add(sub);
			});
		}
		return result;
	}

	/**
	 * 封装主页菜单信息
	 * <p>
	 * 返回 parentId 节点下所有节点信息
	 * </p>
	 * 
	 * @param parentId
	 *            父节点ID
	 * @param level
	 *            节点parentId所对应的层级
	 * @return key#action 权值</br>
	 *         key#name 菜单名</br>
	 *         key#level 菜单所在层级(首层为1,以后一次加1)</br>
	 *         key#page</br>
	 *         key#class 菜单样式</br>
	 *         key#children 子节点</br>
	 */
	private List<Map<String, Object>> packMainMeun(Integer parentId, int level) {
		List<Map<String, Object>> result = new ArrayList<>();
		List<SysMenuPermission> childList = findChildList(parentId);
		if (CollectionUtils.isEmpty(childList)) {
			return result;
		}
		List<Map<String, Object>> subList = new ArrayList<>();
		for (SysMenuPermission child : childList) {
			int currLevel = child.getParentId() == null ? level : level + 1;
			Map<String, Object> sub = new HashMap<>();
			sub.put("action", child.getPermissionKey());
			sub.put("name", child.getName());
			sub.put("page", child.getUri());
			sub.put("class", child.getIcon());
			sub.put("level", currLevel);
			List<Map<String, Object>> nextList = packMainMeun(child.getId(), currLevel);
			nextList = CollectionUtils.isEmpty(nextList) ? null : nextList;
			sub.put("children", nextList);
			subList.add(sub);
		}
		result.addAll(subList);
		return result;
	}

	/**
	 * 封装首页菜单信息(Home Menu)
	 * <p>
	 * Home 菜单权限信息在数据库中ID值为{@code 1}
	 * </p>
	 * 
	 * @return key#action 权值</br>
	 *         key#name 菜单名</br>
	 *         key#level 菜单所在层级(首层为1,以后一次加1)</br>
	 *         key#page</br>
	 *         key#class 菜单样式</br>
	 *         key#children 子节点</br>
	 */
	private Map<String, Object> packHomeMenu() {
		SysMenuPermission homeMenu = menuPermissionDao.findById2(HOME_MENU_ID);
		if (Objects.isNull(homeMenu)) {
			return new HashMap<>(1);
		}
		return new HashMap<String, Object>() {
			{
				put("action", homeMenu.getPermissionKey());
				put("name", homeMenu.getName());
				put("page", homeMenu.getUri());
				put("class", homeMenu.getIcon());
				put("level", 1);
				put("children", null);
			}
		};
	}

	/**
	 * 组装所有授权Menu
	 * <p>
	 * 采用递归算法，结果集，采用参数形式传递 （allocated）
	 * </p>
	 *
	 * @param allPerm
	 *            所有权限
	 * @param authorized
	 *            授权Menu集合
	 * @param currId
	 *            当前菜单ID
	 *
	 */
	private void buildAuthMenu(Map<Integer, SysMenuPermission> allPerm, Set<SysMenuPermission> authorized, int currId) {
		SysMenuPermission meun = allPerm.get(currId);
		if (meun != null) {
			authorized.add(meun);
			if (meun.getParentId() != null) {
				buildAuthMenu(allPerm, authorized, meun.getParentId());
			}
		}
	}

	/**
	 * 获取所有孩子节点
	 * 
	 * @param parentId
	 *            父节点ID (可以为空)
	 * @return 孩子节点集合
	 */
	private List<SysMenuPermission> findChildList(Integer parentId) {
		return menuPermissionDao.findByParentId(parentId, new Sort(Sort.Direction.ASC, "priority"));
	}
}
