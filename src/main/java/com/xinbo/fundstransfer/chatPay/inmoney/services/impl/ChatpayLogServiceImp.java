package com.xinbo.fundstransfer.chatPay.inmoney.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.repository.BizAccountTranslogRepository;
import com.xinbo.fundstransfer.chatPay.inmoney.repository.BizChatpayLogRepository;
import com.xinbo.fundstransfer.chatPay.inmoney.services.ChatpayLogService;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.repository.IncomeRequestRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 	聊天支付记录（出款和入款的配对） 服务
 * @author ERIC
 *
 */
@Slf4j
@Service
public class ChatpayLogServiceImp implements ChatpayLogService {
	private static Integer CHATPAYLOG_DOING = 0;
	@Autowired
	private BizChatpayLogRepository bizChatpayLogRepository;
	@Autowired
	private IncomeRequestRepository incomeRequestRepository;
	@Autowired
	private OutwardRequestRepository outwardRequestRepository;
	@Autowired
	private BizAccountTranslogRepository bizAccountTranslogRepository;
	
	@Override
	public BizChatpayLog findByIncomeOrderNoAndStatus(String incomeOrderNo) {
		BizChatpayLog chatpayLog = bizChatpayLogRepository.findByIncomeOrderNoAndStatus(incomeOrderNo, CHATPAYLOG_DOING);
		if (chatpayLog == null) {
			log.error("单号：{} 不存在", incomeOrderNo);
			throw new IllegalArgumentException("单号：" + incomeOrderNo + " 不存在");
		}
		fillIncome(chatpayLog);
		fillOutward(chatpayLog);
		return chatpayLog;
	}

	@Override
	public List<BizChatpayLog> findByOutwardOrderNo(String outwardOrderNo) {
		List<BizChatpayLog> logs = bizChatpayLogRepository.findByOutwardOrderNo(outwardOrderNo);
		for (BizChatpayLog l : logs) {
			fillIncome(l);
			fillOutward(l);
		}
		return logs;
	}
	
	@Override
	public BizChatpayLog save(BizChatpayLog chatpayLog) {
		return bizChatpayLogRepository.saveAndFlush(chatpayLog);
	}
	
	/**
	 * 填充BizChatpayLog的入款信息
	 * @param chatpayLog
	 * @param incomeOrderNo
	 */
	private void fillIncome(BizChatpayLog chatpayLog) {
		if(chatpayLog.getIncomeOrderNo() == null) {
			log.error("ID：{} 聊天支付记录中入款单号为空", chatpayLog.getId());
			throw new IllegalArgumentException("ID：" + chatpayLog.getId() + " 聊天支付记录中入款单号为空");
		}
		if(chatpayLog.getIncomeType() == null) {
			log.error("ID：{} 聊天支付记录中入款类型为空", chatpayLog.getId());
			throw new IllegalArgumentException("ID：" + chatpayLog.getId() + " 聊天支付记录中入款类型为空");
		}
		String incomeOrderNo = chatpayLog.getIncomeOrderNo();
		if(chatpayLog.getIncomeType().intValue()==1) { //会员入款
			List<BizIncomeRequest> orders = incomeRequestRepository.findByOrderNo(incomeOrderNo);
			if (orders == null || orders.size() != 1) {
				log.error("单号：{}  在入款单表中的个数 {} ", incomeOrderNo, orders == null ? 0 : orders.size());
				throw new IllegalArgumentException("单号：" + incomeOrderNo + " 在入款单表中不存在");
			}
			chatpayLog.setIncomeRequest(orders.get(0));			
		} else {	//兼职代付
			BizAccountTranslog translog = bizAccountTranslogRepository.findByCode(incomeOrderNo);
			if (translog == null) {
				log.error("单号：{}  在兼职交易记录表中 不存在 ", incomeOrderNo);
				throw new IllegalArgumentException("单号：" + incomeOrderNo + " 在兼职交易记录表中不存在");
			}
			chatpayLog.setTranslog(translog);
		}
	}
	
	/**
	 * 填充BizChatpayLog的出款信息
	 * @param chatpayLog
	 * @param incomeOrderNo
	 */
	private void fillOutward(BizChatpayLog chatpayLog) {
		if(chatpayLog.getOutwardOrderNo() == null) {
			log.error("ID：{} 聊天支付记录中出款单号为空", chatpayLog.getId());
			throw new IllegalArgumentException("ID：" + chatpayLog.getId() + " 聊天支付记录中出款单号为空");			
		}
		if(chatpayLog.getOutwardType() == null) {
			log.error("ID：{} 聊天支付记录中出款类型为空", chatpayLog.getId());
			throw new IllegalArgumentException("ID：" + chatpayLog.getId() + " 聊天支付记录中出款类型为空");
		}
		String outwardOrderNo = chatpayLog.getOutwardOrderNo();
		if(chatpayLog.getOutwardType().intValue()==1) { //会员出款
			BizOutwardRequest outRequest = outwardRequestRepository.findByOrderNo(outwardOrderNo);
			if (outRequest == null) {
				log.error("单号：{}  在出款单表中不存在 ", outwardOrderNo);
				throw new IllegalArgumentException("单号：" + outwardOrderNo + " 在出款单表中不存在");
			}
			chatpayLog.setOutwardRequest(outRequest);			
		} else {	//兼职代收
			BizAccountTranslog translog = bizAccountTranslogRepository.findByCode(outwardOrderNo);
			if (translog == null) {
				log.error("单号：{}  在兼职交易记录表中 不存在 ", outwardOrderNo);
				throw new IllegalArgumentException("单号：" + outwardOrderNo + " 在兼职交易记录表中不存在");
			}
			chatpayLog.setTranslog(translog);
		}
	}

}
