package com.xinbo.fundstransfer.domain.pojo;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import org.apache.commons.lang3.StringUtils;

public class AccInvstDoing {
	private long bankLogId;
	private int invstType;
	private BizBankLog bankLog;
	private String orderNo;
	private int type = 0;

	public AccInvstDoing() {
	}

	public AccInvstDoing(long bankLogId, int invstType, String orderNo) {
		this.bankLogId = bankLogId;
		this.invstType = invstType;
		orderNo = StringUtils.trimToNull(orderNo);
		this.orderNo = orderNo;
		if (StringUtils.isNotBlank(orderNo)) {
			if (StringUtils.startsWith(orderNo, "O")) {
				this.type = SysBalTrans.TASK_TYPE_OUTMEMEBER;
			}
			if (StringUtils.isNumeric(orderNo)) {
				this.type = SysBalTrans.TASK_TYPE_OUTREBATE;
			}
		}
	}

	public long getBankLogId() {
		return bankLogId;
	}

	public void setBankLogId(long bankLogId) {
		this.bankLogId = bankLogId;
	}

	public int getInvstType() {
		return invstType;
	}

	public void setInvstType(int invstType) {
		this.invstType = invstType;
	}

	public BizBankLog getBankLog() {
		return bankLog;
	}

	public void setBankLog(BizBankLog bankLog) {
		this.bankLog = bankLog;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
