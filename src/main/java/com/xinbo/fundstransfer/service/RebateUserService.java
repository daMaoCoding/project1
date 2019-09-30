package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizDeductAmount;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;

public interface RebateUserService {
	BizRebateUser save(BizRebateUser data);

	void flushCache(BizRebateUser data);

	BizRebateUser getFromCacheByUid(String uid);

	String deLpwd(String lped);

	BizRebateUser getFromCacheByUserName(String userName);

	String checkUserAndPass(String userName, String password);

	void cleanCache();

	BizDeductAmount deductAmountByUid(String uid);

	BizDeductAmount saveDeductAmount(BizDeductAmount deductAmount);

}
