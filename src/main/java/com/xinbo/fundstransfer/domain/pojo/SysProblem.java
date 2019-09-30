package com.xinbo.fundstransfer.domain.pojo;

public class SysProblem {

	private int id;
	private long taskTm = 0;
	private long sysTm = 0;

	public SysProblem() {
	}

	public SysProblem(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getTaskTm() {
		return taskTm;
	}

	public void setTaskTm(long taskTm) {
		this.taskTm = taskTm;
	}

	public long getSysTm() {
		return sysTm;
	}

	public void setSysTm(long sysTm) {
		this.sysTm = sysTm;
	}
}
