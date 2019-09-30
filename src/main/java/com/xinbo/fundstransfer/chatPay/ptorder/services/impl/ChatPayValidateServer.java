package com.xinbo.fundstransfer.chatPay.ptorder.services.impl;

import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.chatPay.commons.config.AliOutConfig;
import com.xinbo.fundstransfer.chatPay.commons.enums.BizQrInfoEnums;
import com.xinbo.fundstransfer.chatPay.commons.enums.ReqChannelType;
import com.xinbo.fundstransfer.chatPay.commons.services.AbstractChatPayBaseServer;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.chatPay.ptorder.reqVo.ReqInMoney;
import com.xinbo.fundstransfer.chatPay.ptorder.reqVo.ReqOutMoney;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * ************************
 *
 * @author tony
 */
@Slf4j
@Service
public class ChatPayValidateServer extends AbstractChatPayBaseServer {


    /**
     * 根据配置，验证出款单信息  todo:微信出款配置，黑名单之外的验证
     */
    public void valReqOutMoney(ReqOutMoney reqOutMoney, AliOutConfig aliOutConfig, BizQrInfo bizQrInfo){
         switch (ReqChannelType.getByNumber(reqOutMoney.getChannelType())){
             case ZFB:
                 if(valReqOutMoney(reqOutMoney)  && valAliOutConfig(aliOutConfig)){
                     if(aliOutConfig.getNoUseBlackForAliOut()==1){ //黑名单验证
                         valReqOutMoneyMemberBlackList(bizQrInfo);
                     }
                 }
                 break;
             default:
               throw unSupportException;
         }

    }



    /**
     * 根据配置，验证入款单信息  todo:微信入款配置，黑名单之外的验证
     */
    public void valReqInMoney(ReqInMoney reqInMoney, AliIncomeConfig aliIncomeConfig) {
        switch (ReqChannelType.getByNumber(reqInMoney.getChannelType())){
            case ZFB:
                if(valReqInMoney(reqInMoney)  && valAliInConfig(aliIncomeConfig)){
                    if(aliIncomeConfig.getNoUseBlackForAliIn()==1){ //黑名单验证
                        valReqInMoneyMemberBlackList(reqInMoney);
                    }
                }
                break;
            default:
                throw unSupportException;
        }

    }



    /**
     * 过滤已验证通过的收款码
     */
    protected BizQrInfo valQrInfo(String qrId){
        BizQrInfo qrInfoByQrId = bizQrInfoServer.findQrInfoByQrId(qrId);
        if(qrInfoByQrId==null || qrInfoByQrId.getValQrStatus()!= BizQrInfoEnums.ValQrStatus.SUCCESS.getNum())
            throw new RuntimeException("无验证通过的收款二维码");
        return qrInfoByQrId;
    }


    /**
     * 验证平台请求出款单
     */
    private boolean valReqOutMoney(ReqOutMoney reqOutMoney){
        if(reqOutMoney !=null){
            return true;
        }else{
            throw new RuntimeException("出款请求空");
        }
    }



    /**
     * 验证平台请求入款单
     */
    private boolean valReqInMoney(ReqInMoney reqInMoney){
        if(reqInMoney !=null){
            return true;
        }else{
            throw new RuntimeException("入款请求空");
        }
    }


    /**
     * 验证出入款 支付宝出款配置
     */
    private boolean valAliOutConfig( AliOutConfig aliOutConfig){
        if(aliOutConfig !=null){
            return true;
        }else{
            throw new RuntimeException("支付宝出款配置空");
        }
    }


    /**
     * 验证出入款 支付宝入款配置
     */
    private boolean valAliInConfig(AliIncomeConfig aliIncomeConfig) {
        if(aliIncomeConfig !=null){
            return true;
        }else{
            throw new RuntimeException("支付宝入款配置空");
        }
    }





    /**
     * 验证请求出款构造后参数
     */
    public void valReqOutMoney(String remarkNum, BizHandicap handicap, BizLevel level, BizOutwardRequest bizOutwardRequest) {
        if(StringUtils.isBlank(remarkNum)) throw new RuntimeException("备注码生成错误");
        if(null==handicap) throw new RuntimeException("盘口空");
        if(null==level) throw new RuntimeException("层级空");
        if(null==bizOutwardRequest) throw new RuntimeException("构造出款单错误");
    }

    /**
     * 验证请求入款构造后参数
     */
    public void valReqInMoney(String remarkNum, BizHandicap handicap, BizLevel level, BizIncomeRequest bizIncomeRequest) {
        if(StringUtils.isBlank(remarkNum)) throw new RuntimeException("备注码生成错误");
        if(null==handicap) throw new RuntimeException("盘口空");
        if(null==level) throw new RuntimeException("层级空");
        if(null==bizIncomeRequest) throw new RuntimeException("构造入款单错误");
    }



    /**
     * 验证会员收款码是否黑名单
     */
    public void valReqOutMoneyMemberBlackList(BizQrInfo bizQrInfo) {
        boolean isSuspectMember = blackListService.isBlackList(bizQrInfo.getAccount(), bizQrInfo.getName());
        if(isSuspectMember) throw new RuntimeException("黑名单用户");
    }


    /**
     * 验证入款会员是否黑名单
     */
    private void valReqInMoneyMemberBlackList(ReqInMoney reqInMoney) {
        //入款随便入，无验证
    }





    /**
     * 验证聊天室返回信息
     */
    protected  void valCallCenterRoomInfoResult(String roomNum, String bizId, String[] tokenAndRoomUrl){
        if(StringUtils.isBlank(roomNum)) {
            log.error("[聊天室支付]-聊天室返回错误：{}","房间号空");
            throw new RuntimeException("聊天室错误，房间号空");
        }
        if(StringUtils.isBlank(bizId)) {
            log.error("[聊天室支付]-聊天室返回错误：{}","业务号空");
            throw new RuntimeException("聊天室错误，业务号空");
        }
        if(ArrayUtils.isEmpty(tokenAndRoomUrl) ||ArrayUtils.getLength(tokenAndRoomUrl)!=2 ||  StringUtils.isBlank(tokenAndRoomUrl[0]) || StringUtils.isBlank(tokenAndRoomUrl[1])) {
            log.error("[聊天室支付]-聊天室返回错误：{}","token/url域名空");
            throw new RuntimeException("聊天室错误，token或域名空");
        }
    }



}
