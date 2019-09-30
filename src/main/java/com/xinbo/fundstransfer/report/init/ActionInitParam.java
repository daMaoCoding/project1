package com.xinbo.fundstransfer.report.init;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

public class ActionInitParam {

	private AccountBaseInfo base;

	ActionInitParam(AccountBaseInfo base) {
		this.base = base;
	}

	public AccountBaseInfo getBase() {
		return base;
	}
}
