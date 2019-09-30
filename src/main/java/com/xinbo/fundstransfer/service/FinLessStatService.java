package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.data.domain.PageRequest;

import com.xinbo.fundstransfer.domain.entity.SysUser;

public interface FinLessStatService {

	/**
	 * 入款亏损
	 * 
	 * @param parmap
	 * @retu rn
	 * @throws Exception
	 */
	Map<String, Object> findFinInLessStatis(Map<String, Object> parmap) throws Exception;

	Map<String, Object> findHistory(int accountId, PageRequest pageRequest) throws Exception;

	void freezingProcess(Long id, Long traceId, String uid, String remark, String oldRemark, String type,
			BigDecimal jAmount, String derating) throws Exception;

	Map<String, Object> findFinPending(Map<String, Object> parmap) throws Exception;

	String findStatus(Long id) throws Exception;

	String findOldRemark(Long id) throws Exception;

	void jieDongMoney(Integer uid, String remark, Long id, BigDecimal amount, String type) throws Exception;

	void accomplish(String remark, Long id, String status) throws Exception;

	void cashflow(String remark, Long id) throws Exception;

	void addTrace(Integer id, BigDecimal bankBalance) throws Exception;

	void updateAccountStatus(Long id, SysUser user) throws Exception;

	int findCountsById(int accountId, String type) throws Exception;

	String findCarCountsById(int accountId) throws Exception;

	String findThirdCountsById(String account) throws Exception;

	boolean derating(Integer accountId, BigDecimal derateAmount, String uid, String remark) throws Exception;

}
