package com.xinbo.fundstransfer.domain.enums;

import java.util.stream.Stream;

/**
 * Created by 000 on 2017/6/27.
 * 添加卡类型考虑下之前以有的命名
 */
public enum AccountType {

	UnKnow(0,"未知"),

	/**
	 * 入款卡
	 */
	InBank(1, "入款卡"),

	/**
	 * 入款第三方
	 */
	InThird(2, "入款第三方"),

	/**
	 * 入款支付宝
	 */
	InAli(3, "入款支付宝"),

	/**
	 * 入款微信
	 */
	InWechat(4, "入款微信"),

	/**
	 * 出款卡
	 */
	OutBank(5, "出款卡"),

	/**
	 * 出款第三方
	 */
	OutThird(6,"出款第三方"),

	/**
	 * 备用卡
	 */
	ReserveBank(8, "备用卡"),

	/**
	 * 现金卡
	 */
	CashBank(9, "现金卡"),

	/**
	 * 微信专用
	 */
	BindWechat(10, "微信专用"),

	/**
	 * 支付宝专用
	 */
	BindAli(11,"支付宝专用"),

	/**
	 * 第三方专用
	 */
	ThirdCommon(12, "第三方专用"),

	/**
	 * 下发卡
	 */
	BindCommon(13, "下发卡"),

	/**
	 * 客户卡
	 */
	BindCustomer(14,"客户卡"),

	/**
	 * 企业支付宝
	 */
	AliEnterPrise(15, "企业支付宝"),

	/**
	 * 京东账号
	 */
	InAccountJD(16, "京东账号"),

	/**
	 * 平台同步云闪付账号
	 */
	InAccountYSF(17, "平台同步云闪付账号"),


	/**
	 * 返利网同步支付宝账号
	 */
	InAccountFlwZfb(18, "返利网同步支付宝账号"),


	/**
	 * 返利网同步微信账号
	 */
	InAccountFlwWx(19, "返利网同步微信账号")


	;


	private Integer typeId;
	private String msg;

	AccountType(Integer typeId, String msg) {
		this.typeId = typeId;
		this.msg = msg;
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public static AccountType findByTypeId(Integer typeId) {
		if (typeId == null) {
			return null;
		}
		for (AccountType type : AccountType.values()) {
			if (typeId.equals(type.typeId)) {
				return type;
			}
		}
		return null;
	}


	/**
	 * 通过typeId获取关联的AccountType枚举
	 * 查询不到返回null
	 */
	public static AccountType getByTypeId(int typeId){
		return  Stream.of(AccountType.values()).filter(p -> p.typeId.intValue() == typeId).findFirst().orElse(null);
	}

	public static boolean isIncome(Integer typeId) {
		if (typeId == null) {
			return false;
		}
		if (typeId.equals(InBank.getTypeId()) || typeId.equals(InThird.getTypeId()) || typeId.equals(InAli.getTypeId())
				|| typeId.equals(InWechat.getTypeId())) {
			return true;
		}
		return false;
	}

	/**
	 * 是否下发卡（或叫中转卡，指非入款、出款用卡）
	 * 
	 * @param typeId
	 * @return
	 */
	public static boolean isInternal(Integer typeId) {
		if (typeId == null) {
			return false;
		}
		if (typeId.equals(BindCommon.getTypeId()) || typeId.equals(ThirdCommon.getTypeId())
				|| typeId.equals(ReserveBank.getTypeId()) || typeId.equals(CashBank.getTypeId())
				|| typeId.equals(BindWechat.getTypeId()) || typeId.equals(BindAli.getTypeId())) {
			return true;
		}
		return false;
	}

	public static boolean isBank(Integer typeId) {
		if (typeId == null) {
			return false;
		}
		return !(typeId.equals(InThird.getTypeId()) && typeId.equals(InWechat.getTypeId())
				&& typeId.equals(InAli.getTypeId()) && typeId.equals(OutThird.getTypeId()));
	}
}
