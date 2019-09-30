package com.xinbo.fundstransfer.chatPay.callCenter.reqVo;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ************************
 *  出入款请求客服系统，通过入款单信息获取业务id,请求参数对象
 * @author tony
 */
@Data
@Slf4j
@NoArgsConstructor
public class ReqGetBizIdWithIncomeInfo {


    /**
     * oid : 100
     * code : I20190823151249487943
     * money : 9999.2
     * uid : 999063803355139
     * remarkCode : ddddddi
     * type : 0
     */

    /**
     * 平台盘口标识
     */
    private int oid;

    /**
     * 入款订单号
     */
    private String code;

    /**
     * 金额
     */
    private double money;

    /**
     * 会员id
     */
    private String uid;

    /**
     * 备注验证码
     */
    private String remarkCode;


    /**
     * 用户名(会员账号/兼职账号)
     */
    private String userName;


    /**
     * 来源方类型：0：出入款系统(会员方),1：兼职返利系统(兼职方) 2：客服
     */
    private int type;


    /**
     * 入款超时参数，单位秒
     * 从入款人进入房间开始计算，第一次超时，用户可选择操作（继续付款，已确认付款，离开）
     */
    private int timeOut;

    /**
     * 第二次入款超时参数，单位秒
     * 从入款人进入房间开始计算，第二次超时，用户可选择操作(已确认付款，离开)
     */
    private int timeOut2;



    /*
     * 第三方此入款超时，单位秒
     * 在上面timeOut2的基础上+2分钟(客服系统写死)，第三次超时，用户可选择操作(我知道了)
     */



    /**
     *
     * @param bizIncomeRequest 出入款，入款单
     * @param handicap  出入款，盘口
     * @param type  类型，0：出入款系统(会员方),1：兼职返利系统(兼职方) 2：客服
     * @param aliIncomeConfig 出入款，支付宝入款配置
     */
    public ReqGetBizIdWithIncomeInfo(BizIncomeRequest bizIncomeRequest, BizHandicap handicap, int type, AliIncomeConfig aliIncomeConfig,String remarkCode) {
        if( null!=bizIncomeRequest && handicap!=null && (type==0||type==1||type==2) && null!=aliIncomeConfig  ){
            this.oid = Integer.parseInt(handicap.getCode());  //盘口
            this.code = bizIncomeRequest.getOrderNo();      //订单
            this.money = bizIncomeRequest.getAmount().doubleValue();  //金额
            this.uid  = bizIncomeRequest.getMemberCode();  //会员id
            this.remarkCode = remarkCode; // 备注码
            this.userName = bizIncomeRequest.getMemberUserName();   //会员账号
            this.type =  type;  //来源类型，0：出入款系统(会员方),1：兼职返利系统(兼职方) 2：客服
            this.timeOut = aliIncomeConfig.getIncomeTimeOutTotalMinute()*60;  //入款人不支付超时，第1次弹框提示(等待,已付，离开)，分转秒
            this.timeOut2 = aliIncomeConfig.getIncomeTimeOutTotalMinute()*60;  //入款人不支付超时，第2次弹框提示(已付，离开)，分钟
        }else{
            log.error("[聊天室支付]-获取组装获取业务参数错误。[入款单信息]：{}, 盘口信息：{}， 类型：{}，系统配置：{}",  JSON.toJSONString(bizIncomeRequest),JSON.toJSONString(handicap),type,JSON.toJSONString(aliIncomeConfig));
            throw new RuntimeException("内部错误：创建房间出错");
        }
    }



}
