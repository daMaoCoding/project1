package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.CabanaStatus;
import com.xinbo.fundstransfer.domain.pojo.ChgAcc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccountChangeService {

	void monitor(CabanaStatus status);

	Integer margin(AccountBaseInfo target);

	Map<Integer, Integer> allMargin(List<Integer> accountIds);

	Integer creditLimit(AccountBaseInfo target);

	Integer peakBalance(AccountBaseInfo target);

	BigDecimal buildRelBal(Integer accId);

	ChgAcc buildChgAcc(Set<String> keys);

	void firstUseToLogin(String mobile, Integer accId, long curMils);

	/**
	 * 返回账户当前信用额度
	 *
	 * @param target
	 * @return
	 */
	Integer currCredits(AccountBaseInfo target);

	Map<Integer, Integer> allCurrCredits(List<Integer> accountIds);

	void addToOccuCredits(Integer handicapCode, List<String> accounts, Number amount);

	/**
	 * 确认对账完成
	 *
	 * @param logs
	 */
	void ackReConciliate(String logs);

	BizAccountMore calculateMoreLineLimit(AccountBaseInfo onlineRuningAccount);

	BizAccountMore calculateMoreLineLimit(BizAccountMore more, BigDecimal difference);
}
