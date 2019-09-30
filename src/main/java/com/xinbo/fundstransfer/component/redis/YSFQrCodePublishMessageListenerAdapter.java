package com.xinbo.fundstransfer.component.redis;

import com.xinbo.fundstransfer.report.SystemAccountManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQrCodeEntity;
import com.xinbo.fundstransfer.unionpay.ysf.service.impl.YSFQrcodeGenServiceImpl;

import java.util.Objects;

@Component
public class YSFQrCodePublishMessageListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(YSFQrCodePublishMessageListenerAdapter.class);
	@Autowired
	private YSFQrcodeGenServiceImpl ySFService;
	@Autowired
	private SystemAccountManager systemAccountManager;

	@Override
	public void onMessage(Message msg, byte[] topic) {
		if (Objects.nonNull(systemAccountManager) && systemAccountManager.checkRight4Accounting()) {
			return;
		}
		log.debug("Received msg: {}", msg);
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			YSFQrCodeEntity qrCodeEntity = mapper.readValue(msg.toString(), YSFQrCodeEntity.class);
			if (qrCodeEntity != null) {
				ySFService.onYunSfOnQrCode(qrCodeEntity);
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

}
