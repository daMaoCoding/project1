package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.pojo.FlowStatMatching;

public interface BankLogService {

	BizBankLog findByIdLessThanEqual(Integer fromAccount, Long id);

	/** 统计未匹配的流水总数 */
	HashMap<Integer, Integer> countUnmatchFlowsFromCache(List<String> accountIds);

	/** 统计未匹配的流水总数 */
	List<Object[]> countUnmatchFlows(List<String> accountIds, String startTime, String endTime);

	/** 统计未匹配流水的数量 */
	List<Object[]> countUnmatchBankLogs(List<Integer> fromAccountIdList, Date startTime, Date endTime);

	/** 查询冲正 */
	List<Object[]> queryBackWashBankLong(Pageable pageable, List<String> handicap, String fromAccount, String orderNo,
			String operator, List<Integer> status, BigDecimal amountStart, BigDecimal amountEnd, Date startTime,
			Date endTime);

	/** 查询冲正总记录数 */
	Long countBackWashBankLong(List<String> handicap, String fromAccount, String orderNo, String operator,
			List<Integer> status, BigDecimal amountStart, BigDecimal amountEnd, Date startTime, Date endTime);

	/** 查询冲正总金额 */
	BigDecimal sumBackWashBankLong(List<String> handicap, String fromAccount, String orderNo, String operator,
			List<Integer> status, BigDecimal amountStart, BigDecimal amountEnd, Date startTime, Date endTime);

	/** 查询银行流水 */
	Page<Object> bankLogList(Pageable pageable, BizBankLog bankLog, BigDecimal minAmount, BigDecimal maxAmount,
			Date startTime, Date endTime);

	/** 查询未认领入款流水 */
	Page<Object> noOwner4Income(Pageable pageable, BizBankLog bankLog, BigDecimal minAmount, BigDecimal maxAmount,
			Date startTime, Date endTime, List<Integer> handicapIdToList);

	/** 查询银行流水总金额 */
	List<Object> bankLogList_sumAmount(Pageable pageable, BizBankLog bankLog, BigDecimal minAmount,
			BigDecimal maxAmount, Date startTime, Date endTime);

	/** 查询总金额 入款未认领 */
	BigDecimal bankLogList_sumAmount4(Pageable pageable, BizBankLog bankLog, BigDecimal minAmount, BigDecimal maxAmount,
			Date startTime, Date endTime, List<Integer> handicapIdToList);

	String customerSendMsg(Long requestId, Long accountId, String message, String customerName);

	BizBankLog getBizBankLogByIdForUpdate(Long id);

	String findUnmatchForCompany(Integer[] accountIds, String member, Integer account, BigDecimal fromMoney,
			BigDecimal toMoney, String startTime, String endTime);

	Page<BizBankLog> findUnmatchForCompanyPage(Integer[] accountIds, String member, Integer account,
			BigDecimal fromMoney, BigDecimal toMoney, String startTime, String endTime, PageRequest pageRequest);

	BizBankLog findBankFlowById(Long id);

	/**
	 * 查询总金额
	 */
	String getSumAmount(String payMan, Integer status, Integer handicap, Integer level, Integer fromAccount,
			Integer[] accountId, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime);

	/**
	 * 查询银行流水 分页 无总记录数
	 */
	Page<BizBankLog> findBankLogPageNoCount(String payMan, Integer status, Integer handicap, Integer level,
			Integer[] fromAccount, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, PageRequest pageRequest);

	/**
	 * 统计入款银行卡 未匹配流水总数量 默认是当日 如果传入时间 则根据实际时间查询
	 *
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	List<Object[]> countFlowToMatchAllInAccount(Date startTime, Date endTime);

	/**
	 * 查询总记录数
	 */
	String getCount(String payMan, Integer status, Integer handicap, Integer level, Integer fromAccount,
			Integer[] accountId, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime);

	Page<BizBankLog> findAll(Pageable pageable);

	Page<BizBankLog> findAll(String operator, Specification<BizBankLog> specification, Integer accountId,
			String fieldval, Pageable pageable) throws Exception;

	BizBankLog get(Long id);

	/**
	 * 入款请求ID获取流水信息，该入款请求必须处于匹配状态
	 */
	BizBankLog findByIncomeReqId(Long id);

	BizBankLog save(BizBankLog entity);

	List<BizBankLog> save(List<BizBankLog> entities);

	BizBankLog update(BizBankLog entity);

	void delete(Long id);

	void updateBankLog(Long id, Integer status);

	List<BizBankLog> findAll(Specification<BizBankLog> specification);

	/**
	 * 根据账号id,当前时间与交易时间差查询匹配中金额大于零的数据的金额总和
	 */
	String querySumAmountCondition(Integer fromId, int hours);

	/**
	 * 查询所有转入的金额
	 */
	String queryIncomeTotal(Integer fromId);

	/**
	 * 查询所有转出的金额
	 */
	String queryOutTotal(Integer fromId);

	/**
	 * 流水总计
	 */
	BigDecimal[] findAmountTotal(SearchFilter[] filterToArray);

	/**
	 * 获取正在匹配的流水的统计数据
	 * 
	 * @param accountIdArray
	 *            账号ID集合
	 */
	List<FlowStatMatching> findFlowStat4Matching(Integer[] accountIdArray);

	List<Object> findSenderCard(String startTime, String endTime, List<Integer> handicaps);

	BizBankLog findRebateLimitBankLog(int accountId, BigDecimal amount, String startTime, String endTime);

	List<BizBankLog> finBanks(Date tradingStart, Date tradingEnd, int fromAccountId, int status, BigDecimal amount,
			String owner);

	void updateCsById(Long id, BigDecimal commission);

	void updateStatusRm(Long id, Integer status, String remark);

	int finCounts(int fromId, BigDecimal amount, String startTime, String endTime);

	int getDateTotalBankLog(int accountId);

	void updateBalanceByid(Long id, BigDecimal balance);
}
