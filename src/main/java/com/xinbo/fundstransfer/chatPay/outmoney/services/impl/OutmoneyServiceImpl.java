package com.xinbo.fundstransfer.chatPay.outmoney.services.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.repository.BizAccountTranslogRepository;
import com.xinbo.fundstransfer.chatPay.inmoney.services.ChatpayLogService;
import com.xinbo.fundstransfer.chatPay.outmoney.services.OutmoneyService;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.OrderDetail;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 	聊天室支付：出款相关服务（抽象类），子类包括 MemberOutmoneyServiceImpl（会员出款相关服务），ParttimeOutmoneyServiceImpl（兼职代收相关服务）
 * @author ERIC
 *
 */
@Slf4j
public abstract class OutmoneyServiceImpl implements OutmoneyService {
	@Autowired
	private ChatpayLogService chatpayLogService;
	@Autowired
	private OutwardRequestRepository outwardRequestRepository;
	@Autowired
	private BizAccountTranslogRepository bizAccountTranslogRepository;
	@Autowired
	private AccountRepository accountRepository;

	@Transactional
	@Override
	public void outmoneyOver(String outOrderNo, Long timestamp, Pair<BizOutwardRequest, BizAccountTranslog> entity) {
		List<BizChatpayLog> chatpayLogs = chatpayLogService.findByOutwardOrderNo(outOrderNo);
		for (BizChatpayLog cp : chatpayLogs) {
			if (cp.getStatus() != null && cp.getStatus().intValue() == 0) {
				log.error("出款单号：{}  的入款单 {} 还没有结束入款 ", outOrderNo, cp.getIncomeOrderNo());
				throw new IllegalArgumentException("出款单号：" + outOrderNo + " 的入款单 " + cp.getIncomeOrderNo() + " 还没有结束入款");
			}
		}

		List<OrderDetail> details = getDetailsByOutOrderNo(outOrderNo);
		BigDecimal outedMoney = details.stream().map(x -> x.getMoney()).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));		
		log.info("出款单号：{} 已完成的出款金额: {}", outOrderNo, outedMoney);
		
		log.info("结束出款单");
		outmoneyOverDifferent(entity, outOrderNo, timestamp, details, outedMoney);
	}
	
	abstract void outmoneyOverDifferent(Pair<BizOutwardRequest, BizAccountTranslog> entity, String outOrderNo, Long timestamp, List<OrderDetail> details, BigDecimal outedMoney);

	@Transactional
	@Override
	public void outmoneyNextStep(String outOrderNo, Long timestamp, Pair<BizOutwardRequest, BizAccountTranslog> entity) {
		List<OrderDetail> details = getDetailsByOutOrderNo(outOrderNo);
		BigDecimal outedMoney = details.stream().map(x -> x.getMoney()).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
		log.info("出款单号：{} 已完成的出款金额: {}", outOrderNo, outedMoney);
		
		log.info("对出款单进行下一步处理");
		outmoneyNextStepDifferent(entity, outOrderNo, timestamp, details, outedMoney);
	}
	
	abstract void outmoneyNextStepDifferent(Pair<BizOutwardRequest, BizAccountTranslog> entity, String outOrderNo, Long timestamp, List<OrderDetail> details, BigDecimal outedMoney);

	/**
	 * 根据会员出款单号找出所有的会员入款单
	 * 
	 * @param orderNo
	 * @return
	 */
	private List<OrderDetail> getDetailsByOutOrderNo(String orderNo) {
		List<BizChatpayLog> logs = chatpayLogService.findByOutwardOrderNo(orderNo);

		List<OrderDetail> detailList = new ArrayList<OrderDetail>();
		for (BizChatpayLog l : logs) {
			if (l.getStatus() != 1) {
				continue;
			}
			OrderDetail d = new OrderDetail();
			if (l.getIncomeType() != null && l.getIncomeType().intValue() == 1) {
				d.setFr((byte) 1);
				d.setUser_name(l.getIncomeRequest().getMemberUserName());
			} else {
				d.setFr((byte) 2);
				BizAccountTranslog translog = l.getTranslog();
				BizAccount account = accountRepository.findById2(translog.getAccountId().intValue());
				if (account == null) {
					log.error("账号ID：{}  在账号表中 不存在 ", translog.getAccountId());
					throw new IllegalArgumentException("账号ID：" + translog.getAccountId() + " 在账号表中 不存在");
				}
				d.setUser_name(account.getAlias());
			}
			d.setMoney(l.getAmount());
			d.setOrder_no(l.getIncomeOrderNo());
			detailList.add(d);
		}
		return detailList;
	}
	
	public Pair<BizOutwardRequest, BizAccountTranslog> getOutmoneyEntity(String outOrderNo) {
		BizOutwardRequest outward = outwardRequestRepository.findByOrderNo(outOrderNo);
		if (outward != null) { // 会员出款
			return Pair.of(outward, null);
		} else { // 兼职出款
			BizAccountTranslog translog = bizAccountTranslogRepository.findByCode(outOrderNo);
			if (translog == null) {
				log.error("出款单号：{}  在出款单表和兼职交易记录中都不存在 ", outOrderNo);
				throw new IllegalArgumentException("出款单号：" + outOrderNo + " 在出款单表和兼职交易记录中都不存在");
			}
			return Pair.of(null, translog);
		}
	}
}
