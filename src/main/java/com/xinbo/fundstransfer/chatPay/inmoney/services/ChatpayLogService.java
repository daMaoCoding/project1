package com.xinbo.fundstransfer.chatPay.inmoney.services;

import java.util.List;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;

/**
 * 	聊天支付记录（出款和入款的配对） 服务
 * @author ERIC
 *
 */
public interface ChatpayLogService {
	/**
	 * 	根据入款单号找 聊天支付分配信息
	 * @param incomeOrderNo
	 * @return
	 */
	BizChatpayLog findByIncomeOrderNoAndStatus(String incomeOrderNo);
	
	/**
	 * 	根据出款单号找 聊天支付分配信息
	 * @param outwardOrderNo
	 * @return
	 */
	List<BizChatpayLog> findByOutwardOrderNo(String outwardOrderNo);
	
	/**
	 * 	更新聊天支付分配信息（包括：备注、状态等）
	 * @param chatpayLog
	 * @return
	 */
	BizChatpayLog save(BizChatpayLog chatpayLog);
	
}
