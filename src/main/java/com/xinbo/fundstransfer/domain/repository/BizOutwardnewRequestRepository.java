package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizUsemoneyRequestEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 描述:
 *
 * @author cobby
 * @create 2019-08-30 12:01
 */
public interface BizOutwardnewRequestRepository extends BaseRepository<BizUsemoneyRequestEntity, Long> {

    /**
     * 新公司出款 - 备注
     * @param remark  备注
     * @param id      当前ID
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set remark = :remark ,update_time= now() where  id = :id")
    int genRemark(@Param("remark") String remark ,@Param("id") Long id);

    /**
     * 新公司入款 - 财务审核
     * @param id                   当前ID
     * @param status               状态 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认对账
     * @param review               状态为3时 审核不通过原因
     * @param financeReviewerName  财务审核人Name
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set status = :status ,review=:review ," +
                "finance_reviewer_name=:financeReviewerName ,finance_reviewer_time= now(),update_time= now()  where  id = :id")
    int auditOutWardNew(@Param("id")Long id, @Param("status")Integer status,
                        @Param("review")String review,@Param("financeReviewerName")String financeReviewerName );

    /**
     * 新公司入款 - 下发审核
     * @param id                 当前ID
     * @param status             状态 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认对账
     * @param review             状态为4时 审核不通过原因
     * @param taskReviewerName   下发审核人Name
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set status = :status ,review=:review ," +
            "task_reviewer_name=:taskReviewerName ,task_reviewer_time= now(),update_time= now()  where  id = :id")
    int auditOutWardNewBeSent(@Param("id")Long id, @Param("status")Integer status,
                              @Param("review")String review,@Param("taskReviewerName")String taskReviewerName );

    /**
     * 新公司入款 - 下发锁定
     * @param ids         当前ids
     * @param lockStatus  锁定状态 0-未锁定,1-已锁定
     * @param uid         锁定人Name
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set lock_status = :lockStatus ," +
            "lock_id=:lockId ,lock_name=:lockName ,lock_time = now() ,update_time= now() where  id in (:ids)")
    int outWardBeSentLock(@Param("ids")Integer[] ids, @Param("lockStatus")Integer lockStatus, @Param("lockId")Integer lockId, @Param("lockName")String uid);

    /**
     * 新公司入款 - 下发解锁
     * @param ids  当前ID
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set lock_status = 0," +
            "lock_name = null, lock_id = null, lock_time = null, third_code = null ,update_time= now() where id in (:ids)")
    int outWardBeSentCancelLock(@Param("ids")Integer[] ids);

    /**
     * 新公司入款 - 绑定第三方下发
     * @param ids        当前IDS
     * @param thirdCode 第三方标识
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set third_code = :thirdCode ,update_time= now() ,fee =:fee  where id in (:ids)")
    int beSentThird(@Param("ids")Integer[] ids, @Param("thirdCode")String thirdCode, @Param("fee")BigDecimal fee);

    /**
     * 新公司入款 - 公司用款开始提现
     * @param id        当前ID
     * @param status    5-等待到账
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set status = :status ,update_time= now(),cash_time= now()  where id = :id")
    int takeMoneyThird(@Param("id")Long id, @Param("status")int status);

    /**
     * 新公司入款 - 第三方下发完成
     * @param id  当前ID
     * @param status   状态等待出款
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set status = :status ,update_time= now(),sent_time= now()  where id = :id")
    int thirdOutAccountFinish(@Param("id")Long id, @Param("status")int status);

    /**
     * 新公司入款 - 第三方下发提现存储bizIncomeRequest表ID
     * @param bizIncomeId bizIncomeRequest表ID
     * @param id   当前ID
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set biz_income_id = :bizIncomeId ,update_time= now()  where id = :id")
    void updataBizIncomeRequest(@Param("bizIncomeId") Long bizIncomeId, @Param("id") Long id);

    /**
     * 新公司入款 - 下发失败  --  回退到 1审核成功 2本人已锁定 3未绑定第三方状态
     * @param id 当前ID
     * @param status   状态 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认对账
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoney_request set status = :status," +
            " biz_income_id = null , review = :review ,update_time= now(),cash_time= null   where id = :id")
    int thirdOutAccountFailing(@Param("id") Long id, @Param("status") int status, @Param("review") String review);

    /**
     * 根据多个ids查询
     * @param ids
     * @return
     */
    @Query(nativeQuery = true, value = "select * from biz_usemoney_request t where t.id in (:ids)")
    List<BizUsemoneyRequestEntity> findAllIds(@Param("ids") Integer[] ids);

    /**
     * select
     *             count(if(del_status = 0, 1, null)) as total -- 总数
     *             , count(if(status = 1, 1, null)) as status1 -- 处理中
     *             , count(if(status = 2, 1, null)) as status2 -- 处理中超时
     *             , count(if(status = 3, 1, null)) as status3 -- 处理完成
     *             , count(if(status = 4, 1, null)) as status4 -- 超时完成
     *             , count(if(lock_id is not null and status in (1,2), 1, null)) as lockMnt -- 锁定
     *             , count(if(lock_id is null and status = 0, 1, null)) as unlockMnt -- 未锁定
     *         from t_owner_feed_back
     *         where type = #{type}
     */
    @Query(nativeQuery = true, value = "" +
            "select " +
            "count(if(status = 0, 1, null)) as status0 ,                               "+  // 财务待审核
            "count(if(status = 1, 1, null)) as status1 ,                               "+  // 下发待审核
            "count(if(status = 2 and lock_id is null, 1, null)) as status2 ,           "+  // 审核通过未锁定
            "count(if(status in(2, 5) and lock_id is not null, 1, null)) as status3 ,  "+  // 锁定未下发完成
            "count(if(lock_id = :lockId and status = 2, 1, null)) as status4 ,         "+  // 我已锁定未下发完成
            "count(if(lock_id = :lockId and status = 5, 1, null)) as status5           "+  // 我已锁定绑定点击第三方提现正在出款中
            " from biz_usemoney_request")
    Object statistics(@Param("lockId") Integer lockId);

}


