package com.xinbo.fundstransfer.assign.entity;

import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

/**
 * PC转账手机抓流水卡
 * @author Administrator
 *
 */
public class OutPcInMobile extends OutInSameTime {

	public OutPcInMobile(AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId, int amount) {
		super(Constants.TARGET_TYPE_ROBOT, info, accountId, levelId, type, zone, handicapId, amount);
	}

}
