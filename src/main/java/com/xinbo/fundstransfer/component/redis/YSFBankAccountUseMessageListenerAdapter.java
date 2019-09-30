package com.xinbo.fundstransfer.component.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import com.xinbo.fundstransfer.unionpay.ysf.util.YSFLocalCacheUtil;

@Component
public class YSFBankAccountUseMessageListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(YSFBankAccountUseMessageListenerAdapter.class);
	@Autowired
	private YSFLocalCacheUtil ySFLocalCacheUtil;

	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("Received msg: {}", msg);
		try {
			ySFLocalCacheUtil.setAccountUseTime(msg.toString());
		} catch (Exception e) {
			log.error("设置云闪付银行卡最近使用时间时异常", e);
		}
	}

}
