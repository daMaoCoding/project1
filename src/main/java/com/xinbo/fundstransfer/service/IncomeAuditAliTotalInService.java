package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.BizAliLog;

public interface IncomeAuditAliTotalInService {

	/**
	 * 根据参数统计入款支付宝未匹配的流水
	 * 
	 * @param handicap
	 * @param AliNumber
	 * @param type
	 * @param startTime
	 * @param endTime
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> statisticalAliLog(int handicap, String AliNumber, String startTime, String endTime,
			PageRequest pageRequest) throws Exception;

	/**
	 * 根据支付宝号查询流水和入款单信息
	 * 
	 * @param AliId
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param invoicePageRequest
	 * @param banklogPageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findMBAndInvoice(int AliId, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer, PageRequest invoicePageRequest,
			PageRequest banklogPageRequest) throws Exception;

	/**
	 * 查询支付宝入款已经匹配的数据
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param AliNumber
	 * @param type
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findAliMatched(int handicap, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer, String AliNumber, PageRequest pageRequest)
			throws Exception;

	/**
	 * 查询取消的单
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findAliCanceled(int handicap, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, PageRequest pageRequest) throws Exception;

	/**
	 * 查询未认领的流水
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param AliNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findAliUnClaim(int handicap, String startTime, String endTime, String member, String AliNo,
			BigDecimal fromAmount, BigDecimal toAmount, PageRequest pageRequest) throws Exception;

}
