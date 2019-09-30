package com.xinbo.fundstransfer.component.redis.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.xinbo.fundstransfer.domain.pojo.AccountStatInOut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.text.MessageFormat;

/**
 * ************************
 * Topic消息处理
 * @author tony
 */
@Slf4j
public class RedisMsgReceiver extends MessageListenerAdapter {

    private final MessageDelegate messageDelegate;

    public RedisMsgReceiver(final MessageDelegate messageDelegate) {
        this.messageDelegate = messageDelegate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        synchronized (this){
            String messageBody    = new String(message.getBody());
            String messageChannel = new String(message.getChannel());
            String messagePattern = new String(pattern);
            Object messageBean = null;
            if(StringUtils.isBlank(messageBody) || StringUtils.isBlank(messageChannel)) return;
            String errMsg = MessageFormat.format("Topic: {0}, pattern: {1}, Body: {2}", messageChannel, messagePattern, messageBody);
            try {
                  if(messageDelegate.getMessageType() != String.class)
                      messageBean = JSON.parseObject(messageBody, messageDelegate.getMessageType(), Feature.AllowUnQuotedFieldNames);
                  if(messageDelegate.handleMessage(messageChannel,messageBean!=null?messageBean:messageBody)!=1) throw new RuntimeException();
            } catch (Exception e) {
                log.error(  "处理RedisTopic消息失败: {}",errMsg,e);
            }
        }
    }
}