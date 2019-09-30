package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.SysUserRole;
import com.xinbo.fundstransfer.domain.repository.SysUserRoleRepository;
import com.xinbo.fundstransfer.service.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SysUserRoleServiceImpl implements SysUserRoleService {
	@Autowired
	private SysUserRoleRepository sysUserRoleRepository;

	@Override
	public List<SysUserRole> findByUserId(Integer userId) {
		List<SysUserRole> list = sysUserRoleRepository.findByUserId(userId);
		return list;
	}

	@Override
	@Transactional
	public void alertRoleOfUser(Integer userId, Integer[] roleIdArray) {
		if (userId == null) {
			return;
		}
		Set<Integer> mutilIdSet = new HashSet<>();
		sysUserRoleRepository.findByUserId(userId).forEach((p) -> {
			if (roleIdArray != null && roleIdArray.length > 0) {
				for (Integer roleId : roleIdArray) {
					if (roleId.equals(p.getRoleId())) {
						mutilIdSet.add(p.getRoleId());
					}
				}
			}
			if (!mutilIdSet.contains(p.getRoleId())) {
				sysUserRoleRepository.delete(p.getId());
			}
		});
		if (roleIdArray == null || roleIdArray.length == 0) {
			return;
		}
		for (Integer roleId : roleIdArray) {
			if (!mutilIdSet.contains(roleId)) {
				sysUserRoleRepository.saveAndFlush(new SysUserRole(userId, roleId));
			}
		}
	}

	@Override
	public List<SysUserRole> findAll(Specification<SysUserRole> specification) {
		return sysUserRoleRepository.findAll(specification);
	}

	@Override
	public List<SysUserRole> findByRoleId(Integer roleId) {
		return sysUserRoleRepository.findByRoleId(roleId);
	}

	@Override
	@Transactional
	public void deleteByRoleId(Integer roleId) {
		sysUserRoleRepository.deleteByRoleId(roleId);
	}
}
