package com.xinbo.fundstransfer.chatPay.callCenter.reqVo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * ************************
 * 客服系统送上兼职退出请求参数
 * @author tony
 */
@Data
public class ReqRebateUserOut {

    /**
     * 兼职UID
     */
    @NotNull(message = "UID不能为空")
    private String  uid;

    /**
     * 兼职退出类型(1主动退出，2被动退出(被踢)，3断线)
     */
    @Min(value = 1,message = "兼职退出类型错误")
    @Max(value = 3,message = "兼职退出类型错误")
    @NotNull(message = "兼职退出类型错误")
    private int  outType;


    /**
     * 请求时间戳
     */
    @NotNull(message = "时间戳不能为空")
    private Long  timestamp;


}
