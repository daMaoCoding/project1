package com.xinbo.fundstransfer.chatPay.inmoney.services.impl;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.params.InmoneyNotifyInfoDto;
import com.xinbo.fundstransfer.chatPay.inmoney.params.P2pKillIncomDto;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.CancelInOrderBack;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.ChangeUserChatPayBlacklistBack;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.SureInOrderBack;
import com.xinbo.fundstransfer.component.net.http.restTemplate.PlatformServiceApiStatic;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.repository.IncomeRequestRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 会员入款单支付相关处理
 * @author ERIC
 *
 */
@Slf4j
@Service("MemberInmoneyServiceImpl")
public class MemberInmoneyServiceImpl extends InmoneyServiceImpl {
	@Autowired
	private PlatformServiceApiStatic platformServiceApiStatic;
	@Autowired
	private IncomeRequestRepository incomeRequestRepository;
	
	@Override
	void killIncomeOrderDifferent(P2pKillIncomDto p2pKillIncomDto, BizChatpayLog chatpayLog) {
		BizIncomeRequest incomeRequest = chatpayLog.getIncomeRequest();
		log.info("盘口：{}，会员：{} 通知平台拉黑名单，不能继续使用该通道", incomeRequest.findOid(), incomeRequest.getMemberUserName());

		ChangeUserChatPayBlacklistBack blackList = new ChangeUserChatPayBlacklistBack();
		blackList.setOid(incomeRequest.findOid());
		blackList.setType(new Integer(1).byteValue());
		blackList.setUser_name(incomeRequest.getMemberUserName());
		blackList.setAdmin_type(new Integer(1).byteValue());
		blackList.setAdmin_name(p2pKillIncomDto.getOrderNumber());
		blackList.setChannel_type(incomeRequest.findChannelType());
		blackList.setOpt_time(p2pKillIncomDto.getTimestamp());
		platformServiceApiStatic.changeUserChatPayBlacklist(blackList);
		
	}

	@Override
	public void inmoneyCancelDifferent(Long timestamp, BizChatpayLog chatpayLog, String remark) {
		BizIncomeRequest incomeRequest = chatpayLog.getIncomeRequest();
		Integer oid = incomeRequest.findOid();
		String orderNo = incomeRequest.getOrderNo();		

		log.info("取消入款订单incomeRequest");
		incomeRequestRepository.cancelOrder(incomeRequest.getHandicap(), Collections.singletonList(incomeRequest.getOrderNo()));

		log.info("平台取消订单");
		CancelInOrderBack cancelInOrderBack = new CancelInOrderBack();
		cancelInOrderBack.setOid(oid);
		cancelInOrderBack.setCode(orderNo);
		cancelInOrderBack.setCancel_time(System.currentTimeMillis());
		platformServiceApiStatic.cancelInOrder(cancelInOrderBack);
		
	}

	@Override
	InmoneyNotifyInfoDto inmoneyConfirmDifferent(Long timestamp, BizChatpayLog chatpayLog, String remark) {
		BizIncomeRequest incomRequest = chatpayLog.getIncomeRequest();
		log.info("确认入款订单incomeRequest");
		incomeRequestRepository.updateStatusById(incomRequest.getId(), 1);
		
		log.info("平台确认订单");
		SureInOrderBack sureInOrderBack = new SureInOrderBack();
		sureInOrderBack.setOid(incomRequest.findOid());
		sureInOrderBack.setCode(incomRequest.getOrderNo());
		sureInOrderBack.setIn_time(timestamp);
		platformServiceApiStatic.sureInOrder(sureInOrderBack);

		return new InmoneyNotifyInfoDto(incomRequest.getChatPayBizId(),incomRequest.getOrderNo(),incomRequest.getChatPayRoomNum(),incomRequest.getChatPayRoomToken());
	}
}
