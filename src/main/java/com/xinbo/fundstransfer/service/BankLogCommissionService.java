package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizBankLogCommission;

import java.util.List;

public interface BankLogCommissionService {

	void save(List<BizBankLogCommission> commissionList);

	List<BizBankLogCommission> findByReturnSummaryIdIn(List<Long> returnSummaryIdList);

	void deleteByAccountIdAndCalcTime(Integer accountId, String commissionTime);
}
