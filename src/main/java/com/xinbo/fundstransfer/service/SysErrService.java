package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizSysErr;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public interface SysErrService {

	Page<BizSysErr> findPage(Specification<BizSysErr> specification, Pageable pageable);

	BizSysErr save(AccountBaseInfo base, BigDecimal sysBal, BigDecimal bankBal);

	BizSysErr save(BizSysErr err, BigDecimal sysBal, BigDecimal bankBal);

	void delete(Long id);

	BizSysErr findById(Long id);

	BizSysErr findNotFinishedByAccId(Long accId);

	BizSysErr finished(BizSysErr err, SysUser operator, String remark);

	void lock(Long errId, SysUser operator) throws Exception;

	void unlock(Long errId, SysUser operator) throws Exception;

	void clean(Long errorId);
}
