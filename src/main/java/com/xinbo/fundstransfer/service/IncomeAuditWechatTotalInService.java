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
import com.xinbo.fundstransfer.domain.entity.BizAliLog;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizWechatLog;
import com.xinbo.fundstransfer.domain.entity.SysUser;

public interface IncomeAuditWechatTotalInService {

	/**
	 * 根据参数统计入款微信未匹配的流水
	 * 
	 * @param handicap
	 * @param wechatNumber
	 * @param type
	 * @param startTime
	 * @param endTime
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> statisticalWechatLog(int handicap, String wechatNumber, String startTime, String endTime,
			PageRequest pageRequest) throws Exception;

	/**
	 * 根据微信号查询流水和入款单信息
	 * 
	 * @param wechatId
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
	Map<String, Object> findMBAndInvoice(int wechatId, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer, PageRequest invoicePageRequest,
			PageRequest banklogPageRequest) throws Exception;

	/**
	 * 查询微信入款已经匹配的数据
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param wechatNumber
	 * @param type
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findWechatMatched(int handicap, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer, String wechatNumber, PageRequest pageRequest)
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
	Map<String, Object> findWechatCanceled(int handicap, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, PageRequest pageRequest) throws Exception;

	/**
	 * 查询未认领的流水
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param wechatNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findWechatUnClaim(int handicap, String startTime, String endTime, String member,
			String wechatNo, BigDecimal fromAmount, BigDecimal toAmount, PageRequest pageRequest) throws Exception;

}
