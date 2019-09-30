package com.xinbo.fundstransfer.chatPay.commons.config;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * ************************
 * 聊天室支付 账号分配配置
 * 配置： UserProfileKey#CHAT_PAY_INMONEY_ASSIGN_PLANTUSER
 *        UserProfileKey#CHAT_PAY_INMONEY_ASSIGN_REBATEUSER
 *        UserProfileKey#CHAT_PAY_OUTMONEY_ASSIGN_PLANTUSER
 *        UserProfileKey#CHAT_PAY_OUTMONEY_ASSIGN_REBATEUSER
 *        UserProfileKey#CHAT_PAY_INMONEY_MATCHING_WITH_ZONE
 *        UserProfileKey#CHAT_PAY_INMONEY_MATCHING_WITH_LEVEL
 *        UserProfileKey#CHAT_PAY_OUTMONEY_MATCHING_WITH_ZONE
 *        UserProfileKey#CHAT_PAY_OUTMONEY_MATCHING_WITH_LEVEL
 * @author tony
 */
@Data
@AllArgsConstructor
public class AssignAccountConfig {

    /**
     * 入款是否分配给平台出款会员
     */
    boolean inmoneyAssignToPlantUser;

    /**
     * 入款是否分配给返利网兼职
     */
    boolean inmoneyAssignToRebateUser;


    /**
     * 出款是否分配给平台入款会员
     */
    boolean outmoneyAssignToPlantUser;

    /**
     * 出款是否分配给返利网兼职
     */
    boolean outmoneyAssignToRebateUser;

    /**
     * 入款分配-匹配区域(兼职和会员)
     */
    boolean inmoneyMatchingWithZone;

    /**
     * 入款分配-匹配层级(兼职和会员)
     */
    boolean inmoneyMatchingWithLevel;

    /**
     * 出款分配-匹配区域(兼职和会员)
     */
    boolean outmoneyMatchingWithZone;

    /**
     * 出款分配-匹配区域(兼职和会员)
     */
    boolean outmoneyMatchingWithLevel;

    /**
     * 没进入聊天室超时-全单取消
     */
    boolean notLoginRoomTimeout;


}
