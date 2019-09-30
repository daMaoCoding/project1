package com.xinbo.fundstransfer.service.impl;

import com.sun.jndi.url.iiopname.iiopnameURLContextFactory;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountBinding;
import com.xinbo.fundstransfer.domain.repository.AccountBindingRepository;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.AccountBindingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Service
@Transactional
@Slf4j
public class AccountBindingServiceImpl implements AccountBindingService {
	@Autowired
	private AccountBindingRepository accountBindingDao;
	@Autowired
	private AccountRepository accountRepository;

	/**
	 * 入款微信，入款支付宝，入款第三方 与下发卡绑定
	 *
	 * @param incomeAccountId
	 *            入款长啊后
	 * @param issueAccountId
	 *            下发卡账号ID
	 * @param bind1Unbind0
	 *            1:绑定；0：解除绑定
	 */
	@Override
	public void bindOrUnbind(List<Integer> issueAccountId, Integer incomeAccountId, Integer bind1Unbind0)
			throws Exception {
		if (issueAccountId.size() == 1) {
			BizAccount incomeAccount = accountRepository.findById2(issueAccountId.get(0));
			if (incomeAccount == null) {
				throw new Exception("入款账号不存在.");
			}
		} else {
			issueAccountId.stream().forEach(p -> {
				BizAccount incomeAccount = accountRepository.findById2(p);
				if (incomeAccount == null) {
					try {
						throw new Exception("入款账号不存在.");
					} catch (Exception e) {
						e.printStackTrace();
					}
					return;
				}
			});
		}

		BizAccount issueAccount = accountRepository.findById2(incomeAccountId);
		if (issueAccount == null) {
			throw new Exception("下发账号不存在.");
		}
		if (bind1Unbind0 != 0 && bind1Unbind0 != 1) {
			throw new Exception("0:解除绑定；1：绑定.");
		}
		List<BizAccountBinding> allToList = accountBindingDao.findByAccountId(incomeAccountId);
		if (bind1Unbind0 == 0 && CollectionUtils.isEmpty(allToList)) {
			log.debug("没有绑定的记录需要解绑");
			throw new Exception("没有绑定的记录需要解绑");
		}
		List<BizAccountBinding> specToList = new ArrayList<>();
		if (!CollectionUtils.isEmpty(allToList)) {
			specToList = allToList.stream().filter((p) -> issueAccountId.contains(p.getBindAccountId()))
					.collect(Collectors.toList());
			boolean flag0 = bind1Unbind0 == 0 && specToList.size() == 0;
			if (flag0) {
				log.debug("没有绑定的记录需要解绑");
				throw new Exception("没有绑定的记录需要解绑");
			}
			boolean flag1 = bind1Unbind0 == 1 && specToList.size() > 0;
			if (flag1) {
				log.debug("第三方账号id:{} 已绑定:{}  不能重复绑定!", incomeAccountId, ObjectMapperUtils.serialize(issueAccountId));
				throw new Exception("不能重复绑定");
			}
			if (bind1Unbind0 == 0 && specToList.size() > 0) {
				log.debug("解绑 第三方账号id:{},解绑的账号id:{}", incomeAccountId, ObjectMapperUtils.serialize(issueAccountId));
				specToList.forEach((p) -> accountBindingDao.delete(p.getId()));
				return;
			}
		}
		if (bind1Unbind0 == 1 && specToList.size() == 0) {
			if (issueAccountId.size() == 1) {
				BizAccountBinding byAccountIdAndBindedId = findByAccountIdAndBindedId(incomeAccountId,
						issueAccountId.get(0));
				if (null != byAccountIdAndBindedId) {
					log.debug("已经绑定 不能重复绑定");
					throw new Exception("不能重复绑定");
				}
				BizAccountBinding dto = new BizAccountBinding();
				dto.setAccountId(incomeAccountId);
				dto.setBindAccountId(issueAccountId.get(0));
				accountBindingDao.saveAndFlush(dto);
			} else {
				List<BizAccountBinding> toSaveList = new ArrayList<>();
				for (Integer toBindId : issueAccountId) {
					BizAccountBinding dto = new BizAccountBinding();
					dto.setAccountId(incomeAccountId);
					dto.setBindAccountId(toBindId);
					toSaveList.add(dto);
				}
				if (!CollectionUtils.isEmpty(toSaveList)) {
					accountBindingDao.save(toSaveList);
				}
			}

		}
	}

	/**
	 * 根据入款账号查询所绑定的下发卡账号
	 *
	 * @param accountId
	 *            入款账号
	 */
	@Override
	public List<Integer> findBindAccountId(Integer accountId) {
		if (accountId == null) {
			return Collections.emptyList();
		}
		List<BizAccountBinding> dataToList = accountBindingDao.findByAccountId(accountId);
		if (CollectionUtils.isEmpty(dataToList)) {
			return Collections.emptyList();
		}
		List<Integer> result = new ArrayList<>();
		dataToList.forEach((p) -> result.add(p.getBindAccountId()));
		return result;
	}

	/**
	 * 根据下发卡账号查询所绑定的入款账号
	 *
	 * @param bindAccountId
	 *            下发卡账号
	 */
	@Override
	public List<Integer> findAccountId(Integer bindAccountId) {
		if (bindAccountId == null) {
			return Collections.emptyList();
		}
		List<BizAccountBinding> dataToList = accountBindingDao.findByBindAccountId(bindAccountId);
		if (CollectionUtils.isEmpty(dataToList)) {
			return Collections.emptyList();
		}
		List<Integer> result = new ArrayList<>();
		dataToList.forEach((p) -> result.add(p.getAccountId()));
		return result;
	}

	@Override
	public List<BizAccountBinding> findByBindAccountIdList(List<Integer> bindAccountIdList) {
		if (CollectionUtils.isEmpty(bindAccountIdList)) {
			return Collections.emptyList();

		}
		Specification<BizAccountBinding> specif = DynamicSpecifications.build(BizAccountBinding.class,
				new SearchFilter("bindAccountId", SearchFilter.Operator.IN, bindAccountIdList.toArray()));
		return accountBindingDao.findAll(specif);
	}

	@Override
	public List<BizAccountBinding> findByAccountIdList(List<Integer> accountIdList) {
		if (CollectionUtils.isEmpty(accountIdList)) {
			return Collections.emptyList();
		}
		Specification<BizAccountBinding> specif = DynamicSpecifications.build(BizAccountBinding.class,
				new SearchFilter("accountId", SearchFilter.Operator.IN, accountIdList.toArray()));
		return accountBindingDao.findAll(specif);
	}

	/**
	 * 根据 第三方入款账号id 和 绑定的账号id 查询是否已经绑定
	 *
	 * @param accountId
	 * @param bindedId
	 * @return
	 */
	@Override
	public BizAccountBinding findByAccountIdAndBindedId(Integer accountId, Integer bindedId) {
		BizAccountBinding res = accountBindingDao.findDistinctFirstByAccountIdAndBindAccountId(accountId, bindedId);
		return res;
	}
}
