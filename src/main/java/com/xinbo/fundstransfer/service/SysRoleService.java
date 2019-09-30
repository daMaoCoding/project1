package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.SysRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface SysRoleService {

	List<SysRole> findList();

	List<SysRole> findList(Specification<SysRole> specification);

	Page<SysRole> findAll(Pageable pageable);

	Page<SysRole> findByName(Specification<SysRole> specification, Pageable pageable);

	SysRole findById(Integer id);

	SysRole saveOrUpdate(SysRole entity) throws Exception;

	void delete(Integer id);

	List<SysRole> findAllByIdIn(List<Integer> ids);

	List<Object> countPeopleByRoId();
}
