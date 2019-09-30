package com.xinbo.fundstransfer.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.BizBlackList;

public interface BlackListService {

	/**
	 * 根据会员出款账号 和会员真实姓名 判断是否在黑名单
	 * 
	 * @param account
	 * @return
	 */
	boolean isBlackList(String account, String name);

	List<BizBlackList> findByName(String name);

	List<BizBlackList> findByAccount(String account);

	BizBlackList findByNameAndAccount(String name, String account);

	Page<BizBlackList> findPage(Specification<BizBlackList> specification, Pageable pageable) throws Exception;

	void delete(Integer id);

	BizBlackList saveAndFlush(BizBlackList bizBlackList);
}
