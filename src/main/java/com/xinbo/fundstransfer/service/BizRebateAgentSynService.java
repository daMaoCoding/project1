package com.xinbo.fundstransfer.service;


import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.agent.BizRebateAgentSyn;

public interface BizRebateAgentSynService {
    /**
     * 同步代理，请求参数检查
     */
    SimpleResponseData checkReqParam(BizRebateAgentSyn bizRebateAgentReq);

    /**
     * 同步代理信息/入库/更新
     */
    void syn(BizRebateAgentSyn bizRebateAgentReq);
}
