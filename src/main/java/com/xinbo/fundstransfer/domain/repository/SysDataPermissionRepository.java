package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xinbo.fundstransfer.domain.entity.SysDataPermission;

public interface SysDataPermissionRepository
		extends BaseRepository<SysDataPermission, Integer> {
	void deleteByUserId(Integer userId);

}
