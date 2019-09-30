package com.xinbo.fundstransfer.service;

import java.util.List;

import org.json.JSONArray;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.SysUserProfile;

public interface SysUserProfileService {
	List<SysUserProfile> get(Integer uid);

	/** 获取系统设置的值，优先从缓存获取，如果没有再查询，更新的时候也要更新缓存 */
	SysUserProfile findByUserIdAndPropertyKey(boolean update, Integer uid, String propertyKey);

	SysUserProfile findByUserIdAndPropertyKey(Integer uid, String propertyKey);

	SysUserProfile save(SysUserProfile entity);

	SysUserProfile saveAndFlush(SysUserProfile entity);

	List<SysUserProfile> findAll(Specification<SysUserProfile> specification);

	void deleteById(int id);

	SysUserProfile findByPropertyKeyAndUserId(String key, Integer userId);

	/**
	 * 根据用户id 查找已配置的快速链接
	 * 
	 * @param userId
	 * @return
	 */
	List<SysUserProfile> findQuickLinkList(Integer userId);

	List<SysUserProfile> findByPropertyKey(String propertyKey);

	int getSysUserProfileZoneByUserId(Integer userId);

	List<Integer> getSysUserProfileHandicapByZone(int userId);

	List<Integer> getHandicapCodeByZone(int zone);

	List<String> getUserIdsByZone(int zone);

	int getZoneByHandicap(int handicap, String key);

	List<SysUserProfile> updateSysUserProfile(JSONArray data);
}
