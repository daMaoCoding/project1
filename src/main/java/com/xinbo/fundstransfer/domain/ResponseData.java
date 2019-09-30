package com.xinbo.fundstransfer.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ResponseData<T>  {

	/**
	 * 状态，1成功，其它为失败值
	 */
	private int status;

	/**
	 * 操作结果消息，成功或失败的提示信息
	 */
	private String message;

	/**
	 * 返回结果数据对象
	 */
	private T data;

	public ResponseData() {
	}

	public ResponseData(int status) {
		this.status = status;
	}

	public ResponseData(int status, String message) {
		this.status = status;
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

}
