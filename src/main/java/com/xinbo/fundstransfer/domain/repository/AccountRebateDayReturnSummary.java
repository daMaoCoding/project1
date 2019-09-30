package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebateDay;
import com.xinbo.fundstransfer.domain.entity.BizAccountReturnSummary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccountRebateDayReturnSummary
		extends BaseRepository<BizAccountReturnSummary, Integer>{
	@Query(value = "select account from BizAccountReturnSummary d where d.calcTime=?1")
	List<Integer> findAccountByCalcTime(String calcTime);
}
