package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.SysRole;
import com.xinbo.fundstransfer.domain.repository.SysRoleRepository;
import com.xinbo.fundstransfer.service.SysRoleMenuService;
import com.xinbo.fundstransfer.service.SysRoleService;
import com.xinbo.fundstransfer.service.SysUserRoleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysRoleServiceImpl implements SysRoleService {
	@Autowired
	private SysRoleMenuService sysRoleMenuService;
	@Autowired
	private SysUserRoleService sysUserRoleService;
	@Autowired
	private SysRoleRepository sysRoleRepository;

	@Override
	public List<SysRole> findAllByIdIn(List<Integer> ids) {
		return sysRoleRepository.findAllByIdIn(ids);
	}

	@Override
	public Page<SysRole> findAll(Pageable pageable) {
		return sysRoleRepository.findAll(pageable);
	}

	@Override
	public Page<SysRole> findByName(Specification<SysRole> specification, Pageable pageable) {
		return sysRoleRepository.findAll(specification, pageable);
	}

	@Override
	public SysRole findById(Integer id) {
		return sysRoleRepository.findById2(id);
	}

	@Override
	@Transactional
	public SysRole saveOrUpdate(SysRole vo) throws Exception {
		if (vo == null) {
			throw new Exception("参数为空.");
		}
		if (StringUtils.isBlank(vo.getName())) {
			throw new Exception("角色名为空");
		}
		vo.setName(StringUtils.trimToEmpty(vo.getName()));
		SysRole role = sysRoleRepository.findByName(vo.getName());
		if ((vo.getId() == null && role != null)
				|| (vo.getId() != null && role != null && !vo.getId().equals(role.getId()))) {
			throw new Exception("该角色已存在");
		}
		vo = sysRoleRepository.saveAndFlush(vo);
		return vo;
	}

	@Override
	@Transactional
	public void delete(Integer id) {
		sysRoleMenuService.deleteByRoleId(id);
		sysUserRoleService.deleteByRoleId(id);
		sysRoleRepository.delete(id);
	}

	@Override
	public List<SysRole> findList() {
		return sysRoleRepository.findAll(new Sort(Sort.Direction.ASC, "id"));
	}

	@Override
	public List<SysRole> findList(Specification<SysRole> specification) {
		return sysRoleRepository.findAll(specification);
	}

	@Override
	public List<Object> countPeopleByRoId() {
		return sysRoleRepository.countPeopleByRoId();
	}
}
