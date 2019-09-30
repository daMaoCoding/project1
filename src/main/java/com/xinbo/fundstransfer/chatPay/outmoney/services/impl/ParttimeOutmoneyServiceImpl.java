package com.xinbo.fundstransfer.chatPay.outmoney.services.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.chatPay.commons.enums.RedisKeyEnums;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.chatPay.inmoney.repository.BizAccountTranslogRepository;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.OrderDetail;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

/**
 * 	聊天室支付：兼职代收相关服务
 * @author ERIC
 *
 */
@Slf4j
@Service("ParttimeOutmoneyServiceImpl")
public class ParttimeOutmoneyServiceImpl extends OutmoneyServiceImpl {
	@Autowired
	private BizAccountTranslogRepository bizAccountTranslogRepository;
	@Autowired
	RedisService redisService;
	@Autowired
	private AccountService accountService;	
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	void outmoneyOverDifferent(Pair<BizOutwardRequest, BizAccountTranslog> entity, String outOrderNo, Long timestamp, List<OrderDetail> details, BigDecimal outedMoney) {
		BizAccountTranslog translog = entity.getRight();
		if (outedMoney.abs().compareTo(new BigDecimal(0.001)) < 0) {  
			log.info("出款取消 {}", translog.getBizId());
			translog.setStatus(2);
		} else { 
			log.info("出款完成 {}", translog.getBizId());
			translog.setStatus(1);
			log.info("写入Redis的CHATPAY_SYN_REBATE_USER_ORDER，后续推送给返利网");
			try {
				redisService.rightPush(RedisKeyEnums.CHATPAY_SYN_REBATE_USER_ORDER.getKey(), mapper.writeValueAsString(toMap(translog)));
			} catch (JsonProcessingException e) {
				log.error("写入Redis的CHATPAY_SYN_REBATE_USER_ORDER报错", e);
			}			
		}
		translog.setFinshTranTime(new Date(timestamp));
		bizAccountTranslogRepository.saveAndFlush(translog);	
	}
	
	@Override
	void outmoneyNextStepDifferent(Pair<BizOutwardRequest, BizAccountTranslog> entity, String outOrderNo, Long timestamp, List<OrderDetail> details, BigDecimal outedMoney) {
		BizAccountTranslog translog = entity.getRight();
		if ((outedMoney.abs().compareTo(new BigDecimal(0.001)) < 0)) {
			log.info("出款取消 {}", translog.getBizId());
			translog.setStatus(2); 
		} else {
			log.info("出款完成 {}", translog.getBizId());
			translog.setStatus(1); 
			log.info("写入Redis的CHATPAY_SYN_REBATE_USER_ORDER，后续推送给返利网");
			try {
				redisService.rightPush(RedisTopics.BANK_STATEMENT, mapper.writeValueAsString(toMap(translog)));
			} catch (JsonProcessingException e) {
				log.error("写入Redis的CHATPAY_SYN_REBATE_USER_ORDER报错", e);
			}			
		}
		translog.setFinshTranTime(new Date(timestamp));
		bizAccountTranslogRepository.saveAndFlush(translog);
	}
	
	private Map<String, Object> toMap(BizAccountTranslog translog) {
		String tradeType = "2";
		if(translog.getAccountId() == null) {
			throw new IllegalArgumentException("BizAccountTranslog:" + translog.getId() + "的account_id为空");
		}
		AccountBaseInfo account = accountService.getFromCacheById(translog.getAccountId().intValue());
		if(account == null) {
			throw new IllegalArgumentException("账号ID："+translog.getAccountId()+"在biz_account中不存在");
		}
		if(account.getType() != null && account.getType().intValue() == AccountType.InAccountFlwZfb.getTypeId().intValue()) {
			tradeType = "1";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("uid", translog.getRebateUid());
		map.put("tradeType", tradeType);
		map.put("tradeStatus", "1");
		map.put("tradeNo", translog.getCode());
		map.put("roomNo", translog.getRoomNum());
		map.put("creator", translog.getMember());
		map.put("receiver", account.getAlias());
		map.put("chkCode", translog.getRoomToken());
		map.put("amount", translog.getMoney());
		map.put("tradeTime", sdf.format(new Date()));
		map.put("catchTime", translog.getCreatetime() == null ? sdf.format(new Date()) : sdf.format(translog.getCreatetime()));
		return map;
	}	
}
