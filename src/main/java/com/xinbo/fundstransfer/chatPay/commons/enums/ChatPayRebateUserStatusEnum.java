package com.xinbo.fundstransfer.chatPay.commons.enums;



import java.util.stream.Stream;

/**
 * 聊天室支付 返利网兼职 状态
 */
public enum ChatPayRebateUserStatusEnum {


    /**
     * 兼职在线状态
     * 1.兼职主动上线
     * 2.任务分配中(锁定)
     * 3.任务已分配，等待加入聊天室
     * 4.已加入聊天室
     * 5.等待付款
     * 6.已确认付款
     * 7.已确认收款(入款完毕)
     * 8.离开聊天室(下线，除非再次调用上线)
     * 9.被踢出()
     * 10.兼职下线(退出)
     */


        /**
         *  1.兼职主动上线
         */
        ON_LINE(1),

        /**
         *  2.任务分配中(锁定)
         */
        ACCOUNTING(2),

        /**
         *  3.任务已分配，等待加入聊天室
         */
        WATING_LOGIN_ROOM(3),

        /**
         *  4.已加入聊天室
         */
        LOGINED_ROOM(4),

        /**
         *  5.等待付款
         */
        WATING_PAY(5),

        /**
         *  6.已确认付款
         */
        PAY_FINISH(6),

        /**
         *  7.已确认收款(入款完毕)
         */
        CONFIM_RECEIVE(7),

        /**
         *  8.离开聊天室(下线，除非再次调用上线)
         */
        LOGOUT_ROOM(8),

        /**
         *  9.被踢出
         */
        KILL_LOGOUT_FOOM(9),


        /**
         *  10.兼职下线(退出)
         */
        OFF_LINE(10),



        UNKNOWENUM(000);
        Integer num = null;


        ChatPayRebateUserStatusEnum(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }
        public static ChatPayRebateUserStatusEnum getByNumber(int number) {
            return  Stream.of(ChatPayRebateUserStatusEnum.values()).filter(p -> p.num == number).findFirst().orElse(ChatPayRebateUserStatusEnum.UNKNOWENUM);
        }



}
