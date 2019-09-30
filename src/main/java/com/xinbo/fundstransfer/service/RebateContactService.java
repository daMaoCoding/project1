package com.xinbo.fundstransfer.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.BizRebateContact;
import com.xinbo.fundstransfer.domain.entity.SysUser;

public interface RebateContactService {

	Page<BizRebateContact> findPage(Specification<BizRebateContact> specification, Pageable pageable) throws Exception;

	void delete(SysUser operator, Long id);

	void save(SysUser operator, BizRebateContact rebateContact);

}
