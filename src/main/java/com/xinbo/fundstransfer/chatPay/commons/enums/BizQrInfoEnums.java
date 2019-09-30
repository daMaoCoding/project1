package com.xinbo.fundstransfer.chatPay.commons.enums;


import java.util.stream.Stream;

/**
 * ************************
 * 二维码验证 枚举
 * @author tony
 */
public interface BizQrInfoEnums {


    /**
     * 聊天室支付 二维码来源(0返利网，1平台)
     * 兼职/会员
     */
    enum QrFrom {
        /**
         *  0返利网
         */
        FWL(0),

        /**
         * 1平台
         */
        PT(1),
        UNKNOWENUM(000);
        Integer num = null;
        QrFrom(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }
        public static  QrFrom getByNumber(int number) {
            return  Stream.of(QrFrom.values()).filter(p -> p.num == number).findFirst().orElse(QrFrom.UNKNOWENUM);
        }
    }





    /**
     * 聊天室支付 二维码类型(0微信收款码，1支付宝收款码)
     * 兼职/会员
     */
    enum QrType {
        /**
         * 0微信收款码
         */
        WX(0),

        /**
         * 1支付宝收款码
         */
        ZFB(1),
        UNKNOWENUM(000);
        Integer num = null;
        QrType(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }
        public static  QrType getByNumber(int number) {
            return  Stream.of(QrType.values()).filter(p -> p.num == number).findFirst().orElse(QrType.UNKNOWENUM);
        }
    }




    /**
     * 聊天室支付 二维码状态(1正常，0停用 。 默认1)
     * 兼职/会员
     */
    enum QrStatus {
        /**
         *  1正常
         */
        RUNNING(1),

        /**
         * 0停用
         */
        STOP(0),
        UNKNOWENUM(000);
        Integer num = null;
        QrStatus(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }
        public static  QrStatus getByNumber(int number) {
            return  Stream.of(QrStatus.values()).filter(p -> p.num == number).findFirst().orElse(QrStatus.UNKNOWENUM);
        }
    }






    /**
     * 聊天室支付 二维码验证状态(0新生成，1任务取走验证中，2验证成功 ，3验证失败)
     * 兼职/会员
     */
    enum ValQrStatus {
        /**
         * 0新生成
         */
        NEW(0),

        /**
         * 1任务取走验证中
         */
        VALING(1),


        /**
         * 2验证成功
         */
        SUCCESS(2),


        /**
         * 3验证失败
         */
        ERROR(3),


        UNKNOWENUM(000);
        Integer num = null;
        ValQrStatus(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }
        public static ValQrStatus getByNumber(int number) {
            return  Stream.of(ValQrStatus.values()).filter(p -> p.num == number).findFirst().orElse(ValQrStatus.UNKNOWENUM);
        }
    }




    /**
     * 聊天室支付 二维码验证结果通知(返利网/平台)状态(1通知验证结果成功，0通知验证结果失败)
     * 兼职/会员
     */
    enum ValQrNotifStatus {
        /**
         * 0通知验证结果失败
         */
        ERROR(0),

        /**
         * 1通知验证结果成功
         */
        SUCCESS(1),

        UNKNOWENUM(000);
        Integer num = null;
        ValQrNotifStatus(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }
        public static ValQrNotifStatus getByNumber(int number) {
            return  Stream.of(ValQrNotifStatus.values()).filter(p -> p.num == number).findFirst().orElse(ValQrNotifStatus.UNKNOWENUM);
        }
    }















}
