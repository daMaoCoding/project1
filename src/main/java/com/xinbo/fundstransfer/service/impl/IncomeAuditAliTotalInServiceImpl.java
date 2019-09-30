package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.domain.repository.IncomeAuditAliTotalInRepository;
import com.xinbo.fundstransfer.service.IncomeAuditAliTotalInService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class IncomeAuditAliTotalInServiceImpl implements IncomeAuditAliTotalInService {
	static final Logger log = LoggerFactory.getLogger(IncomeAuditAliTotalInServiceImpl.class);
	@Autowired
	private IncomeAuditAliTotalInRepository incomeAuditAliTotalInRepository;
	@Autowired
	RequestBodyParser requestBodyParser;

	@Override
	public Map<String, Object> statisticalAliLog(int handicap, String AliNumber, String startTime, String endTime,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage = incomeAuditAliTotalInRepository.statisticalAliLog(handicap, AliNumber, startTime,
				endTime, pageRequest);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		return map;
	}

	@Override
	public Map<String, Object> findAliMatched(int handicap, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, String AliNumber,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		dataToPage = incomeAuditAliTotalInRepository.findAliMatched(handicap, startTime, endTime, member, orderNo,
				fromAmount, toAmount, payer, AliNumber, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = incomeAuditAliTotalInRepository.totalFindAliMatched(handicap, startTime, endTime,
				member, orderNo, fromAmount, toAmount, payer, AliNumber);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findAliCanceled(int handicap, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		dataToPage = incomeAuditAliTotalInRepository.findAliCanceled(handicap, startTime, endTime, member, orderNo,
				fromAmount, toAmount, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = incomeAuditAliTotalInRepository.totalFindAliCanceled(handicap, startTime, endTime,
				member, orderNo, fromAmount, toAmount);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", dataToPage);
		return map;
	}

	@Override
	public Map<String, Object> findAliUnClaim(int handicap, String startTime, String endTime, String member,
			String AliNo, BigDecimal fromAmount, BigDecimal toAmount, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		dataToPage = incomeAuditAliTotalInRepository.findAliUnClaim(handicap, startTime, endTime, member, AliNo,
				fromAmount, toAmount, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = incomeAuditAliTotalInRepository.totalFindAliUnClaim(handicap, startTime, endTime,
				member, AliNo, fromAmount, toAmount);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findMBAndInvoice(int AliId, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, PageRequest invoicePageRequest,
			PageRequest banklogPageRequest) throws Exception {
		// 查询入款单
		Page<Object> invoiceDataToPage = incomeAuditAliTotalInRepository.findInvoice(AliId, startTime, endTime, member,
				orderNo, fromAmount, toAmount, invoicePageRequest);
		// 入款单查询总计进行返回
		java.lang.Object[] invoiceTotal = incomeAuditAliTotalInRepository.totalFindInvoice(AliId, startTime, endTime,
				member, orderNo, fromAmount, toAmount);

		// 查询流水信息
		Page<Object> BankLogDataToPage = incomeAuditAliTotalInRepository.findBankLogMatch(AliId, startTime, endTime,
				payer, fromAmount, toAmount, banklogPageRequest);
		// 入款单查询总计进行返回
		java.lang.Object[] BankLogTotal = incomeAuditAliTotalInRepository.totalFindBankLogMatch(AliId, startTime,
				endTime, payer, fromAmount, toAmount);
		Map<String, Object> map = new HashMap<>();
		map.put("invoiceDataToPage", invoiceDataToPage);
		map.put("invoiceTotal", invoiceTotal);
		map.put("BankLogDataToPage", BankLogDataToPage);
		map.put("BankLogTotal", BankLogTotal);
		return map;
	}

}
