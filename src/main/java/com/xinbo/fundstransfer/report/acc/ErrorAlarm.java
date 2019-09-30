package com.xinbo.fundstransfer.report.acc;

import org.apache.commons.lang3.StringUtils;

public class ErrorAlarm {

	private long errorId = 0;

	private long occureTm = 0;

	public ErrorAlarm(String msg) {
		if (StringUtils.isBlank(msg))
			return;
        msg = StringUtils.trimToEmpty(msg);
		if (StringUtils.isNumeric(msg)) {
			this.errorId = Long.parseLong(msg);
			this.occureTm = 0;
			return;
		}
		String[] info = msg.split(":");
		int l = info.length;
		for (int index = 0; index < l; index++) {
			if (index == 0) {
				this.errorId = Long.parseLong(info[0]);
			} else if (index == 1) {
				this.occureTm = StringUtils.isNumeric(info[1]) ? Long.parseLong(info[1]) : 0;
			}
		}
	}

	public static String genMsg(ErrorAlarm alarm) {
		return String.format("%s:%s", String.valueOf(alarm.getErrorId()), String.valueOf(alarm.getOccureTm()));
	}

	public void setErrorId(long errorId) {
		this.errorId = errorId;
	}

	public long getErrorId() {
		return this.errorId;
	}

	public long getOccureTm() {
		return occureTm;
	}

	public void setOccureTm(long occureTm) {
		this.occureTm = occureTm;
	}
}
