package com.xinbo.fundstransfer.chatPay.commons.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

/**
 * redis锁Key
 */
public enum RedisLockKeyEnums {

    /**
     * 未知
     */
    UNKNOWENUM("000"),


    /**
     * 定时清理超时没入房间的token订单
     */
    CLEAN_NOT_LOGIN_ROOM("CLEAN_NOT_LOGIN_ROOM"),



    /**
     * 同步通知返利网-兼职-每笔结束交易，[单台机器锁定] 执行通知，锁名
     */
    CHATPAY_SYN_REBATE_USER_ORDER_LOCK("CHATPAY_SYN_REBATE_USER_ORDER_LOCK"),


    /**
     * 会员入款单找到房间并加入，锁房间，使用时候，应具体到锁哪个房间号
     */
    CHATPAY_ADD_MEBER_INCOME_ORDER_LOCK("CHATPAY_ADD_MEBER_INCOME_ORDER_LOCK"),







    ;
    String lockKey = null;
    RedisLockKeyEnums(String lockKey) {
        this.lockKey = lockKey;
    }
    public String getLockKey() {
        return lockKey;
    }
    public static RedisLockKeyEnums getByLockKey(String lockKey) {
        return Stream.of(RedisLockKeyEnums.values()).filter(p -> p.getLockKey().equals(lockKey)).findFirst().orElse(RedisLockKeyEnums.UNKNOWENUM);
    }



    /**
     * 拼接lockKey
     */
    public String concat(String abc) {
        if(StringUtils.isBlank(abc)) return getLockKey();
        return getLockKey().concat(":").concat(abc);
    }


}
