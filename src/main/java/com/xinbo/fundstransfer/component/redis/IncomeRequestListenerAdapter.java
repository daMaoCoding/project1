package com.xinbo.fundstransfer.component.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.net.socket.MinaMonitorServer;
import com.xinbo.fundstransfer.component.websocket.IncomeRequestWebSocketEndpoint;
import com.xinbo.fundstransfer.domain.enums.IncomeAuditWsEnum;
import com.xinbo.fundstransfer.domain.pojo.IncomeAuditWs;

/**
 * 公司入款
 * 
 *
 *
 */
@Component
public class IncomeRequestListenerAdapter extends MessageListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(IncomeRequestListenerAdapter.class);
	@Autowired
	IncomeRequestWebSocketEndpoint incomeRequestWebSocketEndpoint;
	@Autowired
	MinaMonitorServer minaMonitorServer;
	ObjectMapper mapper = new ObjectMapper();

	@Override
	public void onMessage(Message msg, byte[] topic) {
		log.debug("Received msg: {}", msg);
		try {
			// 发送消息通知前端
			incomeRequestWebSocketEndpoint.sendMessage(msg.toString());
			IncomeAuditWs noticeEntity = mapper.readValue(msg.toString(), IncomeAuditWs.class);
			// 通知工具抓流水
			if (noticeEntity.getIncomeAuditWsFrom().intValue() == IncomeAuditWsEnum.FromIncomeReq.ordinal()) {
				MessageEntity<Integer> o = new MessageEntity<Integer>();
				o.setAction(ActionEventEnum.CAPTURE.ordinal());
				o.setData(noticeEntity.getAccountId());
				minaMonitorServer.messageSentByAccountId(mapper.writeValueAsString(o), noticeEntity.getAccountId());
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

}
