package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizTransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TransactionLogRepository
		extends BaseRepository<BizTransactionLog, Long> {

	List<BizTransactionLog> findByOrderId(Long OrderId);

	BizTransactionLog findByOrderIdAndType(Long orderId, Integer type);

	BizTransactionLog findByToBanklogId(Long toBanklogId);

	BizTransactionLog findByFromBanklogId(Long fromBanklogId);

	List<BizTransactionLog> findByTypeNotAndOrderIdIn(int typeNot, List<Long> orderIdIn);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_transaction_log a set a.to_banklog_id=0  where a.id=?1")
	int updateToBanklogIdZero(int id);
}