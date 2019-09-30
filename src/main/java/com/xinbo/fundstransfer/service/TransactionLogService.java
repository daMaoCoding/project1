package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.BizTransactionLog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TransactionLogService {

	Page<BizTransactionLog> findAll(Pageable pageable) throws Exception;

	Page<BizTransactionLog> findAll(Specification<BizTransactionLog> specification, Pageable pageable) throws Exception;

	BigDecimal[] findAmountAndFeeByTotal(SearchFilter[] filterToArray);

	BizTransactionLog get(Long id);

	BizTransactionLog save(BizTransactionLog entity);

	/**
	 * 平账用，同步修改账号系统余额
	 * 
	 * @param entity
	 * @return
	 */
	BizTransactionLog flatBalance(BizTransactionLog entity);

	BizTransactionLog update(BizTransactionLog entity);

	void delete(Long id);

	/** 通过入款请求id查询 匹配操作人 */
	BizTransactionLog findByReqId(Long OrderId);

	/** 通过入款请求id 和 type查询 匹配操作人 */
	BizTransactionLog findByOrderIdAndType(Long orderId, Integer type);

	/** 通过银行流水id查询 */
	BizTransactionLog findByToBanklogId(Long toBanklogId);

	BizTransactionLog findByFromBanklogId(Long fromBanklogId);

	List<BizTransactionLog> findByTypeNotAndOrderIdIn(int typeNot, List<Long> orderIdIn);

	void updateByFromIdToIdAmount(Integer fromId,Integer toId,BigDecimal amount);

	boolean checkNonCredit(AccountBaseInfo base);
}
