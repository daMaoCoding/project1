package com.xinbo.fundstransfer.domain.enums;

import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 入款银行卡子类型
 *
 * @author Administrator
 */
public enum InBankSubType {
	/** 入款银行卡-普通 */
	IN_BANK_DEFAULT(0, "入款银行卡-普通"),
	/** 入款银行卡-支付宝 */
	IN_BANK_ALIIN(1, "入款银行卡-支付宝"),
	/** 入款银行卡-微信 */
	IN_BANK_WECHATIN(2, "入款银行卡-微信"),
	/** 入款银行卡-云闪付 */
	IN_BANK_YSF(3, "入款银行卡-云闪付"),
	/** 入款银行卡-云闪付混合 */
	IN_BANK_YSF_MIX(4, "入款银行卡-云闪付混合");
	private Integer subType;
	private String subTypeDesc;

	InBankSubType(Integer subType, String subTypeDesc) {
		this.subType = subType;
		this.subTypeDesc = subTypeDesc;
	}

	public static List<Integer> getAllSubType() {
		return Arrays.stream(InBankSubType.values()).map(p -> p.getSubType()).collect(Collectors.toList());
	}

	public static InBankSubType getInfoBySubType(Integer subType) {
		if (ObjectUtils.isEmpty(subType)) {
			return null;
		}
		for (InBankSubType subType1 : InBankSubType.values()) {
			if (ObjectUtils.nullSafeEquals(subType1.getSubType(), subType)) {
				return subType1;
			}
		}
		return null;
	}

	public static String getSubTypeDesc(Integer subType) {
		if (ObjectUtils.isEmpty(subType)) {
			return null;
		}
		for (InBankSubType subType1 : InBankSubType.values()) {
			if (ObjectUtils.nullSafeEquals(subType1.getSubType(), subType)) {
				return subType1.getSubTypeDesc();
			}
		}
		return null;
	}

	public Integer getSubType() {
		return subType;
	}

	public void setSubType(Integer subType) {
		this.subType = subType;
	}

	public String getSubTypeDesc() {
		return subTypeDesc;
	}

	public void setSubTypeDesc(String subTypeDesc) {
		this.subTypeDesc = subTypeDesc;
	}
}
