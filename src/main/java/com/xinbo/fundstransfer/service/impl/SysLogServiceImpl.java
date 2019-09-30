package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.DynamicPredicate;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.service.SysLogService;

@Service
public class SysLogServiceImpl implements SysLogService {
	@Autowired
	private SysLogRepository sysLogDao;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	IncomeRequestRepository incomeRequestDao;
	@Autowired
	SysInvstRepository sysInvstDao;

	@Override
	public List<BizSysLog> findByAccountIdAndBankLogIdInAndCreateTimeGreaterThan(Integer accountId,
			List<Long> bankLogIdIn, Date createTimeGreaterThan) {
		return sysLogDao.findByAccountIdAndBankLogIdInAndCreateTimeGreaterThan(accountId, bankLogIdIn,
				createTimeGreaterThan);
	}

	@Override
	public Page<BizSysInvst> findPage4Invst(Specification<BizSysInvst> specification, Pageable pageable) {
		return sysInvstDao.findAll(specification, pageable);
	}

	@Override
	public BigDecimal[] findAmountTotal4Invst(SearchFilter[] filterToArray) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizSysInvst> root = query.from(BizSysInvst.class);
		javax.persistence.criteria.Predicate[] predicateArray = DynamicPredicate.build(cb, query, root,
				BizSysInvst.class, filterToArray);
		query.multiselect(cb.sum(root.<BigDecimal>get("amount")));
		query.where(predicateArray);
		Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
		return new BigDecimal[] { (BigDecimal) objArray[0] };
	}

	@Override
	public Page<BizSysLog> findPage(Specification<BizSysLog> specification, Pageable pageable) {
		return sysLogDao.findAll(specification, pageable);
	}

	@Override
	public BigDecimal[] findTotal(SearchFilter[] filterToArray) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizSysLog> root = query.from(BizSysLog.class);
		javax.persistence.criteria.Predicate[] predicateArray = DynamicPredicate.build(cb, query, root, BizSysLog.class,
				filterToArray);
		query.multiselect(cb.sum(root.<BigDecimal>get("fee")), cb.sum(root.<BigDecimal>get("balance")));
		query.where(predicateArray);
		Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
		return new BigDecimal[] { (BigDecimal) objArray[0], (BigDecimal) objArray[1] };
	}

	@Override
	@Transactional
	public BigDecimal[] findAmountTotal(SearchFilter[] filterToArray) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizSysLog> root = query.from(BizSysLog.class);
		javax.persistence.criteria.Predicate[] predicateArray = DynamicPredicate.build(cb, query, root, BizSysLog.class,
				filterToArray);
		query.multiselect(cb.sum(root.<BigDecimal>get("amount")));
		query.where(predicateArray);
		Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
		return new BigDecimal[] { (BigDecimal) objArray[0] };
	}
}
