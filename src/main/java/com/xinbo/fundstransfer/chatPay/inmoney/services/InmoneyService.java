package com.xinbo.fundstransfer.chatPay.inmoney.services;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.params.P2pKillIncomDto;
import com.xinbo.fundstransfer.chatPay.outmoney.params.P2pOutwardConfimDto;

/**
 * 	聊天室支付：入款相关服务
 * @author ERIC
 *
 */
public interface InmoneyService {
	/**
	 * 	添加聊天支付记录的备注
	 */
	BizChatpayLog appendChatpayLogRemark(BizChatpayLog chatpayLog, String remark);
	
	/**
	 *	 入款方不支付超时被客服踢出
	 * @param p2pKillIncomDto
	 */
	void killIncomeOrder(P2pKillIncomDto p2pKillIncomDto, BizChatpayLog chatpayLog);
	
	/**
	 * 	入款单取消入款
	 * @param incomeRequest
	 * @param timestamp
	 * @param chatpayLog
	 * @param remark
	 */
	void inmoneyCancel(Long timestamp, BizChatpayLog chatpayLog, String remark);
	
	/**
	 * 入款单确认入款
	 * @param incomRequest	入款单
	 * @param timestamp		时间戳
	 * @param chatpayLog	聊天支付记录
	 * @param remark		备注
	 * @param isNotify		是否需要通知出款人员和入款人员
	 */
	void inmoneyConfirm(Long timestamp, BizChatpayLog chatpayLog, String remark, boolean isNotify);
	
	/**
	 * 	出款方确认入款成功
	 * @param p2pOutwardConfimDto
	 */
	void outwardSideInmoneyConfirm(P2pOutwardConfimDto p2pOutwardConfimDto, BizChatpayLog chatpayLog);	
}
