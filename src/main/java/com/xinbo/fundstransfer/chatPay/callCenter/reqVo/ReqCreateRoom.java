package com.xinbo.fundstransfer.chatPay.callCenter.reqVo;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.commons.enums.ReqChannelType;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


/**
 * ************************
 * 出入款调用客服系统创建房间 ，请求参数
 * @author tony
 */
@Data
@Slf4j
@NoArgsConstructor
public class ReqCreateRoom {

    /**
     * oid : 111111
     * code : 2222O85
     * accountType : 1
     */


    /**
     * 平台盘口号，必传
     */
    private int oid;


    /**
     * （必传）出、入款单号
     */
    private String code;


    /**
     * （必传）账号类型：0：支付宝,1：微信
     */
    private int accountType;




    public  ReqCreateRoom(BizOutwardRequest bizOutwardRequest,BizHandicap handicap ){
        if(null!=bizOutwardRequest && null!=handicap && StringUtils.isNotBlank(bizOutwardRequest.getOrderNo()) && bizOutwardRequest.getOutwardRequestType()!=null  ){
            this.oid = Integer.parseInt(handicap.getCode());
            this.code = bizOutwardRequest.getOrderNo();
            this.accountType = ReqChannelType.getCallCenterReqChannelType(bizOutwardRequest.getOutwardRequestType());
        }else{
            log.error("[聊天室支付]-[CRK]-构建房间出错。出款单信息：{}, 盘口:{}", JSON.toJSONString(bizOutwardRequest),JSON.toJSONString(handicap));
            throw new RuntimeException("内部错误：创建房间出错");
        }
    }






}
