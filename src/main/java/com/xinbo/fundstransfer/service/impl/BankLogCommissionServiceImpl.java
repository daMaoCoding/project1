package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.BizBankLogCommission;
import com.xinbo.fundstransfer.domain.repository.BanklogCommissionRespository;
import com.xinbo.fundstransfer.service.BankLogCommissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class BankLogCommissionServiceImpl implements BankLogCommissionService {
	@Autowired
	private BanklogCommissionRespository banklogCommissionDao;

	@Override
	@Transactional
	public void save(List<BizBankLogCommission> commissionList) {
		if (CollectionUtils.isEmpty(commissionList))
			return;
		banklogCommissionDao.save(commissionList);
	}

	@Override
	public List<BizBankLogCommission> findByReturnSummaryIdIn(List<Long> returnSummaryIdList) {
		return banklogCommissionDao.findByReturnSummaryIdIn(returnSummaryIdList);
	}

	@Override
	@Transactional
	public void deleteByAccountIdAndCalcTime(Integer accountId, String commissionTime) {
		banklogCommissionDao.deleteByAccountIdAndCalcTime(accountId, commissionTime);
	}

}
