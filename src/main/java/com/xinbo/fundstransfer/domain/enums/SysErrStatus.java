package com.xinbo.fundstransfer.domain.enums;

import java.util.Objects;

public enum SysErrStatus {

	Locking(10, "等待排查"), Locked(20, "锁定中"), FinishedNormarl(3, "恢复启用"), FinishedFreeze(4, "永久禁用");
	private Integer status = null;
	private String msg = null;

	SysErrStatus() {
	}

	SysErrStatus(Integer status, String msg) {
		this.status = status;
		this.msg = msg;
	}

	public static boolean finish(SysErrStatus st) {
		if (Objects.isNull(st)) {
			return false;
		}
		return finish(st.getStatus());
	}

	public static boolean finish(Integer status) {
		return Objects.equals(status, SysErrStatus.FinishedFreeze.getStatus())
				|| Objects.equals(status, SysErrStatus.FinishedNormarl.getStatus());
	}

	public static SysErrStatus findByStatus(Integer status) {
		if (status == null) {
			return null;
		}
		for (SysErrStatus errStatus : SysErrStatus.values()) {
			if (status.equals(errStatus.status)) {
				return errStatus;
			}
		}
		return null;
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
}