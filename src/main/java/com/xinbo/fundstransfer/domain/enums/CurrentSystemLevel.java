package com.xinbo.fundstransfer.domain.enums;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author 000
 *
 */
public enum CurrentSystemLevel {
	// 取值应满足2的n次幂，因为一个账号可以同时属于内层与外层。 外层或者指定层的账号不允许修改为内层和中层
	Outter(1, "外层"), Inner(2, "内层"), Middle(4, "中层"), Designated(8, "指定层");

	private int value;
	private String name;

	CurrentSystemLevel(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public static CurrentSystemLevel valueOf(int value) {
		for (CurrentSystemLevel level : CurrentSystemLevel.values()) {
			if (level.getValue() == value) {
				return level;
			}
		}
		return null;
	}

	public static Integer findByName(String name) {
		Integer result = null;
		name = StringUtils.trimToEmpty(name);
		for (CurrentSystemLevel level : CurrentSystemLevel.values()) {
			if (name.contains(level.getName())) {
				result = result == null ? level.getValue() : (result + level.getValue());
			}
		}
		return result;
	}

	public int getValue() {
		return value;
	}

	public String getName() {
		return name;
	}
}
