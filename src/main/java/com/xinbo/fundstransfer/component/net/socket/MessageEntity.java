package com.xinbo.fundstransfer.component.net.socket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * 消息实体，最终转换成json格式传递
 * 
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageEntity<T> {

	/**
	 * 服务端传递指令，客户端接收后，按指令执行相关操作，约定事件参考ActionEventEnum定义
	 */
	int action;

	/**
	 * 根据action指令传递的泛型参数
	 */
	T data;

	/**
	 * 所在主机IP
	 */
	String ip;

	/**
	 * 账号类型
	 */
	Integer type;

	/**
	 * 账号内外层
	 */
	Integer currSysLevel;

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Integer getCurrSysLevel() {
		return currSysLevel;
	}

	public void setCurrSysLevel(Integer currSysLevel) {
		this.currSysLevel = currSysLevel;
	}

	@Override
	public String toString() {
		return new ReflectionToStringBuilder(this).toString();
	}

}
