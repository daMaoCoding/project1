package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebateDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccountRebateDayRepository
		extends BaseRepository<BizAccountRebateDay, Integer>{
	@Query(value = "select account from BizAccountRebateDay d where d.calcTime=?1")
	List<Integer> findAccountByCalcTime(String calcTime);
	
	@Query(value = "select account from BizAccountRebateDay d where d.calcTime=?1 and d.amount=50 and d.account in (?2)")
	List<Integer> findAccountByCalcTime5(String calcTime, List<Integer> accounts);
}
