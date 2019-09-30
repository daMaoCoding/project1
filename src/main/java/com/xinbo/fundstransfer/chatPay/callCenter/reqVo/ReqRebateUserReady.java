package com.xinbo.fundstransfer.chatPay.callCenter.reqVo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * ************************
 * 客服系统送上兼职上线请求参数
 * @author tony
 */
@Data
public class ReqRebateUserReady {

    /**
     * 兼职UID
     */
    @NotNull(message = "UID不能为空")
    private String  uid;

    /**
     * 等待任务类型(0,手动任务(入款+出款) 1手动任务(入款) 2自动任务(入款，需独立安装工具))
     */
    @Min(value = 0,message = "任务类型错误")
    @Max(value = 2,message = "任务类型错误")
    @NotNull(message = "任务类型不能空")
    private int  jobType;


    /**
     * 请求时间戳
     */
    @NotNull(message = "时间戳不能为空")
    private Long  timestamp;


}
