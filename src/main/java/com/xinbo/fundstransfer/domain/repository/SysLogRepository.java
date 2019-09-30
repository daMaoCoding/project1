package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface SysLogRepository extends BaseRepository<BizSysLog, Long> {

	List<BizSysLog> findFirst5000ByIdBetween(long stardId, long endId);

	List<BizSysLog> findByAccountIdAndAmountAndBankLogId(Integer accountId, BigDecimal amount, Long bankLogId);

	List<BizSysLog> findByAccountIdAndTypeAndOrderId(Integer accountId, int type, Long orderId);

	@Query(nativeQuery = true, value = "select * from biz_sys_log where account_id=?1 and status=1 and amount<0 and success_time is not null and create_time>=?2 order by success_time desc limit 1")
	BizSysLog find4Fee(int accountId, String stTm);

	List<BizSysLog> findByAccountIdAndBankLogIdInAndCreateTimeGreaterThan(Integer accountId, List<Long> bankLogIdIn,
			Date createTimeGreaterThan);
}