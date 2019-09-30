package com.xinbo.fundstransfer.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizHost;

public interface HostService {

	Page<BizHost> findPage(Specification<BizHost> specification, Pageable pageable) throws Exception;
	
	/**
	 * 根据查询条件搜索主机
	 * @param seachStr 完整匹配：编号/IP	模糊匹配：主机名/账号
	 * @return
	 * @throws Exception
	 */
	List<BizHost> findList(String seachStr) throws Exception;
	
	List<Integer> findIdList(SearchFilter... filterToArray) throws Exception;

	void saveAndFlush(BizHost vo) throws Exception;

	BizHost findById(Integer id) throws Exception;
	
	BizHost findByIP(String ip) throws Exception;

	void delete(Integer id) throws Exception;
	
	String[] loadHostTotal() throws Exception;
}
