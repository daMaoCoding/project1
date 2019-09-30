package com.xinbo.fundstransfer.service;

import java.util.List;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysDataPermission;
import com.xinbo.fundstransfer.domain.entity.SysUser;

public interface SysDataPermissionService {
	/**
	 * 根据用户id查询用户数据权限：盘口，层级
	 */
	List<SysDataPermission> findSysDataPermission(Integer userId);

	/**
	 * 根据用户id，层级id ,盘口id保存数据
	 */
	List<SysDataPermission> savePermission(String levelIds, String handicapIds, Integer id);

	/**
	 * 根据用户id删除
	 */
	void deleteByUserId(Integer userId);

	/**
	 * 根据当前用户查询盘口信息数据
	 */
	List<BizHandicap> getHandicapByUserId(SysUser sysUser);

	/**
	 * 根据当前用户查询盘口信息数据
	 */
	List<BizHandicap> getOnlyHandicapByUserId(SysUser sysUser);

	/**
	 * 根据当前用户查询盘口信息数据,如果查不到则返回没有绑定权限的盘口,如新盘口
	 */
	List<BizHandicap> findByPermFirstThenNoPerm(SysUser sysUser);

	/**
	 * 根据用户id查询层级Id
	 */
	List<Integer> findLevelIdList(Integer userId);

	/**
	 * 根据用户id查询层级Id 层级id数组查询 已分配给用户的层级
	 */
	List<Integer> findPermLevelIdBylevelIdsAndUserId(Integer[] levelIds, Integer userId);

	List<String> handicapCodeList(String handicap, SysUser sysUser1);

	List<String> levelIdsList(String level, SysUser sysUser1);

	List<String> handicapIdsList(String handicap, SysUser sysUser1);

	String findUserHandicapFromCache(Integer userId);

	void flushUserHandicapCache(Integer userId);
}
