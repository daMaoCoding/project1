package com.xinbo.fundstransfer.service.impl;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.domain.entity.BizRebateContact;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.repository.RebateContactRepository;
import com.xinbo.fundstransfer.service.RebateContactService;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RebateContactServiceImpl implements RebateContactService {
	private static final Logger log = LoggerFactory.getLogger(RebateUserServiceImpl.class);
	@Autowired
	private RebateContactRepository rebateContactRepository;

	@Override
	public Page<BizRebateContact> findPage(Specification<BizRebateContact> specification, Pageable pageable)
			throws Exception {
		Page<BizRebateContact> rebatePage = rebateContactRepository.findAll(specification, pageable);
		return rebatePage;
	}

	@Override
	@Transactional
	public void delete(SysUser operator, Long id) {
		BizRebateContact rebateContact = rebateContactRepository.findById2(id);
		if (rebateContact != null && (rebateContact.getStatus() == null || rebateContact.getStatus() != 2)) {
			rebateContact.setStatus(2);
			rebateContact.setOperator(operator.getUid());
			rebateContact.setUpdateTime(new Date());
			BizRebateContact contact = rebateContactRepository.saveAndFlush(rebateContact);
			log.info("delete >> delete rebateContact rebateContact {},operator {}", contact.toString(),
					operator.getUid());
		}
	}

	@Override
	@Transactional
	public void save(SysUser operator, BizRebateContact rebateContact) {
		rebateContact.setOperator(operator.getUid());
		Date currDate = new Date();
		rebateContact.setCreateTime(currDate);
		rebateContact.setUpdateTime(currDate);
		BizRebateContact contact = rebateContactRepository.saveAndFlush(rebateContact);
		log.info("delete >> add rebateContact  {},operator {}", contact.toString(), operator.getUid());

	}
}
