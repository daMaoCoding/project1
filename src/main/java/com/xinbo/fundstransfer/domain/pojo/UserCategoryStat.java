package com.xinbo.fundstransfer.domain.pojo;

/**
 * Created by 000 on 2017/10/11.
 */
public class UserCategoryStat {
	/**
	 * 分类编码
	 */
	private Integer code;

	/**
	 * 分类名称
	 */
	private String name;

	/**
	 * 总人数
	 */
	private int total;

	/**
	 * 在线数
	 */
	private int online;

	public UserCategoryStat(Integer code, String name, int total, int online) {
		this.code = code;
		this.name = name;
		this.total = total;
		this.online = online;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getOnline() {
		return online;
	}

	public void setOnline(int online) {
		this.online = online;
	}
}
