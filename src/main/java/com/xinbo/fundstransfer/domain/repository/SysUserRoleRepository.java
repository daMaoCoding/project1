package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SysUserRoleRepository
		extends BaseRepository<SysUserRole, Integer> {
	List<SysUserRole> findByUserId(Integer userId);

	void deleteByRoleId(Integer roleId);

	List<SysUserRole> findByRoleId(Integer roleId);
}
