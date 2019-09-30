package com.xinbo.fundstransfer.chatPay.commons.enums;

import java.util.stream.Stream;

/**
 * 聊天室支付 兼职 上线 预接收 任务类型
 */
public enum ChatPayRebateUserJobTypeEnum {


    /**
     * 等待任务类型
     * 0.手动任务(入款+出款)
     * 1.手动任务(入款)
     * 2.自动任务(入款，需独立安装工具))
     */


    /**
     *  0.手动任务(入款+出款)
     */
    IN_OUT_MANUAL(0),

    /**
     *  1.手动任务(入款)
     */
    IN_MANUAL(1),

    /**
     *  2.自动任务(入款，需独立安装工具))
     */
    IN_AUTO(2),



    UNKNOWENUM(000);
    Integer num = null;


    ChatPayRebateUserJobTypeEnum(Integer num) {
        this.num = num;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
    public static ChatPayRebateUserJobTypeEnum getByNumber(int number) {
        return  Stream.of(ChatPayRebateUserJobTypeEnum.values()).filter(p -> p.num == number).findFirst().orElse(ChatPayRebateUserJobTypeEnum.UNKNOWENUM);
    }

}
