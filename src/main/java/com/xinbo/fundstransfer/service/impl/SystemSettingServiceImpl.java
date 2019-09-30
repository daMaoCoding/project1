package com.xinbo.fundstransfer.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.repository.SystemSettingRepository;
import com.xinbo.fundstransfer.service.SystemSettingService;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {
	private static Cache<Object, Double> cacheSysUserProfile = CacheBuilder.newBuilder().maximumSize(20000)
			.expireAfterWrite(1, TimeUnit.HOURS).build();
	@Autowired
	private SystemSettingRepository systemSettingRepository;

	@Override
	public Double getOutDrawLimitApproveInCache(String key) {
		if (key == null || "".equals(key)) {
			return new Double(0);
		}
		Double val = cacheSysUserProfile.getIfPresent(key);
		if (val != null) {
			return val;
		}
		SysUserProfile sysUserProfile = systemSettingRepository.findByPropertyKeyLike(key);
		if (sysUserProfile != null) {
			cacheSysUserProfile.put("OUTDRAW_LIMIT_APPROVE", new Double(sysUserProfile.getPropertyValue()));
		}
		return new Double(sysUserProfile.getPropertyValue());
	}

	@Override
	public List<SysUserProfile> findAll(Integer id) {
		List<SysUserProfile> list = systemSettingRepository.findByUserId(id);
		return list;
	}

	@Override
	@Transactional
	public SysUserProfile saveSetting(SysUserProfile sysUserProfile) {
		return systemSettingRepository.saveAndFlush(sysUserProfile);
	}

	@Override
	@Transactional
	public void deleteById(Integer id) {
		systemSettingRepository.delete(id);
	}

	@Override
	public SysUserProfile findById(Integer id) {
		return systemSettingRepository.findById2(id);
	}

	@Override
	public List<SysUserProfile> findByPropertyKey(String propertyKey) {
		return systemSettingRepository.findByPropertyKey(propertyKey);
	}
}
