package com.xinbo.fundstransfer.domain.repository;

import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xinbo.fundstransfer.domain.entity.SysRoleMenuPermission;

public interface SysRoleMenuRepository
		extends BaseRepository<SysRoleMenuPermission, Integer> {
	SysRoleMenuPermission findByRoleIdAndMenuPermissionId(Integer roleId, Integer menuId);

	void deleteByRoleId(Integer roleId);

	List<SysRoleMenuPermission> findByRoleId(Integer roleId);
}
