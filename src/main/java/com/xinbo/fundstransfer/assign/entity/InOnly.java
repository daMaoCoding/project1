package com.xinbo.fundstransfer.assign.entity;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

/**
 *  专注入款卡（PC或返利网）
 * @author Administrator
 *
 */
public class InOnly extends InAccount {

	public InOnly(AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId, int amount) {
		super(info, accountId, levelId, type, zone, handicapId, amount);
	}

}
