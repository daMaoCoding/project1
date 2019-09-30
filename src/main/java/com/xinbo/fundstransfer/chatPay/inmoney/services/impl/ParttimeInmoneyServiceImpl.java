package com.xinbo.fundstransfer.chatPay.inmoney.services.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.chatPay.commons.enums.RedisKeyEnums;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.params.InmoneyNotifyInfoDto;
import com.xinbo.fundstransfer.chatPay.inmoney.params.P2pKillIncomDto;
import com.xinbo.fundstransfer.chatPay.inmoney.repository.BizAccountTranslogRepository;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

/**
 * 兼职代付单支付相关处理
 * 
 * @author ERIC
 *
 */
@Slf4j
@Service("ParttimeInmoneyServiceImpl")
public class ParttimeInmoneyServiceImpl extends InmoneyServiceImpl {
	@Autowired
	private BizAccountTranslogRepository accountTranslogRepository;
	@Autowired
	RedisService redisService;
	@Autowired
	private AccountService accountService;	
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	void killIncomeOrderDifferent(P2pKillIncomDto p2pKillIncomDto, BizChatpayLog chatpayLog) {
		log.info("兼职被踢暂时不做处理");
	}

	@Override
	public void inmoneyCancelDifferent(Long timestamp, BizChatpayLog chatpayLog, String remark) {
		BizAccountTranslog translog = chatpayLog.getTranslog();
		log.info("取消兼职代付记录: {}", translog.getBizId());
		
		translog.setStatus(2);
		translog.setFinshTranTime(new Date(timestamp));
		accountTranslogRepository.saveAndFlush(translog);
		
	}

	@Override
	InmoneyNotifyInfoDto inmoneyConfirmDifferent(Long timestamp, BizChatpayLog chatpayLog, String remark) {
		BizAccountTranslog translog = chatpayLog.getTranslog();
		log.info("确认兼职代付记录: {}",translog.getBizId());
		
		translog.setStatus(1);
		translog.setFinshTranTime(new Date(timestamp));
		accountTranslogRepository.saveAndFlush(translog);

		log.info("写入Redis的CHATPAY_SYN_REBATE_USER_ORDER，后续推送给返利网");
		try {
			redisService.rightPush(RedisKeyEnums.CHATPAY_SYN_REBATE_USER_ORDER.getKey(), mapper.writeValueAsString(toMap(translog)));
		} catch (JsonProcessingException e) {
			log.error("写入Redis的CHATPAY_SYN_REBATE_USER_ORDER报错", e);
		}
		return new InmoneyNotifyInfoDto(translog.getBizId(),translog.getCode(),translog.getRoomNum(),translog.getRoomToken());
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
		map.put("amount", translog.getMoney().negate());
		map.put("tradeTime", sdf.format(translog.getFinshTranTime()));
		map.put("catchTime", translog.getCreatetime() == null ? sdf.format(new Date()) : sdf.format(translog.getCreatetime()));
		return map;
	}
}
