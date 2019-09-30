package com.xinbo.fundstransfer.domain.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by 000 on 2017/6/27.
 */
public enum BankLogStatus {

	/**
	 * 状态，0-正在匹配（Matching），1-已匹配(Matched)
	 */
	Matching(0, "匹配中"), Matched(1, "已匹配"), NoOwner(3, "未认领"), Disposed(4, "已处理"), Fee(5, "手续费"), Refunding(6, "冲正，未处理"), Refunded(7, "冲正，已处理"), Interest(8, "利息/结息"), DeficitManual(9, "亏损-人工"), DeficitSysBug(10, "亏损-系统"), DeficitOther(11, "亏损-其他"), ExtFunds(12, "外部资金");
	;

	private Integer status = null;
	private String msg = null;

	BankLogStatus(Integer status, String msg) {
		this.status = status;
		this.msg = msg;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public static BankLogStatus findByStatus(Integer status) {
		if (status == null) {
			return null;
		}
		for (BankLogStatus log : BankLogStatus.values()) {
			if (status.equals(log.getStatus())) {
				return log;
			}
		}
		return null;
	}

	public static Integer[] transStatusToArray(Integer[] statusArray) {
		if (statusArray == null || statusArray.length == 0) {
			return statusArray;
		}
		List<Integer> result = new ArrayList<>();
		Stream.of(statusArray).mapToInt((p) -> p.equals(NoOwner.getStatus()) ? Matching.getStatus() : p).distinct()
				.forEach((p) -> result.add(p));
		return result.toArray(new Integer[result.size()]);
	}

}
