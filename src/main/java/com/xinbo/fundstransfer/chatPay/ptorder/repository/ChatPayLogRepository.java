package com.xinbo.fundstransfer.chatPay.ptorder.repository;

import java.util.List;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;

public interface ChatPayLogRepository extends BaseRepository<BizChatpayLog, Long> {

	BizChatpayLog findByIncomeOrderNoAndStatus(String incomeOrderNo, Integer status);

	List<BizChatpayLog> findByOutwardOrderNo(String outwardOrderNo);
}
