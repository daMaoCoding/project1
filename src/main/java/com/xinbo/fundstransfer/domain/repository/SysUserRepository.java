package com.xinbo.fundstransfer.domain.repository;

import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.SysUser;

public interface SysUserRepository extends BaseRepository<SysUser, Integer> {

	SysUser getByUid(String uid);

	Page<SysUser> findAll(Pageable pageable);

	SysUser findById2(Integer id);

	List<SysUser> findByUsernameLike(String userName);

	/** 查询某个盘口下的主管或者财务人员 */
	@Query(nativeQuery = true, value = " select distinct u.*  from sys_user u ,sys_user_role ur ,sys_role r ,sys_data_permission d ,biz_handicap h , biz_level l  "
			+ " where u.id = ur.user_id and ur.role_id=r.id and d.user_id=u.id  "
			+ "            and  CAST(d.field_value AS DECIMAL(8))= l.id and l.handicap_id = h.id  and (:handicap is null or h.id=:handicap) and  u.status=0  "
			+ "            and  (r.description like '%主管%' or r.description like '%财务%') ")
	List<SysUser> findUsersForCompanyAuditor(@Param("handicap") Integer handicap);

	@Query(nativeQuery = true, value = "select count(1) from biz_account where type=5 and status=1 and holder=?1")
	int findHolders(Integer id);

	@Modifying
	@Query(nativeQuery = true, value = "delete from sys_user where id=?1")
	void deleteSysUser(Integer id);

	@Modifying
	@Query(nativeQuery = true, value = "delete from sys_user_profile where user_id=?1")
	void deleteUserProfile(Integer id);

	@Modifying
	@Query(nativeQuery = true, value = "delete from sys_user_role where user_id=?1")
	void deleteUserRole(Integer id);

	@Modifying
	@Query(nativeQuery = true, value = "delete from sys_data_permission where user_id=?1")
	void deleteDataPermission(Integer id);

	// 根据角色获取下面所属的人员
	@Query(nativeQuery = true, value = "select A.*,date_format(A.create_time,'%Y-%m-%d %H:%i:%s') createTime"
			+" from sys_user A,sys_user_role B"
			+" where (?2=0 or B.role_id=?2) and A.id=B.user_id and (?2!=0 or A.status=0) and (?1 is null or A.uid like concat('%',?1,'%'))"
			, countQuery = SqlConstants.SEARCH_FINDPERPLESBYROLEID_COUNTQUERY)
	Page<Object> findPerplesByRoleId(String account, Integer roleId, Pageable pageable);
	
	@Query(nativeQuery = true, value = "select A.*,date_format(A.create_time,'%Y-%m-%d %H:%i:%s') createTime"
			+" from sys_user A"
			+" where A.status=0 and A.id not in(select user_id from sys_user_role where role_id=?2) and (?1 is null or A.uid like concat('%',?1,'%'))"
			, countQuery = SqlConstants.SEARCH_FINDUSERS_COUNTQUERY)
	Page<Object> findUsers(String account,Integer roleId, Pageable pageable);
	
	//删除用户的角色
	@Modifying
	@Query(nativeQuery = true, value = "delete from sys_user_role where role_id=?1 and user_id in (?2)")
	void deleteUserRoleByid(Integer roleId, List userIdsList);
	
	//添加用户到相应的角色
	@Modifying
	@Query(nativeQuery = true, value = "insert into sys_user_role(user_id,role_id) values (?2,?1)")
	void addUserToRole(Integer roleId, String userId);
	
	// 根据角色获取下面所属的人员
	@Query(nativeQuery = true, value = "select id,category from sys_user where (?1 is null or (uid like concat('%',?1,'%') or username like concat('%',?1,'%')))")
	List<Object> queryUsers(String userName);
	
}