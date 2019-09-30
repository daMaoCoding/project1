package com.xinbo.fundstransfer.domain.pojo;

/**
 * Created by 000 on 2017/10/30.
 */
public enum SystemWebSocketCategory {
	System(100, "系统"), OnlineStat(200, "入款账号分配"), LevelList(300, "盘口层级"), MenuList(400, "菜单"), AccountAmountAlarm(500,
			"账户余额告警"), SystemOutwardTaskOperation(600, "出款任务完成通知平台操作"), SystemLockOutwardTaskOperation(700,
					"锁定操作"), SystemUnLockOutwardTaskOperation(800, "解锁操作"), CustomerService(900,
							"客服发消息"), OutwardTaskCancel(1000,
									"取消出款"), MessageToAllUser(1100, "推送给所有在线用户"), AccountAlarmCount(1200, "账号告警");

	private int code;
	private String name;

	SystemWebSocketCategory(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
