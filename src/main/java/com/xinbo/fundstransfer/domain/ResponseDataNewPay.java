package com.xinbo.fundstransfer.domain;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseDataNewPay<T> {
	/**
	 * 接口处理成功返回200，返回其他数字表示有异常
	 */
	private int code;

	/**
	 * 返回 1 成功 返回0 失败
	 */
	private byte status;
	/**
	 * 操作结果消息， 失败的提示信息
	 */
	private String msg;

	/**
	 * 返回结果数据对象
	 */
	private T data;

	public ResponseDataNewPay() {
	}

	public ResponseDataNewPay(byte status, String msg) {
		this.status = status;
		this.msg = msg;
	}

	public ResponseDataNewPay(byte status, String msg, T data) {
		this.status = status;
		this.msg = msg;
		this.data = data;
	}

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		if (StringUtils.isBlank(msg)) {
			msg = "无";
		}
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
