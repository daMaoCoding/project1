package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.DynamicPredicate;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizHost;
import com.xinbo.fundstransfer.domain.repository.HostRepository;
import com.xinbo.fundstransfer.service.HostService;

@Service
@Transactional
public class HostServiceImpl implements HostService {
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private HostRepository hostRepository;

	@Override
	public Page<BizHost> findPage(Specification<BizHost> specification, Pageable pageable) throws Exception {
		return hostRepository.findAll(specification, pageable);
	}
	
	@Override
	public List<BizHost> findList(String seachStr) throws Exception{
		return hostRepository.findList(seachStr);
	}
	
	@Override
	public List<Integer> findIdList(SearchFilter... filterToArray) {
		List<Integer> result = new ArrayList<>();
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizHost> root = query.from(BizHost.class);
		javax.persistence.criteria.Predicate[] predicateArray = DynamicPredicate.build(cb, query, root, BizHost.class,
				filterToArray);
		query.multiselect(root.<BigDecimal>get("id"));
		query.where(predicateArray);
		entityManager.createQuery(query).getResultList().forEach((p) -> result.add(((Integer) p.get(0))));
		return result;
	}

	@Override
	@Transactional
	public void saveAndFlush(BizHost vo) throws Exception {
		hostRepository.saveAndFlush(vo);
	}

	@Override
	@Transactional
	public void delete(Integer id) throws Exception {
		hostRepository.delete(id);
	}

	@Override
	public BizHost findById(Integer id) throws Exception {
		return hostRepository.findOne(id);
	}
	
	@Override
	public BizHost findByIP(String ip) throws Exception {
		return hostRepository.findByIp(ip);
	}
	
	@Override
	public String[] loadHostTotal() throws Exception {
		return hostRepository.loadHostTotal();
	}
}
