package com.xinbo.fundstransfer.component.redis.msgqueue;

/**
 * 自定义 异常错误代码
 */
public enum HandleErrorCodeEum {
	E_INCOME_START(10, "入款审核开始接单异常"), E_INCOME_STOP(11, "入款审核结束接单异常"), E_ALIPAY_UPDATE(12,
			"平台同步支付宝账号变更异常"), E_WECHAT_UPDATE(13, "平台同步微信账号变更异常"), E_REVIEWTASK_START(20,
					"任务排查开始接单异常"), E_REVIEWTASK_PAUSE(21,
							"任务排查暂停接单异常"), E_REVIEWTASK_STOP(22, "任务排查结束接单异常"), E_REVIEWTASK_UPDATE(23, "任务排查结束接单异常");

	private int errorCode;
	private String errorDesc;

	HandleErrorCodeEum(int errorCode, String errorDesc) {
		this.errorCode = errorCode;
		this.errorDesc = errorDesc;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorDesc() {
		return errorDesc;
	}

	public void setErrorDesc(String errorDesc) {
		this.errorDesc = errorDesc;
	}
}
