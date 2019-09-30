package com.xinbo.fundstransfer.component.redis.msgqueue;

/**
 * 自定义异常类，便于捕获和处理
 */
public class HandleException extends Exception {
	private static final long serialVersionUID = 1558933749760093524L;
	private int errorCode;

	public HandleException() {
	}

	public HandleException(String message) {
		super(message);
	}

	public HandleException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public HandleException(HandleErrorCodeEum errorCodeEum) {
		super(errorCodeEum.getErrorDesc());
		this.errorCode = errorCodeEum.getErrorCode();
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
}
