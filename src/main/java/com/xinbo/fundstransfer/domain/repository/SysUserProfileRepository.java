package com.xinbo.fundstransfer.domain.repository;

import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.xinbo.fundstransfer.domain.entity.SysUserProfile;

public interface SysUserProfileRepository
		extends BaseRepository<SysUserProfile, Integer> {
	SysUserProfile findByUserIdAndPropertyKey(Integer uid, String propertyKey);

	List<SysUserProfile> findByUserId(Integer uid);

	SysUserProfile findByPropertyKeyAndUserId(String key, Integer userId);

	List<SysUserProfile> findByPropertyKey(String key);

	/**
	 * 根据用户id 查找已配置的快速链接
	 * 
	 * @param userId
	 * @return
	 */
	@Query(nativeQuery = true, value = "select * from sys_user_profile sup where sup.property_key like 'quickLink%' and user_id=?1 ")
	List<SysUserProfile> findQuickLinkList(Integer userId);
}
