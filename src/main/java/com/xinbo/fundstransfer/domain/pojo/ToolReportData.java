package com.xinbo.fundstransfer.domain.pojo;

import com.xinbo.fundstransfer.component.net.socket.AccountEntity;

import java.util.List;

public class ToolReportData {
	Integer action; // 参照 ActionEventEnum
	String ip;
	List<AccountEntity> data;

	public Integer getAction() {
		return action;
	}

	public void setAction(Integer action) {
		this.action = action;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public List<AccountEntity> getData() {
		return data;
	}

	public void setData(List<AccountEntity> data) {
		this.data = data;
	}
}
