package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.SysUserRole;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface SysUserRoleService {
	List<SysUserRole> findByUserId(Integer userId);

	void alertRoleOfUser(Integer userId, Integer[] roleIdArr);

	void deleteByRoleId(Integer roleId);

	List<SysUserRole> findAll(Specification<SysUserRole> specification);

	List<SysUserRole> findByRoleId(Integer roleId);
}
