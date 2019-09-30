package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SystemSettingRepository
		extends BaseRepository<SysUserProfile, Integer> {
	List<SysUserProfile> findByUserId(Integer userId);

	List<SysUserProfile> findByPropertyKey(String propertyKey);

	SysUserProfile findById2(Integer id);

	SysUserProfile findByPropertyKeyLike(String propertyKey);
}
