package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizAccountExpOpr;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface AccountExpOprService {

	Page<BizAccountExpOpr> findPage(Date stTm, Date edTm, Integer[] handiArray, String alias, String accLike, String bank,
			String opr, Pageable pageable) throws Exception;

	void record(BizAccountExpOpr expOpr);

	void addOperator(Integer id, String operator, SysUser user);

	void addRemark(Integer id, String remark, SysUser user);
}
