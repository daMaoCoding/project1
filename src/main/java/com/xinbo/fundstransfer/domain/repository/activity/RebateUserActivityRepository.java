package com.xinbo.fundstransfer.domain.repository.activity;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.activity.BizAccountFlwActivity;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RebateUserActivityRepository extends BaseRepository<BizAccountFlwActivity, Long> {

	/**
	 * 查找兼职关联活动信息
	 */
	List<BizAccountFlwActivity> findByUid(String userId);

	/**
	 * 查找兼职关联活动信息
	 */
	List<BizAccountFlwActivity> findByUidAndActivityNumber(String userId,String activityNumber);

	/**
	 * 查找兼职关联活动信息
	 */
	BizAccountFlwActivity  findByUidAndActivityNumberAndUserStatus(String userId,String activityNumber,int userStatus);

	/**
	 * 查找兼职关联活动信息
	 */
	List<BizAccountFlwActivity>  findByUidAndUserStatus(String userId,int userStatus);

	/**
	 * 通过兼职活动状态查找兼职活动关联信息
	 */
	List<BizAccountFlwActivity> findByUidAndUserStatus(String uid, Integer userStatus);


	/**
	 * 通过兼职活动状态，获取全部兼职临时活动佣金
	 * 兼职活动状态见 {@link ActivityEnums.UserActivityStatus}
	 */
	@Query(nativeQuery = true,value = "SELECT uid,sum(activity_amount) as amount FROM biz_account_flw_activity WHERE user_status=?1 GROUP BY uid HAVING amount is not NULL")
	List<Map<String, String>> getSumActivityAmountByUserActivityStatus(int status);


}
