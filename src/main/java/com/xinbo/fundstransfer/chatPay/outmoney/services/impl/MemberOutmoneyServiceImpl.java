package com.xinbo.fundstransfer.chatPay.outmoney.services.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.OrderDetail;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.SureNewOutOrderBack;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.SureOrCancelOutOrderBack;
import com.xinbo.fundstransfer.component.net.http.restTemplate.CallCenterServiceApiStatic;
import com.xinbo.fundstransfer.component.net.http.restTemplate.PlatformServiceApiStatic;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.enums.OutwardRequestStatus;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 	聊天室支付：会员出款相关服务
 * @author ERIC
 *
 */
@Slf4j
@Service("MemberOutmoneyServiceImpl")
public class MemberOutmoneyServiceImpl extends OutmoneyServiceImpl {
	@Autowired
	private OutwardRequestRepository outwardRequestRepository;
	@Autowired
	private PlatformServiceApiStatic platformServiceApiStatic;
	@Autowired
	private CallCenterServiceApiStatic callCenterServiceApiStatic;

	@Override
	void outmoneyOverDifferent(Pair<BizOutwardRequest, BizAccountTranslog> entity, String outOrderNo, Long timestamp, List<OrderDetail> details, BigDecimal outedMoney) {
		BizOutwardRequest outward = entity.getLeft();
		if (outedMoney.abs().compareTo(new BigDecimal(0.001)) < 0) {
			log.info("出款全部取消 {}", outward.getOrderNo());
			outmoneyCancel(outward, timestamp);
			return;
		}
		if ((outward.getAmount().subtract(outedMoney)).abs().compareTo(new BigDecimal(0.001)) < 0) {
			log.info("出款全部完成 {}", outward.getOrderNo());
			outmoneyConfirm(outward, details, timestamp, false);
			return;
		}
		log.info("部分出款完成 {}, 完成金额 {} ", outward.getOrderNo(), outedMoney);
		outmoneyConfirmPart(outward, details, outedMoney, timestamp);
	}
	
	@Override
	void outmoneyNextStepDifferent(Pair<BizOutwardRequest, BizAccountTranslog> entity, String outOrderNo, Long timestamp, List<OrderDetail> details, BigDecimal outedMoney) {
		BizOutwardRequest outward = entity.getLeft();
		if ((outward.getAmount().subtract(outedMoney)).abs().compareTo(new BigDecimal(0.001)) < 0) {
			log.info("出款全部完成 {}, 出款确认", outward.getOrderNo());
			outmoneyConfirm(outward, details, timestamp, true);
			return;
		}

		log.info("出款未完成 {}, 继续分配", outward.getOrderNo());
		// TODO: 继续分配	
	}
	
	/**
	 * 会员出款取消
	 * 
	 * @param outwardRequest
	 * @param timestamp
	 */
	private void outmoneyCancel(BizOutwardRequest outwardRequest, Long timestamp) {
		String outOrderNo = outwardRequest.getOrderNo();
		log.info("1. 取消出款单outwardRequest: {}", outOrderNo);
		outwardRequest.setStatus(OutwardRequestStatus.Canceled.getStatus());
		outwardRequestRepository.saveAndFlush(outwardRequest);
		
		log.info("2. 调用平台进行出款取消: {}", outOrderNo);
		SureOrCancelOutOrderBack back = new SureOrCancelOutOrderBack();
		back.setOid(outwardRequest.findOid());
		back.setType((byte) 0);
		back.setCode(outOrderNo);
		back.setOpt_time(timestamp);
		if (platformServiceApiStatic.sureOrCancelOutOrder(back) != 1) {
			log.error("出款单号：{}  去平台取消失败 ", outOrderNo);
			throw new IllegalArgumentException("出款单号：" + outOrderNo + " 去平台取消失败");
		}		
	}

	/**
	 * 会员出款确认
	 * 
	 * @param outwardRequest
	 * @param details
	 * @param timestamp
	 */
	private void outmoneyConfirm(BizOutwardRequest outwardRequest, List<OrderDetail> details, Long timestamp, boolean notifyOutward) {
		String outOrderNo = outwardRequest.getOrderNo();

		log.info("1. 确认出款单: {}", outOrderNo);
		outwardRequest.setStatus(OutwardRequestStatus.Acknowledged.getStatus());
		outwardRequestRepository.saveAndFlush(outwardRequest);

		log.info("2. 调用平台进行出款确认: {}", outOrderNo);
		SureOrCancelOutOrderBack back = new SureOrCancelOutOrderBack();
		back.setOid(outwardRequest.findOid());
		back.setType((byte) 1);
		back.setCode(outOrderNo);
		back.setOpt_time(timestamp);
		back.setDetailList(details);
		if (platformServiceApiStatic.sureOrCancelOutOrder(back) != 1) {
			log.error("出款单号：{}  去平台确认失败 ", outOrderNo);
			throw new IllegalArgumentException("出款单号：" + outOrderNo + " 去平台确认失败");
		}		
		
		if(notifyOutward) {
			log.info("3. 调用客服系统自动踢掉出款人员,提示出款成功: {}", outOrderNo);
			callCenterServiceApiStatic.payComplete(outwardRequest.getChatPayBizId(), outwardRequest.getOrderNo(), outwardRequest.getChatPayRoomNum(), outwardRequest.getChatPayRoomToken());
		}
	}

	/**
	 * 会员出款部分确认
	 * 
	 * @param outwardRequest
	 * @param details
	 * @param timestamp
	 */
	private void outmoneyConfirmPart(BizOutwardRequest outwardRequest, List<OrderDetail> details, BigDecimal money, Long timestamp) {
		String outOrderNo = outwardRequest.getOrderNo();
		log.info("1. 取消原出款单outwardRequest: {}", outOrderNo);
		outwardRequest.setStatus(OutwardRequestStatus.Canceled.getStatus());
		outwardRequestRepository.saveAndFlush(outwardRequest);

		log.info("2. 调用平台进行出款部分确认: {}, 确认金额：{}", outOrderNo, money);
		SureNewOutOrderBack back = new SureNewOutOrderBack();
		back.setOid(outwardRequest.findOid());
		back.setType((byte) 1);
		back.setMoney(money);
		back.setUser_name(outwardRequest.getMember());
		back.setAdmin_name("系统");
		back.setCrk_order_id("1");
		back.setCrk_order_id("部分出款");
		back.setOpt_time(timestamp);
		back.setCancel_code(outOrderNo);
		back.setDetailList(details);
		JSONObject json = platformServiceApiStatic.sureNewOutOrder(back);
		if (json.getIntValue("status") != 1) {
			log.error("出款单号：{}  去平台部分确认失败 ", outOrderNo);
			throw new IllegalArgumentException("出款单号：" + outOrderNo + " 去平台部分确认失败");
		}
		String newOutOrderNo = json.getString("code");

		log.info("3. 补新出款单outwardRequest: {}", newOutOrderNo);
		BizOutwardRequest outwardRequestNew = outwardRequest.clone();
		outwardRequestNew.setOrderNo(newOutOrderNo);
		outwardRequestNew.setAmount(money);
		outwardRequestNew.setUpdateTime(new Date());
		outwardRequestNew.setStatus(OutwardRequestStatus.Acknowledged.getStatus());
		Long l = System.currentTimeMillis() - outwardRequest.getCreateTime().getTime();
		outwardRequestNew.setTimeConsuming(l.intValue() / 1000);
		outwardRequestRepository.saveAndFlush(outwardRequestNew);
	}	
}
