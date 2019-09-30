package com.xinbo.fundstransfer.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizNotice;

public interface NoticeService {

	Page<BizNotice> findPage(Specification<BizNotice> specification, Pageable pageable) throws Exception;
	
	void saveAndFlush(BizNotice vo) throws Exception;

	BizNotice findById(Integer id) throws Exception;

	void delete(Integer id) throws Exception;
}
