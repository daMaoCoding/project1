package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface SysLogService {

	List<BizSysLog> findByAccountIdAndBankLogIdInAndCreateTimeGreaterThan(Integer accountId, List<Long> bankLogIdIn,
			Date createTimeGreaterThan);

	Page<BizSysInvst> findPage4Invst(Specification<BizSysInvst> specification, Pageable pageable);

	BigDecimal[] findAmountTotal4Invst(SearchFilter[] filterToArray);

	Page<BizSysLog> findPage(Specification<BizSysLog> specification, Pageable pageable);

	BigDecimal[] findTotal(SearchFilter[] filterToArray);

	BigDecimal[] findAmountTotal(SearchFilter[] filterToArray);
}
