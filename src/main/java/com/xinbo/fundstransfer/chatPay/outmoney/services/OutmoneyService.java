package com.xinbo.fundstransfer.chatPay.outmoney.services;

import org.apache.commons.lang3.tuple.Pair;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;

/**
 * 	聊天室支付：出款相关服务
 * @author ERIC
 *
 */
public interface OutmoneyService {
	/**
	 * 	结束出款任务(出款任务可能完全没完成，也可能部分没完成。)
	 * @param outOrderNo
	 * @param timestamp
	 */
	void outmoneyOver(String outOrderNo, Long timestamp, Pair<BizOutwardRequest, BizAccountTranslog> entity);
	
	/**
	 * 	出款下一步处理(判断出款是否完成，完成就结束出款任务，未完成就继续分配)
	 * @param outOrderNo
	 * @param timestamp
	 */
	void outmoneyNextStep(String outOrderNo, Long timestamp, Pair<BizOutwardRequest, BizAccountTranslog> entity);
	
	/**
	 * 根据出款单号找到会员出款单或兼职的代收单
	 * @param outOrderNo
	 * @return
	 */
	Pair<BizOutwardRequest, BizAccountTranslog> getOutmoneyEntity(String outOrderNo);
}
