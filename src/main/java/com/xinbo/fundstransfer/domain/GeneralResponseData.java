package com.xinbo.fundstransfer.domain;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL) 
public class GeneralResponseData<T> implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	public enum ResponseStatus {
		SUCCESS(1), FAIL(-1);
		private int value;

		private ResponseStatus(int value) {
			this.value = value;
		}

		public static ResponseStatus valueOf(int value) {
			switch (value) {
			case 1:
				return SUCCESS;
			default:
				return FAIL;
			}
		}

		public int getValue() {
			return value;
		}
	}


	/**
	 * 状态，1-成功，其它为失败值
	 */
	private int status;

	/**
	 * 操作结果消息，成功或失败的提示信息
	 */
	private String message;

	/**
	 * 分页信息
	 */
	private Paging page;

	/**
	 * 返回结果数据对象
	 */
	private T data;

	public GeneralResponseData() {
	}
	
	public GeneralResponseData(int status) {
		this.status = status;
	}

	public GeneralResponseData(int status, String message) {
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

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Paging getPage() {
		return page;
	}

	public void setPage(Paging page) {
		this.page = page;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

}
