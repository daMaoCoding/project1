package com.xinbo.fundstransfer.report.up;

import java.math.BigDecimal;
import java.util.Objects;

import com.xinbo.fundstransfer.report.SysBalUtils;
import org.apache.commons.lang3.StringUtils;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;

public class ReportYSF {

	private long flogId = 0;
	private Long taskId;
	private Integer taskType;
	private String orderNo;
	private long crawlTm = 0;
	private Integer fromAccount;
	private BigDecimal amount;
	private String oppOwner;
	private String oppAccount;
	private String summary;

	public ReportYSF(String msg) {
		if (StringUtils.isBlank(msg))
			return;
		String[] info = msg.split(":");
		int l = info.length;
		for (int index = 0; index < l; index++) {
			if (index == 0) {
				this.flogId = Long.parseLong(info[0]);
			} else if (index == 1) {
				this.taskId = StringUtils.isNumeric(info[1]) ? Long.parseLong(info[1]) : null;
			} else if (index == 2) {
				this.taskType = StringUtils.isNumeric(info[2]) ? Integer.parseInt(info[2]) : null;
			} else if (index == 3) {
				this.orderNo = StringUtils.trimToNull(info[3]);
			} else if (index == 4) {
				this.crawlTm = StringUtils.isNumeric(info[4]) ? Long.parseLong(info[4]) : null;
			} else if (index == 5) {
				this.fromAccount = StringUtils.isNumeric(info[5]) ? Integer.parseInt(info[5]) : null;
			} else if (index == 6) {
				this.amount = new BigDecimal(info[6]);
			} else if (index == 7) {
				this.oppOwner = StringUtils.trimToEmpty(info[7]);
			} else if (index == 8) {
				this.oppAccount = StringUtils.trimToEmpty(info[8]);
			} else if (index == 9) {
				this.summary = StringUtils.trimToEmpty(info[9]);
			}
		}
	}

	public static String genMsg(BizBankLog statement) {
		String flogIdStr = Objects.isNull(statement.getId()) ? StringUtils.EMPTY : statement.getId().toString();
		String taskIdStr = Objects.isNull(statement.getTaskId()) ? StringUtils.EMPTY : statement.getTaskId().toString();
		String taskTypeStr = Objects.isNull(statement.getTaskType()) ? StringUtils.EMPTY
				: statement.getTaskType().toString();
		String orderNoStr = StringUtils.trimToEmpty(statement.getOrderNo());
		String crawlTmStr = String.valueOf(System.currentTimeMillis());
		String fromAccountStr = String.valueOf(statement.getFromAccount());
		String amountStr = SysBalUtils.radix2(statement.getAmount()).toString();
		String oppOwnerStr = StringUtils.trimToEmpty(statement.getToAccountOwner()).replaceAll(":", StringUtils.EMPTY);
		String oppAccountStr = StringUtils.trimToEmpty(statement.getToAccount()).replaceAll(":", StringUtils.EMPTY);
		String summaryStr = StringUtils.trimToEmpty(statement.getSummary()).replaceAll(":", StringUtils.EMPTY);
		return String.format("%s:%s:%s:%s:%s:%s:%s:%s:%s:%s", flogIdStr, taskIdStr, taskTypeStr, orderNoStr, crawlTmStr,
				fromAccountStr, amountStr, oppOwnerStr, oppAccountStr, summaryStr);
	}

	public long getFlogId() {
		return flogId;
	}

	public Long getTaskId() {
		return taskId;
	}

	public Integer getTaskType() {
		return taskType;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public long getCrawlTm() {
		return crawlTm;
	}

	public Integer getFromAccount() {
		return fromAccount;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getOppOwner() {
		return oppOwner;
	}

	public String getOppAccount() {
		return oppAccount;
	}

	public String getSummary() {
		return summary;
	}
}
