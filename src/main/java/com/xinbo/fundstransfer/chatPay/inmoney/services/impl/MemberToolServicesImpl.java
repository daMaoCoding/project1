package com.xinbo.fundstransfer.chatPay.inmoney.services.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizChatpayLog;
import com.xinbo.fundstransfer.chatPay.inmoney.params.BillLogDto;
import com.xinbo.fundstransfer.chatPay.inmoney.services.ChatpayLogService;
import com.xinbo.fundstransfer.chatPay.inmoney.services.InmoneyService;
import com.xinbo.fundstransfer.chatPay.inmoney.services.MemberToolServices;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.repository.IncomeRequestRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 	支付宝和微信工具上报流水 然后进行匹配的服务
 * @author ERIC
 *
 */
@Slf4j
@Service
public class MemberToolServicesImpl implements MemberToolServices {
	@Autowired
	private ChatpayLogService chatpayLogService;	
	@Autowired
	private IncomeRequestRepository incomeRequestRepository;
	@Autowired
	@Qualifier("ParttimeInmoneyServiceImpl")
	private InmoneyService parttimeInmoneyService;
	@Autowired
	@Qualifier("MemberInmoneyServiceImpl")
	private InmoneyService memberInmoneyService;

	@Override
	public void repotZfbBillsLog(BillLogDto billLogDto) {
		String toAccount = billLogDto.getToAccount();
		String toRealName = billLogDto.getToRealName();
		if(toAccount == null || toRealName == null) {
			String msg = String.format("对方账号：%s, 对方姓名： %s 不能为空", toAccount, toRealName);
			log.error(msg);
			throw new IllegalArgumentException(msg);			
		}

		log.info("1. 根据“备注码、入款时间、对方真实姓名、对方支付宝账号”找到入款单，进行匹配确认", billLogDto.toString());
		Date stTm = new Date(billLogDto.getTimestamp() - 3600000 * 2), edTm = new Date(billLogDto.getTimestamp());
		List<BizIncomeRequest> orders = incomeRequestRepository.find4BzmAmountCreateTime(billLogDto.getBzm(), billLogDto.getAmount(), stTm, edTm);
		if(orders == null || orders.size() == 0) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String sEdTm = sdf.format(edTm);
			String msg = String.format("备注码：%s, 金额：%d 在 %s 往前两小时找不到入款单", billLogDto.getBzm(), billLogDto.getAmount(), sEdTm);
			log.error(msg);
			throw new IllegalArgumentException(msg);
		}
		
		String[] accountParts = toAccount.split("*");
		String[] realNameParts = toRealName.split("*");
		BizOutwardRequest outwardRequestOK = null;
		BizIncomeRequest incomRequestOK = null;
		BizChatpayLog chatpayLogOK = null;
		for(BizIncomeRequest order : orders) {
			try {
				BizChatpayLog chatpayLog = chatpayLogService.findByIncomeOrderNoAndStatus(order.getOrderNo());
				BizOutwardRequest outwardRequest = chatpayLog.getOutwardRequest();
				String account = outwardRequest.getToAccount();
				if(!contains(accountParts, account)) {
					String msg = String.format("流水中的对方账号：%s 与出款单的账号：% 不一致", toAccount, account);
					log.error(msg);
					continue;				
				}
				String realName = outwardRequest.getToAccountOwner();
				if(!contains(realNameParts, realName)) {
					String msg = String.format("流水中的对方真实姓名：%s 与出款单的真实姓名：% 不一致", toRealName, realName);
					log.error(msg);
					continue;				
				}
				outwardRequestOK = outwardRequest;
				incomRequestOK = order;
				chatpayLogOK = chatpayLog;
				break;
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		
		if(outwardRequestOK == null || incomRequestOK == null || chatpayLogOK == null) {
			String msg = "根据“备注码、入款时间、对方真实姓名、对方支付宝账号”找不到入款单和出款单";
			log.error(msg);
			throw new IllegalArgumentException(msg);
		}
		
        log.info("2. 进行入款确认:{}", chatpayLogOK.toString());
        if(chatpayLogOK.incomeIsMember()) {
        	memberInmoneyService.inmoneyConfirm(billLogDto.getTimestamp(), chatpayLogOK, "通过流水匹配成功", true);
        } else {
        	parttimeInmoneyService.inmoneyConfirm(billLogDto.getTimestamp(), chatpayLogOK, "通过流水匹配成功", true);
        }
	}

	@Override
	public void repotWxBillsLog(BillLogDto billLogDto) {
		repotZfbBillsLog(billLogDto);
	}

	private boolean contains(String[] parts, String whole) {
		for (String p : parts) {
			if (p == null || p.trim().equals("")) {
				continue;
			}
			if (!whole.contains(p)) {
				return false;
			}
		}
		return true;
	}	
}
