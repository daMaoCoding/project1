package com.xinbo.fundstransfer.domain.pojo;

public class SysBalPush<T> {

	public static final int CLASSIFY_BANK_BAL = 0;
	public static final int CLASSIFY_TRANSFER = 1;
	public static final int CLASSIFY_BANK_LOGS = 2;
	public static final int CLASSIFY_BANK_LOG_ = 3;
	public static final int CLASSIFY_BANK_MAN_MGR = 5;
	public static final int CLASSIFY_INIT = 6;
	public static final int CLASSIFY_INVST_ERROR = 7;
	public static final int CLASSIFY_STREAM_ALARM = 8;

	public static final int CLASSIFY_BANK_LOGS_TIME = 4;

	private int target;
	private int classify;
	private T data;
	private long currTm;

	public SysBalPush() {
		this.currTm = System.currentTimeMillis();
	}

	public SysBalPush(int target, int classify, T data) {
		this.target = target;
		this.classify = classify;
		this.data = data;
		this.currTm = System.currentTimeMillis();
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}

	public int getClassify() {
		return classify;
	}

	public void setClassify(int classify) {
		this.classify = classify;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public long getCurrTm() {
		return currTm;
	}

}
