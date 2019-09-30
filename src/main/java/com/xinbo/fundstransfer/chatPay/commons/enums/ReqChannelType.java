package com.xinbo.fundstransfer.chatPay.commons.enums;

import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.enums.OutwardRequestType;

import java.util.stream.Stream;

/**
 * ************************
 *  聊天室通道类型-请求类型
 *  出款类型(0微信出款，1支付宝出款)
 * @author tony
 */
public enum  ReqChannelType {

    /**
     *  0.微信
     */
    WX(0,"微信"),

    /**
     *  1.支付宝
     */
    ZFB(1,"支付宝"),

    UNKNOWENUM(000,"未知");

    Integer num = null;
    String bankName = null;

    ReqChannelType(Integer num,String bankName) {
        this.num = num;
        this.bankName =bankName;
    }

    public String getBankName() {
        return bankName;
    }
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
    public static ReqChannelType getByNumber(int number) {
        return  Stream.of(ReqChannelType.values()).filter(p -> p.num == number).findFirst().orElse(ReqChannelType.UNKNOWENUM);
    }


    /**
     * 平台-出入款-->请求通道转入款类型
     */
    public static IncomeRequestType getIncomeType(int number){
        if(number==0) return IncomeRequestType.PlatFromWechat;
        if(number==1) return IncomeRequestType.PlatFromAli;
        throw new RuntimeException("聊天室请求通道类型-未配置："+number);
    }



    /**
     * 平台-出入款-->请求通道转出款类型
     */
    public static OutwardRequestType getOutwardType(int number){
        if(number==0) return OutwardRequestType.CHAT_PAY_WX;
        if(number==1) return OutwardRequestType.CHAT_PAY_ZFB;
        throw new RuntimeException("请求通道类型-未配置："+number);
    }


    /**
     * 平台-出入款-->请求通道转账号类型
     */
    public static AccountType getAccountType(int number){
        if(number==0) return AccountType.InAccountFlwWx;
        if(number==1) return AccountType.InAccountFlwZfb;
        throw new RuntimeException("聊天室请求通道类型-未配置："+number);
    }



    /**
     * 出入款-聊天室-->创建房间等，出款类型转聊天室通道类型
     */
    public static int getCallCenterReqChannelType(OutwardRequestType outwardRequestType){
        switch (outwardRequestType){
            case CHAT_PAY_ZFB:
                return 0;
            case CHAT_PAY_WX:
                return 1;
            default:
                throw new RuntimeException("出款单请求类型错误");
        }

    }





}
