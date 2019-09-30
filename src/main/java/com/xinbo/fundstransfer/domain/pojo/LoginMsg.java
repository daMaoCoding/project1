package com.xinbo.fundstransfer.domain.pojo;

public enum LoginMsg {
	Success("1", "SUCCESS"), UserOrPassNull("2", "用户名或密码为空！"), UserNotExtOrStatusErr("3", "用户不存在或状态不对！"), UserOrPassErr(
			"4",
			"用户名或密码不对！"), UserHasNoCard("5", "用户名下无卡！"), BankNotDevelop("6", "银行未开发！"), CheckException("7", "校验异常！");
	private String msg;
	private String msgDesc;

	LoginMsg(String msg, String msgDesc) {
		this.msg = msg;
		this.msgDesc = msgDesc;
	}

	public String getMsg() {
		return msg;
	}

	public String getMsgDesc() {
		return msg;
	}

	public static String getDescByMsg(String msg) {
		if (msg == null) {
			return null;
		}
		for (LoginMsg loginMsg : LoginMsg.values()) {
			if (msg.equals(loginMsg.msg)) {
				return loginMsg.msgDesc;
			}
		}
		return null;
	}
}
