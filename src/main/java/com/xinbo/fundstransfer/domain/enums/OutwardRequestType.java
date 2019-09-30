package com.xinbo.fundstransfer.domain.enums;


import java.util.stream.Stream;

/**
 * 出款类型
 * 加入聊天室支付时新加类，之前没有类型，数据库中字段值null，预留-1银行卡
 */
public enum OutwardRequestType {

    /**
     *  0.银行卡
     */
    YHK(-1,"银行卡"),


   /**
     *  聊天室支付-微信出款
     */
    CHAT_PAY_WX(0,"微信"),


    /**
     *  聊天室支付-支付宝出款
     */
    CHAT_PAY_ZFB(1,"支付宝"),



    UNKNOWENUM(000,"未知");

    Integer num = null;
    String bankName = null;

    OutwardRequestType(Integer num,String bankName) {
        this.num = num;
        this.bankName =bankName;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
    public static OutwardRequestType getByNumber(int number) {
        return  Stream.of(OutwardRequestType.values()).filter(p -> p.num == number).findFirst().orElse(OutwardRequestType.UNKNOWENUM);
    }

}
