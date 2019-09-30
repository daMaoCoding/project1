package com.xinbo.fundstransfer.domain.enums;

/**
 * 用户类别
 * 
 *
 *
 */
public enum UserCategory {
	ADMIN(-1), GENERAL(0), Robot(1);
	private int value;

	private UserCategory(int value) {
		this.value = value;
	}

	public static UserCategory valueOf(int value) {
		switch (value) {
		case 0:
			return GENERAL;
		case -1:
			return ADMIN;
		default:
			return null;
		}
	}

	public int getValue() {
		return value;
	}
}
