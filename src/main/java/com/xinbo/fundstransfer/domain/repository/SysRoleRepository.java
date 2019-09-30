package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SysRoleRepository extends BaseRepository<SysRole, Integer> {
	SysRole findById2(Integer id);

	SysRole findByName(String name);

	List<SysRole> findAllByIdIn(List<Integer> ids);

	@Query(nativeQuery = true, value = "select role_id,count(1) from sys_user_role group by role_id")
	List<Object> countPeopleByRoId();

}
