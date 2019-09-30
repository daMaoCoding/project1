package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizSysErr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

public interface SysErrRepository extends BaseRepository<BizSysErr, Long> {

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_sys_err set status=?3,collector=?4,collect_time=?5 ,consume_time=?6,remark=?7 where id=?1 and status=?2")
	void updInf(Long id, Integer frSt, Integer toSt, Integer collector, Date collectTime, Long consumeTime,
			String remark);

	@Query(nativeQuery = true, value = "select * from biz_sys_err where target=?1 and status not in (3,4) limit 1")
	BizSysErr findNotFinishedByAccId(Long accId);


}