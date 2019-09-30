package com.xinbo.fundstransfer.domain.enums;

/**
 * 出款审核状态
 *
 */
public enum OutwardRequestStatus {

	/**
	 * 正在审核
	 */
	Processing(0, "正在审核"),

	/**
	 * 审核通过
	 */
	Approved(1, "审核通过"),

	/**
	 * 拒绝
	 */
	Reject(2, "拒绝"),

	/**
	 * 主管处理
	 */
	ManagerProcessing(3, "主管处理"),

	/**
	 * 已取消
	 */
	Canceled(4,"已取消"),

	/**
	 * 出款成功，平台已确认
	 */
	Acknowledged(5, "出款成功，平台已确认"),


	/**
	 * 出款成功，与平台确认失败
	 */
	Failure(6, "出款成功，与平台确认失败"),

	/**
	 * 聊天室支付-等待匹配
	 */
	ChatPayWaitMatch(7,"聊天室支付-等待匹配"),


	/**
	 * 聊天室支付-进行中
	 */
	ChatPayRooming(8,"聊天室支付-进行中"),

	/**
	 * 聊天室支付-成功
	 */
	ChatPaySuccess(9,"聊天室支付-成功"),


	/**
	 * 聊天室支付-失败
	 */
	ChatPayError(10,"聊天室支付-失败");



	private Integer status = null;
	private String msg = null;

	OutwardRequestStatus(Integer status, String msg) {
		this.status = status;
		this.msg = msg;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public static OutwardRequestStatus findByStatus(Integer status) {
		if (status == null) {
			return null;
		}
		for (OutwardRequestStatus accountStatus : OutwardRequestStatus.values()) {
			if (status.equals(accountStatus.status)) {
				return accountStatus;
			}
		}
		return null;
	}
}
