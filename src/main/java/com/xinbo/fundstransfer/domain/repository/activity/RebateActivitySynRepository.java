package com.xinbo.fundstransfer.domain.repository.activity;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 返利网活动
 */
public interface RebateActivitySynRepository extends BaseRepository<BizFlwActivitySyn, Integer> {
    /**
     * 活动编号查找活动
     */
    List<BizFlwActivitySyn> findByActivityNumber(String activityNumber);

    /**
     * 活动编号和活动状态查找活动
     */
    BizFlwActivitySyn findByActivityNumberAndActivityStatus(String activityNumber,int activityStatus);

    /**
     * id查找活动
     */
    BizFlwActivitySyn findById2(Integer id);


    /**
     * 查找兼职进行中的活动信息
     */
    @Query(nativeQuery = true, value = "SELECT * FROM biz_flw_activity_syn   WHERE id in(SELECT B.activity_id FROM biz_account_flw_activity B WHERE B.uid=?1 AND B.user_status=1)")
    List<BizFlwActivitySyn>  findInActivityWithUid(String uid);


    /**
     * 查找目前进行中的活动
     */
    @Query(nativeQuery = true,value = "SELECT * FROM biz_flw_activity_syn WHERE activity_status=1 and  NOW()>activity_start_time and  NOW()<activity_end_time")
    List<BizFlwActivitySyn> findAvailableActivity();

}
