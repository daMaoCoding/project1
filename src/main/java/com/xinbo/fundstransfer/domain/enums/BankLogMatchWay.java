package com.xinbo.fundstransfer.domain.enums;

public enum BankLogMatchWay {

	FlowFindOrder(0, "流水找单"), OrderFindFlow(1, "单找流水");

	private int way;
	private String msg;

	BankLogMatchWay(int way, String msg) {
		this.way = way;
		this.msg = msg;
	}

	public int getWay() {
		return way;
	}

	public void setWay(int way) {
		this.way = way;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
