package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.activity.BizAccountFlwActivity;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 返利网用户活动
 */
public interface RebateUserActivityService {

    /**
     * uid 查找参与全部活动
     */
    List<BizAccountFlwActivity> findByUid(String userId);

    /**
     * 保存用户活动关联关系
     */
    BizAccountFlwActivity save(BizAccountFlwActivity bizAccountFlwActivity);

    /**
     * 基本参数验证
     */
    SimpleResponseData reqParamCheck(BizAccountFlwActivity bizAccountFlwActivity);

    /**
     * 逻辑验证
     */
    SimpleResponseData reqLogicCheck(BizAccountFlwActivity bizAccountFlwActivity);

    /**
     * 加入/退出活动
     */
    SimpleResponseData joinActive(BizAccountFlwActivity bizAccountFlwActivity) throws Exception;

    /**
     * 更新参加活动账号redis缓存
     */
    void updateReidsWithAccountsInActivity();

    /**
     * 返利网用户活动MQ消息处理
     */
    void onMessage(String topic,String msg);


    /**
     * 兼职是否可提现<br />
     * 判断兼职参与的所有活动，是否有规定不允许提现的活动
     */
    boolean allowWithdrawal(String uid);


    /**
     * 参与活动的兼职，优先分配任务
     * 获取全部参与活动的返利网兼职账号绑定的卡-id,
     * 注意：要验证卡是否在线等操作请调用者自行验证。
     */
    Set<String> getAllAccountsInActivity();


    /**
     *  获取兼职活动中的全部临时活动佣金
     */
     Map<String, BigDecimal>  getSumActivityAmountByUserActivityStatusIsIn();



}
