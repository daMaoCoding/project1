package com.xinbo.fundstransfer.chatPay.commons.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;


/**
 * ************************
 * Redis 增减键
 * @author tony
 */
public enum RedisKeyEnums {

    /**
     * 未知
     */
    UNKNOWENUM("000"),


    /**
     * 兼职当日入款总额-支付宝
     */
    CHATPAY_REBATE_USER_DAYLY_INMONEY_ZFB("CHATPAY_REBATE_USER_DAYLY_INMONEY_ZFB"),

    /**
     * 兼职当日出款总额-支付宝
     */
    CHATPAY_REBATE_USER_DAYLY_OUTMONEY_ZFB("CHATPAY_REBATE_USER_DAYLY_OUTMONEY_ZFB"),



    /**
     * 兼职当日入款总额-微信
     */
    CHATPAY_REBATE_USER_DAYLY_INMONEY_WX("CHATPAY_REBATE_USER_DAYLY_INMONEY_WX"),


    /**
     * 兼职当日出款总额-微信
     */
    CHATPAY_REBATE_USER_DAYLY_OUTMONEY_WX("CHATPAY_REBATE_USER_DAYLY_OUTMONEY_WX"),



    /**
     * 会员当日入款总额-支付宝
     */
    CHATPAY_MEMBER_DAYLY_INMONEY_ZFB("CHATPAY_MEMBER_DAYLY_INMONEY_ZFB"),

    /**
     * 会员当日出款总额-支付宝
     */
    CHATPAY_MEMBER_DAYLY_OUTMONEY_ZFB("CHATPAY_MEMBER_DAYLY_OUTMONEY_ZFB"),



    /**
     * 会员当日入款总额-微信
     */
    CHATPAY_MEMBER_DAYLY_INMONEY_WX("CHATPAY_MEMBER_DAYLY_INMONEY_WX"),


    /**
     * 会员当日出款总额-微信
     */
    CHATPAY_MEMBER_DAYLY_OUTMONEY_WX("CHATPAY_MEMBER_DAYLY_OUTMONEY_WX"),



    /**
     * 同步通知返利网-兼职-每笔结束交易，单台机器锁定执行通知，锁名
     */
    CHATPAY_SYN_REBATE_USER_ORDER("CHATPAY_SYN_REBATE_USER_ORDER"),


    /**
     * 区域层级房间类型
     */
    CHATPAY_ZONE_LEVEL_ROOM("CHATPAY_ZONE_LEVEL_ROOM"),







    ;
    String key = null;

    RedisKeyEnums(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }



    public static RedisKeyEnums getByKey(String key) {
       return Stream.of(RedisKeyEnums.values()).filter(p -> p.getKey().equals(key)).findFirst().orElse(RedisKeyEnums.UNKNOWENUM);
    }



    /**
     * 拼接key
     */
    public String concat(String abc) {
        if(StringUtils.isBlank(abc)) return getKey();
        return getKey().concat(":").concat(abc);
    }




}
