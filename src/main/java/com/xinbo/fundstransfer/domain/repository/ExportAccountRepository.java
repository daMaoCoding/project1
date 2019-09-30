package com.xinbo.fundstransfer.domain.repository;

import java.util.Date;
import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizOutwardLog;

public interface ExportAccountRepository extends BaseRepository<BizOutwardLog, Long> {
	
	@Query(nativeQuery = true, value = "SELECT " 
			+ " from_id ," 
			+ " to_account "
			+ " FROM fundsTransfer.biz_outward_log " 
			+ " WHERE "
			+ " from_id = :fromId " 
			+ " AND (:startTime is null or update_time >= :startTime ) "
			+ " AND (:endTime is null or update_time <= :endTime ) ")
	Object[] freezedOutwardLog(@Param("fromId") Integer fromId, @Param("startTime") Date startTime,
			@Param("endTime") Date endTime);
	
	@Query(nativeQuery = true, value = "SELECT " 
			+ " id, " 
			+ " account, " 
			+ " alias, " 
			+ " bank_type "
			+ " FROM fundsTransfer.biz_account " 
			+ " WHERE "
			+ " id in(:accountIds) ")
	Object[] searchAccountByIds(@Param("accountIds") List<Integer> accountIds);
}