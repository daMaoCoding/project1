package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by 000 on 2017/6/27.
 */
public enum OutwardTaskStatus {

	/**
	 * 未出款
	 */
	Undeposit(0, "未出款"),
	/**
	 * 已出款
	 */
	Deposited(1, "已出款"),
	/**
	 * 主管处理
	 */
	ManagerDeal(2, "主管处理"),
	/**
	 * 主管取消
	 */
	ManageCancel(3, "主管取消"),
	/**
	 * 主管拒绝
	 */
	ManageRefuse(4,"主管拒绝"),
	/**
	 * 流水匹配
	 */
	Matched(5, "流水匹配"),
	/**
	 * 待排查
	 */
	Failure(6, "待排查"),
	/**
	 * 无效记录，已重新出款
	 */
	Invalid(7, "无效记录，已重新出款"),
	/**
	 * 银行维护
	 */
	DuringMaintain(8,"银行维护"),
	/**
	 * 待审核
	 */
	ToAudit(9, "待审核"),
	/**
	 * 未知
	 */
	Unknown(10, "未知"),
	/**
	 * 机器确认转账失败
	 */
	Result3(11, "机器确认转账失败");

	private Integer status = null;
	private String msg = null;

	OutwardTaskStatus(Integer status, String msg) {
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

	public static OutwardTaskStatus findByStatus(Integer status) {
		if (status == null) {
			return null;
		}
		for (OutwardTaskStatus accountStatus : OutwardTaskStatus.values()) {
			if (status.equals(accountStatus.status)) {
				return accountStatus;
			}
		}
		return null;
	}

	/**
	 * 是否完成出款 只要状态不等于Undeposit，就认为该任务出款完成， 非Undeposit状态，是出入款内部流转状态
	 *
	 * @param status
	 * @return
	 */
	public static boolean isComplete(Integer status) {
		return !status.equals(Undeposit.getStatus());
	}
}
