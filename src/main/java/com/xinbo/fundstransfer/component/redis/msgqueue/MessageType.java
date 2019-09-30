package com.xinbo.fundstransfer.component.redis.msgqueue;

/**
 * Created by Administrator on 2018/10/20.
 */
public enum MessageType {
	INCOME_APPROVE_START(10, "入款审核开始接单"), INCOME_APPROVE_STOP(11, "入款审核结束接单"), INCOME_APPROVE_ALIPAYACCUPDATE(12,
			"平台同步支付宝账号变更"), INCOME_APPROVE_WECHATACCUPDATE(13, "平台同步微信账号变更"), TASK_REVIEW_START(20,
					"任务排查开始接单"), TASK_REVIEW_PAUSE(21,
							"任务排查暂停接单"), TASK_REVIEW_STOP(22, "任务排查结束接单"), TASK_REVIEW_UPDATE(23, "转主管或者排查接单");
	private int type;
	private String desc;

	MessageType(int type, String typeDesc) {
		this.type = type;
		this.desc = typeDesc;
	}

	public static MessageType getMessageTypeByType(int type) {
		for (MessageType messageType : MessageType.values()) {
			if (messageType.getType() == type) {
				return messageType;
			}
		}
		return null;
	}

	public static String getDescByType(int type) {
		for (MessageType messageType : MessageType.values()) {
			if (messageType.getType() == type) {
				return messageType.getDesc();
			}
		}
		return null;
	}

	public static String getMessageType(MessageType type) {
		if (type != null) {
			if (type.getType() == INCOME_APPROVE_START.getType() || type.getType() == INCOME_APPROVE_STOP.getType()
					|| type.getType() == INCOME_APPROVE_ALIPAYACCUPDATE.getType()
					|| type.getType() == INCOME_APPROVE_WECHATACCUPDATE.getType()) {
				return "INCOME_APPROVE";
			}
			if (type.getType() == TASK_REVIEW_START.getType() || type.getType() == TASK_REVIEW_PAUSE.getType()
					|| type.getType() == TASK_REVIEW_STOP.getType() || type.getType() == TASK_REVIEW_UPDATE.getType()) {
				return "TASK_REVIEW";
			}

		}
		return null;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
