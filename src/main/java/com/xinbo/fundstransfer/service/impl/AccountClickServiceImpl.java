package com.xinbo.fundstransfer.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.repository.AccountClickRepository;
import com.xinbo.fundstransfer.service.*;
import org.springframework.util.CollectionUtils;

/**
 * 账号操作记录
 *
 * @author
 */
@Service
public class AccountClickServiceImpl implements AccountClickService {
	@Autowired
	private AccountClickRepository accountClickRespository;

	@Override
	public Page<BizAccountClick> findAll(Specification<BizAccountClick> specification, PageRequest pageRequest) {
		return accountClickRespository.findAll(specification, pageRequest);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addClickLog(Integer accountId, String remark) {
		BizAccountClick vo = new BizAccountClick();
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		vo.setOperator(operator.getUid());
		vo.setTime(new Date());
		vo.setAccountId(accountId);
		vo.setRemark(remark);
		accountClickRespository.saveAndFlush(vo);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addClickLogList(List<Integer> accountIdList, String remark) {
		if (StringUtils.isBlank(remark) || CollectionUtils.isEmpty(accountIdList)) {
			return;
		}
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		Date date = new Date();
		List<BizAccountClick> vos = new ArrayList<>();
		Iterator it = accountIdList.iterator();
		for (; it.hasNext();) {
			BizAccountClick vo = new BizAccountClick();
			vo.setOperator(operator.getUid());
			vo.setTime(date);
			vo.setRemark(remark);
			vo.setAccountId(Integer.valueOf(it.next().toString()));
			vos.add(vo);
		}
		accountClickRespository.saveAll(vos);
		accountClickRespository.flush();
	}

}
