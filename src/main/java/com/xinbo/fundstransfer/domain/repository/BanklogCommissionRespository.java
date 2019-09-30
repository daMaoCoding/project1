package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizBankLogCommission;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BanklogCommissionRespository extends BaseRepository<BizBankLogCommission, Long> {

	List<BizBankLogCommission> findByReturnSummaryIdIn(List<Long> returnSummaryIdList);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "delete from biz_bank_log_commission where account_id=?1 and calc_time=?2")
	void deleteByAccountIdAndCalcTime(Integer accountId, String calcTime);

	List<BizBankLogCommission> findByCalcTime(String calcTime);
}
