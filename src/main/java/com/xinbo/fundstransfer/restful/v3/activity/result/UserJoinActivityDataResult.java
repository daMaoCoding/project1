package com.xinbo.fundstransfer.restful.v3.activity.result;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ************************
 *  flw接口-兼职退出活动接口返回结果
 * @author tony
 */
@Data
public class UserJoinActivityDataResult {
    private int quitStatus;  //1已完成任务，2未完成
    private BigDecimal margin;    //结算后现在额度(无需其他计算)
    private BigDecimal tmpMargin;   //活动规则的 赠送的临时额度
    private BigDecimal activityTotalMargin; //本次活动累计额外赠送的佣金
}
