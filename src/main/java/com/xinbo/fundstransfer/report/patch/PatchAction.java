package com.xinbo.fundstransfer.report.patch;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

public abstract class PatchAction {

	public static final String PREFIX_PATCH_ACTION = "PATCH_ACTION_";

	public static final String SORT_ACTION_TYPE_SORT0 = "SORT0";

	public static final String SORT_ACTION_TYPE_Init0 = "Init0";

	public static final String SORT_ACTION_TYPE_Init1 = "Init1";

	public static final String SORT_ACTION_TYPE_QuarterInterest = "QuarterInterest";

	public static final String SORT_ACTION_TYPE_DepositSameAmount = "DepositSameAmount";

	public static final String SORT_ACTION_TYPE_YunSFAbsentFlow = "YunSFAbsentFlow";

	public static final String SORT_ACTION_TYPE_YunSFInomeSameAmount = "YunSFInomeSameAmount";

	@Autowired
	protected StoreHandler storeHandler;

	/**
	 * @return {@code false} 没有排序 或 排过序且排序不成功 </br>
	 *         {@code true } 排过序且排序成功
	 */
	public abstract boolean deal(StringRedisTemplate template, BizAccount account, ReportCheck reportCheck);
}
