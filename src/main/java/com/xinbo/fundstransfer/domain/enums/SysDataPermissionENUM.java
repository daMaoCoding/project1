package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by  on 2017/7/4.
 */
public enum SysDataPermissionENUM {
	/**
	 * 层级 field_name
	 * 
	 */
	LEVELCODE("LEVELCODE"),
	/**
	 * 盘口 field_name
	 *
	 */
	HANDICAPCODE("HANDICAPCODE");

	private String value;

	SysDataPermissionENUM(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
