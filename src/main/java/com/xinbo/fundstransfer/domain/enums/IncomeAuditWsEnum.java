package com.xinbo.fundstransfer.domain.enums;

public enum IncomeAuditWsEnum {
	/**
	 * 入款请求 0
	 */
	FromIncomeReq,
	/**
	 * 银行流水 1
	 */
	FromInBankLog,
	/**
	 * 帐号分配 2
	 */
	FromAllocate,
	/**
	 * 匹配成功 3
	 */
	FromMatched,
	/**
	 * 取消成功 4
	 */
	SucceedCanceled,
	/**
	 * 取消失败 5
	 */
	FailedCanceled,
	/**
	 * 客服消息 6
	 */
	CustomerMessage,
	/**
	 * 客服消息 7
	 */
	CustomerAddOrder,
	/**
	 * 打开socket接单的时候,不分配账号通知消息 8
	 */
	NOTASSIGNACCOUNT,
	/**
	 * 分配支付宝 微信账号之后 刷新页面通知
	 */
	FRESHPAGE_ALIWECHATASSGINED

}
