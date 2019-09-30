package com.xinbo.fundstransfer.chatPay.flwsynzfbwx.services;

import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo.ReqRebateUserWxZfbAccount;
import com.xinbo.fundstransfer.chatPay.ptqrval.reqVo.ReqPtQrVal;
import com.xinbo.fundstransfer.chatPay.valtools.reqVo.ReqBackValQrJob;
import com.xinbo.fundstransfer.chatPay.valtools.reqVo.ReqGetValQrJob;
import com.xinbo.fundstransfer.domain.entity.BizAccount;

import java.util.List;

/**
 * ************************
 * 二维码验证
 * @author tony
 */
public interface BizQrInfoServer {

    /**
     * 构建二维码验证信息-来自返利网
     */
    BizQrInfo convertAndCheck(ReqRebateUserWxZfbAccount reqRebateUserWxZfbAccount);

    /**
     * 保存二维码验证信息
     */
    BizQrInfo saveAndFlush(BizQrInfo bizQrInfo);

    /**
     * 构建二维码验证信息-来自平台
     */
    BizQrInfo convertAndCheck(ReqPtQrVal reqPtQrVal);

    /**
     * 获取要验证二维码任务
     */
    BizQrInfo findValQrJob();

    /**
     * 工具端接收任务
     * @return
     */
    BizQrInfo valQrJobAccept(ReqGetValQrJob reqGetValQrJob, BizQrInfo bizQrInfo);

    /**
     * 通过二维码id获取二维码信息(无缓存)
     */
    BizQrInfo findQrInfoByQrId(String qrId);

    /**
     * 更新工具验证结果
     * @return
     */
    void backValQrJobAccept(ReqBackValQrJob reqBackValQrJob, BizQrInfo bizQrInfo);

    /**
     * 通知平台和返利网二维码验证状态
     * @return
     */
    int notifyFlwAndPt(BizQrInfo bizQrInfo);

    /**
     * 更新 account & accountMore
     * 返利网同步支付宝/微信账号
     */
    BizAccount saveOrUpdateAccountAndAccountMore(ReqRebateUserWxZfbAccount reqRebateUserWxZfbAccount) throws Exception;


    /**
     * 二维码查询
     */
    List<BizQrInfo> findAllByUidAndQrStatusAndValQrStatusAndQrContentIn(String uid, int qrStatus, int valQrStatus, String[] qrContent);

}
