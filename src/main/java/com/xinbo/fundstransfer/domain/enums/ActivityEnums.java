package com.xinbo.fundstransfer.domain.enums;

import java.util.stream.Stream;

/**
 * 返利网活动状态
 */

public interface ActivityEnums {
     int unkowNumber =000 ;


    /**
     * 活动状态
     */
     enum ActivityStatus {
        /**
         * 正常
         */
        OK(1),

        /**
         * 停止/删除/取消
         */
        CANCEL(2),

        UnknowEnum(unkowNumber);

         Integer num = null;
        ActivityStatus(Integer num) {
            this.num = num;
        }
        public static ActivityStatus getByNumber(int number) {
            return  Stream.of(ActivityStatus.values()).filter(p -> p.num == number).findFirst().orElse(ActivityStatus.UnknowEnum);
        }
        public Integer getNum() {
             return num;
         }

         public void setNum(Integer num) {
             this.num = num;
         }
    }

    /**
     * 参加本活动是否可提现
     */
    enum ActivityAllowWithdrawal {
        /**
         * 不可提现
         */
        NO(0),

        /**
         * 可以提现
         */
        YES(1),
        UnknowEnum(unkowNumber);
        Integer num = null;
        ActivityAllowWithdrawal(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }
        public static ActivityAllowWithdrawal getByNumber(int number) {
            return  Stream.of(ActivityAllowWithdrawal.values()).filter(p -> p.num == number).findFirst().orElse(ActivityAllowWithdrawal.UnknowEnum);
        }
    }

    /**
     * 允许佣金当作额度使用（不允许0，允许1）
     */
    enum ActivityAllowUseCommissions {
        /**
         * 不允许
         */
        NO(0),

        /**
         * 允许
         */
        YES(1),
        UnknowEnum(unkowNumber);
        Integer num = null;
        ActivityAllowUseCommissions(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }

        public static ActivityAllowUseCommissions getByNumber(int number) {
            return  Stream.of(ActivityAllowUseCommissions.values()).filter(p -> p.num == number).findFirst().orElse(ActivityAllowUseCommissions.UnknowEnum);
        }
    }

    /**
     * 兼职参与活动状态：用户活动状态(1进行中，2完成任务结束，3时间结束，4兼职退出，5系统活动取消）
     */
    enum UserActivityStatus{
        UnknowEnum(unkowNumber),
        /**
         * 进行中/开始
         */
        InActivity(1),
        /**
         * 完成任务退出
         */
        FinishActivity(2),

        /**
         * 活动时间结束退出
         */
        ActivityTimeFinish(3),

        /**
         * 用户退出
         */
        UserQuit(4),
        /**
         * 系统活动取消
         */
        ActivityCancel(5);
        Integer num = null;
        UserActivityStatus(Integer num) {
            this.num = num;
        }
        public Integer getNum() {
            return num;
        }
        public void setNum(Integer num) {
            this.num = num;
        }

        public static UserActivityStatus getByNumber(int i){
            return  Stream.of(UserActivityStatus.values()).filter(p -> p.num == i).findFirst().orElse(UserActivityStatus.UnknowEnum);
        }


    }

    /**
     * 兼职状态-  0-启用 1-审核中 2-停用 3-冻结（可登陆不可领取）
     */
    enum RebateUserStatus{
        UnknowEnum(unkowNumber),
        /**
         * 启用
         */
        RUNNING(0),
        /**
         * 审核中
         */
        WaitForAudit(1),
        /**
         * 停用
         */
        STOP(2),
        /**
         * 冻结
         */
        FREEZE(3);

        Integer num = null;
        RebateUserStatus(Integer num) {
            this.num = num;
        }
        public Integer getNum() {
            return num;
        }
        public void setNum(Integer num) {
            this.num = num;
        }
        public static RebateUserStatus getByNumber(int number) {
            return  Stream.of(RebateUserStatus.values()).filter(p -> p.num == number).findFirst().orElse(RebateUserStatus.UnknowEnum);
        }
    }

    /**
     * more表，兼职是否有在参与返利网的活动(0无，1有)
     */
    enum AccountMoreActivityInStatus{
        UnknowEnum(unkowNumber),
        /**
         * 没有参加返利网活动
         */
        NO(0),
        /**
         * 有参加返利网活动
         */
        YES(1);

        Integer num = null;
        AccountMoreActivityInStatus(Integer num) {
            this.num = num;
        }
        public Integer getNum() {
            return num;
        }
        public void setNum(Integer num) {
            this.num = num;
        }
        public static AccountMoreActivityInStatus getByNumber(int number) {
            return  Stream.of(AccountMoreActivityInStatus.values()).filter(p -> p.num == number).findFirst().orElse(AccountMoreActivityInStatus.UnknowEnum);
        }
    }

    /**
     * 兼职退出活动，返回返利网用户活动结果，1完成任务，2未完成
     */
    enum QuitStatus{
        UnknowEnum(unkowNumber),
        /**
         * 完成活动任务
         */
        Finish(1),
        /**
         * 未完成活动任务
         */
        NotFinish(2);

        Integer num = null;
        QuitStatus(Integer num) {
            this.num = num;
        }
        public Integer getNum() {
            return num;
        }
        public void setNum(Integer num) {
            this.num = num;
        }
        public static QuitStatus getByNumber(int number) {
            return  Stream.of(QuitStatus.values()).filter(p -> p.num == number).findFirst().orElse(QuitStatus.UnknowEnum);
        }
    }


}
