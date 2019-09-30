package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SysInvstRepository extends BaseRepository<BizSysInvst, Long> {

	List<BizSysInvst> findByErrorId(Long errorId);

	List<BizSysInvst> findByAccountIdAndBankLogId(Integer accountId, Long bankLogId);
}