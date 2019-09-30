package com.xinbo.fundstransfer.chatPay.callCenter.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xinbo.fundstransfer.chatPay.commons.enums.ChatPayRebateUserJobTypeEnum;
import com.xinbo.fundstransfer.chatPay.commons.enums.ChatPayRebateUserStatusEnum;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ************************
 *  返利网 兼职 开始接任务(上线)/分配任务/分配任务下线/兼职退出/等状态缓存
 * @author tony
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatPayRebateUserRedisStatus {
    List<BizAccount> accounts;
    ChatPayRebateUserStatusEnum chatPayRebateUserStatus;
    ChatPayRebateUserJobTypeEnum chatPayRebateUserJobTypeEnum;
    Long timstamp;
}
