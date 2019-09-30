package com.xinbo.fundstransfer.assign.entity;

import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

/**
 *  云闪付卡（返利网）
 * @author Administrator
 *
 */
public class OutYunSF extends OutInSameTime {

	public OutYunSF(AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId, int amount) {
		super(Constants.TARGET_TYPE_MOBILE, info, accountId, levelId, type, zone, handicapId, amount);
	}

}
