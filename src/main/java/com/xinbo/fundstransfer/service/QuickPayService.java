package com.xinbo.fundstransfer.service;

import java.util.List;

import com.xinbo.fundstransfer.domain.entity.BizOtherAccount;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccountBindEntity;
import com.xinbo.fundstransfer.domain.pojo.Account;

public interface QuickPayService {
	BizOtherAccount save(BizOtherAccount data);

	BizOtherAccount getFromCacheByAccountNo(String accountNo);

	void flushCache(BizOtherAccount data);

	void saveBind(Integer otherAccountId, Integer accountid);

	void deleteBind(Integer otherAccountId, Integer accountid);

	int counts(Integer otherAccountId, Integer accountid);

	void deleteBindAll(Integer otherAccountId);

	void deleteById(Integer id, String accountNo);

	void cleanCache();

	BizOtherAccount getByUid(int uid);

	void bindingAndStatus(String rebateUserName, List<Account> accounts, String message);

	int getBindAccountIdNum(Integer accid);
}
