package com.xinbo.fundstransfer.domain.pojo;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;

/**
 * Created by 000 on 2017/10/13.
 */
public enum UserCategory {

	Outward(100, "出款"), IncomeAudit(200, "入款"), Finance(300, "财务");

	private int code;
	private String name;

	public final static int prefixHandicapCode = 400;

	public final static int allocatedAndAll = 1000;

	UserCategory(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public static int getCode(BizHandicap handicap) {
		return prefixHandicapCode + handicap.getId();
	}

	public static String getName(BizHandicap handicap) {
		return handicap.getName();// 盘口编码改为盘口名称
	}

	public static boolean isHandicapUser(int code) {
		return code > prefixHandicapCode;
	}

	public static Integer getHandicapId(int code) {
		return code > prefixHandicapCode ? code - prefixHandicapCode : null;
	}

	public static UserCategory findByCode(int code) {
		for (UserCategory cat : UserCategory.values()) {
			if (cat.getCode() == code) {
				return cat;
			}
		}
		return null;
	}
}
