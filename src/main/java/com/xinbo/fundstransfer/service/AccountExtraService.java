package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountExtra;
import com.xinbo.fundstransfer.domain.entity.SysUser;

/**
 * 账号表操作记录
 * 
 * @author  2018.3.28
 *
 */
public interface AccountExtraService {

	void insertRow(BizAccountExtra accountExtra);

	void addAccountExtraLog(Integer accountId, String Uid);

	/**
	 * 保存账号操作信息
	 *
	 * @param oldAccount
	 * @param newAccount
	 * @param Uid
	 */
	void saveAccountExtraLog(BizAccount oldAccount, BizAccount newAccount, String Uid);

	Page<BizAccountExtra> findAll(Specification<BizAccountExtra> specification, PageRequest pageRequest);

	void saveAccountRate(BizAccount account);

	void updateThirdAccountBl(int id, BigDecimal amount);
}
