package com.xinbo.fundstransfer.report;

import org.apache.commons.lang3.StringUtils;

public class AccountSystemSuspendingAlarm {

	private long exceedInCount = 0;

	public AccountSystemSuspendingAlarm(String msg) {
		if (StringUtils.isBlank(msg))
			return;
		msg = StringUtils.trimToEmpty(msg);
		if (StringUtils.isNumeric(msg)) {
			this.exceedInCount = Long.parseLong(msg);
			return;
		}
		String[] info = msg.split(":");
		int l = info.length;
		for (int index = 0; index < l; index++) {
			if (index == 0) {
				this.exceedInCount = Long.parseLong(info[0]);
			}
		}
	}

	public static String genMsg(boolean isExceedInCount) {
		return String.format("%s", isExceedInCount ? "1" : "0");
	}

	public boolean isExceedInCount() {
		return exceedInCount > 0;
	}
}
