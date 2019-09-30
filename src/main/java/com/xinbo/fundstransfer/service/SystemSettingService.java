package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.SysUserProfile;

import java.util.List;

public interface SystemSettingService {
	Double getOutDrawLimitApproveInCache(String key);// OUTDRAW_LIMIT_APPROVE

	List<SysUserProfile> findAll(Integer userId);

	SysUserProfile findById(Integer id);

	List<SysUserProfile> findByPropertyKey(String propertyKey);

	SysUserProfile saveSetting(SysUserProfile sysUserProfile);

	void deleteById(Integer id);
}
