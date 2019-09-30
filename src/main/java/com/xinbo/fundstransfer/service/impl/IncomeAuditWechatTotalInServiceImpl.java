package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.domain.repository.IncomeAuditWechatTotalInRepository;
import com.xinbo.fundstransfer.service.IncomeAuditWechatTotalInService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class IncomeAuditWechatTotalInServiceImpl implements IncomeAuditWechatTotalInService {
	static final Logger log = LoggerFactory.getLogger(IncomeAuditWechatTotalInServiceImpl.class);
	@Autowired
	private IncomeAuditWechatTotalInRepository incomeAuditWechatTotalInRepository;
	@Autowired
	RequestBodyParser requestBodyParser;

	@Override
	public Map<String, Object> statisticalWechatLog(int handicap, String wechatNumber, String startTime, String endTime,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage = incomeAuditWechatTotalInRepository.statisticalWechatLog(handicap, wechatNumber,
				startTime, endTime, pageRequest);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		return map;
	}

	@Override
	public Map<String, Object> findWechatMatched(int handicap, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, String wechatNumber,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		dataToPage = incomeAuditWechatTotalInRepository.findWechatMatched(handicap, startTime, endTime, member, orderNo,
				fromAmount, toAmount, payer, wechatNumber, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = incomeAuditWechatTotalInRepository.totalFindWechatMatched(handicap, startTime,
				endTime, member, orderNo, fromAmount, toAmount, payer, wechatNumber);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findWechatCanceled(int handicap, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		dataToPage = incomeAuditWechatTotalInRepository.findWechatCanceled(handicap, startTime, endTime, member,
				orderNo, fromAmount, toAmount, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = incomeAuditWechatTotalInRepository.totalFindWechatCanceled(handicap, startTime,
				endTime, member, orderNo, fromAmount, toAmount);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findWechatUnClaim(int handicap, String startTime, String endTime, String member,
			String wechatNo, BigDecimal fromAmount, BigDecimal toAmount, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		dataToPage = incomeAuditWechatTotalInRepository.findWechatUnClaim(handicap, startTime, endTime, member,
				wechatNo, fromAmount, toAmount, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = incomeAuditWechatTotalInRepository.totalFindWechatUnClaim(handicap, startTime,
				endTime, member, wechatNo, fromAmount, toAmount);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findMBAndInvoice(int wechatId, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, PageRequest invoicePageRequest,
			PageRequest banklogPageRequest) throws Exception {
		// 查询入款单
		Page<Object> invoiceDataToPage = incomeAuditWechatTotalInRepository.findInvoice(wechatId, startTime, endTime,
				member, orderNo, fromAmount, toAmount, invoicePageRequest);
		// 入款单查询总计进行返回
		java.lang.Object[] invoiceTotal = incomeAuditWechatTotalInRepository.totalFindInvoice(wechatId, startTime,
				endTime, member, orderNo, fromAmount, toAmount);

		// 查询流水信息
		Page<Object> BankLogDataToPage = incomeAuditWechatTotalInRepository.findBankLogMatch(wechatId, startTime,
				endTime, payer, fromAmount, toAmount, banklogPageRequest);
		// 入款单查询总计进行返回
		java.lang.Object[] BankLogTotal = incomeAuditWechatTotalInRepository.totalFindBankLogMatch(wechatId, startTime,
				endTime, payer, fromAmount, toAmount);
		Map<String, Object> map = new HashMap<>();
		map.put("invoiceDataToPage", invoiceDataToPage);
		map.put("invoiceTotal", invoiceTotal);
		map.put("BankLogDataToPage", BankLogDataToPage);
		map.put("BankLogTotal", BankLogTotal);
		return map;
	}

}
