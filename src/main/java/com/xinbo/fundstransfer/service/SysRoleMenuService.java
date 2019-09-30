package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.SysRoleMenuPermission;

import java.util.List;

public interface SysRoleMenuService {
	SysRoleMenuPermission getByRoleIdAndMenuPermissionId(Integer roleId, Integer menuId);

	List<SysRoleMenuPermission> saveByRoleId(Integer roleId, Integer[] permissionId);

	void deleteByRoleId(Integer roleId);

}
