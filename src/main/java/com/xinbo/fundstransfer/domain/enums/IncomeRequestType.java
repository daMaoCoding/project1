package com.xinbo.fundstransfer.domain.enums;

import java.util.stream.Stream;

/**
 * 1-100预留给平台，101-200为系统中转类型，注意：此值会原封不动插入系统流水表的类型，0在这里不可用，预留给出款，这样在系统流水表中可以按流水类型统计相关信息
 * Created by Eden on 2017/6/27.
 */
public enum IncomeRequestType {
	/**
	 * 平台支付宝入款
	 */
	PlatFromAli(1, "平台支付宝入款"),
	/**
	 * 平台微信入款
	 */
	PlatFromWechat(2, "平台微信入款"),
	/**
	 * 平台银行卡入款
	 */
	PlatFromBank(3, "平台银行卡入款"),
	/**
	 * 平台第三方入款
	 */
	PlatFromThird(4, "平台第三方入款"),
	/**
	 * 平台第三方入款
	 */
	PlatFromJD(5, "平台京东账号入款"),
	/**
	 * 云闪付入款
	 */
	PlatFromYSF(6, "平台云闪付入款"),

	/**
	 * 支付宝提现
	 */
	WithdrawAli(101, "支付宝提现"),
	/**
	 * 微信提现
	 */
	WithdrawWechat(102, "微信提现"),
	/**
	 * 第三方体现
	 */
	WithdrawThird(103, "第三方提现"),

	/**
	 * 支付宝下发
	 */
	IssueAli(104, "支付宝下发"),
	/**
	 * 微信下发
	 */
	IssueWechat(105, "微信下发"),
	/**
	 * 公司卡下发
	 */
	IssueCompBank(106, "公司卡下发"),
	/**
	 * 银行卡下发
	 */
	IssueComnBank(107, "银行卡下发"),

	/**
	 * 出款第三方转账
	 */
	TransferOutThird(108, "出款第三方转账"),
	/**
	 * 出款银行卡转账
	 */
	TransferOutBank(109, "出款银行卡转账"),

	/**
	 * 备用之间互转
	 */
	ReserveToOutBank(110, "备用金到出款卡/备用之间互转"),

	/**
	 * 第三方提现到客户卡 客户卡在账户表不做存储
	 */
	WithdrawThirdToCustomer(111, "第三方提现到客户卡"),

	ReserveToReserve(112, "备用卡之间互转"),

	PlatFormToFundsTransfer(113, "平台转往出入款"),

	/**
	 * 公司用款
	 */
	BizUseMoney(114, "公司用款"),

	THIRDOUTFORMEMBER(115, "三方账号给会员出款或者拆单出款"),

	RebateCommission(400, "返利网提现"),

	RebateLimit(401, "返利网提额");

	private Integer type = null;
	private String msg = null;

	IncomeRequestType(Integer type, String msg) {
		this.type = type;
		this.msg = msg;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public static IncomeRequestType findByType(Integer incomeRequestType) {
		if (incomeRequestType == null) {
			return null;
		}
		for (IncomeRequestType type : IncomeRequestType.values()) {
			if (type.getType().equals(incomeRequestType)) {
				return type;
			}
		}
		return null;
	}

	/**
	 * 是否平台入款
	 *
	 * @param type
	 * @return
	 */
	public static boolean isPlatform(int type) {
		return (type < 101 && type > 0);
	}

	public static IncomeRequestType getByNumber(int number) {
		return  Stream.of(IncomeRequestType.values()).filter(p -> p.type == number).findFirst().orElse(null);
	}

}
