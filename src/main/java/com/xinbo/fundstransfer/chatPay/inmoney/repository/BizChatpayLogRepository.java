package com.xinbo.fundstransfer.chatPay.inmoney.repository;

import java.util.List;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;

/**
 * 	聊天支付记录（出款和入款的配对）DB 仓库
 * @author ERIC
 *
 */
public interface BizChatpayLogRepository extends BaseRepository<BizChatpayLog, Long> {

	BizChatpayLog findByIncomeOrderNoAndStatus(String incomeOrderNo, Integer status);

	List<BizChatpayLog> findByOutwardOrderNo(String outwardOrderNo);
}
