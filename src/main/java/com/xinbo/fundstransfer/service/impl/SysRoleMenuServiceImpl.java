package com.xinbo.fundstransfer.service.impl;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.component.shiro.ShiroAuthorizingRealm;
import com.xinbo.fundstransfer.domain.entity.SysRoleMenuPermission;
import com.xinbo.fundstransfer.domain.repository.SysRoleMenuRepository;
import com.xinbo.fundstransfer.service.SysRoleMenuService;
@Slf4j
@Service
public class SysRoleMenuServiceImpl implements SysRoleMenuService {


	@Autowired
	private SysRoleMenuRepository sysRoleMenuRepository;

	@Override
	public SysRoleMenuPermission getByRoleIdAndMenuPermissionId(Integer roleId, Integer menuId) {
		return sysRoleMenuRepository.findByRoleIdAndMenuPermissionId(roleId, menuId);
	}

	@Override
	@Transactional
	public List<SysRoleMenuPermission> saveByRoleId(Integer roleId, Integer[] permissionId) {
		List<SysRoleMenuPermission> saveList = new ArrayList<>();
		if (null != roleId) {
			try {
				// 查询
				List<SysRoleMenuPermission> existList = sysRoleMenuRepository.findByRoleId(roleId);
				if (existList != null && existList.size() > 0) {
					// 先删除 再保存
					deleteByRoleId(roleId);
				}
				for (Integer perId : permissionId) {
					SysRoleMenuPermission sysRoleMenuPermission = new SysRoleMenuPermission();
					sysRoleMenuPermission.setMenuPermissionId(perId);
					sysRoleMenuPermission.setRoleId(roleId);
					sysRoleMenuRepository.save(sysRoleMenuPermission);
					saveList.add(sysRoleMenuPermission);
				}
				updateShiroRealm();

			} catch (Exception e) {
				log.error("service调用保存菜单权限失败：" + e);
			}
		}
		return saveList;
	}

	@Override
	@Transactional
	public void deleteByRoleId(Integer roleId) {
		try {
			sysRoleMenuRepository.deleteByRoleId(roleId);
		} catch (Exception e) {
			log.error("service调用删除菜单权限失败:" + e);
		}
	}

	protected void updateShiroRealm() {
		Subject subject = SecurityUtils.getSubject();
		RealmSecurityManager rsm = (RealmSecurityManager) SecurityUtils.getSecurityManager();
		ShiroAuthorizingRealm shiroRealm = (ShiroAuthorizingRealm) rsm.getRealms().iterator().next();
		String realmName = subject.getPrincipals().getRealmNames().iterator().next();
		SimplePrincipalCollection principals = new SimplePrincipalCollection(subject.getPrincipal(), realmName);
		subject.runAs(principals);
		shiroRealm.getAuthorizationCache().remove(subject.getPrincipal());
		shiroRealm.getAuthorizationCache().remove(subject.getPrincipals());
		subject.releaseRunAs();
    }
}
