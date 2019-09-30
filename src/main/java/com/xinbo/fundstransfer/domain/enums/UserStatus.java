package com.xinbo.fundstransfer.domain.enums;

/**
 * User status
 * 
 * 
 *
 */
public enum UserStatus {
	ENABLED(0), DISABLED(1);
	private int value;

	private UserStatus(int value) {
		this.value = value;
	}

	public static UserStatus valueOf(int value) {
		switch (value) {
		case 0:
			return ENABLED;
		case 1:
			return DISABLED;
		default:
			return null;
		}
	}

	public int getValue() {
		return value;
	}
}
