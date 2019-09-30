package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.DynamicPredicate;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizThirdLog;
import com.xinbo.fundstransfer.domain.repository.ThirdLogRepository;
import com.xinbo.fundstransfer.service.ThirdLogService;

@Service
public class ThirdLogServiceImpl implements ThirdLogService {
	private static final Logger log = LoggerFactory.getLogger(ThirdLogServiceImpl.class);
	@Autowired
	private ThirdLogRepository thirdLogRepository;
	@PersistenceContext
    private EntityManager entityManager;
	@Override
	public BizThirdLog get(Long id) {
		return thirdLogRepository.findOne(id);
	}

	@Override
	@Transactional
	public BizThirdLog save(BizThirdLog entity) {
		return thirdLogRepository.save(entity);
	}

	@Override
	@Transactional
	public BizThirdLog update(BizThirdLog entity) {
		return thirdLogRepository.saveAndFlush(entity);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		thirdLogRepository.delete(id);
	}

	@Override
	public Page<BizThirdLog> findAll(Specification<BizThirdLog> specification, Pageable pageable) {
		return thirdLogRepository.findAll(specification, pageable);
	}
	
	@Override
    public String findAmountTotal(SearchFilter[] filterToArray) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<BizThirdLog> root = query.from(BizThirdLog.class);
        Predicate[] predicateArray = DynamicPredicate.build(cb, query, root, BizThirdLog.class, filterToArray);
        query.multiselect(cb.sum(root.<BigDecimal>get("amount")));
        query.where(predicateArray);
        Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
        if(objArray[0]==null) {
			return "0";
		}else {
			return objArray[0].toString();
		}
	}
	@Override
	public String findFeeTotal(SearchFilter[] filterToArray) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizThirdLog> root = query.from(BizThirdLog.class);
		Predicate[] predicateArray = DynamicPredicate.build(cb, query, root, BizThirdLog.class, filterToArray);
		query.multiselect(cb.sum(root.<BigDecimal>get("fee")));
		query.where(predicateArray);
		Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
		if(objArray[0]==null) {
			return "0";
		}else {
			return objArray[0].toString();
		}
	}
}
