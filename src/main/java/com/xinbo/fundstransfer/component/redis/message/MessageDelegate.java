package com.xinbo.fundstransfer.component.redis.message;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * ************************
 * Topic消息处理
 * @author tony
 */
public abstract class  MessageDelegate<T>   {

    private  Class messageType;

    public abstract int handleMessage(String messageChannel,T message);

    public Class getMessageType(){
        if(messageType!=null) return messageType;
        Type type = this.getClass().getGenericSuperclass();
        Type[] params = ((ParameterizedType) type).getActualTypeArguments();
        return messageType =(Class) params[0];
    }
 }