package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountClick;
import com.xinbo.fundstransfer.domain.entity.SysUser;

/**
 * 账号表操作记录
 * 
 * @author 2018.3.28
 *
 */
public interface AccountClickService {

	/**
	 * 新增操作记录
	 * @param accountId 账号ID
	 * @param remark 操作描述语句
	 */
	void addClickLog(Integer accountId, String remark);
	
	/**
	 * 批量新增操作记录
	 * @param accountIdList 账号ID集合
	 * @param remark  操作描述语句
	 */
	void addClickLogList(List<Integer> accountIdList, String remark);

	Page<BizAccountClick> findAll(Specification<BizAccountClick> specification, PageRequest pageRequest);

}
