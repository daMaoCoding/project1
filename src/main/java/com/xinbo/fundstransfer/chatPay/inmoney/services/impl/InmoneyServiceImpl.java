package com.xinbo.fundstransfer.chatPay.inmoney.services.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.params.InmoneyNotifyInfoDto;
import com.xinbo.fundstransfer.chatPay.inmoney.params.P2pKillIncomDto;
import com.xinbo.fundstransfer.chatPay.inmoney.services.ChatpayLogService;
import com.xinbo.fundstransfer.chatPay.inmoney.services.InmoneyService;
import com.xinbo.fundstransfer.chatPay.outmoney.params.P2pOutwardConfimDto;
import com.xinbo.fundstransfer.chatPay.outmoney.services.OutmoneyService;
import com.xinbo.fundstransfer.component.net.http.restTemplate.CallCenterServiceApiStatic;

import lombok.extern.slf4j.Slf4j;

/**
 * 	聊天室支付：入款相关服务（抽象类），子类包括 MemberInmoneyServiceImpl（会员入款相关服务），ParttimeInmoneyServiceImpl（兼职代付相关服务）
 * @author ERIC
 *
 */
@Slf4j
public abstract class InmoneyServiceImpl implements InmoneyService {
	@Autowired
	private ChatpayLogService chatpayLogService;
	@Autowired
	@Qualifier("ParttimeOutmoneyServiceImpl")
	private OutmoneyService parttimeOutmoneyService;
	@Autowired
	@Qualifier("MemberOutmoneyServiceImpl")
	private OutmoneyService memberOutmoneyService;	
	@Autowired
	private CallCenterServiceApiStatic callCenterServiceApiStatic;

	@Override
	public BizChatpayLog appendChatpayLogRemark(BizChatpayLog chatpayLog, String remark) {
		log.info("1. 聊天支付记录id:{} 更新备注：{}", chatpayLog.getId(), remark);
		chatpayLog.appendRemark(remark);
		chatpayLogService.save(chatpayLog);
		return chatpayLog;
	}

	@Transactional
	@Override
	public void killIncomeOrder(P2pKillIncomDto p2pKillIncomDto, BizChatpayLog chatpayLog) {
		String orderNo = p2pKillIncomDto.getOrderNumber();
		String remark = String.format("入款方 %s 不支付超时被客服 %s 踢出", orderNo, p2pKillIncomDto.getOptAccount());
		log.info("1. {}", remark);
		inmoneyCancel(p2pKillIncomDto.getTimestamp(), chatpayLog, remark);
		
		log.info("2. 将入款人员拉黑");
		killIncomeOrderDifferent(p2pKillIncomDto, chatpayLog);
	}
	
	abstract void killIncomeOrderDifferent(P2pKillIncomDto p2pKillIncomDto, BizChatpayLog chatpayLog);

	@Transactional
	@Override
	public void inmoneyCancel(Long timestamp, BizChatpayLog chatpayLog, String remark) {
		log.info("1. 取消聊天支付记录: {}", chatpayLog.getId());
		chatpayLog.appendRemark(remark);
		chatpayLog.setStatus(2);
		chatpayLogService.save(chatpayLog);

		log.info("2. 取消入款订单: {}", chatpayLog.getIncomeOrderNo());
		inmoneyCancelDifferent(timestamp, chatpayLog, remark);
		
		log.info("3. 出款单进行下一步操作: {}", chatpayLog.getOutwardOrderNo());
		if(chatpayLog.outwardIsMember()) {
			memberOutmoneyService.outmoneyNextStep(chatpayLog.getOutwardOrderNo(), timestamp, Pair.of(chatpayLog.getOutwardRequest(), null));
		} else {
			parttimeOutmoneyService.outmoneyNextStep(chatpayLog.getOutwardOrderNo(), timestamp, Pair.of(null, chatpayLog.getTranslog()));
		}
	}
	
	abstract void inmoneyCancelDifferent(Long timestamp, BizChatpayLog chatpayLog, String remark);

	@Transactional
	@Override
	public void inmoneyConfirm(Long timestamp, BizChatpayLog chatpayLog, String remark, boolean isNotify) {
		log.info("1. 确认聊天支付记录: {}", chatpayLog.getId());
		chatpayLog.appendRemark(remark);
		chatpayLog.setStatus(1);
		chatpayLogService.save(chatpayLog);
		
		log.info("2. 确认入款单或兼职代付记录: {}", chatpayLog.getIncomeOrderNo());
		InmoneyNotifyInfoDto inmoneyNotifyInfo = inmoneyConfirmDifferent(timestamp, chatpayLog, remark);
		
		if (isNotify) {
			log.info("3. 调用客服系统：向出款人员显示入款金额和参数，调用客服系统自动踢掉入款人员（提示入款成功）");
			if (!callCenterServiceApiStatic.inUserPaySuccessMatchingWater(inmoneyNotifyInfo,chatpayLog.getOutwardRequest() )) {
				log.error("单号：{}  通知客服系统流水匹配成功失败 ", chatpayLog.getIncomeRequest().getOrderNo());
			}
		}

		log.info("4. 出款单进行下一步操作: {}", chatpayLog.getOutwardOrderNo());
		if(chatpayLog.outwardIsMember()) {
			memberOutmoneyService.outmoneyNextStep(chatpayLog.getOutwardOrderNo(), timestamp, Pair.of(chatpayLog.getOutwardRequest(), null));
		} else {
			parttimeOutmoneyService.outmoneyNextStep(chatpayLog.getOutwardOrderNo(), timestamp, Pair.of(null, chatpayLog.getTranslog()));
		}		
	}
	
	abstract InmoneyNotifyInfoDto inmoneyConfirmDifferent(Long timestamp, BizChatpayLog chatpayLog, String remark);

	@Override
	public void outwardSideInmoneyConfirm(P2pOutwardConfimDto p2pOutwardConfimDto, BizChatpayLog chatpayLog) {
		log.info("1. 出款方确认入款单或兼职代付记录: {}", p2pOutwardConfimDto.getOrderNumber());
		
		inmoneyConfirm(p2pOutwardConfimDto.getTimestamp(), chatpayLog, "出款方确认成功", false);
	}
}
