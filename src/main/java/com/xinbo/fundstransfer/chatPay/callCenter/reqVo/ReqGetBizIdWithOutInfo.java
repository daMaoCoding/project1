package com.xinbo.fundstransfer.chatPay.callCenter.reqVo;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.chatPay.commons.config.AliOutConfig;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * ************************
 *  出入款请求客服系统，通过出款单信息获取业务id,请求参数对象
 * @author tony
 */
@Data
@Slf4j
@NoArgsConstructor
public class ReqGetBizIdWithOutInfo {


    /**
     * oid : 100
     * code : I20190823151249487943
     * money : 9999.2
     * uid : 999063803355139
     * inName : 刘
     * account : jjj2331313
     * qrcode : ddddddi
     * type : 0
     */

    /**
     * 盘口标识符
     */
    private int oid;

    /**
     * 出款单号
     */
    private String code;

    /**
     * 金额
     */
    private double money;

    /**
     * 用户uid
     */
    private String uid;


    /**
     * 用户名(会员账号/兼职账号)
     */
    private String userName;



    /**
     * 收款人的姓
     */
    private String inName;


    /**
     * 支付宝或微信账号
     */
    private String account;

    /**
     * 二维码
     */
    private String qrcode;

    /**
     * 来源方类型：0：出入款系统(会员方),1：兼职返利系统(兼职方) 2：客服
     */
    private int type;


    /**
     * 出款超时参数，单位秒
     * 此参数，客服系统每间隔n秒弹出提示框，出款会员可选操作(继续提款，离开)
     */
    private int timeOut;


    /**
     * 超时倒计时，单位秒
     * 此参数，客服系统弹出的提示框，倒计时n秒，倒计时结束，默认用户选择(离开)，会发送取消订单号
     */
    private int countDown;



    /**
     * @param bizOutwardRequest  出入款，出款单
     * @param handicap  出入款，盘口
     * @param type    类型，0：出入款系统(会员方),1：兼职返利系统(兼职方) 2：客服
     * @param aliOutConfig 出入款，支付宝出款配置
     */
    public  ReqGetBizIdWithOutInfo(BizOutwardRequest bizOutwardRequest, BizHandicap handicap, int type, AliOutConfig aliOutConfig){
        if( null!=bizOutwardRequest && handicap!=null && (type==0||type==1||type==2) && null!=aliOutConfig  ){
            this.oid = Integer.parseInt(handicap.getCode());  //盘口
            this.code = bizOutwardRequest.getOrderNo();      //订单
            this.money = bizOutwardRequest.getAmount().doubleValue();  //金额
            this.uid  = bizOutwardRequest.getMemberCode();  //会员id
            this.userName = bizOutwardRequest.getMember();   //会员账号
            this.inName = bizOutwardRequest.getToAccountOwner();//出款会员姓名
            this.account = bizOutwardRequest.getToAccount(); //出款会员 支付宝或微信账号
            this.qrcode = bizOutwardRequest.getChatPayQrContent() ;//出款会员 收款二维码
            this.type =  type;  //来源类型，0：出入款系统(会员方),1：兼职返利系统(兼职方) 2：客服
            this.timeOut = aliOutConfig.getAlertConfimEachSecondForAliOut();  //客服系统每间隔n秒弹出提示框，出款会员可选操作(继续提款，离开)
            this.countDown = aliOutConfig.getConfimCountDownSecondForAliOut(); //客服系统弹出的提示框，倒计时n秒，倒计时结束，默认用户选择(离开)，会发送取消订单号
        }else{
            log.error("[聊天室支付]-获取组装获取业务参数错误。[出款单信息]：{}, 盘口信息：{}， 类型：{}，系统配置：{}",  JSON.toJSONString(bizOutwardRequest),JSON.toJSONString(handicap),type,JSON.toJSONString(aliOutConfig));
            throw new RuntimeException("内部错误：创建房间出错");
        }
    }

}
