package com.xinbo.fundstransfer.component.redis.msgqueue;

/**
 * Created by Administrator on 2018/10/20. 消息队列模型
 */
public class MessageModel {
	private Integer operatorId;// 可以用户id,账号id
	private Integer type;

	public Integer getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public MessageModel() {
	}

	public MessageModel(Integer type, Integer operatorId) {
		this.type = type;
		this.operatorId = operatorId;
	}

	public Integer getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(Integer operatorId) {
		this.operatorId = operatorId;
	}
}
