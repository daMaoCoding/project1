package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.entity.BizOutwardLog;

/**
 * 会员出款历史记录
 */
public interface BizOutwardLogRepository
		extends BaseRepository<BizOutwardLog, Long> {

	@Transactional
	@Modifying(clearAutomatically = true)
	// 如果有相同的mem_code 则更新所有的字段
	@Query(nativeQuery = true, value = "insert into biz_outward_log(from_id,to_account,update_time)VALUES (?1,?2,now()) ON DUPLICATE KEY UPDATE update_time=now()")
	int updateOnDuplicate(Integer fromId, String toAccount);

}
