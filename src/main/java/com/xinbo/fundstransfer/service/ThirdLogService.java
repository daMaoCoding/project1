package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizThirdLog;

public interface ThirdLogService {

	Page<BizThirdLog> findAll(Specification<BizThirdLog> specification, Pageable pageable);

	BizThirdLog get(Long id);

	BizThirdLog save(BizThirdLog entity);

	BizThirdLog update(BizThirdLog entity);

	void delete(Long id);
	
	String findAmountTotal(SearchFilter[] filterToArray);
	String findFeeTotal(SearchFilter[] filterToArray);
}
