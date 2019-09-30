package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AccountMoreService {

	void updateBalance(Integer id, BigDecimal balance);

	BizAccountMore saveAndFlash(BizAccountMore arg0);

	BizAccountMore deleteAcc(String uid, Integer accId);

	BizAccountMore getFromCacheByUid(String uid);

	BizAccountMore getFromByUid(String uid);

	BizAccountMore getFromCacheByMobile(String mobile);

	Map<Integer, BizAccountMore> allFromCacheByMobile(List<Integer> accountIds);

	BizAccountMore findByMobile(String mobile);

	/**
	 * replace cache content by {@code more}
	 */
	void flushCache(BizAccountMore more);

	void cleanCache();

	List<BizAccountMore> getAccountMoreByaccountId(int accountId);

	void updateMobileByUid(String uid, String mobile);

	void setToZeroCredit(SysUser operator, String mobile);

	void updateMargin(Integer id, BigDecimal margin);

	void updateAccountsByid(Integer id, String accounts);

	Map<String, Object> findPage(String rebateUsername, BigDecimal marginMin, BigDecimal marginMax,
			BigDecimal currMarginMin, BigDecimal currMarginMax, BigDecimal totalRebateMin, BigDecimal totalRebateMax,
			String developType, String sortProperty, Integer sortDirection, Pageable request);
}
