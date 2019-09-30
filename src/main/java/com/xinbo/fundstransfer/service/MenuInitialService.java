package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.SysMenuPermission;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MenuInitialService {

	/**
	 * 获取用户菜单信息
	 * 
	 * @param userId
	 *            用户ID
	 */
	List<Map<String, Object>> findMenuList(Integer userId);

	/**
	 * 从内存中获取用户的权限信息
	 * 
	 * @param userId
	 *            用户ID
	 * @return 用户权限信息
	 */
	List<SysMenuPermission> findFromCahce(int userId);

	/**
	 * 清空某用户的缓存权限数据
	 * 
	 * @param userId
	 *            用户ID
	 */
	void invalidCache(Integer userId);

	/**
	 * 根据权限获取授权用户
	 *
	 * @param perms
	 *            权限集
	 * @return 授权用户集
	 */
	Set<Integer> findUserIdByPerm(Set<String> perms);
}
