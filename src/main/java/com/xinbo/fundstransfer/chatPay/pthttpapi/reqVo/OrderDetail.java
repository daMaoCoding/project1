package com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * ************************
 * 出入款调用平台，明细订单数据
 * @author wira
 */
@Data
public class OrderDetail {

    /**
     * fr : 付款人来源(1平台，2兼职)
     * user_name : 付款人账号(会员账号或兼职账号)
     * money : 付款金额
     * order_no : 单号(会员入款单或者兼职代付任务单号)
     * opt_time : 完成时间(毫秒时间戳)(收款人主动确认或者付款人自己上报流水确认)
     */

    /**
     * 付款人来源(1平台，2兼职)
     */
    private byte fr;

    /**
     * 付款人账号(会员账号或兼职账号)
     */
    private String user_name;

    /**
     * 付款金额
     */
    private BigDecimal money;

    /**
     * 单号(会员入款单或者兼职代付任务单号)
     */
    private String order_no;

    /**
     * 完成时间(毫秒时间戳)(收款人主动确认或者付款人自己上报流水确认)
     */
    private Long opt_time;


}
