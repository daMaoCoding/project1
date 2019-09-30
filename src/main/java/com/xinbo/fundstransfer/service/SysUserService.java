package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.UserCategoryStat;

public interface SysUserService {

	List<SysUser> findUsersForCompanyAuditor(Integer handicap);

	List<SysUser> findByNameLike(String name);

	Set<Integer> findOnlineUserId();

	boolean online(int userId);

	void broadCastCategoryInfo();

	void broadCastCategoryInfo(int countOfAllocatedIncomeAuditAccount, int countOfAllIncomeAuditAccount);

	Map<Integer, String> findCategory(SysUser opr);

	List<UserCategoryStat> findUserCategoryInfo(SysUser opr, String userNameLike);

	SysUser findFromCacheById(Integer id);

	SysUser findByUid(String uid);

	List<SysUser> findOutWardTaskUser();

	List<SysUser> findByUidLike(String uidLike);

	List<SysUser> findByNameAndCategory(String nameLike, Integer[] category);

	Page<SysUser> findPage(Specification<SysUser> specification, Pageable pageable);

	SysUser saveOrUpdate(SysUser opr, Integer resetPassword, BigDecimal moneyLimit, BigDecimal auditLimit,
			SysUser userVo, SysUser userInDb) throws Exception;

	void invalidateInCache(Integer userId);

	SysUser saveAndFlush(SysUser sysUser);

	int findHolders(Integer id);

	void deleteUser(Integer id);

	Map<String, Object> findPerplesByRoleId(Integer type, String account, Integer roleId, PageRequest pageRequest)
			throws Exception;

	void deleteUserRole(Integer roleId, List userIdsList);

	void addUserToRole(Integer roleId, List<String> userIdsList);

	List<SysUser> findUserByPermissionKey(String permissionKey);
}
