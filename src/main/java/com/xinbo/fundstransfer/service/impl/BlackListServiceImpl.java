package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.BizBlackList;
import com.xinbo.fundstransfer.domain.repository.BlackListepository;
import com.xinbo.fundstransfer.service.BlackListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class BlackListServiceImpl implements BlackListService {
	@Autowired
	private BlackListepository blackListepository;
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<BizBlackList> findByName(String name) {
		return blackListepository.findByName(name);
	}

	@Override
	public List<BizBlackList> findByAccount(String account) {
		return blackListepository.findByAccount(account);
	}

	@Override
	public BizBlackList findByNameAndAccount(String name, String account) {
		return blackListepository.findByNameAndAccount(name, account);
	}

	@Override
	@Transactional
	public void delete(Integer id) {
		blackListepository.delete(id);
		blackListepository.flush();
	}

	@Override
	@Transactional
	public BizBlackList saveAndFlush(BizBlackList bizBlackList) {
		return blackListepository.saveAndFlush(bizBlackList);
	}

	@Override
	public Page<BizBlackList> findPage(Specification<BizBlackList> specification, Pageable pageable) throws Exception {
		Page<BizBlackList> page = blackListepository.findAll(specification, pageable);
		return page;
	}

	/**
	 * 描述 :判断出款订单的出款银行卡账号及对应的开户人是否在黑名单里
	 * 
	 * @param account
	 *            出款银行账号
	 * @param name
	 *            出款银行账号开户人
	 * @return
	 */
	@Override
	public boolean isBlackList(String account, String name) {
		boolean isBlackList = false;
		if (!ObjectUtils.isEmpty(account)) {
			List<BizBlackList> accountList = blackListepository.findByAccount(account);
			if (!ObjectUtils.isEmpty(accountList)) {
				isBlackList = true;
				return isBlackList;
			}
		}
		if (!isBlackList && !ObjectUtils.isEmpty(name)) {
			List<BizBlackList> nameList = blackListepository.findByName(name);
			if (!ObjectUtils.isEmpty(nameList)) {
				isBlackList = true;
				return isBlackList;
			}
		}
		return isBlackList;
	}

}
