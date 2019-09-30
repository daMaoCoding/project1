package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.SysMenuPermission;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigInteger;
import java.util.List;

public interface SysMenuPermissionRepository extends BaseRepository<SysMenuPermission, Integer> {
	List<SysMenuPermission> findByParentId(Integer parentId, Sort sort);

	@Query(nativeQuery = true, value = "select 1 union select distinct ur.user_id from sys_menu_permission p,sys_role_menu_permission rp,sys_user_role ur where p.id=rp.menu_permission_id and rp.role_id=ur.role_id and  left(p.permission_key,instr(p.permission_key,':')-1)=?1")
	List<BigInteger> findUserIdListByShortPermission(String shortPermission);

	@Query(nativeQuery = true, value = "select m.* from sys_menu_permission m,sys_role_menu_permission rm,sys_user_role ur where m.id=rm.menu_permission_id and rm.role_id=ur.role_id and ur.user_id=?1")
	List<SysMenuPermission> findByUserId(int userId);

	SysMenuPermission findById2(Integer id);
}
