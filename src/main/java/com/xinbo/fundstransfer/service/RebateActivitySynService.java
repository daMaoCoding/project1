package com.xinbo.fundstransfer.service;


import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;

import java.util.Date;
import java.util.List;

/**
 * ************************
 * 返利网活动同步
 * @author tony
 */
public interface RebateActivitySynService   {

    /**
     * 检查基本参数
     */
    SimpleResponseData reqParamCheck(BizFlwActivitySyn bizFlwActivitySyn);

    /**
     * 参数验证
     */
    SimpleResponseData reqLogicCheck(BizFlwActivitySyn bizFlwActivitySyn);

    /**
     * 同步活动
     */
    BizFlwActivitySyn synActive(BizFlwActivitySyn bizFlwActivitySyn, Date now);



    /**
     * 查找目前进行中的活动
     * 没缓存，不要调用太频
     */
    List<BizFlwActivitySyn> findAvailableActivity();

}
