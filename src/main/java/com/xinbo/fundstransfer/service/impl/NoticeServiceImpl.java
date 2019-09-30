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
import com.xinbo.fundstransfer.domain.entity.BizNotice;
import com.xinbo.fundstransfer.domain.repository.NoticeRepository;
import com.xinbo.fundstransfer.service.NoticeService;

@Service
@Transactional
public class NoticeServiceImpl implements NoticeService {
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private NoticeRepository noticeRepository;

	@Override
	public Page<BizNotice> findPage(Specification<BizNotice> specification, Pageable pageable) throws Exception {
		return noticeRepository.findAll(specification, pageable);
	}
	
	@Override
	@Transactional
	public void saveAndFlush(BizNotice vo) throws Exception {
		noticeRepository.saveAndFlush(vo);
	}

	@Override
	@Transactional
	public void delete(Integer id) throws Exception {
		noticeRepository.delete(id);
	}

	@Override
	public BizNotice findById(Integer id) throws Exception {
		return noticeRepository.findOne(id);
	}
	
}
