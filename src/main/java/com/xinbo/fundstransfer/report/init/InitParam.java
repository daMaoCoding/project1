package com.xinbo.fundstransfer.report.init;

import com.xinbo.fundstransfer.domain.entity.SysUser;

public class InitParam {

	private SysUser operator;
	private String remark;
	private Long errorId;

	public InitParam() {
	}

	public InitParam(SysUser operator, String remark, Long errorId) {
		this.operator = operator;
		this.remark = remark;
		this.errorId = errorId;
	}

	public SysUser getOperator() {
		return operator;
	}

	public void setOperator(SysUser operator) {
		this.operator = operator;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Long getErrorId() {
		return errorId;
	}

	public void setErrorId(Long errorId) {
		this.errorId = errorId;
	}
}
